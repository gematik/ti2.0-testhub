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
package de.gematik.ti20.simsvc.client.model.dto;

import de.gematik.ti20.simsvc.client.model.CardImageData;
import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;

/** Data Transfer Object (DTO) for card information. Contains basic information about a card. */
public class CardInfoDto {

  private String cardId;
  private String cardType;
  private int slotId;
  private String label;

  /** Default constructor. */
  public CardInfoDto() {}

  /**
   * Constructor with all fields.
   *
   * @param cardType Type of the card (e.g., EGK, HBA, HPIC)
   * @param slotId Slot where the card is inserted
   * @param label Human-readable label for the card
   */
  public CardInfoDto(
      final String cardId, final String cardType, final int slotId, final String label) {
    this.cardId = cardId;
    this.cardType = cardType;
    this.slotId = slotId;
    this.label = label;
  }

  /**
   * Helper method to create CardInfoDto from a CardImageData.
   *
   * @param card CardImageData object
   * @param slotId Slot identifier
   * @return CardInfoDto containing card information
   */
  public static CardInfoDto from(final CardImageData card, final int slotId) {
    if (card instanceof VirtualCardImageData virtualCardImageData) {
      return new CardInfoDto(
          virtualCardImageData.cardId(),
          virtualCardImageData.cardType(),
          slotId,
          virtualCardImageData.cardType());
    } else if (card instanceof EgkInfoDto egkInfoDto) {
      return new CardInfoDto(egkInfoDto.cardId(), "EGK", slotId, "egk-" + egkInfoDto.getKvnr());
    }

    return null;
  }

  public String getCardId() {
    return cardId;
  }

  public void setCardId(final String cardId) {
    this.cardId = cardId;
  }

  /**
   * Get the card type.
   *
   * @return Card type
   */
  public String getCardType() {
    return cardType;
  }

  /**
   * Set the card type.
   *
   * @param cardType Card type
   */
  public void setCardType(String cardType) {
    this.cardType = cardType;
  }

  /**
   * Get the slot ID.
   *
   * @return Slot ID
   */
  public int getSlotId() {
    return slotId;
  }

  /**
   * Set the slot ID.
   *
   * @param slotId Slot ID
   */
  public void setSlotId(int slotId) {
    this.slotId = slotId;
  }

  /**
   * Get the card label.
   *
   * @return Card label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Set the card label.
   *
   * @param label Card label
   */
  public void setLabel(String label) {
    this.label = label;
  }
}
