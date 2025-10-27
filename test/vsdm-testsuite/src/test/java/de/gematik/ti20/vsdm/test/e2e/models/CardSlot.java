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
package de.gematik.ti20.vsdm.test.e2e.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardSlot {

  @JsonProperty("cardId")
  private String cardId;

  @JsonProperty("cardType")
  private String cardType;

  @JsonProperty("slotId")
  private Integer slotId;

  @JsonProperty("label")
  private String label;

  @JsonCreator
  public CardSlot(
      @JsonProperty("cardId") String cardId,
      @JsonProperty("cardType") String cardType,
      @JsonProperty("slotId") Integer slotId,
      @JsonProperty("label") String label) {
    this.cardId = cardId;
    this.cardType = cardType;
    this.slotId = slotId;
    this.label = label;
  }
}
