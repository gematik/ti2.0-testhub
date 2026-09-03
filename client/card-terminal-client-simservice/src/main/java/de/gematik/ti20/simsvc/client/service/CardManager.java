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

import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service for managing card connections and operations. */
@Service
public class CardManager {

  private final SlotManager slotManager;
  private final Map<String, CardConnection> connections;

  /**
   * Constructor for CardManager.
   *
   * @param slotManager Service to manage slots
   */
  @Autowired
  public CardManager(SlotManager slotManager) {
    this.slotManager = slotManager;
    this.connections = new ConcurrentHashMap<>();
  }

  /**
   * List all available cards across all slots.
   *
   * @return List of card handles
   */
  public List<CardHandleDto> listAllCards() {
    List<CardHandleDto> cardHandles = new ArrayList<>();

    for (int slotId = 0; slotId < slotManager.getSlotCount(); slotId++) {
      if (slotManager.isCardPresent(slotId)) {
        CardImage card = slotManager.getCardInSlot(slotId);
        String cardHandle = generateCardHandle(card);

        CardHandleDto cardHandleDto =
            new CardHandleDto(cardHandle, card.getCardType().name(), slotId, card.getLabel());

        cardHandles.add(cardHandleDto);
      }
    }

    return cardHandles;
  }

  /**
   * Find a card by its handle.
   *
   * @param cardHandle Card handle to find
   * @return CardImage or null if not found
   */
  public CardImage findCardByHandle(String cardHandle) {
    for (int slotId = 0; slotId < slotManager.getSlotCount(); slotId++) {
      if (slotManager.isCardPresent(slotId)) {
        CardImage card = slotManager.getCardInSlot(slotId);
        String generatedHandle = generateCardHandle(card);

        if (generatedHandle.equals(cardHandle)) {
          return card;
        }
      }
    }

    return null;
  }

  /**
   * Find the slot ID for a given card.
   *
   * @param targetCard Card to find slot for
   * @return Slot ID or 0 if not found
   */
  private int findSlotIdForCard(CardImage targetCard) {
    for (int slotId = 0; slotId < slotManager.getSlotCount(); slotId++) {
      if (slotManager.isCardPresent(slotId)) {
        CardImage card = slotManager.getCardInSlot(slotId);
        if (card != null && card.equals(targetCard)) {
          return slotId;
        }
      }
    }
    return 0; // Default to slot 0 if not found
  }

  /**
   * Generate a unique card handle.
   *
   * @param card Card image
   * @return Card handle
   */
  private String generateCardHandle(CardImage card) {
    // Use the card ID if available, otherwise generate a random UUID
    if (card.getId() != null && !card.getId().isEmpty()) {
      return card.getId();
    }

    return UUID.randomUUID().toString();
  }

  /** Inner class representing a connection to a card. */
  private static class CardConnection {
    private final CardImage card;
    private final int slotId;

    /**
     * Constructor for CardConnection.
     *
     * @param card Card image
     * @param slotId Slot ID
     */
    public CardConnection(CardImage card, int slotId) {
      this.card = card;
      this.slotId = slotId;
    }

    /**
     * Get the card image.
     *
     * @return Card image
     */
    public CardImage getCard() {
      return card;
    }

    /**
     * Get the slot ID.
     *
     * @return Slot ID
     */
    public int getSlotId() {
      return slotId;
    }
  }
}
