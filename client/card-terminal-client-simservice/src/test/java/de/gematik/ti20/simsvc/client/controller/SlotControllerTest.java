/*-
 * #%L
 * Card Terminal Simulator
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.CardInfoDto;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class SlotControllerTest {

  private SlotManager slotManager;
  private SlotController controller;

  @BeforeEach
  void setUp() {
    slotManager = mock(SlotManager.class);
    controller = new SlotController(slotManager);
  }

  @Test
  void getCardInSlot_returnsCardInfo() {
    final VirtualCardImageData cardImage = mock(VirtualCardImageData.class);
    when(cardImage.cardType()).thenReturn("EGK");

    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.getCardInSlot(1)).thenReturn(cardImage);

    ResponseEntity<CardInfoDto> response = controller.getCardInSlot(1);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("EGK", response.getBody().getLabel());
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
  void insertCard_success() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final VirtualCardImageData cardImage = new VirtualCardImageData(Map.of());
    when(slotManager.insertCard(0, cardImage)).thenReturn(true);

    ResponseEntity<CardInfoDto> response = controller.insertCard(0, "<xml/>");
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("SMCB", response.getBody().getLabel());
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

  // ---------------------------------------------------------------------------
  // insertCardDataJson (JSON endpoint)
  // ---------------------------------------------------------------------------

  @Test
  void insertCardDataJson_success() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);

    String json =
        """
        {
          "kvnr": "X110639491",
          "iknr": "109500969",
          "firstName": "Kriemhild",
          "lastName": "Muster",
          "patientName": "Kriemhild Muster",
          "dateOfBirth": "19900717",
          "insuranceName": "Test GKV-SV",
          "validUntil": "20261231",
          "valid": true
        }
        """;

    ResponseEntity<CardInfoDto> response = controller.insertCardDataJson(0, json);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("egk-X110639491", response.getBody().getLabel());
  }

  @Test
  void insertCardDataJson_invalidJson_throwsBadRequest() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.insertCardDataJson(1, "not-valid-json{{{"));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.BAD_REQUEST.value()), ex.getStatusCode());
  }

  @Test
  void insertCardDataJson_slotNotFound_throws() {
    when(slotManager.isValidSlotId(5)).thenReturn(false);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertCardDataJson(5, "{}"));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.NOT_FOUND.value()), ex.getStatusCode());
  }

  @Test
  void insertCardDataJson_slotOccupied_throws() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(true);
    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertCardDataJson(1, "{}"));
    assertEquals(HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()), ex.getStatusCode());
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
}
