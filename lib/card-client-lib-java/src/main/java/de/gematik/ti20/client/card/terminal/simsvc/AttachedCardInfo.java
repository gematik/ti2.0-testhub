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
package de.gematik.ti20.client.card.terminal.simsvc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.gematik.ti20.client.card.card.CardType;

/** Class representing card information from the CardSimulator API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttachedCardInfo {

  @JsonProperty("cardHandle")
  private String id;

  @JsonProperty("cardType")
  private String type;

  @JsonProperty("slotId")
  private Integer slotId;

  @JsonProperty("cardLabel")
  private String label;

  /** Default constructor for JSON deserialization. */
  public AttachedCardInfo() {}

  /**
   * Constructs a new CardInfo.
   *
   * @param id the card ID
   * @param type the card type
   * @param slotId slot id
   * @param label
   */
  @JsonCreator
  public AttachedCardInfo(
      @JsonProperty("cardHandle") String id,
      @JsonProperty("cardType") String type,
      @JsonProperty("slotId") Integer slotId,
      @JsonProperty("cardLabel") String label) {
    this.id = id;
    this.type = type;
    this.slotId = slotId;
    this.label = label;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public Integer getSlotId() {
    return slotId;
  }

  public String getLabel() {
    return label;
  }

  @JsonIgnore
  public CardType getCardType() {

    switch (type.toUpperCase()) {
      case "EGK":
      case "EGKG2":
      case "EGKG2_1":
        return CardType.EGK;
      case "HBA":
      case "HBAG2":
      case "HBAG2_1":
        return CardType.HBA;
      case "SMC-B":
      case "SMCB":
      case "SMCBG2":
      case "SMCBG2_1":
      case "HPIC":
        return CardType.SMC_B;
      default:
        return CardType.UNKNOWN;
    }
  }
}
