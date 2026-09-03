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
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service for managing card slots. */
@Service
public class SlotManager {

  private final int slotCount;
  private final Map<Integer, CardImage> slots;

  /**
   * Constructor for SlotManager.
   *
   * @param slotCount Number of slots to manage from configuration
   */
  @Autowired
  public SlotManager(@Value("${card.terminal.slots:4}") int slotCount) {
    this.slotCount = slotCount;
    this.slots = new HashMap<>();
  }

  /**
   * Get the number of slots managed by this SlotManager.
   *
   * @return Number of slots
   */
  public int getSlotCount() {
    return slotCount;
  }

  /**
   * Check if a slot ID is valid.
   *
   * @param slotId Slot ID to check
   * @return true if the slot ID is valid, false otherwise
   */
  public boolean isValidSlotId(int slotId) {
    return slotId >= 0 && slotId < slotCount;
  }

  /**
   * Check if a card is present in a slot.
   *
   * @param slotId Slot ID to check
   * @return true if a card is present, false otherwise
   */
  public boolean isCardPresent(int slotId) {
    return isValidSlotId(slotId) && slots.containsKey(slotId);
  }

  /**
   * Get the card in a slot.
   *
   * @param slotId Slot ID
   * @return CardImage if present, null otherwise
   */
  public CardImage getCardInSlot(int slotId) {
    if (!isValidSlotId(slotId)) {
      return null;
    }
    return slots.get(slotId);
  }

  /**
   * Insert a card into a slot.
   *
   * @param slotId Slot ID
   * @param card CardImage to insert
   * @return true if insertion was successful, false otherwise
   */
  public boolean insertCard(int slotId, CardImage card) {
    if (!isValidSlotId(slotId) || isCardPresent(slotId) || card == null) {
      return false;
    }

    slots.put(slotId, card);
    return true;
  }

  /**
   * Remove a card from a slot.
   *
   * @param slotId Slot ID
   * @return true if removal was successful, false otherwise
   */
  public boolean removeCard(int slotId) {
    if (!isValidSlotId(slotId) || !isCardPresent(slotId)) {
      return false;
    }

    slots.remove(slotId);
    return true;
  }
}
