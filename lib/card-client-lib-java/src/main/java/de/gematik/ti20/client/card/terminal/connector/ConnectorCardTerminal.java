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
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.CardType;
import de.gematik.ti20.client.card.config.ConnectorConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminal;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalType;
import de.gematik.ti20.client.card.terminal.connector.signature.ConnectorSignatureService;
import de.gematik.ti20.client.card.terminal.connector.signature.SignatureService;
import java.util.ArrayList;
import java.util.List;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;

/** Implementation of CardTerminal for Connector card terminals (TI 2.0). */
public class ConnectorCardTerminal extends CardTerminal {

  private final ConnectorConnectionConfig config;
  private Dispatch<SOAPMessage> dispatch;
  private ConnectorClient client;
  private SignatureService signatureService;

  /** Constructs a new Connector card terminal. */
  public ConnectorCardTerminal(ConnectorConnectionConfig config) {

    super(config.getName(), CardTerminalType.CONNECTOR);
    this.config = config;

    try {
      this.client =
          ConnectorClientFactory.createClient(
              config.getEndpointAddress(),
              config.getMandantId(),
              config.getClientSystemId(),
              config.getWorkplaceId(),
              config.getUserId());
      this.dispatch = SoapDispatcher.createDispatch(config.getEndpointAddress());
      this.signatureService =
          new ConnectorSignatureService(
              client,
              dispatch,
              config.getMandantId(),
              config.getClientSystemId(),
              config.getWorkplaceId(),
              config.getUserId());
    } catch (Exception e) {
      log.error("Failed to initialize Connector terminal", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<AttachedCard> getAttachedCards() throws CardTerminalException {
    if (client == null) {
      throw new CardTerminalException("Terminal is not connected or not properly initialized");
    }

    List<AttachedCard> cards = new ArrayList<>();

    try {
      String[] cardHandles = client.getCards();

      for (String cardHandle : cardHandles) {
        String cardType = client.getCardType(cardHandle);
        CardType type = mapCardType(cardType);

        // Generate a unique ID based on the card handle
        String id = getName() + "-" + cardHandle;

        ConnectorAttachedCard card = new ConnectorAttachedCard(id, type, this, cardHandle);
        cards.add(card);
      }
    } catch (Exception e) {
      throw new CardTerminalException("Failed to get available cards", e);
    }

    return cards;
  }

  /** {@inheritDoc} */
  @Override
  public CardConnection connect(AttachedCard card) throws CardTerminalException {
    if (client == null) {
      throw new CardTerminalException("Terminal is not connected or not properly initialized");
    }

    if (!(card instanceof ConnectorAttachedCard)) {
      throw new CardTerminalException("Card is not a Connector card");
    }

    ConnectorAttachedCard connectorCard = (ConnectorAttachedCard) card;

    try {
      String connectionHandle = client.connect(connectorCard.getCardHandle());
      return new ConnectorCardConnection(connectorCard, connectionHandle, this);
    } catch (Exception e) {
      throw new CardTerminalException("Failed to connect to card", e);
    }
  }

  /**
   * Transmits an APDU to a card.
   *
   * @param connectionHandle the connection handle
   * @param apdu the APDU to transmit
   * @return the response APDU
   * @throws CardTerminalException if transmission fails
   */
  public byte[] transmitAPDU(String connectionHandle, byte[] apdu) throws CardTerminalException {
    if (client == null) {
      throw new CardTerminalException("Terminal is not connected or not properly initialized");
    }

    try {
      return client.transmit(connectionHandle, apdu);
    } catch (Exception e) {
      throw new CardTerminalException("Failed to transmit APDU", e);
    }
  }

  /**
   * Gets the signature service for this terminal. The signature service provides direct access to
   * the Connector's SignatureService functionality for creating signatures without manually
   * handling APDU sequences.
   *
   * @return the signature service
   * @throws CardTerminalException if the terminal is not ready
   */
  public SignatureService getSignatureService() throws CardTerminalException {
    if (signatureService == null) {
      throw new CardTerminalException(
          "Terminal is not connected or signature service not properly initialized");
    }

    return signatureService;
  }

  /**
   * Disconnects a card connection.
   *
   * @param connectionHandle the connection handle
   * @throws CardTerminalException if disconnection fails
   */
  public void disconnect(String connectionHandle) throws CardTerminalException {
    if (client == null) {
      throw new CardTerminalException("Terminal is not connected or not properly initialized");
    }

    try {
      client.disconnect(connectionHandle);
    } catch (Exception e) {
      throw new CardTerminalException("Failed to disconnect card", e);
    }
  }

  /**
   * Maps a Connector card type string to a CardType enum value.
   *
   * @param cardType the Connector card type string
   * @return the corresponding CardType enum value
   */
  private CardType mapCardType(String cardType) {
    if (cardType == null) {
      return CardType.UNKNOWN;
    }

    switch (cardType.toUpperCase()) {
      case "EGK":
        return CardType.EGK;
      case "HBA":
        return CardType.HBA;
      case "SMC-B":
        return CardType.SMC_B;
      default:
        return CardType.UNKNOWN;
    }
  }
}
