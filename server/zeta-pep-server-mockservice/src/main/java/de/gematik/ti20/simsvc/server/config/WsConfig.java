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
package de.gematik.ti20.simsvc.server.config;

import de.gematik.ti20.simsvc.server.controller.WsProxyController;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@Order(1)
public class WsConfig implements WebSocketConfigurer {

  private final String targetUrl;
  private final WsProxyController wsProxyController;

  public WsConfig(HttpConfig httpConfig, WsProxyController wsProxyController) {
    this.targetUrl = httpConfig.getUrl();
    this.wsProxyController = wsProxyController;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    wsProxyController.addHandlers(registry);
  }
}
