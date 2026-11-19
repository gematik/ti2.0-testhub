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

import org.junit.jupiter.api.Test;

class JwtVerificationStepsTest {

  @Test
  void mapsIngressToConfiguredHostAndPortWithOrWithoutInternalPort() {
    for (String authority : new String[] {"popp-zeta-ingress", "popp-zeta-ingress:443"}) {
      assertThat(
              JwtVerificationSteps.resolveDockerHostname(
                  "https://" + authority + "/auth/realms/zeta-guard",
                  "docker-node",
                  "33400",
                  "33401"))
          .isEqualTo("https://docker-node:33400/auth/realms/zeta-guard");
    }
  }

  @Test
  void mapsServerToConfiguredHostAndPublishedPort() {
    assertThat(
            JwtVerificationSteps.resolveDockerHostname(
                "https://popp-server:8443/.well-known/openid-federation",
                "docker-node",
                "33400",
                "33401"))
        .isEqualTo("https://docker-node:33401/.well-known/openid-federation");
  }

  @Test
  void preservesLocalHostDefaults() {
    assertThat(
            JwtVerificationSteps.resolveDockerHostname(
                "https://popp-zeta-ingress/auth", "localhost", "11443", "18443"))
        .isEqualTo("https://localhost:11443/auth");
  }

  @Test
  void leavesExternalHostsAndPathsContainingContainerNamesUnchanged() {
    String url = "https://popp.dev.poppservice.de/popp-server:8443/popp-zeta-ingress";
    assertThat(JwtVerificationSteps.resolveDockerHostname(url, "docker-node", "33400", "33401"))
        .isEqualTo(url);
  }
}
