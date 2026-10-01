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
import de.gematik.zeta.services.ZetaJwtTestFactory;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;

/**
 * Steps to create a valid Authorization header for ZETA-PEP WebSocket handshake and to obtain JWTs
 * from the ZETA-PDP mock.
 */
@Slf4j
public class ZetaPepJwtSteps {

  @Gegebensei("ein gültiger ZETA-PEP AccessToken wird erzeugt")
  @Given("a valid ZETA-PEP access token is created")
  public void createValidPepAccessToken() {
    var bearer = ZetaJwtTestFactory.createBearerToken();
    TigerGlobalConfiguration.putValue("ZETA_PEP_AUTHZ", bearer);
    TigerGlobalConfiguration.putValue("tiger.httpClient.defaultHeader.Authorization", bearer);

    // The Docker PEP (ngx_pep) requires a DPoP proof header for every request.
    String pepUrl =
        TigerGlobalConfiguration.resolvePlaceholders("${pepProxyUrl|http://127.0.0.1:2101}");
    String testPath = TigerGlobalConfiguration.resolvePlaceholders("${pepTestPath|/v3/api-docs}");
    String dpopProof = ZetaJwtTestFactory.createDpopProofForRequest("GET", pepUrl + testPath);
    TigerGlobalConfiguration.putValue("tiger.httpClient.defaultHeader.DPoP", dpopProof);
  }

  @Gegebensei("ein ungültiger ZETA-PEP AccessToken wird erzeugt")
  @Given("an invalid ZETA-PEP access token is created")
  public void createInvalidPepAccessToken() {
    // simplest invalid token: valid-ish JWT structure but broken signature
    var bearer = "Bearer invalid.invalid.invalid";
    TigerGlobalConfiguration.putValue("ZETA_PEP_AUTHZ", bearer);
    // Set directly as default header to bypass RBel serialization in Tiger steps
    TigerGlobalConfiguration.putValue("tiger.httpClient.defaultHeader.Authorization", bearer);
  }

  @Wenn("sende Token-Exchange-Request für Client {string} an {string} über Tiger-Proxy {string}")
  @When("send token exchange request for client {string} to {string} via Tiger proxy {string}")
  public void sendTokenExchangeViaTigerProxy(String clientId, String targetUrl, String proxyUrl) {
    log.info("Sending token exchange request for client '{}'", clientId);
    // Platzhalter auflösen
    String resolvedTarget = TigerGlobalConfiguration.resolvePlaceholders(targetUrl);
    String resolvedProxy = TigerGlobalConfiguration.resolvePlaceholders(proxyUrl);

    URI proxyUri = URI.create(resolvedProxy);

    // Full Keycloak token exchange with proper auth (SMC-B subject token + client_assertion + DPoP)
    ZetaJwtTestFactory.doTokenExchangeViaProxy(
        resolvedTarget, proxyUri.getHost(), proxyUri.getPort());
    // Response wird vom Tiger-Proxy mitgeschnitten und kann mit TGR-Steps geprüft werden
  }
}
