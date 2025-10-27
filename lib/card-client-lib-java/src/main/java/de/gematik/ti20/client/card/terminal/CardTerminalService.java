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
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.config.ConnectorConnectionConfig;
import de.gematik.ti20.client.card.config.PcScConnectionConfig;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.client.card.terminal.connector.ConnectorCardTerminal;
import de.gematik.ti20.client.card.terminal.pcsc.PcScCardTerminal;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorCardTerminal;
import java.util.ArrayList;
import java.util.List;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Card terminal service class that provides methods to manage and interact with card terminals. */
public class CardTerminalService {

  private static final Logger log = LoggerFactory.getLogger(CardTerminalService.class);

  private final List<CardTerminalConnectionConfig> connectionConfigs;

  public CardTerminalService(final List<CardTerminalConnectionConfig> configs) {
    connectionConfigs = configs != null ? configs : new ArrayList<>();
  }

  public List<CardTerminalConnectionConfig> getTerminalConnectionConfigs() {
    return connectionConfigs;
  }

  public void addTerminalConnectionConfig(final CardTerminalConnectionConfig config) {
    if (config != null) {
      connectionConfigs.add(config);
    } else {
      log.warn("Attempted to add null terminal connection configuration");
    }
  }

  public void setTerminalConnectionConfigs(final List<CardTerminalConnectionConfig> configs) {
    if (configs != null) {
      connectionConfigs.clear();
      connectionConfigs.addAll(configs);
    } else {
      log.warn("Attempted to set null terminal connection configurations");
    }
  }

  /**
   * Returns a list of available card terminals based on the provided configurations.
   *
   * @return list of available card terminals
   */
  public List<CardTerminal> getAvailableTerminals() {
    final List<CardTerminal> terminals = new ArrayList<>();

    if (connectionConfigs == null || connectionConfigs.isEmpty()) {
      log.warn("No terminal configurations provided");
      return terminals;
    }

    for (final CardTerminalConnectionConfig config : connectionConfigs) {
      try {
        final CardTerminal terminal = createTerminal(config);
        if (terminal != null) {
          terminals.add(terminal);
        }
      } catch (final CardTerminalException e) {
        log.error("Failed to create terminal from config: " + config.getName(), e);
      }
    }

    return terminals;
  }

  /**
   * Returns a list of all cards currently attached to all available terminals based on the provided
   * configurations.
   *
   * @return list of attached cards
   */
  public List<? extends AttachedCard> getAttachedCards() throws CardTerminalException {
    var terminals = getAvailableTerminals();

    final List<AttachedCard> cards = new ArrayList<>();
    for (var terminal : terminals) {
      cards.addAll(terminal.getAttachedCards());
    }

    return cards;
  }

  public EgkInfo getEgkInfo(final AttachedCard attachedCard) throws CardTerminalException {
    return attachedCard.getTerminal().getEgkInfo(attachedCard);
  }

  /**
   * Creates a card terminal instance based on the provided configuration.
   *
   * @param config the terminal configuration
   * @return the created card terminal
   * @throws CardTerminalException if terminal creation fails
   */
  protected CardTerminal createTerminal(final CardTerminalConnectionConfig config)
      throws CardTerminalException {
    if (config == null) {
      throw new CardTerminalException("Terminal configuration cannot be null");
    }

    final CardTerminalType type =
        config.getType() != null ? config.getType() : CardTerminalType.UNKNOWN;
    final String name = config.getName();

    log.debug("Creating terminal of type {} with name {}", type, name);

    return switch (type) {
      case PCSC -> createPcScTerminal((PcScConnectionConfig) config);
      case CONNECTOR -> createConnectorTerminal((ConnectorConnectionConfig) config);
      case SIMSVC -> createSimulatorTerminal((SimulatorConnectionConfig) config);
      default -> throw new CardTerminalException("Unsupported terminal type: " + type);
    };
  }

  private CardTerminal createPcScTerminal(PcScConnectionConfig config)
      throws CardTerminalException {
    try {
      TerminalFactory factory = TerminalFactory.getDefault();
      CardTerminals terminals = factory.terminals();

      for (javax.smartcardio.CardTerminal terminal : terminals.list()) {
        if (terminal.getName().equals(config.getReaderName())) {
          // Using explicit cast to CardTerminal since UsbCardTerminal is now a subclass
          return (CardTerminal) new PcScCardTerminal(config, terminal);
        }
      }

      throw new CardTerminalException("USB terminal not found: " + config.getReaderName());
    } catch (CardException e) {
      throw new CardTerminalException("Failed to access USB card terminals", e);
    }
  }

  private CardTerminal createConnectorTerminal(ConnectorConnectionConfig config) {
    // Using explicit cast to CardTerminal since ConnectorCardTerminal is now a subclass
    return (CardTerminal) new ConnectorCardTerminal(config);
  }

  private CardTerminal createSimulatorTerminal(SimulatorConnectionConfig config) {
    // Using explicit cast to CardTerminal since SimulatorCardTerminal is now a subclass
    return (CardTerminal) new SimulatorCardTerminal(config);
  }
}
