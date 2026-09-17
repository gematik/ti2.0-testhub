/*-
 * #%L
 * VSDM Client Simulator Service
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
package de.gematik.ti20.simsvc.client.card;

import java.util.Map;

public class AttachedCard {

  private final String id;
  private final String terminalUrl;
  private final String cardType;
  private final Integer slotId;

  public static AttachedCard from(final String terminalUrl, final Map<String, Object> values) {
    return new AttachedCard(
        terminalUrl,
        values.get("cardHandle").toString(),
        values.get("cardType").toString(),
        Integer.valueOf(values.get("slotId").toString()));
  }

  /**
   * Constructs a new card with the specified ID and type.
   *
   * @param id the unique identifier of the card
   */
  protected AttachedCard(
      final String terminalUrl, final String id, final String cardType, final Integer slotId) {
    this.terminalUrl = terminalUrl;
    this.id = id;
    this.cardType = cardType;
    this.slotId = slotId;
  }

  /**
   * Returns the unique identifier of this card.
   *
   * @return the card identifier
   */
  public String getId() {
    return id;
  }

  public Integer getSlotId() {
    return slotId;
  }

  public String getTerminalUrl() {
    return terminalUrl;
  }

  public String getCardType() {
    return cardType;
  }
}
