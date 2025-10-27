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
package de.gematik.ti20.client.card.card;

import de.gematik.ti20.client.card.terminal.CardTerminal;

/**
 * Abstract base class representing a card that can be attached to a card terminal. This abstraction
 * covers various types of health cards in the German healthcare system.
 */
public abstract class AttachedCard {

  protected final String id;
  protected final CardType type;
  protected final CardTerminal terminal;

  /**
   * Constructs a new card with the specified ID and type.
   *
   * @param id the unique identifier of the card
   * @param type the card type
   */
  protected AttachedCard(final String id, final CardType type, final CardTerminal terminal) {
    this.id = id;
    this.type = type;
    this.terminal = terminal;
  }

  /**
   * Returns the unique identifier of this card.
   *
   * @return the card identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the type of this card (EGK, HBA, SMC_B, etc.).
   *
   * @return the card type
   */
  public CardType getType() {
    return type;
  }

  /**
   * Returns the terminal that currently attached by the card
   *
   * @return
   */
  public CardTerminal getTerminal() {
    return terminal;
  }

  /**
   * Returns additional information about this card. Default implementation returns type description
   * and ID.
   *
   * @return a string containing card information
   */
  public String getInfo() {
    return String.format("Type: %s, ID: %s", type.getDescription(), id);
  }

  public boolean isSmcb() {
    return type == CardType.SMC_B;
  }

  public boolean isEgk() {
    return type == CardType.EGK;
  }
}
