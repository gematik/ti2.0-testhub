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
package de.gematik.ti20.simsvc.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.dto.TransmitResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlotManagerTest {

  private SlotManager slotManager;
  private ApduProcessor apduProcessor;
  private CardImage card;

  @BeforeEach
  void setUp() {
    apduProcessor = mock(ApduProcessor.class);
    card = mock(CardImage.class);
    slotManager = new SlotManager(4, apduProcessor);
  }

  @Test
  void testGetSlotCount() {
    assertEquals(4, slotManager.getSlotCount());
  }

  @Test
  void testIsValidSlotId_ValidSlots() {
    assertTrue(slotManager.isValidSlotId(0));
    assertTrue(slotManager.isValidSlotId(1));
    assertTrue(slotManager.isValidSlotId(2));
    assertTrue(slotManager.isValidSlotId(3));
  }

  @Test
  void testIsValidSlotId_InvalidSlots() {
    assertFalse(slotManager.isValidSlotId(-1));
    assertFalse(slotManager.isValidSlotId(4));
    assertFalse(slotManager.isValidSlotId(100));
  }

  @Test
  void testIsCardPresent_NoCard() {
    assertFalse(slotManager.isCardPresent(0));
    assertFalse(slotManager.isCardPresent(1));
  }

  @Test
  void testIsCardPresent_WithCard() {
    slotManager.insertCard(0, card);
    assertTrue(slotManager.isCardPresent(0));
    assertFalse(slotManager.isCardPresent(1));
  }

  @Test
  void testIsCardPresent_InvalidSlot() {
    assertFalse(slotManager.isCardPresent(-1));
    assertFalse(slotManager.isCardPresent(4));
  }

  @Test
  void testGetCardInSlot_ValidSlotWithCard() {
    slotManager.insertCard(0, card);
    assertEquals(card, slotManager.getCardInSlot(0));
  }

  @Test
  void testGetCardInSlot_ValidSlotWithoutCard() {
    assertNull(slotManager.getCardInSlot(0));
  }

  @Test
  void testGetCardInSlot_InvalidSlot() {
    assertNull(slotManager.getCardInSlot(-1));
    assertNull(slotManager.getCardInSlot(4));
  }

  @Test
  void testInsertCard_Success() {
    assertTrue(slotManager.insertCard(0, card));
    assertTrue(slotManager.isCardPresent(0));
    assertEquals(card, slotManager.getCardInSlot(0));
  }

  @Test
  void testInsertCard_InvalidSlot() {
    assertFalse(slotManager.insertCard(-1, card));
    assertFalse(slotManager.insertCard(4, card));
  }

  @Test
  void testInsertCard_NullCard() {
    assertFalse(slotManager.insertCard(0, null));
    assertFalse(slotManager.isCardPresent(0));
  }

  @Test
  void testInsertCard_SlotAlreadyOccupied() {
    slotManager.insertCard(0, card);
    CardImage anotherCard = mock(CardImage.class);

    assertFalse(slotManager.insertCard(0, anotherCard));
    assertEquals(card, slotManager.getCardInSlot(0));
  }

  @Test
  void testRemoveCard_Success() {
    slotManager.insertCard(0, card);
    assertTrue(slotManager.removeCard(0));
    assertFalse(slotManager.isCardPresent(0));
    assertNull(slotManager.getCardInSlot(0));
  }

  @Test
  void testRemoveCard_InvalidSlot() {
    assertFalse(slotManager.removeCard(-1));
    assertFalse(slotManager.removeCard(4));
  }

  @Test
  void testRemoveCard_NoCardPresent() {
    assertFalse(slotManager.removeCard(0));
  }

  @Test
  void testTransmitCommand_Success() {
    slotManager.insertCard(0, card);

    ApduResponse response = mock(ApduResponse.class);
    when(response.toHex()).thenReturn("6100");
    when(response.getSw1()).thenReturn((byte) 0x61);
    when(response.getSw2()).thenReturn((byte) 0x00);

    when(apduProcessor.processCommand(eq(card), any(ApduCommand.class))).thenReturn(response);

    TransmitResponseDto result = slotManager.transmitCommand(0, "00A40000");

    assertNotNull(result);
    assertEquals("00A40000", result.getResponse());
    assertEquals("6100", result.getStatusWord());
    assertEquals("6100", result.getStatusMessage());
  }

  @Test
  void testTransmitCommand_InvalidSlot() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> slotManager.transmitCommand(-1, "00A40000"));
    assertEquals("Invalid slot ID: -1", exception.getMessage());

    exception =
        assertThrows(
            IllegalArgumentException.class, () -> slotManager.transmitCommand(4, "00A40000"));
    assertEquals("Invalid slot ID: 4", exception.getMessage());
  }

  @Test
  void testTransmitCommand_NoCardPresent() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> slotManager.transmitCommand(0, "00A40000"));
    assertEquals("No card present in slot: 0", exception.getMessage());
  }

  @Test
  void testTransmitCommand_ProcessingReturnsNull() {
    slotManager.insertCard(0, card);
    when(apduProcessor.processCommand(eq(card), any(ApduCommand.class))).thenReturn(null);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> slotManager.transmitCommand(0, "00A40000"));
    assertEquals("Error processing command: Command processing failed", exception.getMessage());
  }

  @Test
  void testTransmitCommand_ProcessingThrowsException() {
    slotManager.insertCard(0, card);
    when(apduProcessor.processCommand(eq(card), any(ApduCommand.class)))
        .thenThrow(new RuntimeException("Processing error"));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> slotManager.transmitCommand(0, "00A40000"));
    assertTrue(exception.getMessage().contains("Error processing command"));
    assertTrue(exception.getMessage().contains("Processing error"));
  }

  @Test
  void testTransmitCommand_StatusWordFormatting() {
    slotManager.insertCard(0, card);

    ApduResponse response = mock(ApduResponse.class);
    when(response.toHex()).thenReturn("9000");
    when(response.getSw1()).thenReturn((byte) 0x90);
    when(response.getSw2()).thenReturn((byte) 0x00);

    when(apduProcessor.processCommand(eq(card), any(ApduCommand.class))).thenReturn(response);

    TransmitResponseDto result = slotManager.transmitCommand(0, "00A40000");

    assertEquals("9000", result.getStatusWord());
  }

  @Test
  void testMultipleSlots() {
    CardImage card1 = mock(CardImage.class);
    CardImage card2 = mock(CardImage.class);

    assertTrue(slotManager.insertCard(0, card1));
    assertTrue(slotManager.insertCard(1, card2));

    assertTrue(slotManager.isCardPresent(0));
    assertTrue(slotManager.isCardPresent(1));
    assertFalse(slotManager.isCardPresent(2));

    assertEquals(card1, slotManager.getCardInSlot(0));
    assertEquals(card2, slotManager.getCardInSlot(1));

    assertTrue(slotManager.removeCard(0));
    assertFalse(slotManager.isCardPresent(0));
    assertTrue(slotManager.isCardPresent(1));
  }

  @Test
  void testConstructorWithDifferentSlotCount() {
    SlotManager customSlotManager = new SlotManager(2, apduProcessor);

    assertEquals(2, customSlotManager.getSlotCount());
    assertTrue(customSlotManager.isValidSlotId(0));
    assertTrue(customSlotManager.isValidSlotId(1));
    assertFalse(customSlotManager.isValidSlotId(2));
  }
}
