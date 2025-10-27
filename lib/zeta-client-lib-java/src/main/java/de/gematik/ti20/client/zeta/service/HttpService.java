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
package de.gematik.ti20.client.zeta.service;

import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.exception.ZetaHttpResponseException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest.HeaderName;
import de.gematik.ti20.client.zeta.request.ZetaWsRequest;
import de.gematik.ti20.client.zeta.response.ZetaHttpResponse;
import de.gematik.ti20.client.zeta.websocket.ZetaWsEventHandler;
import de.gematik.ti20.client.zeta.websocket.ZetaWsSession;
import java.net.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpService {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  protected final ZetaClientService zetaClientService;
  private final OkHttpClient client;

  public HttpService(ZetaClientService zetaClientService) {
    this.zetaClientService = zetaClientService;
    this.client =
        createClientWithEnvProxy()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build();
  }

  protected OkHttpClient getClient() {
    return client;
  }

  public ZetaHttpResponse send(final ZetaHttpRequest request) throws ZetaHttpException {
    log.debug("Sending message with traceId {} to {}", request.getTraceId(), request.getUrl());
    extendRequest(request);
    final long startTime = System.currentTimeMillis();

    try (final Response response = getClient().newCall(request.build()).execute()) {
      if (response.isSuccessful() || response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
        return new ZetaHttpResponse(response);
      } else {
        throw new ZetaHttpResponseException(response.code(), response.body().string(), request);
      }
    } catch (final ZetaHttpResponseException e) {
      if (e.getCode() != 401) {
        log.error("Request failed with code {} and message '{}'", e.getCode(), e.getMessage(), e);
      }
      throw e;
    } catch (final SocketTimeoutException e) {
      long elapsedTime = System.currentTimeMillis() - startTime;
      log.error(
          "Timeout after {} ms for request with traceId {} to {}",
          elapsedTime,
          request.getTraceId(),
          request.getUrl());

      throw new ZetaHttpException("HTTP Request failed: " + e.getMessage(), e, request);
    } catch (final Exception e) {
      throw new ZetaHttpException("HTTP Request failed: " + e.getMessage(), e, request);
    }
  }

  public ZetaWsSession connect(final ZetaWsRequest request, final ZetaWsEventHandler eventHandler)
      throws ZetaHttpResponseException {
    log.debug("Opening connection with traceId {} to {}", request.getTraceId(), request.getUrl());

    extendRequest(request);

    ZetaWsSession wsSession = new ZetaWsSession(request, eventHandler);
    wsSession.open(client);

    return wsSession;
  }

  private void extendRequest(final ZetaHttpRequest request) {
    if (!request.hasHeader(HeaderName.UserAgent.toString())) {
      request.setHeaderUserAgent(this.zetaClientService.getZetaClientConfig().getUserAgent());
    }
  }

  public static OkHttpClient.Builder createClientWithEnvProxy() {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();

    String proxyUrl = System.getenv("HTTP_PROXY");
    if (proxyUrl == null || proxyUrl.isEmpty()) {
      proxyUrl = System.getenv("http_proxy"); // Check for lowercase as well
    }

    if (proxyUrl != null && !proxyUrl.isEmpty()) {
      try {
        URI proxyUri = new URI(proxyUrl);
        String host = proxyUri.getHost();
        int port = proxyUri.getPort();

        if (host != null && port != -1) {
          builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
        }
      } catch (URISyntaxException e) {
        System.err.println("Invalid proxy URL from environment variable: " + proxyUrl);
        throw new RuntimeException("Invalid proxy URL: " + proxyUrl, e);
      }
    }

    return builder;
  }
}
