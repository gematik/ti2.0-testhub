/*-
 * #%L
 * Card Terminal Simulator
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.client.model.CardImageData;
import de.gematik.ti20.simsvc.client.model.EgkIdentifier;
import de.gematik.ti20.simsvc.client.model.SmcbIdentifier;
import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.CardInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.repository.CardImageIdentifier;
import de.gematik.ti20.simsvc.client.repository.CardImageRepository;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import de.gematik.ti20.simsvc.client.service.VirtualCardImageLoader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
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
@Slf4j
public class SlotController {

  private final SlotManager slotManager;
  private final CardImageRepository cardImageRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final VirtualCardImageLoader cardImageLoader = new VirtualCardImageLoader();

  /**
   * Constructor for SlotController.
   *
   * @param slotManager Service to manage slots
   */
  @Autowired
  public SlotController(
      final SlotManager slotManager, final CardImageRepository cardImageRepository) {
    this.slotManager = slotManager;
    this.cardImageRepository = cardImageRepository;
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

    final CardImageData card = slotManager.getCardInSlot(slotId);
    if (card == null) {
      return ResponseEntity.noContent().build();
    }

    final CardInfoDto cardInfo = CardInfoDto.from(card, slotId);
    return ResponseEntity.ok(cardInfo);
  }

  /**
   * Insert a card into a specific slot.
   *
   * @param slotId Slot identifier
   * @param xmlCardData XML representation of the card data
   * @return Information about the inserted card
   */
  @Deprecated(since = "3.20.0", forRemoval = true)
  @PutMapping(value = "/{slotId}", consumes = MediaType.APPLICATION_XML_VALUE)
  public ResponseEntity<CardInfoDto> insertCard(
      @PathVariable int slotId, @RequestBody String xmlCardData) {
    log.info("Inserting card as XML card image");

    checkValidSlot(slotId);

    try {
      final VirtualCardImageData card = cardImageLoader.load(xmlCardData);
      slotManager.insertCard(slotId, card);

      final CardInfoDto cardInfo = CardInfoDto.from(card, slotId);
      return ResponseEntity.status(HttpStatus.CREATED).body(cardInfo);
    } catch (final Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid card data: " + e.getMessage(), e);
    }
  }

  /**
   * Insert a card into a specific slot.
   *
   * @param slotId Slot identifier
   * @param jsonCardData JSON representation of the card data
   * @return Information about the inserted card
   */
  @Deprecated(since = "3.20.0", forRemoval = true)
  @PutMapping(value = "/{slotId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CardInfoDto> insertCardDataJson(
      @PathVariable final int slotId, @RequestBody final String jsonCardData) {
    log.info("Inserting card as JSON card data: {}", jsonCardData);

    checkValidSlot(slotId);

    try {
      final EgkInfoDto egkInfoDto = objectMapper.readValue(jsonCardData, EgkInfoDto.class);
      slotManager.insertCard(slotId, egkInfoDto);

      final CardInfoDto cardInfo = CardInfoDto.from(egkInfoDto, slotId);
      return ResponseEntity.status(HttpStatus.CREATED).body(cardInfo);
    } catch (final Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid card data: " + e.getMessage(), e);
    }
  }

  /**
   * Insert a card into a specific slot.
   *
   * @param slotId Slot identifier
   * @param egkIdentifier IKNR and KVNR used to look up the card image
   * @return Information about the inserted card
   */
  @PutMapping(value = "/{slotId}/egk", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CardInfoDto> insertEgk(
      @PathVariable final int slotId, @RequestBody final EgkIdentifier egkIdentifier) {
    log.info("Inserting card by egkIdentifier: {}", egkIdentifier);

    checkValidSlot(slotId);

    final List<CardImageIdentifier> matchingCards =
        cardImageRepository.findByEgkIdentifier(egkIdentifier);
    if (matchingCards.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "No card image found for the provided EgkIdentifier: " + egkIdentifier);
    }
    if (matchingCards.size() > 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Multiple card images found for the provided EgkIdentifier: " + egkIdentifier);
    }
    final CardImageIdentifier matchingCard = matchingCards.get(0);

    try {
      final String xmlCardData =
          new DefaultResourceLoader()
              .getResource(matchingCard.path())
              .getContentAsString(StandardCharsets.UTF_8);

      final VirtualCardImageData card = cardImageLoader.load(xmlCardData);
      slotManager.insertCard(slotId, card);

      final CardInfoDto cardInfo = CardInfoDto.from(card, slotId);
      return ResponseEntity.status(HttpStatus.CREATED).body(cardInfo);
    } catch (final Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid card data: " + e.getMessage(), e);
    }
  }

  /**
   * Insert a card into a specific slot.
   *
   * @param slotId Slot identifier
   * @param smcbIdentifier IKNR and KVNR used to look up the card image
   * @return Information about the inserted card
   */
  @PutMapping(value = "/{slotId}/smcb", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<CardInfoDto> insertSmcb(
      @PathVariable final int slotId, @RequestBody final SmcbIdentifier smcbIdentifier) {
    log.info("Inserting card by smcbIdentifier: {}", smcbIdentifier);

    checkValidSlot(slotId);

    final List<CardImageIdentifier> matchingCards =
        cardImageRepository.findBySmcbIdentifier(smcbIdentifier);
    if (matchingCards.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "No card image found for the provided SmcbIdentifier: " + smcbIdentifier);
    }
    if (matchingCards.size() > 1) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Multiple card images found for the provided SmcbIdentifier: " + smcbIdentifier);
    }
    final CardImageIdentifier matchingCard = matchingCards.get(0);

    try {
      final String xmlCardData =
          new DefaultResourceLoader()
              .getResource(matchingCard.path())
              .getContentAsString(StandardCharsets.UTF_8);

      final VirtualCardImageData card = cardImageLoader.load(xmlCardData);
      slotManager.insertCard(slotId, card);

      final CardInfoDto cardInfo = CardInfoDto.from(card, slotId);
      return ResponseEntity.status(HttpStatus.CREATED).body(cardInfo);
    } catch (final Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid card data: " + e.getMessage(), e);
    }
  }

  private void checkValidSlot(final int slotId) {
    if (!slotManager.isValidSlotId(slotId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found: " + slotId);
    }

    if (slotManager.isCardPresent(slotId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot already contains a card");
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
}
