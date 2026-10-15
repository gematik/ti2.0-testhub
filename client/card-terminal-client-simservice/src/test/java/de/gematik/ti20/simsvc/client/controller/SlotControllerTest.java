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

import de.gematik.ti20.simsvc.client.model.EgkIdentifier;
import de.gematik.ti20.simsvc.client.model.SmcbIdentifier;
import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.CardInfoDto;
import de.gematik.ti20.simsvc.client.repository.CardImageIdentifier;
import de.gematik.ti20.simsvc.client.repository.CardImageRepository;
import de.gematik.ti20.simsvc.client.repository.EgkCardImageIdentifier;
import de.gematik.ti20.simsvc.client.repository.SmcbCardImageIdentifier;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class SlotControllerTest {

  private SlotManager slotManager;
  private CardImageRepository cardImageRepository;
  private SlotController controller;

  @BeforeEach
  void setUp() {
    slotManager = mock(SlotManager.class);
    cardImageRepository = mock(CardImageRepository.class);
    controller = new SlotController(slotManager, cardImageRepository);
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

  @Test
  void insertEgk_loadsClasspathCard() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final EgkIdentifier identifier = new EgkIdentifier("109500969", "X110639491");
    when(cardImageRepository.findByEgkIdentifier(identifier))
        .thenReturn(
            List.of(
                new EgkCardImageIdentifier(
                    identifier.iknr(), identifier.kvnr(), "cards/slot-controller-card.xml")));

    ResponseEntity<CardInfoDto> response = controller.insertEgk(0, identifier);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(slotManager).insertCard(eq(0), any(VirtualCardImageData.class));
  }

  @Test
  void insertEgk_slotNotFound_throwsNotFound() {
    when(slotManager.isValidSlotId(5)).thenReturn(false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.insertEgk(5, new EgkIdentifier("109500969", "X110639491")));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verifyNoInteractions(cardImageRepository);
  }

  @Test
  void insertEgk_slotOccupied_throwsConflict() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.insertEgk(1, new EgkIdentifier("109500969", "X110639491")));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verifyNoInteractions(cardImageRepository);
  }

  @Test
  void insertEgk_noMatchingCard_throwsNotFound() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final EgkIdentifier identifier = new EgkIdentifier("109500969", "X110639491");
    when(cardImageRepository.findByEgkIdentifier(identifier)).thenReturn(List.of());

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertEgk(0, identifier));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verify(slotManager, never()).insertCard(anyInt(), any(VirtualCardImageData.class));
  }

  @Test
  void insertEgk_multipleMatchingCards_throwsConflict() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final EgkIdentifier identifier = new EgkIdentifier("109500969", "X110639491");
    when(cardImageRepository.findByEgkIdentifier(identifier))
        .thenReturn(
            List.of(
                new EgkCardImageIdentifier(
                    identifier.iknr(), identifier.kvnr(), "cards/slot-controller-card.xml"),
                new EgkCardImageIdentifier(
                    identifier.iknr(), identifier.kvnr(), "cards/slot-controller-card.xml")));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertEgk(0, identifier));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(slotManager, never()).insertCard(anyInt(), any(VirtualCardImageData.class));
  }

  @Test
  void insertEgk_missingClasspathCard_throwsBadRequest() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final EgkIdentifier identifier = new EgkIdentifier("109500969", "X110639491");
    when(cardImageRepository.findByEgkIdentifier(identifier))
        .thenReturn(
            List.of(
                new EgkCardImageIdentifier(
                    identifier.iknr(), identifier.kvnr(), "cards/missing-card.xml")));

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertEgk(0, identifier));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verify(slotManager, never()).insertCard(anyInt(), any(VirtualCardImageData.class));
  }

  @Test
  void insertEgk_loadsFilesystemCard(@TempDir final Path directory) throws Exception {
    final Path file = directory.resolve("card image.xml");
    Files.writeString(file, "<xml/>");
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final EgkIdentifier identifier = new EgkIdentifier("109500969", "X110639491");
    when(cardImageRepository.findByEgkIdentifier(identifier))
        .thenReturn(
            List.of(
                new EgkCardImageIdentifier(
                    identifier.iknr(), identifier.kvnr(), file.toUri().toString())));

    final ResponseEntity<CardInfoDto> response = controller.insertEgk(0, identifier);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(slotManager).insertCard(eq(0), any(VirtualCardImageData.class));
  }

  @Test
  void insertSmcb_loadsClasspathCard() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final SmcbIdentifier identifier = new SmcbIdentifier("1-SMC-B-Testkarte--883110000168650");
    when(cardImageRepository.findBySmcbIdentifier(identifier))
        .thenReturn(
            List.of(
                new SmcbCardImageIdentifier(
                    identifier.actorId(), "SMC_B_80276883110000168650_gema5.xml")));

    final ResponseEntity<CardInfoDto> response = controller.insertSmcb(0, identifier);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("SMCB", response.getBody().getCardType());
    verify(slotManager).insertCard(eq(0), argThat(card -> "SMCB".equals(card.cardType())));
  }

  @Test
  void insertSmcb_slotNotFound_throwsNotFound() {
    when(slotManager.isValidSlotId(5)).thenReturn(false);

    final ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.insertSmcb(5, new SmcbIdentifier("actor-1")));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verifyNoInteractions(cardImageRepository);
  }

  @Test
  void insertSmcb_slotOccupied_throwsConflict() {
    when(slotManager.isValidSlotId(1)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(true);

    final ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.insertSmcb(1, new SmcbIdentifier("actor-1")));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verifyNoInteractions(cardImageRepository);
  }

  @Test
  void insertSmcb_noMatchingCard_throwsNotFound() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final SmcbIdentifier identifier = new SmcbIdentifier("actor-1");
    when(cardImageRepository.findBySmcbIdentifier(identifier)).thenReturn(List.of());

    final ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertSmcb(0, identifier));

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verify(slotManager, never()).insertCard(anyInt(), any(VirtualCardImageData.class));
  }

  @Test
  void insertSmcb_multipleMatchingCards_throwsConflict() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final SmcbIdentifier identifier = new SmcbIdentifier("actor-1");
    final CardImageIdentifier matching = new SmcbCardImageIdentifier("actor-1", "cards/smcb.xml");
    when(cardImageRepository.findBySmcbIdentifier(identifier))
        .thenReturn(List.of(matching, matching));

    final ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertSmcb(0, identifier));

    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verify(slotManager, never()).insertCard(anyInt(), any(VirtualCardImageData.class));
  }

  @Test
  void insertSmcb_missingClasspathCard_throwsBadRequest() {
    when(slotManager.isValidSlotId(0)).thenReturn(true);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    final SmcbIdentifier identifier = new SmcbIdentifier("actor-1");
    when(cardImageRepository.findBySmcbIdentifier(identifier))
        .thenReturn(List.of(new SmcbCardImageIdentifier("actor-1", "cards/missing-smcb.xml")));

    final ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> controller.insertSmcb(0, identifier));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verify(slotManager, never()).insertCard(anyInt(), any(VirtualCardImageData.class));
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
