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
package de.gematik.ti20.client.card.terminal.connector;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardType;

/** Implementation of Card for Connector card terminals (TI 2.0). */
public class ConnectorAttachedCard extends AttachedCard {

  private final String cardHandle;

  /**
   * Constructs a new Connector card.
   *
   * @param id the card identifier
   * @param type the card type
   * @param cardHandle the Connector card handle
   */
  public ConnectorAttachedCard(
      String id, CardType type, ConnectorCardTerminal terminal, String cardHandle) {
    super(id, type, terminal);
    this.cardHandle = cardHandle;
  }

  /** {@inheritDoc} */
  @Override
  public String getInfo() {
    return String.format("Type: %s, Handle: %s", getType().getDescription(), cardHandle);
  }

  /**
   * Returns the Connector card handle.
   *
   * @return the card handle
   */
  public String getCardHandle() {
    return cardHandle;
  }
}
