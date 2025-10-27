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
package de.gematik.ti20.client.card.terminal;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class representing a card terminal that can communicate with health cards. This is
 * the main abstraction for different types of card terminals (USB, Connector, Simulator, etc.).
 */
public abstract class CardTerminal {

  protected static final Logger log = LoggerFactory.getLogger(CardTerminal.class);

  protected final String name;
  protected final CardTerminalType type;

  /**
   * Constructs a new card terminal with the specified name.
   *
   * @param name the name of this terminal
   */
  protected CardTerminal(String name, CardTerminalType type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Returns the name of this card terminal.
   *
   * @return the terminal name
   */
  public String getName() {
    return name;
  }

  public CardTerminalType getType() {
    return type;
  }

  /**
   * Returns a list of cards attached to this terminal. This is an abstract method that must be
   * implemented by subclasses.
   *
   * @return list of attached cards
   * @throws CardTerminalException if an error occurs while communicating with the terminal
   */
  public abstract List<? extends AttachedCard> getAttachedCards() throws CardTerminalException;

  public EgkInfo getEgkInfo(final AttachedCard attachedCard) throws CardTerminalException {
    throw new CardTerminalException("getEgkInfo not implemented");
  }

  /**
   * Establishes a connection to the specified card. This is an abstract method that must be
   * implemented by subclasses.
   *
   * @param card the card to connect to
   * @return a connection to the card
   * @throws CardTerminalException if connection cannot be established
   */
  public abstract CardConnection connect(AttachedCard card) throws CardTerminalException;
}
