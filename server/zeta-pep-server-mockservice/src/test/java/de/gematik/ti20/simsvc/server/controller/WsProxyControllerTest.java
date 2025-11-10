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
// Datei: src/test/java/de/gematik/ti20/simsvc/server/controller/WsProxyControllerTest.java
package de.gematik.ti20.simsvc.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.config.HttpConfig;
import de.gematik.ti20.simsvc.server.service.TokenService;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

class WsProxyControllerTest {

  private WsProxyController wsProxyController;
  private HttpConfig httpConfig;
  private TokenService tokenService;
  private WsHandshakeInterceptor wsHandshakeInterceptor;

  @BeforeEach
  void setUp() {
    httpConfig = mock(HttpConfig.class);
    tokenService = mock(TokenService.class);
    wsHandshakeInterceptor = mock(WsHandshakeInterceptor.class);
    wsProxyController = new WsProxyController(httpConfig, tokenService, wsHandshakeInterceptor);
  }

  @Test
  void testCreateTargetUrl() throws Exception {
    WebSocketSession session = mock(WebSocketSession.class);
    when(httpConfig.getUrl()).thenReturn("http://backend.local/api");
    when(session.getUri()).thenReturn(new URI("/ws/test"));

    var method =
        WsProxyController.class.getDeclaredMethod("createTargetUrl", WebSocketSession.class);
    method.setAccessible(true);
    String result = (String) method.invoke(wsProxyController, session);

    assertEquals("ws://backend.local/api/ws/test", result);
  }

  @Test
  void thatTransportErrorsCloseConnection() throws IOException {
    final WebSocketSession mock = mock(WebSocketSession.class);
    wsProxyController.handleTransportError(mock, new RuntimeException());
    verify(mock, times(1)).close(any());
  }

  @Test
  void thatAfterConnectionClosedDoesNotRaiseException() throws Exception {
    final WebSocketSession mock = mock(WebSocketSession.class);
    final CloseStatus noReason = new CloseStatus(CloseStatus.NORMAL.getCode(), "No reason");
    wsProxyController.afterConnectionClosed(mock, noReason);
  }

  @Test
  void thatHandleTextMessageDoesNotRaiseException() throws Exception {
    final WebSocketSession mock = mock(WebSocketSession.class);
    wsProxyController.handleTextMessage(mock, new TextMessage(""));
  }

  @Test
  void thatAfterConnectionEstablishedDoesNotThrowException() throws Exception {
    final WebSocketSession mock = mock(WebSocketSession.class);
    when(httpConfig.getUrl()).thenReturn("ws://example.com/some-path");
    when(mock.getUri()).thenReturn(URI.create("ws://example.com/"));
    when(mock.getAttributes()).thenReturn(Map.of("processedHeaders", new HttpHeaders()));
    wsProxyController.afterConnectionEstablished(mock);
  }

  @Test
  void thatAddHandlersRegistersSelf() {
    final WebSocketHandlerRegistration handlerRegistration =
        mock(WebSocketHandlerRegistration.class);
    when(handlerRegistration.setAllowedOrigins(anyString()))
        .thenReturn(handlerRegistration); // just return self to avoid NPE

    final WebSocketHandlerRegistry mock = mock(WebSocketHandlerRegistry.class);
    when(mock.addHandler(any(WebSocketHandler.class), anyString())).thenReturn(handlerRegistration);

    wsProxyController.addHandlers(mock);
    verify(mock, times(1)).addHandler(wsProxyController, "/ws/**");
  }
}
