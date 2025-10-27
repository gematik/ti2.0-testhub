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
package de.gematik.ti20.simsvc.client.service;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.rest.fd.MediaType;
import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.message.TokenMessage;
import de.gematik.ti20.client.popp.service.PoppClientService;
import de.gematik.ti20.client.popp.service.PoppTokenSession;
import de.gematik.ti20.client.popp.service.PoppTokenSessionEventHandler;
import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.exception.ZetaHttpResponseException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.response.ZetaHttpResponse;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import de.gematik.ti20.simsvc.client.config.VsdmConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.vsdm.fhir.builder.VsdmBundleBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPatientBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class VsdmClientService implements PoppTokenSessionEventHandler {

  public static final String HEADER_VSDM_PZ = "VSDM-Pz";
  public static final String HEADER_ETAG = "etag";

  private final VsdmConfig vsdmConfig;
  private final PoppClientService poppClientService;
  private final ZetaClientService zetaClientService;

  private final PoppTokenRepository poppTokenRepository;
  private final VsdmDataRepository vsdmDataRepository;

  private List<CardTerminalConnectionConfig> terminalConnectionConfigs;

  private final FhirService fhirService;

  private Map<String, CompletableFuture<TokenMessage>> tokenFutures = new ConcurrentHashMap<>();

  public VsdmClientService(
      final VsdmConfig vsdmConfig,
      final PoppClientService poppClientService,
      final FhirService fhirService,
      final PoppTokenRepository poppTokenRepository,
      final VsdmDataRepository vsdmDataRepository) {
    this.vsdmConfig = vsdmConfig;

    this.poppClientService = poppClientService;
    this.zetaClientService = poppClientService.getZetaClientService();
    this.fhirService = fhirService;

    this.poppTokenRepository = poppTokenRepository;
    this.vsdmDataRepository = vsdmDataRepository;

    this.terminalConnectionConfigs = new ArrayList<>();
  }

  public ResponseEntity<String> read(
      final String terminalId,
      final Integer egkSlotId,
      final Integer smcbSlotId,
      final boolean isFhirXml,
      final boolean forceUpdate,
      final String poppTokenInjected,
      final String ifNoneMatch) {

    final AttachedCard attachedCard = getAttachedCard(terminalId, egkSlotId);

    final String poppToken =
        poppTokenInjected != null
            ? poppTokenInjected
            : requestPoppToken(terminalId, egkSlotId, smcbSlotId, attachedCard, forceUpdate);
    log.debug("Received PoPP token: {}", poppToken);

    final ResponseEntity<String> vsd =
        requestVsd(
            terminalId,
            egkSlotId,
            smcbSlotId,
            attachedCard,
            poppToken,
            ifNoneMatch,
            isFhirXml,
            forceUpdate);
    log.debug("Received VSD: {}", vsd);

    return vsd;
  }

  public AttachedCard getAttachedCard(final String terminalId, final Integer slotId) {
    log.debug("Getting attached card for terminal ID: {}, slot ID: {}", terminalId, slotId);

    List<? extends AttachedCard> cards = null;

    try {
      cards = poppClientService.getAttachedCards();
    } catch (final Exception e) {
      log.error("Error getting attached EGK cards from terminal", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }

    final AttachedCard attachedCard =
        cards.stream()
            .filter(card -> ((SimulatorAttachedCard) card).getSlotId().equals(slotId))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No card found in slot " + slotId));

    log.debug("Using card with ID: {}", attachedCard.getId());

    return attachedCard;
  }

  protected String requestPoppToken(
      final String terminalId,
      final Integer egkSlotId,
      final Integer smcbSlotId,
      final AttachedCard attachedCard,
      final boolean forceUpdate) {
    log.debug("Requesting PoPP token for attached card: {}", attachedCard.getId());

    final String poppTokenFromRepository =
        forceUpdate ? null : poppTokenRepository.get(terminalId, egkSlotId, attachedCard.getId());

    if (poppTokenFromRepository != null) {
      log.debug("PoPP token found in repository: {}", poppTokenFromRepository);
      return poppTokenFromRepository;
    }

    final CompletableFuture<TokenMessage> poppTokenFuture = new CompletableFuture<>();
    tokenFutures.put(attachedCard.getId(), poppTokenFuture);

    try {
      poppClientService.startPoppTokenSession(attachedCard, smcbSlotId, this);
    } catch (final PoppClientException e) {
      log.error("Error starting PoppTokenSession with card {}", attachedCard.getId(), e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }
    log.debug("PoppTokenSession started for card {}", attachedCard.getId());

    try {
      final TokenMessage result = poppTokenFuture.get(20, TimeUnit.SECONDS);
      final String poppTokenFromService = result.getToken();

      log.debug("Received PoPP token from service: {}", poppTokenFromService);
      poppTokenRepository.put(terminalId, egkSlotId, attachedCard.getId(), poppTokenFromService);

      return poppTokenFromService;
    } catch (final TimeoutException e) {
      log.error(
          "Timeout error on waiting for completing of PoppTokenSession with card {}",
          attachedCard.getId());
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    } catch (final Exception e) {
      log.error(
          "Error on waiting for completing of PoppTokenSession with card {} ",
          attachedCard.getId(),
          e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }
  }

  protected ResponseEntity<String> requestVsd(
      final String terminal,
      final Integer egkSlotId,
      final Integer smcbSlotId,
      final AttachedCard attachedCard,
      final String poppToken,
      final String ifNoneMatch,
      final boolean isFhirXml,
      final boolean forceUpdate) {

    final VsdmCachedValue vsdmCachedValue =
        forceUpdate ? null : vsdmDataRepository.get(terminal, egkSlotId, attachedCard.getId());
    if (vsdmCachedValue != null) {
      return ResponseEntity.status(HttpStatus.OK)
          .header(HEADER_VSDM_PZ, vsdmCachedValue.getPruefziffer())
          .header(HEADER_ETAG, vsdmCachedValue.getEtag())
          .body(vsdmCachedValue.getVsdmData());
    }

    final String vsdmServerUrl = vsdmConfig.getUrl();

    final ZetaHttpRequest zetaHttpRequest =
        new ZetaHttpRequest(vsdmServerUrl + "/vsdservice/v1/vsdmbundle");

    zetaHttpRequest.setHeader("PoPP", poppToken);
    zetaHttpRequest.setHeader(
        "Accept", (isFhirXml) ? "application/fhir+xml" : "application/fhir+json");
    zetaHttpRequest.setHeader("smcbSlotId", String.valueOf(smcbSlotId));
    if (ifNoneMatch != null) {
      log.debug("Setting If-None-Match header: {}", ifNoneMatch);
      zetaHttpRequest.setHeader("If-None-Match", ifNoneMatch);
    } else {
      log.debug("Setting If-None-Match header: empty");
    }

    try {
      // this response can be XML or JSON, but we return only JSON
      final ZetaHttpResponse responseFromServer =
          zetaClientService.sendToPepProxy(zetaHttpRequest, true);

      log.debug(
          "Received response from VSD server: {}; {}",
          responseFromServer.getStatusCode(),
          responseFromServer.getBody().orElse("empty"));

      final String responseToCaller = encodeVsdmBundle(isFhirXml, responseFromServer);
      if (responseFromServer.getStatusCode() == HttpURLConnection.HTTP_OK) {
        if (responseToCaller == null) {
          throw new ResponseStatusException(
              HttpStatus.INTERNAL_SERVER_ERROR, "Could not parse valid FHIR response");
        }
        final HttpHeaders responseHeaders = copyApplicableHeaders(responseFromServer);
        responseHeaders.put("Content-Type", List.of(MediaType.FHIR_JSON.asString()));

        vsdmDataRepository.put(
            terminal,
            egkSlotId,
            attachedCard.getId(),
            new VsdmCachedValue(
                responseHeaders.getETag(),
                responseHeaders.getFirst(HEADER_VSDM_PZ),
                responseToCaller));

        return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(responseToCaller);
      } else {
        return ResponseEntity.status(responseFromServer.getStatusCode())
            .headers(copyApplicableHeaders(responseFromServer))
            .body(responseToCaller);
      }
    } catch (final ZetaHttpResponseException e) {
      return ResponseEntity.status(e.getCode()).body(e.getMessage());
    } catch (final ZetaHttpException e) {
      log.error("Error while connecting to VSDM server: {}", e.getMessage(), e);

      try {
        final String responseToCaller = loadTruncatedDataFromCard(attachedCard);
        if (responseToCaller == null) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(responseToCaller);
      } catch (final CardTerminalException cardEx) {
        log.error("Error while loading truncated data from card: {}", cardEx.getMessage(), cardEx);
        throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
      }
    } catch (final Exception e) {
      log.error("Error on requesting VsdBundle with token", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }
  }

  private String encodeVsdmBundle(
      final boolean isFhirXml, final ZetaHttpResponse responseFromServer) {
    final String responseFromServerBody = responseFromServer.getBody().orElse(null);
    if (responseFromServerBody != null && !responseFromServerBody.isEmpty()) {
      final Resource resource =
          fhirService.parseString(responseFromServerBody, isFhirXml ? "xml" : "json");

      return fhirService.encodeResponse(resource, EncodingType.JSON);
    }

    return null;
  }

  public String loadTruncatedDataFromCard(final AttachedCard attachedCard)
      throws CardTerminalException {
    final EgkInfo egkInfo = poppClientService.getEgkInfo(attachedCard);

    if (!egkInfo.getValid()) {
      return null;
    }

    // send 401, falls nicht valid
    final VsdmBundle truncatedDataBundle =
        VsdmBundleBuilder.create()
            .addEntry(
                VsdmPatientBuilder.create()
                    .withKvnr(egkInfo.getKvnr())
                    .withNames(egkInfo.getLastName(), egkInfo.getFirstName())
                    .build())
            .build();

    return fhirService.encodeResponse(truncatedDataBundle, EncodingType.JSON);
  }

  private HttpHeaders copyApplicableHeaders(final ZetaHttpResponse responseFromServer) {
    final HttpHeaders responseHeaders = new HttpHeaders();
    responseFromServer
        .getHeaders()
        .forEach(
            (key, values) -> {
              if (key.equalsIgnoreCase(HEADER_VSDM_PZ)
                  || key.equalsIgnoreCase(HEADER_ETAG)
                  || key.equalsIgnoreCase("Content-Type")
                  || key.equalsIgnoreCase("Content-Length")) {
                responseHeaders.put(key, values);
              }
            });

    return responseHeaders;
  }

  public List<CardTerminalConnectionConfig> getTerminalConnectionConfigs() {
    return terminalConnectionConfigs;
  }

  public void setTerminalConnectionConfigs(final List<CardTerminalConnectionConfig> configs) {
    log.debug("Setting terminal connection configs: ", terminalConnectionConfigs);

    terminalConnectionConfigs = configs;

    zetaClientService.getZetaClientConfig().setTerminalConnectionConfigs(configs);
    poppClientService.getPoppClientConfig().setTerminalConnectionConfigs(configs);
  }

  @Override
  public void onConnectedToTerminalSlot(final PoppTokenSession pts) {
    log.debug("onConnectedToTerminalSlot: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onDisconnectedFromTerminalSlot(final PoppTokenSession pts) {
    log.debug("onDisconnectedFromTerminalSlot: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onCardInserted(final PoppTokenSession pts) {
    log.debug("onCardInserted: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onCardRemoved(final PoppTokenSession pts) {
    log.debug("onCardRemoved: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onCardPairedToServer(final PoppTokenSession pts) {
    log.debug("onCardPairedToServer: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onConnectedToServer(final PoppTokenSession pts) {
    log.debug("onConnectedToServer: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onDisconnectedFromServer(final PoppTokenSession pts) {
    log.debug("onDisconnectedFromServer: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onError(PoppTokenSession poppTokenSession, PoppClientException e) {
    log.error(
        "Error in PoppTokenSession for card {}: {}",
        poppTokenSession.getAttachedCard().getId(),
        e.getMessage(),
        e);

    final String cardId = poppTokenSession.getAttachedCard().getId();
    final CompletableFuture<TokenMessage> poppTokenFuture = tokenFutures.remove(cardId);

    if (poppTokenFuture != null) {
      poppTokenFuture.completeExceptionally(e);
    }
  }

  @Override
  public void onReceivedPoppToken(PoppTokenSession poppTokenSession, TokenMessage tokenMessage) {
    log.debug("onReceivedPoppToken: {}", poppTokenSession.getAttachedCard().getId());

    final String cardId = poppTokenSession.getAttachedCard().getId();
    final CompletableFuture<TokenMessage> poppTokenFuture = tokenFutures.remove(cardId);

    if (poppTokenFuture != null) {
      poppTokenFuture.complete(tokenMessage);
    }
  }

  @Override
  public void onFinished(PoppTokenSession poppTokenSession) {
    log.debug("onFinished: {}", poppTokenSession.getAttachedCard().getId());
  }
}
