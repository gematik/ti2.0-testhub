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

import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.card.apdu.ApduUtil;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of CardConnection for USB card terminals. */
public class PcScCardConnection extends CardConnection {

  private static final Logger log = LoggerFactory.getLogger(PcScCardConnection.class);

  private final CardChannel channel;

  /**
   * Constructs a new USB card connection.
   *
   * @param card the USB card
   * @param channel the card channel
   */
  public PcScCardConnection(PcScAttachedCard card, CardChannel channel) {
    super(card);
    this.channel = channel;
  }

  /** {@inheritDoc} */
  @Override
  public byte[] transmit(byte[] command) throws CardTerminalException {
    if (!isConnected()) {
      throw new CardTerminalException("Connection is closed");
    }

    try {
      CommandAPDU commandApdu = new CommandAPDU(command);
      ResponseAPDU responseApdu = channel.transmit(commandApdu);

      return responseApdu.getBytes();
    } catch (CardException e) {
      throw new CardTerminalException("Failed to transmit command APDU", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation uses the card's digital signature functionality. The actual
   * implementation depends on the specific card type and may require a sequence of APDUs to select
   * the appropriate signature application and perform the signing operation.
   */
  @Override
  public byte[] sign(byte[] data, SignOptions options) throws CardTerminalException {
    if (!isConnected()) {
      throw new CardTerminalException("Connection is closed");
    }

    try {
      PcScAttachedCard card = (PcScAttachedCard) getCard();

      // The actual signing process depends on the card type and may require
      // a sequence of APDUs to select the signature application and perform
      // the signing operation
      switch (card.getType()) {
        case HBA:
          return signWithHBA(data, options);
        case SMC_B:
          return signWithSMCB(data, options);
        case EGK:
          throw new CardTerminalException("EGK does not support signing operations");
        default:
          throw new CardTerminalException("Card type not supported for signing");
      }
    } catch (Exception e) {
      throw new CardTerminalException("Failed to sign data", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    if (isConnected()) {
      try {
        // The javax.smartcardio API requires to disconnect the underlying card
        channel.getCard().disconnect(false);
        super.disconnect(); // Call parent to set connected = false
      } catch (CardException e) {
        log.error("Error disconnecting card connection", e);
      }
    }
  }

  /**
   * Signs data using an HBA card.
   *
   * @param data the data to sign
   * @param options the signature options
   * @return the signature
   * @throws CardTerminalException if signing fails
   */
  private byte[] signWithHBA(byte[] data, SignOptions options) throws CardTerminalException {
    try {
      // Select signature application
      transmit(ApduUtil.buildSelectApplicationApdu("D27600006601"));

      // Verify PIN if needed
      // This step might require user interaction in a real implementation

      // Prepare signing
      byte[] hashData = ApduUtil.hashData(data);

      // Set up security environment with options
      byte[] mseCommand = ApduUtil.createMSESetCommand(options.getKeyReference());
      byte[] mseResponse = transmit(mseCommand);

      if (!ApduUtil.isSuccessResponse(mseResponse)) {
        throw new CardTerminalException(
            "Setup for signing failed: " + ApduUtil.getStatusString(mseResponse));
      }

      // Sign the hash
      byte[] signCommand = ApduUtil.buildSignCommandApdu(hashData);
      byte[] response = transmit(signCommand);

      if (!ApduUtil.isSuccessResponse(response)) {
        throw new CardTerminalException("Signing failed: " + ApduUtil.getStatusString(response));
      }

      return ApduUtil.extractSignature(response);
    } catch (Exception e) {
      throw new CardTerminalException("Error during signing process", e);
    }
  }

  /**
   * Signs data using an SMC-B card.
   *
   * @param data the data to sign
   * @param options the signature options
   * @return the signature
   * @throws CardTerminalException if signing fails
   */
  private byte[] signWithSMCB(byte[] data, SignOptions options) throws CardTerminalException {
    try {
      // The signing process for SMC-B is similar to HBA but might use different AIDs
      // and commands

      // Select signature application
      transmit(ApduUtil.buildSelectApplicationApdu("D27600000601"));

      // Verify PIN if needed
      // This step might require user interaction in a real implementation

      // Set up security environment with options
      byte[] mseCommand = ApduUtil.createMSESetCommand(options.getKeyReference());
      byte[] mseResponse = transmit(mseCommand);

      if (!ApduUtil.isSuccessResponse(mseResponse)) {
        throw new CardTerminalException(
            "Setup for signing failed: " + ApduUtil.getStatusString(mseResponse));
      }

      // Prepare signing
      byte[] hashData = ApduUtil.hashData(data);

      // Sign the hash
      byte[] signCommand = ApduUtil.buildSignCommandApdu(hashData);
      byte[] response = transmit(signCommand);

      if (!ApduUtil.isSuccessResponse(response)) {
        throw new CardTerminalException("Signing failed: " + ApduUtil.getStatusString(response));
      }

      return ApduUtil.extractSignature(response);
    } catch (Exception e) {
      throw new CardTerminalException("Error during signing process", e);
    }
  }
}
