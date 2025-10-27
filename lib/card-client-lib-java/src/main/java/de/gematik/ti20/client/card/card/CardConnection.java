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

import de.gematik.ti20.client.card.terminal.CardTerminalException;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class representing a connection to a card attached to a terminal. Provides methods
 * to interact with the card.
 */
public abstract class CardConnection {

  protected final AttachedCard card;
  protected boolean connected;

  /**
   * Constructs a new card connection.
   *
   * @param card the card to connect to
   */
  protected CardConnection(AttachedCard card) {
    this.card = card;
    this.connected = true;
  }

  /**
   * Returns the card associated with this connection.
   *
   * @return the card
   */
  public AttachedCard getCard() {
    return card;
  }

  /**
   * Checks if the connection to the card is still active.
   *
   * @return true if connected, false otherwise
   */
  public boolean isConnected() {
    return connected;
  }

  /**
   * Transmits the command APDU to the card and returns the response APDU.
   *
   * @param command the command APDU
   * @return the response APDU
   * @throws CardTerminalException if transmission fails
   */
  public abstract byte[] transmit(byte[] command) throws CardTerminalException;

  /**
   * Signs the provided data using the card. Default implementation creates a basic signature.
   * Subclasses may override for card-specific signature algorithms.
   *
   * @param data the data to sign
   * @param options the signature options to use
   * @return the signature
   * @throws CardTerminalException if signing fails
   */
  public byte[] sign(byte[] data, SignOptions options) throws CardTerminalException {
    if (!connected) {
      throw new CardTerminalException("Card connection is closed");
    }

    // Default implementation can be overridden by subclasses
    // for specific signature algorithms
    // This is a placeholder - actual implementation depends on card type
    // and will be provided by subclasses
    throw new UnsupportedOperationException("Sign operation not implemented for this card type");
  }

  /**
   * Signs the provided data using the card with default signature options.
   *
   * @param data the data to sign
   * @return the signature
   * @throws CardTerminalException if signing fails
   */
  public byte[] sign(byte[] data) throws CardTerminalException {
    return sign(data, new SignOptions());
  }

  /**
   * Signs the provided data string using the card. The string will be encoded as UTF-8 bytes before
   * signing.
   *
   * @param data the string data to sign
   * @param options the signature options to use
   * @return the signature
   * @throws CardTerminalException if signing fails
   */
  public byte[] sign(String data, SignOptions options) throws CardTerminalException {
    return sign(data.getBytes(StandardCharsets.UTF_8), options);
  }

  /**
   * Returns various information fields from the default card certificate. This method should be
   * overridden by subclasses to provide the actual certificate data. TODO: add parameters for
   * getting specific certificates, not only the default one
   *
   * @return
   */
  public CardCertInfo getCertInfo() throws CardTerminalException {
    // Default implementation can be overridden by subclasses for specific getCertInfo
    // implementations
    // This is a placeholder - actual implementation depends on card type and will be provided by
    // subclasses
    throw new UnsupportedOperationException("getCertInfo() not implemented for this card type");
  }

  /**
   * Disconnects the connection to the card. This implementation just sets the connected flag to
   * false. Subclasses should override to provide proper resource cleanup.
   */
  public void disconnect() {
    connected = false;
  }

  protected void ensureConnected() throws CardTerminalException {
    if (!connected) {
      throw new CardTerminalException("Card connection is closed");
    }
  }
}
