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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.response.ZetaHttpResponse;
import java.net.Proxy;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

class HttpServiceTest {

  private OkHttpClient mockClient;
  private Call mockCall;
  private Response mockResponse;
  private ZetaClientService mockZetaClientService;
  private ZetaClientConfig mockConfig;
  private ZetaClientConfig.UserAgentConfig mockUserAgent;
  private HttpService httpService;

  @BeforeEach
  void setUp() throws Exception {
    mockClient = mock(OkHttpClient.class);
    mockCall = mock(Call.class);
    mockResponse =
        new Response.Builder()
            .request(new Request.Builder().url("https://example.org").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create("body", MediaType.get("text/plain")))
            .build();

    mockZetaClientService = mock(ZetaClientService.class);
    mockConfig = mock(ZetaClientConfig.class);
    mockUserAgent = new ZetaClientConfig.UserAgentConfig("App", "1.0");

    when(mockZetaClientService.getZetaClientConfig()).thenReturn(mockConfig);
    when(mockConfig.getUserAgent()).thenReturn(mockUserAgent);

    // HttpService verwendet eigenen OkHttpClient, daher Subklasse für Injection
    httpService =
        new HttpService(mockZetaClientService) {
          protected OkHttpClient getClient() {
            return mockClient;
          }
        };
  }

  @Test
  void testSendReturnsZetaHttpResponseOnSuccess() throws Exception {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    Request okRequest = new Request.Builder().url("https://example.org").build();

    when(request.getTraceId()).thenReturn("trace-123");
    when(request.getUrl()).thenReturn("https://example.org");
    when(request.hasHeader(anyString())).thenReturn(true);
    when(request.build()).thenReturn(okRequest);

    when(mockClient.newCall(any())).thenReturn(mockCall);
    when(mockCall.execute()).thenReturn(mockResponse);

    ZetaHttpResponse response = httpService.send(request);

    assertEquals(200, response.getStatusCode());
    assertTrue(response.isSuccessful());
  }

  @Test
  void testSendThrowsOnHttpError() throws Exception {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    Request okRequest = new Request.Builder().url("https://example.org").build();

    Response errorResponse =
        new Response.Builder()
            .request(okRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body(ResponseBody.create("not found", MediaType.get("text/plain")))
            .build();

    when(request.getTraceId()).thenReturn("trace-123");
    when(request.getUrl()).thenReturn("https://example.org");
    when(request.hasHeader(anyString())).thenReturn(true);
    when(request.build()).thenReturn(okRequest);

    when(mockClient.newCall(any())).thenReturn(mockCall);
    when(mockCall.execute()).thenReturn(errorResponse);

    assertThrows(Exception.class, () -> httpService.send(request));
  }

  @Test
  void createClientWithEnvProxy_setsProxy_fromHTTP_PROXY() throws Exception {
    new EnvironmentVariables("HTTP_PROXY", "http://local.host:43210")
        .execute(
            () -> {
              OkHttpClient client = HttpService.createClientWithEnvProxy().build();
              Proxy proxy = client.proxy();
              assertNotNull(proxy, "Proxy should be configured");
              assertEquals(
                  "local.host", ((java.net.InetSocketAddress) proxy.address()).getHostString());
              assertEquals(43210, ((java.net.InetSocketAddress) proxy.address()).getPort());
            });
  }
}
