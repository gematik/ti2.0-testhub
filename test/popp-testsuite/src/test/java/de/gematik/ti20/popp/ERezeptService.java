/*-
 * #%L
 * PoPP Testsuite
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
package de.gematik.ti20.popp;

import de.gematik.test.tiger.common.config.TigerTypedConfigurationKey;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Slf4j
public class ERezeptService {

  private final String SERVER_URL = "https://erpps-test.dev.gematik.solutions/tu";

  private final String API_KEY =
      Optional.ofNullable(System.getProperty("popp.primSys.apikey"))
          .filter(v -> !"replace_me".equals(v))
          .orElseThrow(
              () ->
                  new RuntimeException("Missing API Key for E-Prescription (popp.primSys.apikey)"));

  public static final TigerTypedConfigurationKey<String> POPP_TOKEN =
      new TigerTypedConfigurationKey<>("popp.value.token", String.class);

  public ERezeptService() {}

  public void prescribeForKvnr(String kvnr) {
    validateKvnr(kvnr);

    ObjectNode prescription = JsonNodeFactory.instance.objectNode();
    prescription.putObject("patient").put("kvnr", kvnr);
    log.info("prescription: {}", prescription);

    String docId =
        Optional.ofNullable(System.getProperty("popp.primSys.DocId"))
            .filter(v -> !"replace_me".equals(v))
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Missing Doctor ID for E-Prescription (popp.primSys.DocId)"));

    String url = removeTrailingSlash(SERVER_URL) + "/doc/" + docId + "/prescribe";
    log.info("url: {}", url);

    HttpPost request = new HttpPost(url);

    request.setHeader("apikey", API_KEY);
    request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
    request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
    request.setEntity(new StringEntity(prescription.toString(), ContentType.APPLICATION_JSON));

    SimpleHttpResponse response = execute(request);

    validateResponse(response, 202, "Das E-Rezept konnte nicht eingestellt werden.");
  }

  public void getPrescriptionsByPoppToken() {
    try {
      if (POPP_TOKEN.getValue().isEmpty()) {
        throw new RuntimeException("Empty PoPP-Token");
      }
      log.info("PoppToken: {}", POPP_TOKEN.getValue().get());
      String pharmId =
          Optional.ofNullable(System.getProperty("popp.primSys.PharmId"))
              .filter(v -> !"replace_me".equals(v))
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "Missing Pharma ID for E-Prescription (popp.primSys.PharmId)"));
      URI uri =
          new URIBuilder(removeTrailingSlash(SERVER_URL) + "/pharm/" + pharmId + "/withPoppToken")
              .addParameter("poppToken", POPP_TOKEN.getValue().get())
              .build();

      HttpGet request = new HttpGet(uri);

      request.setHeader("apikey", API_KEY);
      request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

      SimpleHttpResponse response = execute(request);

      validateResponse(
          response, 200, "Die E-Rezepte konnten nicht über den PoPP-Token " + "abgerufen werden.");

    } catch (ERezeptException e) {
      throw e;
    } catch (Exception e) {
      throw new ERezeptException(
          "Die URL für den Abruf der E-Rezepte " + "konnte nicht erstellt werden.", e);
    }
  }

  public void getRestServerInformation() {
    String url = removeTrailingSlash(SERVER_URL) + "/info";

    HttpGet request = new HttpGet(url);

    request.setHeader("apikey", API_KEY);
    request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

    SimpleHttpResponse response = execute(request);

    validateResponse(response, 200, "Die Serverinformationen konnten nicht abgerufen werden.");

    log.info(response.body());

    if (!response.body().contains("doctors")) {
      throw new ERezeptException("Die Serverantwort enthält keine Ärzte.");
    }

    getRestServerActors();
  }

  public void getRestServerActors() {
    String url = removeTrailingSlash(SERVER_URL) + "/actors";

    HttpGet request = new HttpGet(url);

    request.setHeader("apikey", API_KEY);
    request.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

    SimpleHttpResponse response = execute(request);

    validateResponse(response, 200, "Die Serverinformationen konnten nicht abgerufen werden.");

    log.info(response.body());
  }

  /**
   * Führt genau einen Request mit einem isolierten HTTP-Client aus.
   *
   * <p>Andere HTTP-Aufrufe innerhalb der Anwendung werden nicht verändert.
   */
  private SimpleHttpResponse execute(HttpRequest request) {
    try (CloseableHttpClient httpClient = createNoProxyHttpClient();
        CloseableHttpResponse response =
            httpClient.execute((org.apache.http.client.methods.HttpUriRequest) request)) {
      int status = response.getStatusLine().getStatusCode();

      String body =
          response.getEntity() == null
              ? null
              : EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

      return new SimpleHttpResponse(status, body);
    } catch (IOException e) {
      throw new ERezeptException("Der ERPPS-Testserver konnte nicht aufgerufen werden.", e);
    }
  }

  /**
   * Erstellt einen isolierten Apache-HTTP-Client.
   *
   * <p>Dieser Client: - akzeptiert alle Serverzertifikate - prüft den Hostnamen nicht - verwendet
   * niemals einen Proxy - verändert keine globale Unirest-Konfiguration
   */
  private CloseableHttpClient createNoProxyHttpClient() {
    try {
      SSLContext sslContext =
          SSLContexts.custom()
              .loadTrustMaterial(null, (certificateChain, authenticationType) -> true)
              .build();

      SSLConnectionSocketFactory socketFactory =
          new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

      /*
       * Erzwingt für jedes Ziel eine direkte Route.
       * Es wird ausdrücklich kein Proxy in die Route eingetragen.
       */
      HttpRoutePlanner noProxyRoutePlanner = (target, request, context) -> new HttpRoute(target);

      return HttpClients.custom()
          .setSSLSocketFactory(socketFactory)
          .setRoutePlanner(noProxyRoutePlanner)
          .disableCookieManagement()
          .build();
    } catch (Exception e) {
      throw new ERezeptException("Der HTTP-Client ohne Proxy konnte nicht erstellt werden.", e);
    }
  }

  private void validateResponse(SimpleHttpResponse response, int expectedStatus, String message) {
    if (response == null) {
      throw new ERezeptException(message + " Der Server hat keine Antwort geliefert.");
    }

    if (response.status() != expectedStatus) {
      throw new ERezeptException(
          message
              + " Erwarteter HTTP-Status: "
              + expectedStatus
              + ", tatsächlicher HTTP-Status: "
              + response.status()
              + ", Response: "
              + response.body());
    }

    if (response.body() == null || response.body().isBlank()) {
      throw new ERezeptException(message + " Der Response-Body ist leer.");
    }
  }

  private static void validateKvnr(String kvnr) {
    if (kvnr == null || !kvnr.matches("[A-Z][0-9]{9}")) {
      throw new IllegalArgumentException("Ungültiges KVNR-Format: " + maskKvnr(kvnr));
    }
  }

  private static String removeTrailingSlash(String value) {
    String result = value;

    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }

    return result;
  }

  private static String maskKvnr(String kvnr) {
    if (kvnr == null || kvnr.length() < 4) {
      return "***";
    }

    return kvnr.charAt(0) + "******" + kvnr.substring(kvnr.length() - 3);
  }

  private record SimpleHttpResponse(int status, String body) {}

  public static class ERezeptException extends RuntimeException {

    public ERezeptException(String message) {
      super(message);
    }

    public ERezeptException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
