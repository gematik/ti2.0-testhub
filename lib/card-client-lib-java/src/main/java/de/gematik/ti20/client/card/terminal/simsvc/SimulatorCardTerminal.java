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

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminal;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalType;
import java.io.IOException;
import java.util.List;

/**
 * Implementation of CardTerminal for simulated card terminals. This implementation is primarily for
 * testing purposes.
 */
public class SimulatorCardTerminal extends CardTerminal {

  private final SimulatorConnectionConfig config;
  private final SimulatorClient client;

  /**
   * Constructs a new simulator card terminal.
   *
   * @param config the card terminal simsvc config
   */
  public SimulatorCardTerminal(SimulatorConnectionConfig config) {
    super(config.getName(), CardTerminalType.SIMSVC);
    this.config = config;
    this.client = new SimulatorClient(config.getUrl());
  }

  /** {@inheritDoc} */
  @Override
  public List<? extends AttachedCard> getAttachedCards() throws CardTerminalException {

    try {
      List<AttachedCardInfo> availableCards = getClient().getAvailableCards();
      return availableCards.stream().map(card -> new SimulatorAttachedCard(card, this)).toList();
    } catch (IOException e) {
      log.error("Error getting available cards", e);
      throw new CardTerminalException(e.getMessage(), e);
    }
  }

  @Override
  public EgkInfo getEgkInfo(final AttachedCard attachedCard) throws CardTerminalException {
    try {
      return getClient().getEgkInfo((SimulatorAttachedCard) attachedCard);
    } catch (IOException e) {
      log.error("Error getting available cards", e);
      throw new CardTerminalException(e.getMessage(), e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public CardConnection connect(final AttachedCard card) throws CardTerminalException {
    if (!(card instanceof SimulatorAttachedCard)) {
      throw new CardTerminalException("Card is not a simulator card");
    }

    try {
      final CardConnectionInfo cci = getClient().connectToCard(card);
      return new SimulatorCardConnection((SimulatorAttachedCard) card, this, cci);
    } catch (final Exception e) {
      log.error("Error getting available cards", e);
      throw new CardTerminalException(e.getMessage(), e);
    }
  }

  public SimulatorClient getClient() {
    return client;
  }

  public SimulatorConnectionConfig getConfig() {
    return config;
  }
}
