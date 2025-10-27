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
package de.gematik.ti20.client.card.terminal.pcsc;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.CardType;
import de.gematik.ti20.client.card.card.CardTypeDetector;
import de.gematik.ti20.client.card.config.PcScConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalType;
import java.util.ArrayList;
import java.util.List;
import javax.smartcardio.ATR;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;

// TODO: ERSTELLT DURCH KI, NICHT GEPRÜFT

/** Implementation of CardTerminal for USB card readers using javax.smartcardio. */
public class PcScCardTerminal extends de.gematik.ti20.client.card.terminal.CardTerminal {

  private final PcScConnectionConfig config;
  private final javax.smartcardio.CardTerminal terminal;

  /**
   * Constructs a new USB card terminal.
   *
   * @param config
   * @param terminal the underlying javax.smartcardio.CardTerminal
   */
  public PcScCardTerminal(PcScConnectionConfig config, javax.smartcardio.CardTerminal terminal) {
    super(config.getName(), CardTerminalType.PCSC);
    this.config = config;
    this.terminal = terminal;
  }

  /** {@inheritDoc} */
  @Override
  public List<? extends AttachedCard> getAttachedCards() throws CardTerminalException {

    List<AttachedCard> cards = new ArrayList<>();

    try {
      if (terminal.isCardPresent()) {
        javax.smartcardio.Card card = terminal.connect("*");
        ATR atr = card.getATR();

        // Generate a unique ID based on the reader name and ATR
        String id = getName() + "-" + bytesToHex(atr.getBytes());

        // Detect the card type based on ATR
        CardType type = CardTypeDetector.detectTypeFromATR(atr);

        PcScAttachedCard usbCard = new PcScAttachedCard(id, type, this, atr);
        cards.add(usbCard);

        // Disconnect the card to avoid resource issues
        card.disconnect(false);
      }
    } catch (CardException e) {
      throw new CardTerminalException("Failed to get available cards", e);
    }

    return cards;
  }

  /** {@inheritDoc} */
  @Override
  public CardConnection connect(AttachedCard card) throws CardTerminalException {

    if (!(card instanceof PcScAttachedCard)) {
      throw new CardTerminalException("Card is not a USB card");
    }

    PcScAttachedCard usbCard = (PcScAttachedCard) card;

    try {
      javax.smartcardio.Card physicalCard = terminal.connect("*");
      CardChannel channel = physicalCard.getBasicChannel();

      return new PcScCardConnection(usbCard, channel);
    } catch (CardException e) {
      throw new CardTerminalException("Failed to connect to card", e);
    }
  }

  /**
   * Converts a byte array to a hexadecimal string.
   *
   * @param bytes the byte array
   * @return the hexadecimal string
   */
  private static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder();
    for (byte b : bytes) {
      hex.append(String.format("%02X", b));
    }
    return hex.toString();
  }
}
