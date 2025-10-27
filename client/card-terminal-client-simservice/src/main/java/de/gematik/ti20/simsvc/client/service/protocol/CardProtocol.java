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
package de.gematik.ti20.simsvc.client.service.protocol;

import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;

/**
 * Interface for all card protocols that can be executed on a smart card. This defines the contract
 * that specific protocol implementations must follow.
 */
public interface CardProtocol {

  /**
   * Checks if this protocol can handle the given APDU command.
   *
   * @param command The APDU command to check
   * @return true if this protocol can handle the command, false otherwise
   */
  boolean canHandle(ApduCommand command);

  /**
   * Processes an APDU command according to this protocol.
   *
   * @param card The card image to process the command against
   * @param command The APDU command to process
   * @return The APDU response
   */
  ApduResponse processCommand(CardImage card, ApduCommand command);

  /**
   * Resets the protocol state. This should be called when a card is removed or the session ends.
   */
  void reset();

  /**
   * Gets the name of this protocol.
   *
   * @return The protocol name
   */
  String getProtocolName();
}
