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
import de.gematik.ti20.simsvc.server.service.HttpProxyService;
import de.gematik.ti20.simsvc.server.service.TokenService;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class WsProxyControllerTest {

  private WsProxyController wsProxyController;
  private HttpConfig httpConfig;
  private TokenService tokenService;
  private HttpProxyService httpProxyService;

  @BeforeEach
  void setUp() {
    httpConfig = mock(HttpConfig.class);
    tokenService = mock(TokenService.class);
    httpProxyService = mock(HttpProxyService.class);
    wsProxyController = new WsProxyController(httpConfig, tokenService, httpProxyService);
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
}
