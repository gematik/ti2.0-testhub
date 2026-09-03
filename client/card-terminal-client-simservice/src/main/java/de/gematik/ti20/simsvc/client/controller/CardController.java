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

import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import de.gematik.ti20.simsvc.client.service.CardManager;
import de.gematik.ti20.simsvc.client.service.EgkInfoService;
import de.gematik.ti20.simsvc.client.service.SmcBInfoService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for card operations. Provides endpoints for listing cards, establishing
 * connections, transmitting commands, signing data, and closing connections.
 */
@RestController
@RequestMapping("/cards")
public class CardController {

  private static final Logger logger = LoggerFactory.getLogger(CardController.class);

  private final CardManager cardManager;
  private final SmcBInfoService smcBInfoService;
  private final EgkInfoService egkInfoService;

  /**
   * Constructor for CardController.
   *
   * @param cardManager Service to manage cards and connections
   * @param smcBInfoService Service for SMC-B information extraction
   */
  @Autowired
  public CardController(
      CardManager cardManager, SmcBInfoService smcBInfoService, EgkInfoService egkInfoService) {
    this.cardManager = cardManager;
    this.smcBInfoService = smcBInfoService;
    this.egkInfoService = egkInfoService;
  }

  /**
   * List all available cards across all slots.
   *
   * @return List of card handles
   */
  @GetMapping("/")
  public ResponseEntity<List<CardHandleDto>> listCards() {
    List<CardHandleDto> cardHandles = cardManager.listAllCards();
    return ResponseEntity.ok(cardHandles);
  }

  /**
   * Get SMC-B card information including Telematik-ID and ProfessionOID.
   *
   * @param cardHandle Card handle
   * @return SMC-B information
   */
  @GetMapping("/{cardHandle}/smc-b-info")
  public ResponseEntity<SmcBInfoDto> getSmcBInfo(@PathVariable String cardHandle) {
    SmcBInfoDto smcBInfo = smcBInfoService.extractSmcBInfo(cardHandle);
    return ResponseEntity.ok(smcBInfo);
  }

  /**
   * Extract EGK information from the card containing authentic KVNR, IKNR and patient data.
   *
   * @param cardHandle The card handle identifier
   * @return EGK information with real patient data from certificate
   */
  @GetMapping("/{cardHandle}/egk-info")
  public ResponseEntity<?> getEgkInfo(@PathVariable final String cardHandle) {
    try {
      // Find the card image for the given handle
      final CardImage card = cardManager.findCardByHandle(cardHandle);
      if (card == null) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("error", "Card not found");
        errorInfo.put("message", "No card found for handle: " + cardHandle);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorInfo);
      }

      final EgkInfoDto egkInfo = egkInfoService.extractEgkInfo(card);
      return ResponseEntity.ok(egkInfo);
    } catch (Exception e) {
      Map<String, Object> errorInfo = new HashMap<>();
      errorInfo.put("error", "Internal Server Error");
      errorInfo.put("message", "An unexpected error occurred");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorInfo);
    }
  }
}
