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

import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.card.apdu.ApduUtil;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of CardConnection for Connector card terminals (TI 2.0). */
public class ConnectorCardConnection extends CardConnection {

  private static final Logger log = LoggerFactory.getLogger(ConnectorCardConnection.class);

  private final String connectionHandle;
  private final ConnectorCardTerminal terminal;

  /**
   * Constructs a new Connector card connection.
   *
   * @param card the card
   * @param connectionHandle the connection handle
   * @param terminal the terminal
   */
  public ConnectorCardConnection(
      ConnectorAttachedCard card, String connectionHandle, ConnectorCardTerminal terminal) {
    super(card);
    this.connectionHandle = connectionHandle;
    this.terminal = terminal;
  }

  /** {@inheritDoc} */
  @Override
  public byte[] transmit(byte[] command) throws CardTerminalException {
    if (!isConnected()) {
      throw new CardTerminalException("Card connection is closed");
    }

    try {
      return terminal.transmitAPDU(connectionHandle, command);
    } catch (Exception e) {
      throw new CardTerminalException("Failed to transmit APDU", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public byte[] sign(byte[] data, SignOptions options) throws CardTerminalException {
    if (!isConnected()) {
      throw new CardTerminalException("Card connection is closed");
    }

    try {
      // Use the SignatureService for direct signing through the Connector's SignatureService
      // This is more robust and doesn't require low-level APDU sequence management
      try {
        log.debug(
            "Attempting to sign data using SignatureService with options: algorithm={}, type={}, key={}",
            options.getHashAlgorithm(),
            options.getSignatureType(),
            options.getKeyReference());
        return terminal
            .getSignatureService()
            .sign((ConnectorAttachedCard) getCard(), data, options);
      } catch (Exception e) {
        // Fallback to APDU-based signing if SignatureService is not available
        log.warn("SignatureService signing failed, falling back to APDU-based signing", e);

        // 1. Select the signature application
        byte[] selectCmd = ApduUtil.createSelectApdu(ApduUtil.AID_SIGNATURE_APPLICATION);
        byte[] response = transmit(selectCmd);

        if (!ApduUtil.isSuccess(response)) {
          throw new CardTerminalException(
              "Failed to select signature application: "
                  + ApduUtil.getResponseStatusString(response));
        }

        // 2. Verify PIN (or user verifies PIN on card terminal)
        // For test purposes, this step might be skipped for certain card types

        // 3. Set up security environment for signing
        // Use the key reference from options if provided
        byte[] mseCmd = ApduUtil.createMSESetCommand(options.getKeyReference());
        response = transmit(mseCmd);

        if (!ApduUtil.isSuccess(response)) {
          throw new CardTerminalException(
              "Failed to set up security environment: "
                  + ApduUtil.getResponseStatusString(response));
        }

        // 4. Prepare data to be signed (hash and format according to specifications)
        byte[] hash = ApduUtil.createHashFromData(data);

        // 5. Sign the hash
        byte[] signCmd = ApduUtil.createSignCommand(hash);
        response = transmit(signCmd);

        if (!ApduUtil.isSuccess(response)) {
          throw new CardTerminalException(
              "Failed to sign data: " + ApduUtil.getResponseStatusString(response));
        }

        // Extract signature data from response (without status bytes)
        return ApduUtil.extractDataFromResponse(response);
      }
    } catch (CardTerminalException e) {
      throw e;
    } catch (Exception e) {
      throw new CardTerminalException("Failed to sign data", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    if (!isConnected()) {
      return;
    }

    try {
      terminal.disconnect(connectionHandle);
      super.disconnect(); // Set connected flag to false
      log.debug("Card connection disconnected");
    } catch (Exception e) {
      log.error("Failed to disconnect card connection", e);
    }
  }
}
