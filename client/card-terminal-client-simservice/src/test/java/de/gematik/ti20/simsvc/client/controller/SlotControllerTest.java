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

import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.dto.CardInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.TransmitRequestDto;
import de.gematik.ti20.simsvc.client.model.dto.TransmitResponseDto;
import de.gematik.ti20.simsvc.client.service.CardImageParser;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class SlotControllerTest {

  private SlotManager slotManager;
  private CardImageParser cardImageParser;
  private SlotController controller;

  @BeforeEach
  void setUp() {
    slotManager = mock(SlotManager.class);
    cardImageParser = mock(CardImageParser.class);
    controller = new SlotController(slotManager, cardImageParser);
  }

  @Test
  void getCardInSlot_returnsCardInfo() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    CardImage card = mock(CardImage.class);
    when(slotManager.getCardInSlot(1)).thenReturn(card);
    when(card.getId()).thenReturn("id1");
    when(card.getCardType()).thenReturn(CardType.EGK);
    when(card.getLabel()).thenReturn("TestCard");

    ResponseEntity<CardInfoDto> response = controller.getCardInSlot(1);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("id1", response.getBody().getCardId());
  }

  @Test
  void getCardInSlot_slotNotFound_throws() {
    when(slotManager.isValidSlotId(2)).thenReturn(false);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.getCardInSlot(2));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), ex.getStatusCode());
  }

  @Test
  void getCardInSlot_noCard_returnsNoContent() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.getCardInSlot(1)).thenReturn(null);
    ResponseEntity<CardInfoDto> response = controller.getCardInSlot(1);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void insertCard_success() throws Exception {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    CardImage card = mock(CardImage.class);
    when(cardImageParser.parseCardImage(anyString())).thenReturn(card);
    when(card.getId()).thenReturn("id2");
    when(card.getCardType()).thenReturn(CardType.SMCB);
    when(card.getLabel()).thenReturn("Label");
    when(slotManager.insertCard(0, card)).thenReturn(true);

    ResponseEntity<CardInfoDto> response = controller.insertCard(0, "<xml/>");
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("id2", response.getBody().getCardId());
  }

  @Test
  void insertCard_slotNotFound_throws() {
    when(slotManager.isValidSlotId(5)).thenReturn(false);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertCard(5, "<xml/>"));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), ex.getStatusCode());
  }

  @Test
  void insertCard_slotOccupied_throws() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(true);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertCard(1, "<xml/>"));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()), ex.getStatusCode());
  }

  @Test
  void insertCard_invalidXml_throwsBadRequest() throws Exception {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(false);
    when(cardImageParser.parseCardImage(anyString()))
        .thenThrow(new RuntimeException("Parse error"));
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertCard(1, "<bad/>"));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getStatusCode());
    assertEquals(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getStatusCode());
  }

  @Test
  void removeCard_success() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    ResponseEntity<Void> response = controller.removeCard(0);
    verify(slotManager).removeCard(0);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  void removeCard_slotNotFound_throws() {
    when(slotManager.isValidSlotId(3)).thenReturn(false);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.removeCard(3));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), ex.getStatusCode());
  }

  @Test
  void removeCard_noCardPresent_throws() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(false);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.removeCard(1));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), ex.getStatusCode());
  }

  @Test
  void transmitToCardInSlot_success() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    CardImage card = mock(CardImage.class);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("id3");
    TransmitRequestDto req = new TransmitRequestDto();
    req.setCommand("00A40400");
    TransmitResponseDto resp = new TransmitResponseDto("9000", "9000", "OK", "9000");
    when(slotManager.transmitCommand(0, "00A40400")).thenReturn(resp);

    ResponseEntity<TransmitResponseDto> response = controller.transmitToCardInSlot(0, req);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(resp, response.getBody());
  }

  @Test
  void transmitToCardInSlot_slotNotFound_throws() {
    when(slotManager.isValidSlotId(2)).thenReturn(false);
    TransmitRequestDto req = new TransmitRequestDto();
    req.setCommand("00A40400");
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.transmitToCardInSlot(2, req));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), ex.getStatusCode());
  }

  @Test
  void transmitToCardInSlot_noCardPresent_throws() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(false);
    TransmitRequestDto req = new TransmitRequestDto();
    req.setCommand("00A40400");
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.transmitToCardInSlot(1, req));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), ex.getStatusCode());
  }

  @Test
  void transmitToCardInSlot_transmitFails_throwsBadRequest() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    CardImage card = mock(CardImage.class);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("id3");
    when(slotManager.transmitCommand(0, "BAD")).thenThrow(new RuntimeException("fail"));
    TransmitRequestDto req = new TransmitRequestDto();
    req.setCommand("BAD");
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.transmitToCardInSlot(0, req));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getStatusCode());
  }
}
