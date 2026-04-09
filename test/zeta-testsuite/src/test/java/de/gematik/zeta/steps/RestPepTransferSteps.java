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
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.When;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Step definitions for REST-based data transfer via ZETA-PEP proxy. Provides steps for sending HTTP
 * requests with custom headers and body content. All requests are routed through the local
 * Tiger-Proxy so that traffic is recorded for RBel assertions.
 */
@Slf4j
public class RestPepTransferSteps {

  private RestTemplate restTemplate;

  /**
   * Lazily creates the RestTemplate on first use. This ensures TigerGlobalConfiguration is already
   * initialized when we resolve placeholders.
   */
  private RestTemplate getRestTemplate() {
    if (restTemplate == null) {
      restTemplate = createRestTemplate();
    }
    return restTemplate;
  }

  private RestTemplate createRestTemplate() {
    int timeoutSeconds =
        Integer.parseInt(
            TigerGlobalConfiguration.resolvePlaceholders("${zeta.timeouts.httpRequestSeconds}"));
    Duration httpTimeout = Duration.ofSeconds(timeoutSeconds);

    // Route all requests through the local Tiger-Proxy so RBel can record them
    int proxyPort =
        Integer.parseInt(
            TigerGlobalConfiguration.resolvePlaceholders("${ports.localTigerProxyProxyPort}"));

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", proxyPort)));
    factory.setConnectTimeout((int) httpTimeout.toMillis());
    factory.setReadTimeout((int) httpTimeout.toMillis());

    RestTemplate rt = new RestTemplate(factory);
    rt.setErrorHandler(
        new ResponseErrorHandler() {
          @Override
          public boolean hasError(@NonNull ClientHttpResponse response) {
            return false;
          }

          @Override
          public void handleError(
              @NonNull URI url, @NonNull HttpMethod method, @NonNull ClientHttpResponse response) {
            // No-op: don't throw on 4xx/5xx so we can assert status codes in feature files
          }
        });
    return rt;
  }

  /** Resolves Tiger placeholders in URL strings. */
  private String resolveUrl(String url) {
    return TigerGlobalConfiguration.resolvePlaceholders(url);
  }

  @Wenn("REST sende GET Anfrage an {string} mit Authorization {string}")
  @When("REST send GET request to {string} with Authorization {string}")
  public void sendGetRequestWithAuth(String url, String authorization) {
    String resolvedUrl = resolveUrl(url);
    String resolvedAuth = TigerGlobalConfiguration.resolvePlaceholders(authorization);

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", resolvedAuth);
    headers.setContentType(MediaType.APPLICATION_JSON);

    log.info("Sending GET request to {} with Authorization header", resolvedUrl);

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    getRestTemplate().exchange(resolvedUrl, HttpMethod.GET, entity, String.class);
  }

  @Wenn("REST sende GET Anfrage an {string} ohne Authorization")
  @When("REST send GET request to {string} without Authorization")
  public void sendGetRequestWithoutAuth(String url) {
    String resolvedUrl = resolveUrl(url);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    log.info("Sending GET request to {} without Authorization header", resolvedUrl);

    HttpEntity<Void> entity = new HttpEntity<>(headers);
    getRestTemplate().exchange(resolvedUrl, HttpMethod.GET, entity, String.class);
  }
}
