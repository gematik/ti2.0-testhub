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

import de.gematik.ti20.simsvc.server.service.HttpProxyService;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {
  private final HttpProxyService httpProxyService;

  public WsHandshakeInterceptor(final HttpProxyService proxyService) {
    httpProxyService = proxyService;
  }

  @Override
  public boolean beforeHandshake(
      final ServerHttpRequest request,
      final ServerHttpResponse response,
      final WebSocketHandler wsHandler,
      final Map<String, Object> attributes) {
    try {
      final HttpHeaders processedHeaders = httpProxyService.preprocess(request.getHeaders());
      attributes.put("processedHeaders", processedHeaders);
      return true;
    } catch (ResponseStatusException e) {
      log.warn("Websocket request has been rejected with {} {}", e.getStatusCode(), e.getMessage());
      response.setStatusCode(e.getStatusCode());
      return false;
    } catch (Exception e) {
      log.error("Error on handshake for websocket", e);
      response.setStatusCode(HttpStatus.BAD_GATEWAY);
      return false;
    }
  }

  @Override
  public void afterHandshake(
      final ServerHttpRequest request,
      final ServerHttpResponse response,
      final WebSocketHandler wsHandler,
      final Exception exception) {
    log.debug(
        "Websocket afterHandshake {}", exception != null ? exception.getMessage() : "no exception");
  }
}
