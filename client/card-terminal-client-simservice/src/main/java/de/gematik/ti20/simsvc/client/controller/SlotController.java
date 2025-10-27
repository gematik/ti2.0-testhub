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
package de.gematik.ti20.simsvc.client.controller;

import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.dto.CardInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.TransmitRequestDto;
import de.gematik.ti20.simsvc.client.model.dto.TransmitResponseDto;
import de.gematik.ti20.simsvc.client.service.CardImageParser;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for managing card slots. Provides endpoints to insert, remove, and query cards in
 * slots.
 */
@RestController
@RequestMapping("/slots")
public class SlotController {

  private final SlotManager slotManager;
  private final CardImageParser cardImageParser;

  /**
   * Constructor for SlotController.
   *
   * @param slotManager Service to manage slots
   * @param cardImageParser Service to parse card images
   */
  @Autowired
  public SlotController(SlotManager slotManager, CardImageParser cardImageParser) {
    this.slotManager = slotManager;
    this.cardImageParser = cardImageParser;
  }

  /**
   * Retrieve information about a card in a specific slot.
   *
   * @param slotId Slot identifier
   * @return Card information if present
   */
  @GetMapping("/{slotId}")
  public ResponseEntity<CardInfoDto> getCardInSlot(@PathVariable int slotId) {
    if (!slotManager.isValidSlotId(slotId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found: " + slotId);
    }

    CardImage card = slotManager.getCardInSlot(slotId);
    if (card == null) {
      return ResponseEntity.noContent().build();
    }

    CardInfoDto cardInfo = createCardInfoDto(card, slotId);
    return ResponseEntity.ok(cardInfo);
  }

  /**
   * Insert a card into a specific slot.
   *
   * @param slotId Slot identifier
   * @param xmlCardData XML representation of the card data
   * @return Information about the inserted card
   */
  @PutMapping(value = "/{slotId}", consumes = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<CardInfoDto> insertCard(
      @PathVariable int slotId, @RequestBody String xmlCardData) {
    if (!slotManager.isValidSlotId(slotId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found: " + slotId);
    }

    if (slotManager.isCardPresent(slotId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already contains a card");
    }

    try {
      CardImage card = cardImageParser.parseCardImage(xmlCardData);
      slotManager.insertCard(slotId, card);

      CardInfoDto cardInfo = createCardInfoDto(card, slotId);
      return ResponseEntity.status(HttpStatus.CREATED).body(cardInfo);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid card data: " + e.getMessage(), e);
    }
  }

  /**
   * Remove a card from a specific slot.
   *
   * @param slotId Slot identifier
   * @return Response with no content if successful
   */
  @DeleteMapping("/{slotId}")
  public ResponseEntity<Void> removeCard(@PathVariable int slotId) {
    if (!slotManager.isValidSlotId(slotId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found: " + slotId);
    }

    if (!slotManager.isCardPresent(slotId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No card present in slot: " + slotId);
    }

    slotManager.removeCard(slotId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Transmit an APDU command to the card in a specific slot.
   *
   * @param slotId Slot identifier
   * @param request TransmitRequestDto containing the APDU command
   * @return Response containing APDU response
   */
  @PostMapping(value = "/{slotId}/transmit")
  public ResponseEntity<TransmitResponseDto> transmitToCardInSlot(
      @PathVariable int slotId, @RequestBody TransmitRequestDto request) {

    if (!slotManager.isValidSlotId(slotId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found: " + slotId);
    }

    if (!slotManager.isCardPresent(slotId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No card present in slot: " + slotId);
    }

    // Get the card handle for this slot
    CardImage card = slotManager.getCardInSlot(slotId);
    String cardHandle = card.getId();

    try {
      // Forward to the card manager's transmit method
      TransmitResponseDto response = slotManager.transmitCommand(slotId, request.getCommand());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Transmit failed: " + e.getMessage());
    }
  }

  /**
   * Helper method to create CardInfoDto from a CardImage.
   *
   * @param card CardImage object
   * @param slotId Slot identifier
   * @return CardInfoDto containing card information
   */
  private CardInfoDto createCardInfoDto(CardImage card, int slotId) {
    return new CardInfoDto(card.getId(), card.getCardType().name(), slotId, card.getLabel());
  }
}
