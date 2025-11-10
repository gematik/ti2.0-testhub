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

import de.gematik.ti20.simsvc.server.config.HttpConfig;
import de.gematik.ti20.simsvc.server.service.TokenService;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Controller
@Order(1)
public class WsProxyController extends TextWebSocketHandler {

  private final HttpConfig httpConfig;
  private final TokenService tokenService;
  private final Map<WebSocketSession, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
  private final Map<WebSocketSession, WebSocketSession> reverseSessionMap =
      new ConcurrentHashMap<>();
  private final WebSocketClient webSocketClient;
  private HandshakeInterceptor handshakeInterceptor;

  public WsProxyController(
      HttpConfig httpConfig,
      TokenService tokenService,
      WsHandshakeInterceptor wsHandshakeInterceptor) {
    this.httpConfig = httpConfig;
    this.tokenService = tokenService;
    this.webSocketClient = new StandardWebSocketClient();
    this.handshakeInterceptor = wsHandshakeInterceptor;
  }

  public void addHandlers(final WebSocketHandlerRegistry registry) {
    registry
        .addHandler(this, "/ws/**")
        .setAllowedOrigins("*")
        .addInterceptors(handshakeInterceptor);
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession clientSession) throws Exception {
    log.debug("New WebSocket connection established: {}", clientSession.getId());

    final HttpHeaders processedHeaders =
        (HttpHeaders) clientSession.getAttributes().get("processedHeaders");
    final String targetUrl = createTargetUrl(clientSession);

    log.debug("Connecting to backend WebSocket at URL: {}", targetUrl);
    log.debug("Processed headers for backend connection: {}", processedHeaders);

    connectToBackend(clientSession, targetUrl, processedHeaders);
  }

  @Override
  protected void handleTextMessage(WebSocketSession clientSession, TextMessage message)
      throws Exception {
    log.debug("Received message from client {}: {}", clientSession.getId(), message.getPayload());

    final WebSocketSession backendSession = sessionMap.get(clientSession);
    if (backendSession != null && backendSession.isOpen()) {
      // Zusätzliche Header hinzufügen, ähnlich wie im HTTP-Proxy
      enrichMessage(message);
      backendSession.sendMessage(message);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    log.debug("WebSocket connection closed: {} with status: {}", session.getId(), status);
    closeBackendConnection(session, status);
  }

  private String createTargetUrl(WebSocketSession session) {
    final String targetUrl = httpConfig.getUrl().replace("http", "ws");
    return targetUrl + session.getUri().getPath();
  }

  private void connectToBackend(
      final WebSocketSession clientSession, final String targetUrl, final HttpHeaders httpHeaders) {
    try {
      WebSocketHandler backendHandler =
          new TextWebSocketHandler() {
            @Override
            public void handleMessage(WebSocketSession backendSession, WebSocketMessage<?> message)
                throws Exception {
              log.debug(
                  "Received message from client {}: {}",
                  clientSession.getId(),
                  message.getPayload());

              if (clientSession.isOpen()) {
                clientSession.sendMessage(message);
              } else {
                log.warn("Client session {} is closed, cannot send message", clientSession.getId());
              }
            }

            @Override
            public void afterConnectionEstablished(WebSocketSession backendSession) {
              sessionMap.put(clientSession, backendSession);
              reverseSessionMap.put(backendSession, clientSession);
              log.debug(
                  "Backend connection established for client session: {}", clientSession.getId());
            }

            @Override
            public void handleTransportError(WebSocketSession backendSession, Throwable exception) {
              log.error(
                  "Backend transport error for session {}", backendSession.getId(), exception);
              closeClientConnection(
                  reverseSessionMap.get(backendSession),
                  new CloseStatus(1011, "Backend transport error"));
            }

            @Override
            public void afterConnectionClosed(WebSocketSession backendSession, CloseStatus status) {
              log.debug(
                  "Backend connection closed for session {} with status {}",
                  backendSession.getId(),
                  status);

              WebSocketSession clientToClose = reverseSessionMap.remove(backendSession);
              if (clientToClose != null) {
                sessionMap.remove(clientToClose);
                closeClientConnection(clientToClose, status);
              }
            }
          };

      log.debug("Creating WebSocket connection manager for target URL: {}", targetUrl);

      WebSocketConnectionManager connectionManager =
          new WebSocketConnectionManager(webSocketClient, backendHandler, targetUrl);

      log.debug("Setting HTTP Header for connection manager: {}", httpHeaders);

      connectionManager.setHeaders(httpHeaders);
      connectionManager.start();
      Thread.sleep(100);

    } catch (final Exception e) {
      log.error("Failed to connect to backend", e);
      closeClientConnection(clientSession, new CloseStatus(1011, "Failed to connect to backend"));
    }
  }

  private void enrichMessage(TextMessage message) {
    // Ähnlich wie im HTTP-Proxy zusätzliche Informationen hinzufügen
  }

  private void closeBackendConnection(WebSocketSession clientSession, CloseStatus status) {
    WebSocketSession backendSession = sessionMap.remove(clientSession);
    if (backendSession != null && backendSession.isOpen()) {
      try {
        backendSession.close(status);
      } catch (IOException e) {
        log.error("Error closing backend connection", e);
      }
    }
  }

  private void closeClientConnection(WebSocketSession session, CloseStatus status) {
    try {
      session.close(status);
    } catch (IOException e) {
      log.error("Error closing client connection", e);
    }
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.error("WebSocket transport error for session {}", session.getId(), exception);
    closeClientConnection(session, new CloseStatus(1011, "Transport error occurred"));
  }
}
