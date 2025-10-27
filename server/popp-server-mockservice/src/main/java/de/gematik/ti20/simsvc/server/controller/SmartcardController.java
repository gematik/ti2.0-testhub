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

import de.gematik.ti20.simsvc.server.service.SmartcardService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class SmartcardController extends TextWebSocketHandler {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final SmartcardService smartcardService;

  public SmartcardController(final SmartcardService smartcardService) {
    this.smartcardService = smartcardService;
  }

  public void addHandlers(final WebSocketHandlerRegistry registry) {

    registry
        .addHandler(this, "/ws/popp/practitioner/api/v1/token-generation-ehc")
        .setAllowedOrigins("*")
        .addInterceptors(
            new HandshakeInterceptor() {
              @Override
              public boolean beforeHandshake(
                  ServerHttpRequest request,
                  ServerHttpResponse response,
                  WebSocketHandler wsHandler,
                  Map<String, Object> attributes) {
                log.debug("Websocket request for SmartcardController has been accepted.");
                return true;
              }

              @Override
              public void afterHandshake(
                  ServerHttpRequest request,
                  ServerHttpResponse response,
                  WebSocketHandler wsHandler,
                  Exception exception) {
                log.debug("Websocket request for SmartcardController has been handled.");
              }
            });
  }

  @Override
  public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
    log.debug("Websocket session has been established.");
    smartcardService.onConnectionEstablished(session);
  }

  @Override
  protected void handleTextMessage(final WebSocketSession session, final TextMessage message)
      throws Exception {
    log.debug("Websocket message has been received: {}", message.getPayload());
    smartcardService.onMessage(session, message);
  }

  @Override
  public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status)
      throws Exception {
    log.debug("Websocket session has been closed.");
    smartcardService.onConnectionClosed(session, status);
  }
}
