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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.glue.RBelValidatorGlue;
import de.gematik.test.tiger.lib.TigerHttpClient;
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.restassured.http.Method;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Step definitions for policy hot-reload testing via OPA.
 *
 * <p>This test verifies that OPA applies policy changes at runtime without restart. Policies are
 * managed via the OPA REST API:
 *
 * <ul>
 *   <li>PUT /v1/policies/{id} - Upload/update a policy
 *   <li>DELETE /v1/policies/{id} - Remove a policy
 *   <li>GET /v1/policies - List all policies
 * </ul>
 *
 * <p><b>Uploading/deleting/listing policies stays a direct OPA-Admin-API call.</b> This is a pure
 * infrastructure/administration operation (deploying a policy bundle), not something any
 * Primärsystem-Client ever does - no client has an OPA-Admin endpoint, so there is nothing to route
 * this through. In production, OPA would instead pull policy bundles via Bundle-Polling from a
 * Local Artifact Registry; the direct REST API is used here as a pragmatic stand-in for that
 * deployment mechanism, exactly as documented in {@code README.md} of this feature.
 *
 * <p><b>Verifying the EFFECT of a policy change, however, is driven through the real
 * VSDM-Client</b> (its {@code /client/vsdm/vsd} endpoint), the same endpoint already exercised by
 * {@code zeta-gitti/zeta-gitti.feature} ({@link GittiSteps}) and {@code
 * zeta-client-policy/client_policy.feature} ({@link PolicyRejectionSteps}, {@link
 * TigerProxyManipulationsSteps}):
 *
 * <ul>
 *   <li>The uploaded test policies ({@code policy_p1.rego} / {@code policy_p11.rego}) target the
 *       exact same OPA policy id/package ({@code zeta/authz}) as the real production policy ({@code
 *       infra/docker/backend/zeta/policies/authz.rego}), so hot-reloading them genuinely changes
 *       what the real PDP-to-OPA authorization call evaluates for a real client request.
 *   <li>The ZeTA-PDP (Keycloak) only calls OPA once per SMC-B identity, during the initial Dynamic
 *       Client Registration (DCR) - see {@link PolicyRejectionSteps}. To force a fresh OPA decision
 *       for every "PS-Profil sendet eine Anfrage" step, the VSDM-Client's PDP registration is reset
 *       beforehand.
 *   <li>Since all PS-Profiles in this test share the same physical SMC-B test card, the
 *       PS-Profile's {@code professionOID} is injected into the live PDP-to-OPA decision request
 *       via a TigerProxy manipulation - the same mechanism {@code client_policy.feature} uses to
 *       exercise specific OPA-input values against a freshly-evaluated, real decision.
 *   <li>Whether the (hot-reloaded) policy accepted or rejected the request is read from the
 *       Tiger-Proxy-recorded VSDM-Client traffic, instead of asking OPA directly.
 * </ul>
 */
@Slf4j
public class PolicyUpdateabilitySteps {

  private static final String OPA_POLICY_ID = "zeta/authz";
  private static final String OPA_DEFAULT_POLICY_ID = "policies/authz.rego";
  private static final RestTemplate REST_TEMPLATE = new RestTemplate();

  private static final String CARD_TERMINAL_WS_URL = "ws://card-terminal-client";
  private static final String SMCB_CARD_IMAGE =
      "test/vsdm-testsuite/src/test/resources/private/smcb/smcbCardImage.xml";
  private static final int SMCB_SLOT = 1;
  private static final String EGK_CARD_IMAGE =
      "test/vsdm-testsuite/src/test/resources/data/cards/egkCardImage.xml";
  private static final int EGK_SLOT = 2;
  private static final String TIGER_PROXY_ADMIN_BASE_URL_CONFIG_KEY =
      "zeta.paths.tigerProxy.baseUrl";
  private static final String RESPONSE_CODE_VARIABLE = "policyUpdateability.vsdReadResponseCode";

  private final PolicyRejectionSteps policyRejectionSteps = new PolicyRejectionSteps();
  private final CardTerminalSteps cardTerminalSteps = new CardTerminalSteps();
  private final TigerProxyManipulationsSteps tigerProxyManipulationsSteps =
      new TigerProxyManipulationsSteps();
  private final RBelValidatorGlue rbelValidatorGlue = new RBelValidatorGlue();

  /** PS profile definitions: profileName -> professionOid */
  private final Map<String, String> psProfiles = new HashMap<>();

  /** Policy rego content cache: policyName -> rego source */
  private final Map<String, String> policyContent = new HashMap<>();

  private int lastResponseStatusCode;

  // --- Helper methods ---

  /**
   * Base URL of the VSDM-Client's ZeTA-PDP OPA instance ({@code vsdm-zeta-pdp-opa}), the OPA
   * decision this test actually needs to hot-reload: the VSD-Read triggered via {@link
   * #triggerRealClientRequest(String)} performs a fresh OPA decision against THIS OPA instance
   * (routed through docker-tiger-proxy, see {@code vsdm-zeta-pdp}'s DNS-interception setup in
   * {@code compose-vsdm-services.yaml}), not the popp-client's own OPA ({@code popp-zeta-pdp-opa}).
   * The latter is a separate OPA instance not reachable via the TigerProxy-manipulation mechanism
   * (popp-zeta-pdp does not route through docker-tiger-proxy), so uploading test policies there
   * would have no effect on - and in fact incorrectly interferes with - the popp-client's own
   * (unrelated, always-allow) decision.
   */
  private String getOpaBaseUrl() {
    return TigerGlobalConfiguration.resolvePlaceholders(
        "http://${ports.host}:${ports.vsdmOpaPort}");
  }

  private String loadPolicyFromClasspath(String policyName) {
    String resourcePath = "policies/policy_" + policyName.toLowerCase() + ".rego";
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Policy resource not found: " + resourcePath);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load policy from classpath: " + resourcePath, e);
    }
  }

  /**
   * Path (relative to the repository root) of the real production policy the vsdm-zeta-pdp-opa
   * container loads from disk at startup - the single source of truth {@link
   * #loadRealDefaultPolicy()} reads from directly, so no duplicate copy has to be kept in sync
   * under {@code src/test/resources}.
   */
  private static final String REAL_DEFAULT_POLICY_PATH =
      "infra/docker/backend/zeta/policies/authz.rego";

  /**
   * Loads the real production policy directly from the filesystem (as opposed to {@link
   * #loadPolicyFromClasspath(String)}, which is used for the test-only P1/P11 policies). Reading
   * the actual file - instead of maintaining a duplicate copy in {@code src/test/resources} -
   * guarantees {@link #restoreDefaultOpaPolicy()} can never drift out of sync with what
   * vsdm-zeta-pdp-opa actually loads from disk at container startup.
   */
  private String loadRealDefaultPolicy() {
    java.io.File file =
        new java.io.File(System.getProperty("user.dir"))
            .toPath()
            .resolve(REAL_DEFAULT_POLICY_PATH)
            .toFile();
    if (!file.exists()) {
      // Maven may run from test/zeta-testsuite instead of the repository root.
      file =
          new java.io.File(System.getProperty("user.dir"))
              .toPath()
              .resolve("../../" + REAL_DEFAULT_POLICY_PATH)
              .normalize()
              .toFile();
    }
    if (!file.exists()) {
      throw new IllegalStateException(
          "Real default policy not found at: " + file.getAbsolutePath());
    }
    try {
      return java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to read real default policy: " + file.getAbsolutePath(), e);
    }
  }

  private void uploadPolicyToOpa(String regoContent) {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_POLICY_ID;

    HttpHeaders headers = new HttpHeaders();
    // Charset must be explicit: RestTemplate's StringHttpMessageConverter otherwise falls back to
    // ISO-8859-1 for a bare "text/plain" content type, mangling any non-ASCII characters (e.g. the
    // German umlauts in this policy's comments) into invalid UTF-8 byte sequences that OPA's rego
    // compiler then rejects with "illegal utf-8 character".
    headers.setContentType(MediaType.valueOf("text/plain; charset=UTF-8"));
    HttpEntity<String> request = new HttpEntity<>(regoContent, headers);

    ResponseEntity<String> response =
        REST_TEMPLATE.exchange(url, HttpMethod.PUT, request, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("OPA policy upload should succeed")
        .isTrue();

    log.info("Policy uploaded to OPA ({}): HTTP {}", url, response.getStatusCode().value());
  }

  private void deletePolicyFromOpa() {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_POLICY_ID;
    try {
      REST_TEMPLATE.delete(url);
      log.info("Policy deleted from OPA: {}", url);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Policy not found in OPA (already clean): {}", url);
    }
  }

  private void deleteDefaultPolicyFromOpa() {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_DEFAULT_POLICY_ID;
    try {
      REST_TEMPLATE.delete(url);
      log.info("Default policy deleted from OPA: {}", url);
    } catch (HttpClientErrorException.NotFound e) {
      log.info("Default policy not found in OPA (already clean): {}", url);
    }
  }

  /**
   * Restores OPA to its real default policy (read directly from {@code
   * infra/docker/backend/zeta/policies/authz.rego} - the deny-list policy the vsdm-zeta-pdp-opa
   * container also loads from disk at startup, see {@link #loadRealDefaultPolicy()}) by uploading
   * it under {@link #OPA_POLICY_ID}.
   *
   * <p>This is required because {@link #deleteDefaultPolicyFromOpa()} only removes the disk-loaded
   * policy from OPA's in-memory runtime (a container restart would be needed to reload it from
   * disk) - without re-uploading equivalent content, OPA would be left with no {@code
   * zeta.authz.decision} rule at all, causing all subsequent PDP token requests in the same suite
   * run (e.g. other ZETA features sharing this OPA instance) to fail with an undefined-decision
   * error instead of a proper allow/deny.
   *
   * <p><b>Note:</b> the restored policy is the real deny-list policy, NOT an allow-all stub -
   * uploading an allow-all stub here previously (silently) disabled OPA policy enforcement (e.g.
   * for zeta-client-policy/client_policy.feature) for the remainder of any suite run that included
   * policy_updateability.feature.
   */
  private void restoreDefaultOpaPolicy() {
    uploadPolicyToOpa(loadRealDefaultPolicy());
    log.info("OPA restored to its real default (deny-list) policy under id '{}'", OPA_POLICY_ID);
  }

  /**
   * Unconditionally restores OPA to its default allow-all decision after every {@code
   * @policy_updateability} scenario, regardless of whether the scenario itself passed or failed
   * partway through (e.g. due to the pre-existing TigerProxy-manipulation-reset issue). Without
   * this, a restrictive test policy (P1/P11) or no policy at all could leak into subsequent
   * scenarios/features run in the same suite.
   */
  @After("@policy_updateability")
  public void restoreOpaDefaultsAfterScenario() {
    deletePolicyFromOpa();
    restoreDefaultOpaPolicy();
  }

  /**
   * Drives a real, freshly-evaluated OPA decision for the given PS-Profile through the actual
   * VSDM-Client, so that the resulting VSD-Read request/response is captured by the Tiger-Proxy for
   * the subsequent status assertion (see {@link #zetaRespondsWithStatus(String)}).
   *
   * <p>Steps, mirroring {@code zeta-client-policy/client_policy.feature}:
   *
   * <ol>
   *   <li>Reset the VSDM-Client's ZeTA-PDP registration ({@link PolicyRejectionSteps}), so the next
   *       VSD-Read performs a fresh DCR and therefore triggers a fresh OPA decision (OPA is only
   *       queried once per SMC-B identity/DCR, see {@link PolicyRejectionSteps} javadoc).
   *   <li>Reconfigure the card terminal/SMC-B card, since the reset (a {@code docker restart} under
   *       the hood) invalidates the previous WebSocket connection to the card terminal.
   *   <li>Register a TigerProxy manipulation that overwrites {@code
   *       $.body.input.user_info.professionOID} on the live PDP-to-OPA decision request with the
   *       PS-Profile's professionOID (all PS-Profiles share the same physical SMC-B test card).
   *   <li>Trigger the real VSD-Read ({@code /client/vsdm/vsd}) at the VSDM-Client.
   * </ol>
   */
  private void triggerRealClientRequest(String professionOid) {
    policyRejectionSteps.resetZetaPdpRegistration();

    cardTerminalSteps.configureTerminalAtVsdmClient(CARD_TERMINAL_WS_URL);
    cardTerminalSteps.loadCardInSlot(SMCB_CARD_IMAGE, SMCB_SLOT);
    cardTerminalSteps.loadCardInSlot(EGK_CARD_IMAGE, EGK_SLOT);

    // PDP -> OPA traffic is Docker-internal and only visible on the remote/docker-compose
    // TigerProxy, not on this JVM's local one (same reasoning as
    // zeta-client-policy/client_policy.feature and its README "Mechanismus" section).
    TigerGlobalConfiguration.putValue(
        TIGER_PROXY_ADMIN_BASE_URL_CONFIG_KEY,
        TigerGlobalConfiguration.resolvePlaceholders(
            "http://${ports.host}:${ports.remoteTigerProxyAdminPort}"));

    String opaCondition =
        TigerGlobalConfiguration.resolvePlaceholders(
            "isRequest && request.path =~ '.*${zeta.paths.opa.decisionPath}'");
    tigerProxyManipulationsSteps.setTigerProxyManipulationWithExecutions(
        opaCondition, "$.body.input.user_info.professionOID", professionOid, 1);

    try {
      String vsdRequestUrl =
          TigerGlobalConfiguration.resolvePlaceholders(
              "${zeta.paths.client.vsdRequest}&profileVersion=1.1");
      log.info(
          "Triggering VSDM-Client VSD-Read via {} (professionOID={})",
          vsdRequestUrl,
          professionOid);
      // Uses RestAssured via TigerHttpClient (the same mechanism the native "TGR sende eine leere
      // GET Anfrage an" step uses, see zeta-client-policy/client_policy.feature) instead of a
      // plain Spring RestTemplate, so the request/response is captured by Tiger's local Rbel
      // logging and can be found afterwards via awaitVsdReadResponseCode(). A RestTemplate call
      // bypasses this logging entirely, which previously caused
      // "No request with path '.../vsd' found in messages" failures.
      //
      // The "If-None-Match" header is required on every VSD-Read - the VSDM-Client rejects the
      // request with 428 "MISSING_PATIENT_RECORD_VERSION" otherwise, before it ever reaches the
      // ZETA registration/OPA decision (same requirement documented in
      // GittiSteps#registerFirstTimeAtVsdmZetaGuard() and set via "TGR setze den default header
      // If-None-Match" in zeta-asl/asl.feature's Grundlage).
      TigerHttpClient.givenDefaultSpec()
          .header("If-None-Match", "0")
          .request(Method.GET, URI.create(vsdRequestUrl));
    } finally {
      tigerProxyManipulationsSteps.resetTigerProxyManipulation();
    }
  }

  /**
   * Finds the last recorded VSD-Read request/response via the Tiger-Proxy and returns its response
   * code, retrying for a few seconds to bridge the asynchronous docker-compose Tiger-Proxy
   * traffic-forwarding delay (mirrors {@code GittiSteps#awaitAssertion(Runnable)}).
   */
  private String awaitVsdReadResponseCode() {
    String vsdRequestPath =
        TigerGlobalConfiguration.resolvePlaceholders("${zeta.paths.client.vsdRequestPath}");
    AtomicReference<String> responseCode = new AtomicReference<>();
    try {
      Awaitility.await()
          .atMost(Duration.ofSeconds(10))
          .pollInterval(Duration.ofMillis(500))
          .ignoreExceptions()
          .until(
              () -> {
                rbelValidatorGlue.findLastRequestToPath(vsdRequestPath);
                rbelValidatorGlue.storeCurrentResponseNodeTextValueInVariable(
                    "$.responseCode", RESPONSE_CODE_VARIABLE);
                responseCode.set(
                    TigerGlobalConfiguration.resolvePlaceholders(
                        "${" + RESPONSE_CODE_VARIABLE + "}"));
                return true;
              });
    } catch (ConditionTimeoutException e) {
      throw new AssertionError("Timed out waiting for VSD-Read response", e);
    }
    return responseCode.get();
  }

  // --- Cucumber Step Definitions ---

  @Gegebensei("die OPA Policy Registry ist leer")
  public void opaPolicyRegistryIsEmpty() {
    deletePolicyFromOpa();
    deleteDefaultPolicyFromOpa();
    log.info("OPA Policy Registry ist leer (alle zeta/authz Policies entfernt)");
  }

  @Gegebensei("die Policy {string} ist in der OPA Registry verfügbar")
  public void policyIsAvailableInOpaRegistry(String policyName) {
    String rego = loadPolicyFromClasspath(policyName);
    policyContent.put(policyName, rego);

    uploadPolicyToOpa(rego);
    log.info("Policy '{}' ist in der OPA Registry aktiv", policyName);
  }

  @Gegebensei("die Policy {string} ist verfügbar aber NICHT in der OPA Registry veröffentlicht")
  public void policyIsAvailableButNotPublished(String policyName) {
    String rego = loadPolicyFromClasspath(policyName);
    policyContent.put(policyName, rego);
    log.info(
        "Policy '{}' vorbereitet (lokal gecacht), aber nicht in OPA veröffentlicht", policyName);
  }

  @SuppressWarnings("unused") // nonMatchingPolicy is required by the Gherkin step pattern
  @Gegebensei("das PS-Profil {string} passt zu Policy {string} aber nicht zu {string}")
  public void psProfileMatchesPolicy(
      String profileName, String matchingPolicy, String nonMatchingPolicy) {
    String professionOid;
    if ("P1".equals(matchingPolicy)) {
      professionOid = "1.2.276.0.76.4.49"; // Arzt
    } else if ("P11".equals(matchingPolicy)) {
      professionOid = "1.2.276.0.76.4.50"; // Zahnarzt
    } else {
      throw new IllegalArgumentException("Unknown policy: " + matchingPolicy);
    }
    psProfiles.put(profileName, professionOid);
    log.info(
        "PS-Profil '{}' konfiguriert mit professionOid '{}' (passt zu '{}')",
        profileName,
        professionOid,
        matchingPolicy);
  }

  @Gegebensei("OPA ist erreichbar")
  public void opaIsReachable() {
    String healthUrl = getOpaBaseUrl() + "/v1/policies";
    ResponseEntity<String> response = REST_TEMPLATE.getForEntity(healthUrl, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("OPA should be reachable at %s", healthUrl)
        .isTrue();
    log.info("ZETA/OPA ist gestartet und erreichbar");
  }

  @Dann("hat ZETA die Policy {string} geladen")
  public void zetaHasLoadedPolicy(String policyName) {
    String url = getOpaBaseUrl() + "/v1/policies/" + OPA_POLICY_ID;
    ResponseEntity<String> response = REST_TEMPLATE.getForEntity(url, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("OPA should have policy '%s' loaded", policyName)
        .isTrue();
    log.info("Policy '{}' ist in OPA geladen (verifiziert via {})", policyName, url);
  }

  @Wenn("PS-Profil {string} eine Anfrage an ZETA sendet")
  public void psProfileSendsRequest(String profileName) {
    String professionOid = psProfiles.get(profileName);
    assertThat(professionOid)
        .as("PS-Profil '%s' muss vorher konfiguriert worden sein", profileName)
        .isNotNull();

    log.info(
        "PS-Profil '{}' löst eine echte VSD-Anfrage über den VSDM-Client aus (professionOid '{}')",
        profileName,
        professionOid);
    triggerRealClientRequest(professionOid);
    lastResponseStatusCode = Integer.parseInt(awaitVsdReadResponseCode());
  }

  @Wenn("die Policy {string} in der OPA Registry veröffentlicht wird")
  public void publishPolicyToOpaRegistry(String policyName) {
    String rego = policyContent.get(policyName);
    assertThat(rego).as("Policy '%s' muss vorher vorbereitet worden sein", policyName).isNotNull();

    uploadPolicyToOpa(rego);
    log.info("Policy '{}' wurde in der OPA Registry veröffentlicht (Hot-Reload)", policyName);
  }

  @Dann("antwortet ZETA mit Status {string}")
  public void zetaRespondsWithStatus(String expectedStatusPattern) {
    if ("2xx".equals(expectedStatusPattern)) {
      assertThat(lastResponseStatusCode).as("ZETA sollte mit 2xx antworten").isBetween(200, 299);
    } else if ("4xx".equals(expectedStatusPattern)) {
      assertThat(lastResponseStatusCode).as("ZETA sollte mit 4xx antworten").isBetween(400, 499);
    } else {
      assertThat(String.valueOf(lastResponseStatusCode))
          .as("ZETA Statuscode sollte dem Pattern '%s' entsprechen", expectedStatusPattern)
          .matches(expectedStatusPattern.replace("x", "\\d"));
    }
    log.info(
        "ZETA Antwort-Status (VSDM-Client VSD-Read, via Tiger-Proxy beobachtet): {} (erwartet: {})",
        lastResponseStatusCode,
        expectedStatusPattern);
  }
}
