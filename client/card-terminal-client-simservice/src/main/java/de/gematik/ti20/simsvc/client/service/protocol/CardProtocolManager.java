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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Manager for all card protocols. This class acts as a facade for the different protocol
 * implementations and delegates APDU commands to the appropriate protocol handler.
 */
@Service
public class CardProtocolManager {

  private static final Logger logger = LoggerFactory.getLogger(CardProtocolManager.class);

  private final List<CardProtocol> protocols;

  /**
   * Create a new CardProtocolManager with the available protocol implementations.
   *
   * @param protocols List of all available card protocol implementations
   */
  @Autowired
  public CardProtocolManager(List<CardProtocol> protocols) {
    this.protocols = protocols;
    logger.debug("Initialized CardProtocolManager with {} protocols", protocols.size());
    protocols.forEach(p -> logger.debug("Registered protocol: {}", p.getProtocolName()));
  }

  /**
   * Process an APDU command by delegating to the appropriate protocol handler.
   *
   * @param card The card image to process the command against
   * @param command The APDU command to process
   * @return The APDU response, or null if no protocol can handle the command
   */
  public ApduResponse processCommand(CardImage card, ApduCommand command) {
    // Find the first protocol that can handle this command
    for (CardProtocol protocol : protocols) {
      if (protocol.canHandle(command)) {
        logger.debug("Delegating command to {} protocol", protocol.getProtocolName());
        return protocol.processCommand(card, command);
      }
    }

    // No protocol could handle this command
    logger.debug("No protocol handler found for command: {}", command);
    return null;
  }

  /** Reset all protocols. This should be called when a card is removed or the session ends. */
  public void resetAllProtocols() {
    protocols.forEach(CardProtocol::reset);
    logger.debug("All protocols have been reset");
  }

  /**
   * Get a specific protocol by its name.
   *
   * @param protocolName The name of the protocol to get
   * @return The protocol, or null if not found
   */
  public CardProtocol getProtocolByName(String protocolName) {
    return protocols.stream()
        .filter(p -> p.getProtocolName().equalsIgnoreCase(protocolName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Get the PACE protocol service.
   *
   * @return The PACE protocol service, or null if not available
   */
  public PaceProtocolService getPaceProtocol() {
    return (PaceProtocolService) getProtocolByName("PACE");
  }

  /**
   * Get the signature protocol service.
   *
   * @return The signature protocol service, or null if not available
   */
  public SignatureProtocolService getSignatureProtocol() {
    return (SignatureProtocolService) getProtocolByName("SIGNATURE");
  }
}
