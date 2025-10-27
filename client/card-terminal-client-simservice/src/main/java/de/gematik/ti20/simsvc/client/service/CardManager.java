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
package de.gematik.ti20.simsvc.client.service;

import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import de.gematik.ti20.simsvc.client.model.dto.ConnectionPropertiesDto;
import de.gematik.ti20.simsvc.client.model.dto.TransmitResponseDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service for managing card connections and operations. */
@Service
public class CardManager {

  private static final Logger logger = LoggerFactory.getLogger(CardManager.class);

  private final SlotManager slotManager;
  private final ApduProcessor apduProcessor;
  private final Map<String, CardConnection> connections;

  /**
   * Constructor for CardManager.
   *
   * @param slotManager Service to manage slots
   * @param apduProcessor APDU processor service
   */
  @Autowired
  public CardManager(SlotManager slotManager, ApduProcessor apduProcessor) {
    this.slotManager = slotManager;
    this.apduProcessor = apduProcessor;
    this.connections = new ConcurrentHashMap<>();
  }

  /**
   * List all available cards across all slots.
   *
   * @return List of card handles
   */
  public List<CardHandleDto> listAllCards() {
    List<CardHandleDto> cardHandles = new ArrayList<>();

    for (int slotId = 0; slotId < slotManager.getSlotCount(); slotId++) {
      if (slotManager.isCardPresent(slotId)) {
        CardImage card = slotManager.getCardInSlot(slotId);
        String cardHandle = generateCardHandle(card);

        CardHandleDto cardHandleDto =
            new CardHandleDto(cardHandle, card.getCardType().name(), slotId, card.getLabel());

        cardHandles.add(cardHandleDto);
      }
    }

    return cardHandles;
  }

  /**
   * Establish a virtual connection to a card.
   *
   * @param cardHandle Card handle identifier
   * @return Connection properties
   */
  public ConnectionPropertiesDto connectToCard(String cardHandle) {
    // Check if the card handle is valid
    CardImage card = findCardByHandle(cardHandle);
    if (card == null) {
      throw new de.gematik.ti20.simsvc.client.exception.CardNotFoundException(cardHandle);
    }

    // Allow multiple non-exclusive connections to the same card
    // If already connected, return existing connection properties
    if (!connections.containsKey(cardHandle)) {
      // Find the slot ID for this card
      int slotId = findSlotIdForCard(card);
      // Create a new connection only if one doesn't exist
      CardConnection connection = new CardConnection(card, slotId);
      connections.put(cardHandle, connection);
    }

    // Get the ATR (Answer to Reset)
    String atr = getAtrForCard(card);

    // Return connection properties
    return new ConnectionPropertiesDto(
        cardHandle,
        atr,
        "T=1", // Default protocol
        false // Not exclusive by default
        );
  }

  /**
   * Transmit an APDU command to a connected card.
   *
   * @param cardHandle Card handle identifier
   * @param commandHex APDU command as a hex string
   * @return Response containing APDU response
   */
  public TransmitResponseDto transmitCommand(String cardHandle, String commandHex) {
    logger.debug("Transmitting APDU command: {} for card: {}", commandHex, cardHandle);

    try {
      logger.debug("Processing APDU command: {} for card: {}", commandHex, cardHandle);

      // Check if the card is connected
      CardConnection connection = connections.get(cardHandle);
      if (connection == null) {
        logger.error("Card not connected: {}", cardHandle);
        throw new de.gematik.ti20.simsvc.client.exception.CardNotConnectedException(cardHandle);
      }

      logger.debug("Card connection found for handle: {}", cardHandle);

      // Parse the command with error handling
      ApduCommand command;
      try {
        command = ApduCommand.fromHex(commandHex);
        logger.debug(
            "Successfully parsed APDU command: CLA={}, INS={}, P1={}, P2={}, Le={}",
            String.format("%02X", command.getCla()),
            String.format("%02X", command.getIns()),
            String.format("%02X", command.getP1()),
            String.format("%02X", command.getP2()),
            command.getLe());
      } catch (Exception parseError) {
        logger.error("Failed to parse APDU command '{}': {}", commandHex, parseError.getMessage());
        throw new IllegalArgumentException(
            "Invalid APDU command format: " + parseError.getMessage());
      }

      // Process the command using the ApduProcessor
      ApduResponse response;
      try {
        response = apduProcessor.processCommand(connection.getCard(), command);
        logger.debug("APDU response received: SW={}", response.getStatusWordHex());
      } catch (Exception processError) {
        logger.error("Failed to process APDU command: {}", processError.getMessage(), processError);
        throw new RuntimeException("APDU processing failed: " + processError.getMessage());
      }

      // Convert the response to hex strings
      String responseHex = response.toHex();
      String statusWordHex = response.getStatusWordHex();
      String responseData = "";

      if (response.getData() != null && response.getData().length > 0) {
        responseData = Hex.encodeHexString(response.getData()).toUpperCase();
        logger.debug("Response data length: {} bytes", response.getData().length);
      }

      // Create the response DTO
      TransmitResponseDto result =
          new TransmitResponseDto(
              responseHex, statusWordHex, response.getStatusMessage(), responseData);

      logger.debug("Successfully created transmit response");
      return result;

    } catch (Exception e) {
      logger.error("Error during APDU transmission for card {}: {}", cardHandle, e.getMessage(), e);
      throw e; // Re-throw to let GlobalExceptionHandler handle it
    }
  }

  /**
   * Close a virtual connection to a card.
   *
   * @param cardHandle Card handle identifier
   */
  public void disconnectCard(String cardHandle) {
    // Check if the card is connected
    if (!connections.containsKey(cardHandle)) {
      throw new de.gematik.ti20.simsvc.client.exception.CardNotConnectedException(cardHandle);
    }

    // Remove the connection
    connections.remove(cardHandle);
  }

  /**
   * Find a card by its handle.
   *
   * @param cardHandle Card handle to find
   * @return CardImage or null if not found
   */
  public CardImage findCardByHandle(String cardHandle) {
    for (int slotId = 0; slotId < slotManager.getSlotCount(); slotId++) {
      if (slotManager.isCardPresent(slotId)) {
        CardImage card = slotManager.getCardInSlot(slotId);
        String generatedHandle = generateCardHandle(card);

        if (generatedHandle.equals(cardHandle)) {
          return card;
        }
      }
    }

    return null;
  }

  /**
   * Find the slot ID for a given card.
   *
   * @param targetCard Card to find slot for
   * @return Slot ID or 0 if not found
   */
  private int findSlotIdForCard(CardImage targetCard) {
    for (int slotId = 0; slotId < slotManager.getSlotCount(); slotId++) {
      if (slotManager.isCardPresent(slotId)) {
        CardImage card = slotManager.getCardInSlot(slotId);
        if (card != null && card.equals(targetCard)) {
          return slotId;
        }
      }
    }
    return 0; // Default to slot 0 if not found
  }

  /**
   * Generate a unique card handle.
   *
   * @param card Card image
   * @return Card handle
   */
  private String generateCardHandle(CardImage card) {
    // Use the card ID if available, otherwise generate a random UUID
    if (card.getId() != null && !card.getId().isEmpty()) {
      return card.getId();
    }

    return UUID.randomUUID().toString();
  }

  /**
   * Get the ATR (Answer to Reset) for a card.
   *
   * @param card Card image
   * @return ATR as a hex string
   */
  private String getAtrForCard(CardImage card) {
    // In a real implementation, this would extract the ATR from the card data
    // For now, return a simulated ATR based on card type
    switch (card.getCardType()) {
      case EGK:
        return "3B8F80018031C0730F0161FF0143C103000300300400";
      case HBA:
        return "3B9F0080318065B0870401625F0104C03F0073CF";
      case HPIC:
        return "3B9F0080318065B0880401625F0104C03F0073CF";
      default:
        return "3B0000"; // Default ATR
    }
  }

  /** Inner class representing a connection to a card. */
  private static class CardConnection {
    private final CardImage card;
    private final int slotId;

    /**
     * Constructor for CardConnection.
     *
     * @param card Card image
     * @param slotId Slot ID
     */
    public CardConnection(CardImage card, int slotId) {
      this.card = card;
      this.slotId = slotId;
    }

    /**
     * Get the card image.
     *
     * @return Card image
     */
    public CardImage getCard() {
      return card;
    }

    /**
     * Get the slot ID.
     *
     * @return Slot ID
     */
    public int getSlotId() {
      return slotId;
    }
  }
}
