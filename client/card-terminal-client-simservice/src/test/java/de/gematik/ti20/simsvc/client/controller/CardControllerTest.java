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
package de.gematik.ti20.simsvc.client.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.dto.*;
import de.gematik.ti20.simsvc.client.service.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class CardControllerTest {

  private CardManager cardManager;
  private SignatureService signatureService;
  private SmcBInfoService smcBInfoService;
  private EgkInfoService egkInfoService;
  private CardController controller;

  @BeforeEach
  void setUp() {
    cardManager = mock(CardManager.class);
    signatureService = mock(SignatureService.class);
    smcBInfoService = mock(SmcBInfoService.class);
    egkInfoService = mock(EgkInfoService.class);
    controller = new CardController(cardManager, signatureService, smcBInfoService, egkInfoService);
  }

  @Test
  void listCards_returnsCardHandles() {
    List<CardHandleDto> handles =
        List.of(
            new CardHandleDto("id1", "EGK", 1, "label1"),
            new CardHandleDto("id2", "EGK", 2, "label2"));
    when(cardManager.listAllCards()).thenReturn(handles);

    ResponseEntity<List<CardHandleDto>> response = controller.listCards();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(handles, response.getBody());
  }

  @Test
  void connect_returnsConnectionProperties() {
    ConnectionPropertiesDto props = new ConnectionPropertiesDto();
    when(cardManager.connectToCard("handle")).thenReturn(props);

    ResponseEntity<ConnectionPropertiesDto> response = controller.connect("handle");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(props, response.getBody());
  }

  @Test
  void transmit_normalCommand_delegatesToCardManager() {
    Map<String, String> req = Map.of("command", "00A40400");
    TransmitResponseDto transmitResponse = new TransmitResponseDto("9000", "9000", "OK", "9000");
    when(cardManager.transmitCommand("handle", "00A40400")).thenReturn(transmitResponse);

    ResponseEntity<TransmitResponseDto> response = controller.transmit("handle", req);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(transmitResponse, response.getBody());
  }

  @Test
  void transmit_missingCommand_throwsBadRequest() {
    Map<String, String> req = new HashMap<>();
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.transmit("h", req));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getStatusCode());
  }

  @Test
  void sign_success() throws Exception {
    SignRequestDto req = new SignRequestDto();
    SignResponseDto resp = new SignResponseDto();
    when(signatureService.signData("h", req)).thenReturn(resp);

    ResponseEntity<SignResponseDto> response = controller.sign("h", req);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(resp, response.getBody());
  }

  @Test
  void sign_illegalArgumentExceptionWithSHA1_returnsBadRequest() throws Exception {
    SignRequestDto req = new SignRequestDto();
    when(signatureService.signData(any(), any()))
        .thenThrow(new IllegalArgumentException("SHA1 not allowed"));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.sign("h", req));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getStatusCode());
  }

  @Test
  void disconnect_callsCardManager() {
    ResponseEntity<Void> response = controller.disconnect("h");
    verify(cardManager).disconnectCard("h");
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void getSmcBInfo_returnsInfo() {
    SmcBInfoDto info = new SmcBInfoDto();
    when(smcBInfoService.extractSmcBInfo("h")).thenReturn(info);

    ResponseEntity<SmcBInfoDto> response = controller.getSmcBInfo("h");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(info, response.getBody());
  }

  @Test
  void getCertificate_success() throws Exception {
    Map<String, String> req = Map.of("keyType", "AUT");
    Map<String, String> cert = Map.of("cert", "data");
    when(signatureService.getCertificate("h", "AUT")).thenReturn(cert);

    ResponseEntity<Map<String, String>> response = controller.getCertificate("h", req);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(cert, response.getBody());
  }

  @Test
  void getCertificate_exception_returnsBadRequest() throws Exception {
    Map<String, String> req = Map.of();
    when(signatureService.getCertificate(any(), any())).thenThrow(new RuntimeException("fail"));

    ResponseEntity<Map<String, String>> response = controller.getCertificate("h", req);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().get("error").contains("Certificate retrieval failed"));
  }

  @Test
  void getEgkInfo_cardNotFound_returnsNotFound() {
    when(cardManager.findCardByHandle("h")).thenReturn(null);

    ResponseEntity<?> response = controller.getEgkInfo("h");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertTrue(((Map<?, ?>) response.getBody()).containsKey("error"));
  }
}
