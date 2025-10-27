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
package de.gematik.ti20.simsvc.client.model.dto;

/**
 * Data Transfer Object (DTO) for card handles. Contains information about a card that can be used
 * to identify it in the system.
 */
public class CardHandleDto {

  private String cardHandle;
  private String cardType;
  private int slotId;
  private String cardLabel;

  /** Default constructor. */
  public CardHandleDto() {}

  /**
   * Constructor with all fields.
   *
   * @param cardHandle Unique identifier for the card
   * @param cardType Type of the card (e.g., EGK, HBA, HPIC)
   * @param slotId Slot where the card is inserted
   * @param cardLabel Human-readable label for the card
   */
  public CardHandleDto(String cardHandle, String cardType, int slotId, String cardLabel) {
    this.cardHandle = cardHandle;
    this.cardType = cardType;
    this.slotId = slotId;
    this.cardLabel = cardLabel;
  }

  /**
   * Get the card handle.
   *
   * @return Card handle
   */
  public String getCardHandle() {
    return cardHandle;
  }

  /**
   * Set the card handle.
   *
   * @param cardHandle Card handle
   */
  public void setCardHandle(String cardHandle) {
    this.cardHandle = cardHandle;
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
  public String getCardLabel() {
    return cardLabel;
  }

  /**
   * Set the card label.
   *
   * @param cardLabel Card label
   */
  public void setCardLabel(String cardLabel) {
    this.cardLabel = cardLabel;
  }
}
