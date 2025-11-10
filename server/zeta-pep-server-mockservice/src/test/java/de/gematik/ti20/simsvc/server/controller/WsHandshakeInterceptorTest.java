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
package de.gematik.ti20.simsvc.server.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.server.service.HttpProxyService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.WebSocketHandler;

@ExtendWith(MockitoExtension.class)
class WsHandshakeInterceptorTest {

  @Mock private HttpProxyService mockProxyService;

  private WsHandshakeInterceptor wsHandshakeInterceptor;

  @BeforeEach
  public void setup() {
    wsHandshakeInterceptor = new WsHandshakeInterceptor(mockProxyService);
  }

  @Test
  void thatResponseHeadersAreProcessed() {
    final HttpHeaders processedHeaders = new HttpHeaders();
    processedHeaders.set("some-header", "header-value");
    when(mockProxyService.preprocess(any(HttpHeaders.class))).thenReturn(processedHeaders);

    final ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
    when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());
    final ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);
    final WebSocketHandler mockHandler = mock(WebSocketHandler.class);

    final Map<String, Object> attributes = new HashMap<>(1);
    final boolean isSuccessful =
        wsHandshakeInterceptor.beforeHandshake(mockRequest, mockResponse, mockHandler, attributes);
    assertThat(isSuccessful).isTrue();
    assertThat(attributes).containsEntry("processedHeaders", processedHeaders);
  }

  @Test
  void thatResponseExceptionsAreHandled() {
    when(mockProxyService.preprocess(any(HttpHeaders.class)))
        .thenThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT));

    final ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
    when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());
    final ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);
    final WebSocketHandler mockHandler = mock(WebSocketHandler.class);

    final boolean isSuccessful =
        wsHandshakeInterceptor.beforeHandshake(mockRequest, mockResponse, mockHandler, Map.of());
    assertThat(isSuccessful).isFalse();
    verify(mockResponse, times(1)).setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
  }

  @Test
  void thatHandShakeErrorsFailConnection() {
    when(mockProxyService.preprocess(any(HttpHeaders.class)))
        .thenThrow(new RuntimeException("Exception"));

    final ServerHttpRequest mockRequest = mock(ServerHttpRequest.class);
    when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());
    final ServerHttpResponse mockResponse = mock(ServerHttpResponse.class);
    final WebSocketHandler mockHandler = mock(WebSocketHandler.class);

    final boolean isSuccessful =
        wsHandshakeInterceptor.beforeHandshake(mockRequest, mockResponse, mockHandler, Map.of());
    assertThat(isSuccessful).isFalse();
    verify(mockResponse, times(1)).setStatusCode(HttpStatus.BAD_GATEWAY);
  }
}
