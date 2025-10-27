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

import de.gematik.ti20.simsvc.client.exception.CardNotConnectedException;
import de.gematik.ti20.simsvc.client.exception.CardNotFoundException;
import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import de.gematik.ti20.simsvc.client.model.dto.ConnectionPropertiesDto;
import de.gematik.ti20.simsvc.client.model.dto.TransmitResponseDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardManagerTest {

  private CardManager cardManager;
  private SlotManager slotManager;
  private ApduProcessor apduProcessor;
  private CardImage card;

  @BeforeEach
  void setUp() {
    slotManager = mock(SlotManager.class);
    apduProcessor = mock(ApduProcessor.class);
    card = mock(CardImage.class);
    cardManager = new CardManager(slotManager, apduProcessor);
  }

  @Test
  void testListAllCards_WithCards() {
    when(slotManager.getSlotCount()).thenReturn(2);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.isCardPresent(1)).thenReturn(false);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getCardType()).thenReturn(CardType.EGK);
    when(card.getLabel()).thenReturn("Test Card");
    when(card.getId()).thenReturn("test-card-id");

    List<CardHandleDto> result = cardManager.listAllCards();

    assertEquals(1, result.size());
    CardHandleDto cardHandle = result.get(0);
    assertEquals("test-card-id", cardHandle.getCardHandle());
    assertEquals("EGK", cardHandle.getCardType());
    assertEquals(0, cardHandle.getSlotId());
    assertEquals("Test Card", cardHandle.getCardLabel());
  }

  @Test
  void testListAllCards_NoCards() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(false);

    List<CardHandleDto> result = cardManager.listAllCards();

    assertTrue(result.isEmpty());
  }

  @Test
  void testConnectToCard_Success() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");
    when(card.getCardType()).thenReturn(CardType.EGK);

    ConnectionPropertiesDto result = cardManager.connectToCard("test-card-id");

    assertNotNull(result);
    assertEquals("test-card-id", result.getCardHandle());
    assertEquals("3B8F80018031C0730F0161FF0143C103000300300400", result.getAtr());
    assertEquals("T=1", result.getProtocol());
    assertFalse(result.isExclusive());
  }

  @Test
  void testConnectToCard_CardNotFound() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(false);

    assertThrows(CardNotFoundException.class, () -> cardManager.connectToCard("non-existent-card"));
  }

  @Test
  void testTransmitCommand_Success() {
    // Setup connection
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");
    when(card.getCardType()).thenReturn(CardType.EGK);
    cardManager.connectToCard("test-card-id");

    // Setup APDU processing
    ApduResponse response = mock(ApduResponse.class);
    when(response.getStatusWord()).thenReturn(0x9000);
    when(response.getStatusWordHex()).thenReturn("9000");
    when(response.getStatusMessage()).thenReturn("Success");
    when(response.toHex()).thenReturn("9000");
    when(response.getData()).thenReturn(new byte[] {0x01, 0x02});
    when(apduProcessor.processCommand(eq(card), any(ApduCommand.class))).thenReturn(response);

    TransmitResponseDto result = cardManager.transmitCommand("test-card-id", "00A40000");

    assertNotNull(result);
    assertEquals("9000", result.getResponse());
    assertEquals("9000", result.getStatusWord());
    assertEquals("Success", result.getStatusMessage());
    assertEquals("0102", result.getData());
  }

  @Test
  void testTransmitCommand_CardNotConnected() {
    assertThrows(
        CardNotConnectedException.class,
        () -> cardManager.transmitCommand("non-connected-card", "00A40000"));
  }

  @Test
  void testTransmitCommand_InvalidApduFormat() {
    // Setup connection
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");
    when(card.getCardType()).thenReturn(CardType.EGK);
    cardManager.connectToCard("test-card-id");

    assertThrows(
        IllegalArgumentException.class,
        () -> cardManager.transmitCommand("test-card-id", "INVALID_HEX"));
  }

  @Test
  void testDisconnectCard_Success() {
    // Setup connection
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");
    when(card.getCardType()).thenReturn(CardType.EGK);
    cardManager.connectToCard("test-card-id");

    // Should not throw exception
    cardManager.disconnectCard("test-card-id");
  }

  @Test
  void testDisconnectCard_CardNotConnected() {
    assertThrows(
        CardNotConnectedException.class, () -> cardManager.disconnectCard("non-connected-card"));
  }

  @Test
  void testFindCardByHandle_Found() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");

    CardImage result = cardManager.findCardByHandle("test-card-id");

    assertEquals(card, result);
  }

  @Test
  void testFindCardByHandle_NotFound() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(false);

    CardImage result = cardManager.findCardByHandle("non-existent-card");

    assertNull(result);
  }

  @Test
  void testGetAtrForCard_EGK() {
    when(card.getCardType()).thenReturn(CardType.EGK);
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");

    ConnectionPropertiesDto result = cardManager.connectToCard("test-card-id");

    assertEquals("3B8F80018031C0730F0161FF0143C103000300300400", result.getAtr());
  }

  @Test
  void testGetAtrForCard_HBA() {
    when(card.getCardType()).thenReturn(CardType.HBA);
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");

    ConnectionPropertiesDto result = cardManager.connectToCard("test-card-id");

    assertEquals("3B9F0080318065B0870401625F0104C03F0073CF", result.getAtr());
  }

  @Test
  void testGetAtrForCard_HPIC() {
    when(card.getCardType()).thenReturn(CardType.HPIC);
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("test-card-id");

    ConnectionPropertiesDto result = cardManager.connectToCard("test-card-id");

    assertEquals("3B9F0080318065B0880401625F0104C03F0073CF", result.getAtr());
  }

  @Test
  void testGenerateCardHandle_WithId() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn("existing-id");
    when(card.getCardType()).thenReturn(CardType.EGK);
    when(card.getLabel()).thenReturn("Test Card");

    List<CardHandleDto> result = cardManager.listAllCards();

    assertEquals(1, result.size());
    assertEquals("existing-id", result.get(0).getCardHandle());
  }

  @Test
  void testGenerateCardHandle_WithoutId() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);
    when(card.getId()).thenReturn(null);
    when(card.getCardType()).thenReturn(CardType.EGK);
    when(card.getLabel()).thenReturn("Test Card");

    List<CardHandleDto> result = cardManager.listAllCards();

    assertEquals(1, result.size());
    // UUID should be generated, just check it's not null
    assertNotNull(result.get(0).getCardHandle());
  }
}
