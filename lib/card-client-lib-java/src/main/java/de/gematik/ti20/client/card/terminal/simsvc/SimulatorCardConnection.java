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

import de.gematik.ti20.client.card.card.CardCertInfo;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Implementation of CardConnection for simulated card terminals. */
public class SimulatorCardConnection extends CardConnection {

  private static final Logger log = LoggerFactory.getLogger(SimulatorCardConnection.class);

  private final SimulatorCardTerminal terminal;
  private final CardConnectionInfo cardConnectionInfo;

  /**
   * Constructs a new simulator card connection.
   *
   * @param card the simulator card
   * @param terminal the simulator card terminal
   */
  public SimulatorCardConnection(
      SimulatorAttachedCard card, SimulatorCardTerminal terminal, CardConnectionInfo cci) {
    super(card);
    this.terminal = terminal;
    cardConnectionInfo = cci;
  }

  public CardConnectionInfo getCardConnectionInfo() {
    return cardConnectionInfo;
  }

  /** {@inheritDoc} */
  @Override
  public byte[] transmit(byte[] command) throws CardTerminalException {
    ensureConnected();

    try {
      var response =
          terminal
              .getClient()
              .transmitApdu(getCard().getId(), new SimulatorClient.ApduRequest(command));
      return response.getResponse();
    } catch (IOException e) {
      throw new CardTerminalException("Failed to transmit APDU", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation retrieves the certificate contained in the card image
   */
  @Override
  public CardCertInfo getCertInfo() throws CardTerminalException {
    ensureConnected();

    try {
      return terminal.getClient().getCertInfo(getCard().getId());
    } catch (IOException e) {
      throw new CardTerminalException("Failed to get certificate", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation simulates the signing process for different card types.
   */
  @Override
  public byte[] sign(byte[] data, SignOptions options) throws CardTerminalException {
    ensureConnected();

    try {
      return terminal.getClient().signData(getCard().getId(), data, options);
    } catch (IOException e) {
      throw new CardTerminalException("Failed to sign", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void disconnect() {
    if (!isConnected()) {
      return;
    }

    try {
      this.terminal.getClient().disconnectFromCard(this.getCard());
    } catch (Exception e) {
      log.error("Failed to disconnect card connection", e);
    }

    super.disconnect();
  }
}
