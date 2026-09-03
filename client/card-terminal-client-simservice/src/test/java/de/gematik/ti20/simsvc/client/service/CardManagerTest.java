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
package de.gematik.ti20.simsvc.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardManagerTest {

  private CardManager cardManager;
  private SlotManager slotManager;
  private CardImage card;

  @BeforeEach
  void setUp() {
    slotManager = mock(SlotManager.class);
    card = mock(CardImage.class);
    cardManager = new CardManager(slotManager);
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
