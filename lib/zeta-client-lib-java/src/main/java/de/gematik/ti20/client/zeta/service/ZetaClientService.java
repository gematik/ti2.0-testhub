/*
 *
 * Copyright 2025 gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package de.gematik.ti20.client.zeta.service;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardCertInfoSmcb;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.CardType;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.card.SignOptions.HashAlgorithm;
import de.gematik.ti20.client.card.card.SignOptions.SignatureType;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.client.zeta.auth.AuthContext;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.exception.ZetaHttpResponseException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.request.ZetaWsRequest;
import de.gematik.ti20.client.zeta.response.ZetaHttpResponse;
import de.gematik.ti20.client.zeta.websocket.ZetaWsEventHandler;
import de.gematik.ti20.client.zeta.websocket.ZetaWsSession;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;

@Getter
@Slf4j
public class ZetaClientService {

  private final ZetaClientConfig zetaClientConfig;
  private final PepProxyService pepProxyService;
  private final PdpAuthzService pdpAuthzService;
  private final CardTerminalService cardTerminalService;

  public ZetaClientService(ZetaClientConfig zetaClientConfig) {
    this.zetaClientConfig = zetaClientConfig;
    this.cardTerminalService =
        new CardTerminalService(zetaClientConfig.getTerminalConnectionConfigs());
    this.pepProxyService = new PepProxyService(this);
    this.pdpAuthzService = new PdpAuthzService(this);
  }

  /**
   * Returns the list of currently attached cards that are relevant for ZETA processes (SMC-B)
   *
   * @return
   * @throws CardTerminalException
   */
  public synchronized List<? extends AttachedCard> getAttachedCards() throws CardTerminalException {
    return cardTerminalService.getAttachedCards().stream().filter(AttachedCard::isSmcb).toList();
  }

  /**
   * Sends the passed request to the PEP Proxy and returns the response synchronously. Automatically
   * start the authorization process if the response is unauthorized on set authorizeIfNotYet to
   * true. When the authorization process is successful, the request is resent.
   *
   * @param request
   * @param authorizeIfNotYet
   * @return
   */
  public ZetaHttpResponse sendToPepProxy(
      final ZetaHttpRequest request, final boolean authorizeIfNotYet) {
    try {
      return this.getPepProxyService().send(request);
    } catch (final ZetaHttpResponseException re) {
      if (re.getCode() == 502) {
        throw re;
      }
      if (authorizeIfNotYet && re.isUnauthorized()) {
        return authorizeAndResend(request);
      }
      log.error("Server returned error while sending request {}", re.getRequest().getTraceId(), re);
      return new ZetaHttpResponse(re.getCode(), re.getMessage());
    }
  }

  /**
   * Authorizes the request and resends it to the PEP Proxy.
   *
   * @param request
   * @return
   */
  public ZetaHttpResponse authorizeAndResend(final ZetaHttpRequest request) {
    final AuthContext ac = new AuthContext(request);
    final Integer smcbSlotId =
        request.getHeader("smcbSlotId") != null
            ? Integer.parseInt(request.getHeader("smcbSlotId"))
            : null;
    try {
      authorize(ac, smcbSlotId);
    } catch (final Exception e) {
      log.error("Error while authorizing", e);
      return new ZetaHttpResponse(HttpURLConnection.HTTP_FORBIDDEN, e);
    }

    log.debug(
        "Request authorized successfully, resending to PEP Proxy: {}", ac.getRequestAuthorized());

    return sendToPepProxy(ac.getRequestAuthorized(), false);
  }

  /**
   * Connects to the PEP Proxy via websocket and handles the communication with the passed
   * eventHandler asynchronously. Automatically start the authorization process if the response is
   * unauthorized on set authorizeIfNotYet to true. When the authorization process is successful,
   * the websocket connection is reestablished.
   *
   * @param eventHandler
   * @param authorizeIfNotYet
   */
  public void connectToPepProxy(
      ZetaWsRequest request, ZetaWsEventHandler eventHandler, boolean authorizeIfNotYet) {

    if (!authorizeIfNotYet) {
      this.getPepProxyService().connect(request, eventHandler);
      return;
    }

    final CompletableFuture<ZetaWsSession> resultFuture = new CompletableFuture<>();
    var eh =
        new ZetaWsEventHandler() {

          public void onConnected(ZetaWsSession ws) {
            eventHandler.onConnected(ws);
          }

          public void onDisconnected(ZetaWsSession ws) {
            eventHandler.onDisconnected(ws);
            resultFuture.complete(ws);
          }

          public void onException(ZetaWsSession ws, ZetaHttpException e) {
            if (e instanceof ZetaHttpResponseException) {
              if (((ZetaHttpResponseException) e).isUnauthorized()) {
                resultFuture.completeExceptionally(e);
                return;
              }
            }
            eventHandler.onException(ws, e);
            resultFuture.completeExceptionally(e);
          }

          public void onMessage(ZetaWsSession ws) {
            eventHandler.onMessage(ws);
            // resultFuture.complete(ws);
          }
        };

    this.getPepProxyService().connect(request, eh);
    try {
      /*var wsm =*/ resultFuture.get(5, TimeUnit.MINUTES);
    } catch (final ExecutionException e) {
      if (e.getCause() instanceof ZetaHttpResponseException
          && ((ZetaHttpResponseException) e.getCause()).isUnauthorized()) {
        authorizeAndReconnect(request, eventHandler);
        return;
      }

      log.error("CompletableFuture resolution exception on {}", request.getTraceId(), e);
    } catch (final Exception e) {
      log.error("CompletableFuture resolution exception on {}", request.getTraceId(), e);

      eventHandler.onException(eventHandler.getWsSession(), new ZetaHttpException(e, request));
    }
  }

  /**
   * Authorizes the request and reconnects to the PEP Proxy via websocket.
   *
   * @param eventHandler
   */
  public void authorizeAndReconnect(
      final ZetaHttpRequest request, final ZetaWsEventHandler eventHandler)
      throws ZetaHttpException {
    final AuthContext ac = new AuthContext(request);
    final Integer smcbSlotId =
        request.getHeader("smcbSlotId") != null
            ? Integer.parseInt(request.getHeader("smcbSlotId"))
            : null;
    try {
      authorize(ac, smcbSlotId);
    } catch (final ZetaHttpException e) {
      throw e;
    } catch (final Exception e) {
      log.error("Error while authorizing", e);
      throw new ZetaHttpException(e, request);
    }

    connectToPepProxy((ZetaWsRequest) ac.getRequestAuthorized(), eventHandler, false);
  }

  private void authorize(final AuthContext ac, final Integer smcbSlotId) throws Exception {
    log.debug("Authorizing request {}", ac.getRequest().getTraceId());

    discoveryAndConfiguration(ac);

    dynamicClientRegistration(ac);

    tokenExchange(ac, smcbSlotId);
  }

  private void discoveryAndConfiguration(final AuthContext ac) throws Exception {
    requestWellKnownFromPep(ac);
    requestWellKnownFromPdp(ac);
  }

  private void dynamicClientRegistration(final AuthContext ac) throws Exception {
    // TODO: das müsste komplett vom AN implementiert werden inkl Simulator für ZETA Attestation
    // Service
  }

  private void tokenExchange(final AuthContext ac, final Integer smcbSlotId) throws Exception {
    // TODO: das müsste vom AN vollständig implementiert werden wie im neuen Ablauf beschrieben
    createSmcbAccessToken(ac, cardTerminalService, smcbSlotId);

    createDpopProofToken(ac);

    createClientAssertionToken(ac);

    requestAccessToken(ac);
  }

  private void requestWellKnownFromPep(AuthContext ac) {
    this.getPepProxyService().requestWellKnown(ac);
  }

  private void requestWellKnownFromPdp(AuthContext ac) {
    // TODO: implement it additionally
    // this.getPdpAuthzService().requestWellKnown(ac);
  }

  private String requestNonce(AuthContext ac) {
    // TODO
    // this.zetaClientService.getPdpAuthzService().requestNonce(ac);
    return null;
  }

  private void createSmcbAccessToken(
      final AuthContext ac, final CardTerminalService cardTerminalService, final Integer smcbSlotId)
      throws Exception {
    // TODO: diese Abläufe sind noch nicht vollständig beschrieben, es gibt hier noch Unklarheiten,
    //  die zuerst aufgelöst werden müssen, bevor das implementiert werden kann.
    // DIESE IMPLEMENTIERUNG IST TEMPORÄR UND MUSS NOCH ÜBERARBEITET WERDEN!

    final AttachedCard smcb =
        cardTerminalService.getAttachedCards().stream()
            .filter(
                card ->
                    card.getType() == CardType.SMC_B
                        && ((SimulatorAttachedCard) card).getSlotId().equals(smcbSlotId))
            .findFirst()
            .orElse(null);

    if (smcb == null) {
      throw new ZetaHttpResponseException(
          403, "No attached SMC-B card found in the connected card terminals", ac.getRequest());
    }

    final CardConnection smcbConnection = smcb.getTerminal().connect(smcb);

    CardCertInfoSmcb certInfo = (CardCertInfoSmcb) smcbConnection.getCertInfo();
    JwtClaims smcbClaims = getSmcbAccessTokenClaims(ac, certInfo);
    String base64Claims =
        Base64.getUrlEncoder().withoutPadding().encodeToString(smcbClaims.toJson().getBytes());

    String headerJson = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
    String base64Header =
        Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes());

    String jwtUnsigned = base64Header + "." + base64Claims;

    // Hash the unsigned JWT (SHA-256)
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(jwtUnsigned.getBytes());

    byte[] signature =
        smcbConnection.sign(hash, new SignOptions(HashAlgorithm.SHA256, SignatureType.ECDSA));

    // smcbConnection.disconnect();

    ac.setSmcbAccessToken(
        jwtUnsigned + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature));
  }

  private void createDpopProofToken(AuthContext ac) throws Exception {
    // TODO
  }

  private void createClientAssertionToken(AuthContext ac) throws Exception {
    // TODO
  }

  private void requestAccessToken(AuthContext ac) {
    this.getPdpAuthzService().requestAccessToken(ac);
  }

  // TODO: aktuell nicht vollständig klar, wie das Token genau aussehen soll
  private JwtClaims getSmcbAccessTokenClaims(AuthContext ac, CardCertInfoSmcb certInfo)
      throws Exception {
    URL urlAS = new URL(ac.getWellKnownFromPep().getAuthorization_endpoint());
    URL urlToken =
        new URL(
            urlAS.getProtocol(),
            urlAS.getHost(),
            urlAS.getPort(),
            getZetaClientConfig().getPathTokenAS());

    // String urlToken = "TEMPORARY_URL_FOR_TOKEN"; // TODO: Replace with actual URL logic

    JwtClaims claims = new JwtClaims();
    claims.setIssuer(certInfo.getTelematikId()); // "iss": The client ID
    claims.setSubject(certInfo.getTelematikId()); // "sub": The institution or subject identity
    claims.setAudience(urlToken.toString()); // "aud": The Authorization Server
    claims.setExpirationTimeMinutesInTheFuture(5); // Token expires in 30 minutes
    claims.setGeneratedJwtId(); // Unique identifier for the token
    claims.setNotBeforeMinutesInThePast(2); // Token is valid 2 minutes in the past
    claims.setIssuedAtToNow(); // Token issue time

    // Add custom claims
    claims.setClaim(
        "professionOid", certInfo.getProfessionOid()); // Profession OID from the certificate

    return claims;
  }

  //  public List<CardTerminalV1> getTerminals() {
  //    return cardTerminalService.getTerminals();
  //  }
  //
  //  public void addTerminal(CardTerminalConfig terminalConfig) {
  //    cardTerminalService.addTerminal(terminalConfig);
  //  }
  //
  //  public void setTerminal(CardTerminalConfig terminalConfig) {
  //    cardTerminalService.addTerminal(terminalConfig);
  //  }

}
