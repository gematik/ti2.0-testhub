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
package de.gematik.ti20.simsvc.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class SmartcardServiceTest {

  private PoppTokenService tokenService;
  private SmartcardService service;
  private WebSocketSession session;

  @BeforeEach
  void setUp() {
    tokenService = mock(PoppTokenService.class);
    service = new SmartcardService(tokenService);
    session = mock(WebSocketSession.class);
    when(session.getAttributes()).thenReturn(new HashMap<>());
  }

  @Test
  void testOnMessageWithInvalidJsonSendsErrorAndDisconnects() throws Exception {
    TextMessage invalidMessage = new TextMessage("{invalid_json}");
    ArgumentCaptor<TextMessage> textCaptor = ArgumentCaptor.forClass(TextMessage.class);

    service.onMessage(session, invalidMessage);

    verify(session).sendMessage(textCaptor.capture());
    String sentJson = textCaptor.getValue().getPayload();
    assertTrue(sentJson.contains("Failed to parse message"));
    verify(session).close(eq(CloseStatus.BAD_DATA));
  }

  @Test
  void testProcessStartMessage() throws Exception {
    String startMessageJson =
        "{\"type\":\"StartMessage\",\"clientSessionId\":\"test123\",\"cardConnectionType\":\"CT\"}";
    TextMessage message = new TextMessage(startMessageJson);
    ArgumentCaptor<TextMessage> textCaptor = ArgumentCaptor.forClass(TextMessage.class);

    service.onMessage(session, message);

    verify(session).sendMessage(textCaptor.capture());
    String sentJson = textCaptor.getValue().getPayload();
    assertTrue(sentJson.contains("StandardScenario"));
    assertEquals("test123", session.getAttributes().get("clientSessionId"));
    assertEquals("CT", session.getAttributes().get("cardConnectionType"));
  }

  @Test
  void testProcessScenarioResponseWithValidResponse() throws Exception {
    // Setup session attributes
    session.getAttributes().put("clientSessionId", "test123");
    session
        .getAttributes()
        .put(
            "lastSteps",
            List.of(
                new de.gematik.ti20.simsvc.server.model.message.ScenarioStep(
                    "F0EE000000", List.of("9000"))));

    // Mock headers for ZETA-User-Info
    HttpHeaders headers = new HttpHeaders();
    String userInfo =
        Base64.getEncoder()
            .encodeToString(
                "{\"identifier\":\"user123\",\"professionOID\":\"1.2.3.4\"}".getBytes());
    headers.add("ZETA-User-Info", userInfo);
    when(session.getHandshakeHeaders()).thenReturn(headers);

    // Mock token service
    when(tokenService.createToken(any())).thenReturn("mock-token");

    String responseJson =
        "{\"type\":\"ScenarioResponse\",\"steps\":[\"4B564E523A313233343536373839307C494B4E523A313233343536373839309000\"]}";
    TextMessage message = new TextMessage(responseJson);
    ArgumentCaptor<TextMessage> textCaptor = ArgumentCaptor.forClass(TextMessage.class);

    service.onMessage(session, message);

    verify(session).sendMessage(textCaptor.capture());
    String sentJson = textCaptor.getValue().getPayload();
    assertTrue(sentJson.contains("Token"));
    verify(session).close(eq(CloseStatus.NORMAL));
  }

  @Test
  void testProcessErrorMessage() throws Exception {
    String errorMessageJson =
        "{\"type\":\"Error\",\"errorCode\":\"1234\",\"errorDetail\":\"Test error\"}";
    TextMessage message = new TextMessage(errorMessageJson);

    service.onMessage(session, message);

    verify(session).close(eq(CloseStatus.NOT_ACCEPTABLE));
  }

  @Test
  void testUnsupportedMessageType() throws Exception {
    String unsupportedJson = "{\"type\":\"UnsupportedMessage\"}";
    TextMessage message = new TextMessage(unsupportedJson);
    ArgumentCaptor<TextMessage> textCaptor = ArgumentCaptor.forClass(TextMessage.class);

    service.onMessage(session, message);

    verify(session).sendMessage(textCaptor.capture());
    String sentJson = textCaptor.getValue().getPayload();
    assertTrue(sentJson.contains("Error: Unknown BasePoppMessageType: UnsupportedMessage"));
  }
}
