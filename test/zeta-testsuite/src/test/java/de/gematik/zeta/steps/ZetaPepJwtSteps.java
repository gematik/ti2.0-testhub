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
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.zeta.services.ZetaPdpSubjectTokenFactory;
import de.gematik.zeta.services.ZetaPepJwtTestFactory;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Steps to create a valid Authorization header for ZETA-PEP WebSocket handshake and to obtain JWTs
 * from the ZETA-PDP mock.
 */
public class ZetaPepJwtSteps {

  @Gegebensei("ein gültiger ZETA-PEP AccessToken wird erzeugt")
  @Given("a valid ZETA-PEP access token is created")
  public void createValidPepAccessToken() {
    var bearer = ZetaPepJwtTestFactory.createBearerToken();
    TigerGlobalConfiguration.putValue("ZETA_PEP_AUTHZ", bearer);
  }

  @Gegebensei("ein ungültiger ZETA-PEP AccessToken wird erzeugt")
  @Given("an invalid ZETA-PEP access token is created")
  public void createInvalidPepAccessToken() {
    // simplest invalid token: valid-ish JWT structure but broken signature
    TigerGlobalConfiguration.putValue("ZETA_PEP_AUTHZ", "Bearer invalid.invalid.invalid");
  }

  @Wenn("Hole JWT für Client {string} von {string} und speichere in der Variable {string}")
  @When("fetch JWT for client {string} from {string} and store it in variable {string}")
  public void fetchJwtForClientAndStore(String clientId, String tokenEndpoint, String varName) {
    // Tiger-Platzhalter auflösen
    String resolved = TigerGlobalConfiguration.resolvePlaceholders(tokenEndpoint);
    URI uri = URI.create(resolved);

    RestTemplate rt = new RestTemplate();

    // Form-Body für Token-Exchange
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");

    // Syntaktisch gültiges JWT als subject_token erzeugen
    String subjectToken =
        ZetaPdpSubjectTokenFactory.createSubjectToken(clientId, "1.2.276.0.76.4.49");
    form.add("subject_token", subjectToken);
    form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");

    ResponseEntity<String> response = rt.postForEntity(uri, form, String.class);

    // Response im Tiger-Kontext ablegen
    TigerGlobalConfiguration.putValue(varName, response.getBody());
    TigerGlobalConfiguration.putValue(varName + "_status", response.getStatusCode().value());
  }

  @Wenn("sende Token-Exchange-Request für Client {string} an {string} über Tiger-Proxy {string}")
  @When("send token exchange request for client {string} to {string} via Tiger proxy {string}")
  public void sendTokenExchangeViaTigerProxy(String clientId, String targetUrl, String proxyUrl) {
    // Platzhalter auflösen
    String resolvedTarget = TigerGlobalConfiguration.resolvePlaceholders(targetUrl);
    String resolvedProxy = TigerGlobalConfiguration.resolvePlaceholders(proxyUrl);

    URI targetUri = URI.create(resolvedTarget);
    URI proxyUri = URI.create(resolvedProxy);

    // RestTemplate mit HTTP-Proxy konfigurieren
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setProxy(
        new java.net.Proxy(
            java.net.Proxy.Type.HTTP,
            new java.net.InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));

    RestTemplate rt = new RestTemplate(factory);

    // Form-Body für Token-Exchange
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");

    String subjectToken =
        ZetaPdpSubjectTokenFactory.createSubjectToken(clientId, "1.2.276.0.76.4.49");
    form.add("subject_token", subjectToken);
    form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");

    // Request mit Content-Type Header
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    rt.postForEntity(targetUri, request, String.class);
    // Response wird vom Tiger-Proxy mitgeschnitten und kann mit TGR-Steps geprüft werden
  }

  @Dann("prüfe dass {string} ein access_token enthält")
  @Then("verify that {string} contains an access_token")
  public void verifyResponseContainsAccessToken(String responseBody) {
    String resolved = TigerGlobalConfiguration.resolvePlaceholders(responseBody);
    assertThat(resolved).as("Response sollte ein access_token enthalten").contains("access_token");
  }
}
