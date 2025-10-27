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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.service.SmartcardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.*;

class SmartcardControllerTest {

  private SmartcardService smartcardService;
  private SmartcardController controller;

  @BeforeEach
  void setUp() {
    smartcardService = mock(SmartcardService.class);
    controller = new SmartcardController(smartcardService);
  }

  @Test
  void testConstructorInitializesService() throws Exception {
    var field = SmartcardController.class.getDeclaredField("smartcardService");
    field.setAccessible(true);
    assertEquals(smartcardService, field.get(controller));
  }

  @Test
  void testAfterConnectionEstablishedDelegatesToService() throws Exception {
    WebSocketSession session = mock(WebSocketSession.class);
    controller.afterConnectionEstablished(session);
    verify(smartcardService).onConnectionEstablished(session);
  }

  @Test
  void testHandleTextMessageDelegatesToService() throws Exception {
    WebSocketSession session = mock(WebSocketSession.class);
    TextMessage message = new TextMessage("test");
    controller.handleTextMessage(session, message);
    verify(smartcardService).onMessage(session, message);
  }

  @Test
  void testAfterConnectionClosedDelegatesToService() throws Exception {
    WebSocketSession session = mock(WebSocketSession.class);
    CloseStatus status = CloseStatus.NORMAL;
    controller.afterConnectionClosed(session, status);
    verify(smartcardService).onConnectionClosed(session, status);
  }
}
