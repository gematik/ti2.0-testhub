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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
@Slf4j
public class ZetaPepJwtSteps {

  private String getPoppServerUrl() {
    return TigerGlobalConfiguration.resolvePlaceholders("${zeta.server.popp.url}");
  }

  private String getZetaPdpTokenUrl() {
    return TigerGlobalConfiguration.resolvePlaceholders("${zeta.server.pdp.tokenUrl}");
  }

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

  @Wenn("Hole JWT für Client {string} von {string} und speichere in der Variable {string}")
  @When("fetch JWT for client {string} from {string} and store it in variable {string}")
  public void fetchJwtForClientAndStore(String clientId, String tokenEndpoint, String varName) {
    String resolved = TigerGlobalConfiguration.resolvePlaceholders(tokenEndpoint);
    URI uri = URI.create(resolved);

    RestTemplate rt = new RestTemplate();

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    form.add("requested_token_type", "urn:ietf:params:oauth:token-type:access_token");

    String subjectToken =
        ZetaPdpSubjectTokenFactory.createSubjectToken(clientId, "1.2.276.0.76.4.49");
    form.add("subject_token", subjectToken);
    form.add("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");

    ResponseEntity<String> response = rt.postForEntity(uri, form, String.class);

    TigerGlobalConfiguration.putValue(varName, response.getBody());
    TigerGlobalConfiguration.putValue(varName + "_status", response.getStatusCode().value());
  }

  @Dann("prüfe dass {string} ein access_token enthält")
  @Then("verify that {string} contains an access_token")
  public void verifyResponseContainsAccessToken(String responseBody) {
    String resolved = TigerGlobalConfiguration.resolvePlaceholders(responseBody);
    assertThat(resolved).as("Response sollte ein access_token enthalten").contains("access_token");
  }

  @Wenn("erzeuge PoPP-Token über den PoPP-Server {string}")
  @When("generate PoPP-Token via PoPP-Server {string}")
  public void generatePoppToken(String poppServerBaseUrl) {
    String resolved = TigerGlobalConfiguration.resolvePlaceholders(poppServerBaseUrl);
    URI uri = URI.create(resolved + "/popp/test/api/v1/token-generator");

    long now = System.currentTimeMillis() / 1000;
    String requestBody =
        """
        {
          "tokenParamsList": [{
            "proofMethod": "ehc-practitioner-trustedchannel",
            "patientProofTime": "%d",
            "iat": "%d",
            "patientId": "X123456789",
            "insurerId": "123456789",
            "actorId": "1-2012345678",
            "actorProfessionOid": "1.2.276.0.76.4.50"
          }]
        }
        """
            .formatted(now, now);

    RestTemplate rt = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

    var response = rt.postForEntity(uri, request, String.class);
    assertThat(response.getStatusCode().is2xxSuccessful())
        .as("PoPP-Server should return 2xx for token generation")
        .isTrue();

    String token = extractTokenFromGeneratorResponse(response.getBody());
    log.info("PoPP-Token erzeugt: {}...", token.substring(0, Math.min(50, token.length())));

    TigerGlobalConfiguration.putValue("POPP_TOKEN", token);
  }

  @Wenn(
      "sende GET an PEP {string} mit AccessToken {tigerResolvedString} und PoPP-Token"
          + " {tigerResolvedString}")
  @When(
      "send GET to PEP {string} with AccessToken {tigerResolvedString} and PoPP-Token"
          + " {tigerResolvedString}")
  public void sendGetToPepWithAccessTokenAndPoppToken(
      String pepUrl, String tokenResponseJson, String poppToken) {
    String resolved = TigerGlobalConfiguration.resolvePlaceholders(pepUrl);
    URI uri = URI.create(resolved);

    String accessToken = extractAccessToken(tokenResponseJson);

    RestTemplate rt = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    headers.set("PoPP", poppToken);

    HttpEntity<Void> request = new HttpEntity<>(headers);
    rt.exchange(uri, HttpMethod.GET, request, String.class);
  }

  /**
   * Kombinierter Step: erzeugt PoPP-Token, holt Access-Token vom PDP via Token-Exchange und sendet
   * eine GET-Anfrage mit Authorization + PoPP-Header an den PEP. Der PEP leitet die Anfrage an das
   * Backend weiter, wobei der Tiger-Proxy den Traffic mitschneidet.
   */
  @Wenn("sende Ressourcen-Anfrage mit PoPP-Token über PEP an {string}")
  @When("send resource request with PoPP-Token via PEP to {string}")
  public void sendResourceRequestWithPoppTokenViaPep(String pepUrl) {
    String resolvedPepUrl = TigerGlobalConfiguration.resolvePlaceholders(pepUrl);

    // 1. PoPP-Token erzeugen
    generatePoppToken(getPoppServerUrl());
    String poppToken = TigerGlobalConfiguration.resolvePlaceholders("${POPP_TOKEN}");

    // 2. Access-Token via Token-Exchange vom PDP holen
    fetchJwtForClientAndStore("zeta-client", getZetaPdpTokenUrl(), "tokenResponse");
    String tokenResponse = TigerGlobalConfiguration.resolvePlaceholders("${tokenResponse}");
    String accessToken = extractAccessToken(tokenResponse);

    // 3. GET-Request über den Tiger-Proxy an den PEP senden
    int proxyPort =
        Integer.parseInt(
            TigerGlobalConfiguration.resolvePlaceholders("${ports.localTigerProxyProxyPort}"));

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setProxy(
        new java.net.Proxy(
            java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("127.0.0.1", proxyPort)));

    RestTemplate rt = new RestTemplate(factory);
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    headers.set("PoPP", poppToken);

    HttpEntity<Void> request = new HttpEntity<>(headers);
    log.info("Sending resource request with PoPP-Token to PEP: {}", resolvedPepUrl);
    rt.exchange(resolvedPepUrl, HttpMethod.GET, request, String.class);
  }

  private String extractAccessToken(String tokenResponseJson) {
    try {
      JsonNode node = new ObjectMapper().readTree(tokenResponseJson);
      JsonNode atNode = node.get("access_token");
      if (atNode == null || atNode.isNull()) {
        throw new AssertionError(
            "Token response does not contain 'access_token': " + tokenResponseJson);
      }
      return atNode.asText();
    } catch (JsonProcessingException e) {
      throw new AssertionError("Failed to parse token response JSON: " + e.getMessage(), e);
    }
  }

  private String extractTokenFromGeneratorResponse(String responseBody) {
    try {
      JsonNode node = new ObjectMapper().readTree(responseBody);
      JsonNode tokenResults = node.get("tokenResults");
      if (tokenResults == null || !tokenResults.isArray() || tokenResults.isEmpty()) {
        throw new AssertionError(
            "PoPP-Server response does not contain 'tokenResults': " + responseBody);
      }
      return tokenResults.get(0).asText();
    } catch (JsonProcessingException e) {
      throw new AssertionError(
          "Failed to parse PoPP-Server token generator response: " + e.getMessage(), e);
    }
  }
}
