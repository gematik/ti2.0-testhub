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
package de.gematik.ti20.client.card.terminal.connector.signature;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.terminal.CardTerminalException;

/**
 * Interface for the Connector signature service. This service provides methods to sign data
 * directly using the Connector's SignatureService functionality, without the need for manual APDU
 * sequences.
 */
public interface SignatureService {

  /**
   * Signs the provided data with the specified card using the given options.
   *
   * @param card the card to use for signing
   * @param data the data to sign
   * @param options the signature options containing signature type, hash algorithm, etc.
   * @return the signature bytes
   * @throws CardTerminalException if signing fails
   */
  byte[] sign(AttachedCard card, byte[] data, SignOptions options) throws CardTerminalException;
}
