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

import de.gematik.zeta.services.KeycloakAdminHelper;
import de.gematik.zeta.services.ZetaJwtTestFactory;
import io.cucumber.java.After;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber steps for SMC-B {@code SMCB_USER_MAX_CLIENTS} scenarios: enable unmanaged attributes,
 * seed {@code zetaguard.smcbuser.client_ids}, force fresh DCR, and cleanup the SMC-B user.
 */
@Slf4j
public class SmcbMaxClientsSteps {

  @Gegebensei("Keycloak unmanaged attributes sind für SMC-B Attribute aktiviert")
  public void enableUnmanagedAttributes() {
    KeycloakAdminHelper.enableUnmanagedAttributesAdminEdit();
  }

  /**
   * Resets the SMC-B test user's {@code client_ids} attribute (without deleting the user) and
   * clears the cached DCR registration after every {@code @smcb_max_clients} scenario, regardless
   * of pass/fail. Runs as a hook (not a Gherkin step) so scenario bodies only contain arrange/act/
   * assert steps, not this state-machine reset.
   *
   * <p>Deliberately keeps the underlying Keycloak user / federated-identity link intact instead of
   * deleting it: hard-deleting and immediately re-provisioning the same external SMC-B identity in
   * the next scenario was observed to put the PDP's identity broker into a bad state ({@code 409
   * conflict / "Duplicate resource error"} on the very next bootstrap token exchange).
   */
  @After("@smcb_max_clients")
  public void cleanupSmcbUserAfterScenario() {
    String telematikId = ZetaJwtTestFactory.getSmcbTelematikId();
    KeycloakAdminHelper.resetClientIdsIfPresent(telematikId);
    // Drop cached DCR so a later scenario registers a genuinely new client_id/key pair.
    ZetaJwtTestFactory.resetRegistration();
  }

  @Wenn("ich {int} Client-IDs für denselben SMC-B User im Keycloak vorbereite")
  public void seedClientIds(int count) {
    String telematikId = ZetaJwtTestFactory.getSmcbTelematikId();
    String userId = KeycloakAdminHelper.overwriteUserAttributes(telematikId, count);
    log.info("Seeded {} client_ids on SMC-B user {} (telematikId={})", count, userId, telematikId);
  }

  @Wenn("ich eine neue Dynamic Client Registration erzwinge")
  public void forceFreshDcr() {
    ZetaJwtTestFactory.resetRegistration();
    ZetaJwtTestFactory.ensureRegistered();
    log.info("Forced fresh DCR; client_id={}", ZetaJwtTestFactory.getClientId());
  }

  /**
   * Verifies (via the Keycloak Admin API, not backend logs) that {@code
   * zetaguard.smcbuser.client_ids} did not grow past {@code max} — the observable, reliable side
   * effect of the PDP's LRU eviction (A_25748-02) when {@code SMCB_USER_MAX_CLIENTS} is reached:
   * the oldest client is evicted so the list stays bounded instead of growing indefinitely.
   */
  @Und("die Anzahl der Client-IDs des SMC-B Users beträgt höchstens {int}")
  public void assertClientIdsCountAtMost(int max) {
    String telematikId = ZetaJwtTestFactory.getSmcbTelematikId();
    int actual = KeycloakAdminHelper.countClientIds(telematikId);
    assertThat(actual)
        .as("client_ids Liste darf durch LRU-Eviction (A_25748-02) nicht über %d wachsen", max)
        .isLessThanOrEqualTo(max);
  }
}
