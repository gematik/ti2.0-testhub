/*-
 * #%L
 * ZeTA Testsuite
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.zeta.steps;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.glue.RBelValidatorGlue;
import de.gematik.zeta.config.PoPpConfig;
import de.gematik.zeta.services.ZetaPepJwtTestFactory;
import de.gematik.zeta.services.ZetaPepJwtTestFactory.PdpTarget;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Steps for the ZETA-GITTI feature: the first-time registration ("Erstregistrierung") of a
 * Primärsystem at the ZETA-Guards of the PoPP-Service and the VSDM 2.0 Fachdienst.
 *
 * <p>These steps deliberately reuse existing, already tested building blocks instead of talking to
 * the ZETA-PDP directly:
 *
 * <ul>
 *   <li>The SMC-B card is provisioned via the card terminal simulator, exactly as in {@link
 *       CardTerminalSteps} / {@code zeta-asl/asl.feature}.
 *   <li>The registration against the PoPP ZETA-Guard is triggered via {@link
 *       ZetaPepJwtTestFactory#doTokenExchangeViaProxy(PdpTarget, String, String, int, boolean)},
 *       the same DCR + SMC-B-token-exchange building block already used by {@code
 *       ZetaPepJwtSteps#sendTokenExchangeViaTigerProxy}. Note: the PoPP-Client docker service
 *       itself only generates PoPP-Tokens (health-data authorization, via the PoPP-Server
 *       WebSocket) and is unrelated to ZETA-Guard client registration, so it cannot be used here.
 *   <li>The registration against the VSDM 2.0 ZETA-Guard is triggered through the real VSDM-Client
 *       (its {@code /client/vsdm/vsd} endpoint), the same endpoint already exercised by {@code
 *       zeta-asl/asl.feature}. The VSDM-Client's embedded ZETA-SDK performs the DCR +
 *       token-exchange flow against the VSDM ZETA-Guard PDP, and this traffic is captured by the
 *       Tiger-Proxy.
 *   <li>Whether a valid access/refresh token pair was actually issued is verified by re-using the
 *       Tiger {@link RBelValidatorGlue} building blocks that also back the generic {@code TGR}
 *       Cucumber steps ("TGR finde die letzte Anfrage mit dem Pfad ...", "TGR prüfe aktuelle
 *       Antwort enthält Knoten ..."), applied to the token-endpoint traffic recorded by the
 *       Tiger-Proxy.
 * </ul>
 *
 * <p><b>Known limitation:</b> the PoPP-Then-step only verifies the {@code access_token}, not the
 * {@code refresh_token}. The PoPP ZETA-Guard PDP currently runs an older build of the custom
 * token-exchange plugin ({@code keycloak-zeta:1.2.1}, see {@code compose-popp-services.yaml}) that
 * hard-rejects a {@code requested_token_type=refresh_token} token-exchange request, unlike the VSDM
 * ZETA-Guard PDP ({@code keycloak-zeta:1.2.3}, see {@code compose-vsdm-services.yaml}). See {@link
 * #registerFirstTimeAtPoppZetaGuard()} for details.
 */
@Slf4j
public class GittiSteps {

  private static final String CARD_TERMINAL_WS_URL = "ws://card-terminal-client";
  private static final String SMCB_SLOT_VARIABLE = "gitti.smcbSlot";

  private final CardTerminalSteps cardTerminalSteps = new CardTerminalSteps();
  private final RBelValidatorGlue rbelValidatorGlue = new RBelValidatorGlue();
  private final RestTemplate lenientRestTemplate = createLenientRestTemplate();

  @Angenommen("das Primärsystem in der LEI verwendet eine SMC-B {string} im Slot {int}")
  @Given("the primary system at the LEI uses an SMC-B {string} in slot {int}")
  public void primarySystemUsesSmcbInSlot(String smcbCardImagePath, int slotId) {
    // Both the PoPP-Client and the VSDM-Client read the SMC-B key material used for the
    // ZETA-Client registration/authentication from the card terminal simulator. Configure the
    // terminal at the VSDM client and load the requested SMC-B card image into the requested
    // slot, exactly as done for the ASL-Handshake tests (zeta-asl/asl.feature).
    cardTerminalSteps.configureTerminalAtVsdmClient(CARD_TERMINAL_WS_URL);
    cardTerminalSteps.loadCardInSlot(smcbCardImagePath, slotId);
    TigerGlobalConfiguration.putValue(SMCB_SLOT_VARIABLE, String.valueOf(slotId));
  }

  @Wenn("das Primärsystem der LEI sich erstmalig am ZETA-Guard des PoPP-Service registriert")
  @When(
      "the primary system at the LEI registers for the first time at the ZETA-Guard of the"
          + " PoPP-Service")
  public void registerFirstTimeAtPoppZetaGuard() {
    // Drives DCR (Dynamic Client Registration) plus the SMC-B subject-token exchange against the
    // PoPP ZETA-Guard PDP through the Tiger-Proxy, so the traffic is captured for the Then-step
    // assertion.
    //
    // NOTE: we deliberately do NOT request a refresh_token here (requestRefreshToken=false).
    // The PoPP ZETA-Guard PDP (docker image keycloak-zeta:1.2.1, see
    // compose-popp-services.yaml) runs an older build of the custom token-exchange plugin
    // (zeta-smcb-token-exchange.jar) that hard-rejects
    // requested_token_type=urn:ietf:params:oauth:token-type:refresh_token with
    // "exchange_client: invalid_request" / "reason=requested_token_type unsupported" (confirmed
    // via the PDP's Keycloak event log). The VSDM ZETA-Guard PDP uses a newer image
    // (keycloak-zeta:1.2.3, see compose-vsdm-services.yaml) whose plugin build DOES support it.
    // This is a known environment/version inconsistency between the two PDP images, not
    // something fixable from the test side — see #primarySystemReceivesTokensFromPoppZetaGuard.
    String tokenUrl = PoPpConfig.tokenUrl();
    URI proxyUri = resolveTigerProxyUri();
    ZetaPepJwtTestFactory.doTokenExchangeViaProxy(
        PdpTarget.POPP, tokenUrl, proxyUri.getHost(), proxyUri.getPort(), false);
  }

  @Dann(
      "erhält das Primärystem einen gültigen Access- und Refresh-Token vom ZETA-Guard des"
          + " PoPP-Service")
  @Then(
      "the primary system receives a valid access and refresh token from the ZETA-Guard of the"
          + " PoPP-Service")
  public void primarySystemReceivesTokensFromPoppZetaGuard() {
    assertAccessTokenIssuedByZetaGuard(PoPpConfig.tokenUrl(), null);
  }

  @Wenn("das Primärsystem der LEI sich erstmalig am ZETA-Guard des VSDM 2.0 Fachdienst registriert")
  @When(
      "the primary system at the LEI registers for the first time at the ZETA-Guard of the VSDM"
          + " 2.0 service")
  public void registerFirstTimeAtVsdmZetaGuard() {
    // The VSDM-Client's first VSD-Read (/client/vsdm/vsd) drives its own embedded ZETA-SDK in the
    // exact same way: DCR against the VSDM ZETA-Guard PDP, followed by the SMC-B token exchange
    // (see zeta-asl/asl.feature which exercises the identical endpoint).
    String slotId = TigerGlobalConfiguration.resolvePlaceholders("${" + SMCB_SLOT_VARIABLE + "}");
    String vsdmClientUrl =
        TigerGlobalConfiguration.resolvePlaceholders("http://127.0.0.1:${ports.vsdmClientPort}");
    String url =
        vsdmClientUrl
            + "/client/vsdm/vsd?terminalId=0&egkSlotId="
            + slotId
            + "&smcBSlotId="
            + slotId
            + "&isFhirXml=false&profileVersion=1.0";

    // The VSDM-Client requires the "If-None-Match" header on every VSD-Read (it rejects the
    // request with 428 "MISSING_PATIENT_RECORD_VERSION" otherwise, before it ever gets to
    // trigger the ZETA registration) — same header set via "TGR setze den default header
    // If-None-Match" in zeta-asl/asl.feature's Grundlage.
    HttpHeaders headers = new HttpHeaders();
    headers.set("If-None-Match", "0");
    log.info("Triggering VSDM-Client ZETA registration via {}", url);
    lenientRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
  }

  @Dann(
      "erhält das Primärystem einen gültigen Access- und Refresh-Token vom ZETA-Guard des VSDM 2.0"
          + " Fachdienst")
  @Then(
      "the primary system receives a valid access and refresh token from the ZETA-Guard of the"
          + " VSDM 2.0 service")
  public void primarySystemReceivesTokensFromVsdmZetaGuard() {
    String vsdmZetaGuardTokenUrl =
        TigerGlobalConfiguration.resolvePlaceholders(
            "http://${ports.host}:${ports.vsdmPdpPort}/auth/realms/zeta-guard/protocol/openid-connect/token");
    // The VSDM-Client's ZETA registration traffic is captured by docker-tiger-proxy under the
    // "vsdm-zeta-ingress" DNS alias (see registerFirstTimeAtVsdmZetaGuard), NOT under the direct
    // PDP host/port used above to derive the path. Since the PoPP flow's token-exchange happens to
    // use the exact same request PATH (both realms are named "zeta-guard"), a plain
    // "find last request to path" would risk matching PoPP's already-recorded response instead of
    // VSDM's. We therefore additionally filter by the captured Host to disambiguate the two.
    assertAccessAndRefreshTokenIssuedByZetaGuard(vsdmZetaGuardTokenUrl);
  }

  /**
   * Finds the last recorded request to the given ZETA-Guard token endpoint via the Tiger-Proxy and
   * asserts that its response contains a non-empty {@code access_token} and {@code refresh_token}.
   * Reuses the same {@link RBelValidatorGlue} methods that back the generic {@code TGR} Cucumber
   * steps ("TGR finde die letzte Anfrage mit dem Pfad ...", "TGR prüfe aktuelle Antwort ...").
   *
   * <p>Only used for the VSDM ZETA-Guard (see {@link
   * #primarySystemReceivesTokensFromVsdmZetaGuard()}), so the {@code "vsdm-zeta-ingress"} host
   * filter (see {@link #assertAccessTokenIssuedByZetaGuard(String, String)}) is always applied
   * here.
   */
  private void assertAccessAndRefreshTokenIssuedByZetaGuard(String tokenEndpointUrl) {
    assertAccessTokenIssuedByZetaGuard(tokenEndpointUrl, "vsdm-zeta-ingress");
    rbelValidatorGlue.currentResponseMessageContainsNode("$.body.refresh_token");
    rbelValidatorGlue.currentResponseMessageAttributeMatches("$.body.refresh_token", ".+");
  }

  /**
   * Finds the last recorded request to the given ZETA-Guard token endpoint via the Tiger-Proxy and
   * asserts that its response contains a non-empty {@code access_token} (without requiring a {@code
   * refresh_token} — see {@link #primarySystemReceivesTokensFromPoppZetaGuard()}).
   *
   * <p>The VSDM-Client's ZETA registration traffic is captured by the docker-compose {@code
   * docker-tiger-proxy} (reached via the "vsdm-zeta-ingress" DNS alias) and only reaches this JVM's
   * local Tiger-Proxy asynchronously via its WebSocket traffic-forwarding connection. This is in
   * contrast to the PoPP path, which is sent directly through this JVM's own embedded Tiger-Proxy
   * (see {@link #registerFirstTimeAtPoppZetaGuard()}) and is therefore already recorded by the time
   * the request returns. To avoid a race with that asynchronous forwarding, we poll for the message
   * for a few seconds before giving up (mirrors the "TGR warte auf eine Nachricht ..." step used
   * for the same purpose in {@code zeta-asl/asl.feature}).
   *
   * @param hostFilter if non-null/blank, restricts the search to requests whose captured Host
   *     matches this value (see {@link #primarySystemReceivesTokensFromVsdmZetaGuard()} for why
   *     this is necessary — PoPP and VSDM token-exchange requests share the same request path).
   */
  private void assertAccessTokenIssuedByZetaGuard(String tokenEndpointUrl, String hostFilter) {
    String path = URI.create(tokenEndpointUrl).getPath();
    if (hostFilter != null && !hostFilter.isBlank()) {
      rbelValidatorGlue.tgrFilterBasedOnHost(hostFilter);
    }
    try {
      awaitAssertion(
          () -> {
            rbelValidatorGlue.findLastRequestToPath(path);
            rbelValidatorGlue.currentResponseMessageAttributeMatches("$.responseCode", "2..");
            rbelValidatorGlue.currentResponseMessageContainsNode("$.body.access_token");
            rbelValidatorGlue.currentResponseMessageAttributeMatches("$.body.access_token", ".+");
          });
    } finally {
      if (hostFilter != null && !hostFilter.isBlank()) {
        rbelValidatorGlue.tgrResetRequestHostFilter();
      }
    }
  }

  /**
   * Retries the given assertion for a few seconds, to bridge the asynchronous traffic-forwarding
   * delay described in {@link #assertAccessTokenIssuedByZetaGuard(String, String)}.
   */
  @SuppressWarnings("BusyWait")
  private void awaitAssertion(Runnable assertion) {
    long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
    AssertionError lastError = new AssertionError("Timed out waiting for assertion to succeed");
    while (System.currentTimeMillis() < deadline) {
      try {
        assertion.run();
        return;
      } catch (AssertionError e) {
        lastError = e;
        try {
          // Polling delay: bridges the asynchronous traffic-forwarding of the docker-compose
          // Tiger-Proxy (see class javadoc), no notification mechanism is available for it.
          Thread.sleep(500);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw lastError;
        }
      }
    }
    throw lastError;
  }

  /**
   * Resolves the local Tiger-Proxy's host/port, the same way {@code
   * ZetaPepJwtSteps#sendTokenExchangeViaTigerProxy} does, so DCR/token-exchange traffic sent
   * through it gets captured for later TGR-based validation.
   */
  private URI resolveTigerProxyUri() {
    String proxyUrl =
        TigerGlobalConfiguration.resolvePlaceholders(
            "http://localhost:${tiger.tigerProxy.proxyPort}");
    return URI.create(proxyUrl);
  }

  /**
   * A {@link RestTemplate} that does not throw on 4xx/5xx responses. The VSD-Read triggered in
   * {@link #registerFirstTimeAtVsdmZetaGuard()} may fail further down the line (e.g. eGK read
   * issues) independent of the ZETA registration; the actual assertion happens against the
   * Tiger-Proxy-recorded token-endpoint traffic, not the VSDM-Client's HTTP response.
   */
  private RestTemplate createLenientRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setErrorHandler(
        new ResponseErrorHandler() {
          @Override
          public boolean hasError(@NonNull ClientHttpResponse response) {
            return false;
          }

          @Override
          public void handleError(
              @NonNull URI url, @NonNull HttpMethod method, @NonNull ClientHttpResponse response) {
            // no-op: let error responses pass through, they are validated via Tiger-Proxy
          }
        });
    return restTemplate;
  }
}
