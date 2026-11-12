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
import io.cucumber.java.After;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Step definitions for policy hot-reload testing via OPA <b>Bundle-Polling</b> from a Local
 * Artifact Registry - the production mechanism {@link PolicyUpdateabilitySteps} explicitly calls
 * out as the mechanism its own REST-API-based test is only a pragmatic stand-in for (see its class
 * javadoc and {@code README.md} of this feature).
 *
 * <p>Instead of {@code PUT /v1/policies/...}, this test:
 *
 * <ul>
 *   <li>builds a minimal, unsigned OPA policy bundle ({@code .manifest} + {@code authz.rego}) in
 *       memory and publishes it via the Docker API into the named volume mounted into {@code
 *       vsdm-zeta-bundle-registry} (a plain nginx static file server acting as the "Local Artifact
 *       Registry" - a local stand-in for a real OCI registry such as Zot, see {@code
 *       compose-vsdm-services.yaml}),
 *   <li>relies on {@code vsdm-zeta-pdp-opa}'s bundle plugin (configured in {@code
 *       zeta/opa-bundle-config/config.yaml}) to poll and activate it automatically, with no direct
 *       OPA-API call involved at all.
 * </ul>
 *
 * <p>The bundle's {@code .manifest} restricts its root to {@code zeta/bundletest} (package {@code
 * zeta.bundletest.authz}), an isolated test-only namespace that is never queried by any real PDP
 * traffic and therefore cannot collide with the production {@code zeta.authz} package managed by
 * {@link PolicyUpdateabilitySteps} - both hot-reload mechanisms can run against the very same
 * {@code vsdm-zeta-pdp-opa} instance without interfering with each other (verified: OPA rejects any
 * policy whose root is owned by an already-configured bundle, so sharing {@code zeta.authz} between
 * both would not be possible).
 *
 * <p>Because this isolated package is never exercised by production traffic, verification is done
 * by querying OPA's Decision API directly ({@code POST /v1/data/zeta/bundletest/authz/decision}),
 * the same direct-API approach already documented as a valid verification style in this feature's
 * {@code README.md}.
 */
@Slf4j
public class PolicyBundlePollingSteps {

  private static final String BUNDLE_DECISION_PATH = "zeta/bundletest/authz/decision";
  private static final String BUNDLE_MODULE_ID_PREFIX = "zeta_updateability_bundle_test/";
  private static final RestTemplate REST_TEMPLATE = new RestTemplate();

  private final BundleRegistryClient bundleRegistry = new BundleRegistryClient();

  /** Bundle policy rego content cache: policyName -> rego source */
  private final Map<String, String> bundlePolicyContent = new HashMap<>();

  /** PS profile definitions (bundle variant): profileName -> professionOid */
  private final Map<String, String> psProfiles = new HashMap<>();

  private boolean lastDecisionAllow;

  // --- Helper methods ---

  /**
   * Base URL of {@code vsdm-zeta-pdp-opa} - the very same OPA instance {@link
   * PolicyUpdateabilitySteps} uses, see its javadoc for why this is the correct instance.
   */
  private String getOpaBaseUrl() {
    return TigerGlobalConfiguration.resolvePlaceholders(
        "http://${ports.host}:${ports.vsdmOpaPort}");
  }

  private String loadBundlePolicyFromClasspath(String policyName) {
    String resourcePath = "policies/policy_" + policyName.toLowerCase() + "_bundle.rego";
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalArgumentException("Bundle policy resource not found: " + resourcePath);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load bundle policy from classpath: " + resourcePath, e);
    }
  }

  /**
   * Publishes the given rego content to the Local Artifact Registry by (re-)writing the bundle
   * {@code .tar.gz} it serves - the sole trigger for {@code vsdm-zeta-pdp-opa}'s next bundle poll
   * to pick up and activate the change (Hot-Reload), analogous to {@code
   * PolicyUpdateabilitySteps#uploadPolicyToOpa(String)} for the REST-API-based scenario.
   */
  private void publishBundleToRegistry(String regoContent) {
    bundleRegistry.publish(regoContent);
    log.info("Policy bundle published to Local Artifact Registry");
  }

  private void deleteBundleFromRegistry() {
    bundleRegistry.delete();
    log.info("Policy bundle removed from Local Artifact Registry");
  }

  private static String professionOidForBundlePolicy(String policyName) {
    return switch (policyName) {
      case "BP1" -> "1.2.276.0.76.4.49"; // Arzt
      case "BP11" -> "1.2.276.0.76.4.50"; // Zahnarzt
      default -> throw new IllegalArgumentException("Unknown bundle policy: " + policyName);
    };
  }

  /** Queries OPA's Decision API directly for the isolated {@code zeta.bundletest.authz} package. */
  @SuppressWarnings("unchecked")
  private boolean queryBundleDecisionAllow(String professionOid) {
    String url = getOpaBaseUrl() + "/v1/data/" + BUNDLE_DECISION_PATH;
    Map<String, Object> input =
        Map.of("input", Map.of("user_info", Map.of("professionOID", professionOid)));
    ResponseEntity<Map<String, Object>> response =
        REST_TEMPLATE.postForEntity(url, input, (Class<Map<String, Object>>) (Class<?>) Map.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("OPA bundle decision query should succeed")
        .isTrue();
    Map<String, Object> body = response.getBody();
    Map<String, Object> result = body != null ? (Map<String, Object>) body.get("result") : null;
    return result != null && Boolean.TRUE.equals(result.get("allow"));
  }

  /** Checks whether OPA currently has a module loaded that originates from the test bundle. */
  private boolean isBundleModuleLoaded() {
    String url = getOpaBaseUrl() + "/v1/policies";
    ResponseEntity<String> response = REST_TEMPLATE.getForEntity(url, String.class);
    return response.getStatusCode().is2xxSuccessful()
        && response.getBody() != null
        && response.getBody().contains(BUNDLE_MODULE_ID_PREFIX);
  }

  // --- Cucumber Step Definitions ---

  @Gegebensei("die Bundle Registry ist leer")
  public void bundleRegistryIsEmpty() {
    deleteBundleFromRegistry();
    log.info("Local Artifact Registry ist leer (kein Policy-Bundle veröffentlicht)");
  }

  @Gegebensei("die Policy {string} ist als Bundle in der Local Artifact Registry verfügbar")
  public void bundlePolicyIsAvailableInRegistry(String policyName) {
    String rego = loadBundlePolicyFromClasspath(policyName);
    bundlePolicyContent.put(policyName, rego);
    publishBundleToRegistry(rego);
    log.info("Policy-Bundle '{}' in der Local Artifact Registry veröffentlicht", policyName);
  }

  @Gegebensei(
      "die Policy {string} ist als Bundle verfügbar aber NICHT in der Local Artifact Registry"
          + " veröffentlicht")
  public void bundlePolicyIsAvailableButNotPublished(String policyName) {
    String rego = loadBundlePolicyFromClasspath(policyName);
    bundlePolicyContent.put(policyName, rego);
    log.info(
        "Policy-Bundle '{}' vorbereitet (lokal gecacht), aber nicht in der Local Artifact Registry"
            + " veröffentlicht",
        policyName);
  }

  @SuppressWarnings("unused") // nonMatchingPolicy is required by the Gherkin step pattern
  @Gegebensei("das PS-Profil {string} passt zur Bundle-Policy {string} aber nicht zu {string}")
  public void psProfileMatchesBundlePolicy(
      String profileName, String matchingPolicy, String nonMatchingPolicy) {
    psProfiles.put(profileName, professionOidForBundlePolicy(matchingPolicy));
    log.info(
        "PS-Profil '{}' konfiguriert (passt zur Bundle-Policy '{}')", profileName, matchingPolicy);
  }

  @Wenn("die Policy {string} als Bundle in der Local Artifact Registry veröffentlicht wird")
  public void publishBundlePolicyToRegistry(String policyName) {
    String rego = bundlePolicyContent.get(policyName);
    assertThat(rego)
        .as("Bundle-Policy '%s' muss vorher vorbereitet worden sein", policyName)
        .isNotNull();
    publishBundleToRegistry(rego);
    log.info(
        "Policy-Bundle '{}' als aktualisiertes Bundle in der Local Artifact Registry"
            + " veröffentlicht (Hot-Reload)",
        policyName);
  }

  @Dann("hat ZETA die Policy {string} als Bundle geladen")
  public void zetaHasLoadedBundlePolicy(String policyName) {
    String expectedOid = professionOidForBundlePolicy(policyName);
    try {
      Awaitility.await()
          .atMost(Duration.ofSeconds(15))
          .pollInterval(Duration.ofMillis(500))
          .untilAsserted(
              () -> {
                assertThat(isBundleModuleLoaded())
                    .as("OPA should have the test bundle's module loaded")
                    .isTrue();
                assertThat(queryBundleDecisionAllow(expectedOid))
                    .as(
                        "OPA's bundle-sourced decision should already reflect policy '%s'",
                        policyName)
                    .isTrue();
              });
    } catch (ConditionTimeoutException e) {
      throw new AssertionError(
          "Timed out waiting for OPA to hot-reload policy bundle '" + policyName + "'", e);
    }
    log.info("Policy-Bundle '{}' ist via Bundle-Polling in OPA geladen (verifiziert)", policyName);
  }

  @Wenn("PS-Profil {string} eine Anfrage an die Bundle-Policy sendet")
  public void psProfileSendsRequestToBundlePolicy(String profileName) {
    String professionOid = psProfiles.get(profileName);
    assertThat(professionOid)
        .as("PS-Profil '%s' muss vorher konfiguriert worden sein", profileName)
        .isNotNull();
    lastDecisionAllow = queryBundleDecisionAllow(professionOid);
    log.info(
        "PS-Profil '{}' hat die Bundle-Policy-Entscheidung direkt über die OPA Decision API"
            + " abgefragt (professionOid '{}'): allow={}",
        profileName,
        professionOid,
        lastDecisionAllow);
  }

  @Dann("liefert die Bundle-Policy die Entscheidung {string}")
  public void bundlePolicyRespondsWithDecision(String expectedDecision) {
    boolean expectedAllow = "allow".equals(expectedDecision);
    assertThat(lastDecisionAllow)
        .as("Bundle-Policy sollte mit Entscheidung '%s' antworten", expectedDecision)
        .isEqualTo(expectedAllow);
  }

  /**
   * Keeps the Local Artifact Registry clean between scenarios. Unlike {@link
   * PolicyUpdateabilitySteps#restoreOpaDefaultsAfterScenario()}, no OPA-side restoration is needed:
   * the bundle's root ({@code zeta/bundletest}) is an isolated, test-only namespace no real PDP
   * traffic ever queries, so leaving it loaded (or unloaded) between scenarios cannot affect any
   * other feature.
   */
  @After("@policy_updateability_bundle")
  public void cleanUpBundleRegistryAfterScenario() {
    deleteBundleFromRegistry();
  }
}
