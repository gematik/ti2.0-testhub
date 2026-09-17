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

import de.gematik.ti20.simsvc.client.exception.CardNotFoundException;
import de.gematik.ti20.simsvc.client.model.CardImageData;
import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import de.gematik.ti20.simsvc.client.service.EgkInfoService;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import de.gematik.ti20.simsvc.client.service.SmcBInfoService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for card operations. Provides endpoints for listing cards, establishing
 * connections, transmitting commands, signing data, and closing connections.
 */
@RestController
@RequestMapping("/cards")
public class CardController {

  private final SlotManager slotManager;
  private final SmcBInfoService smcBInfoService;
  private final EgkInfoService egkInfoService;

  /**
   * Constructor for CardController.
   *
   * @param slotManager Service to manage slots
   * @param smcBInfoService Service for SMC-B information extraction
   */
  @Autowired
  public CardController(
      final SlotManager slotManager,
      final SmcBInfoService smcBInfoService,
      final EgkInfoService egkInfoService) {
    this.slotManager = slotManager;
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
    final List<CardHandleDto> cardHandles = slotManager.listAllCards();
    return ResponseEntity.ok(cardHandles);
  }

  /**
   * Get SMC-B card information including Telematik-ID and ProfessionOID.
   *
   * @param cardHandle Card handle
   * @return SMC-B information
   */
  @GetMapping("/{cardHandle}/smc-b-info")
  public ResponseEntity<SmcBInfoDto> getSmcBInfo(@PathVariable final String cardHandle) {
    // Find the card image for the given handle
    final CardImageData card = slotManager.findCardByHandle(cardHandle);

    if (card instanceof VirtualCardImageData virtualCardImageData) {
      final SmcBInfoDto smcBInfoDto = smcBInfoService.extractSmcBInfo(virtualCardImageData);

      if (smcBInfoDto != null) {
        return ResponseEntity.ok(smcBInfoDto);
      }
    }

    throw new CardNotFoundException(cardHandle);
  }

  /**
   * Extract EGK information from the card containing authentic KVNR, IKNR and patient data.
   *
   * @param cardHandle The card handle identifier
   * @return EGK information with real patient data from certificate
   */
  @GetMapping("/{cardHandle}/egk-info")
  public ResponseEntity<EgkInfoDto> getEgkInfo(@PathVariable final String cardHandle) {
    // Find the card image for the given handle
    final CardImageData card = slotManager.findCardByHandle(cardHandle);

    if (card instanceof EgkInfoDto egkInfoDto) {
      return ResponseEntity.ok(egkInfoDto);
    } else if (card instanceof VirtualCardImageData virtualCardImageData) {
      final EgkInfoDto egkInfo = egkInfoService.extractEgkInfo(virtualCardImageData);
      if (egkInfo != null) {
        return ResponseEntity.ok(egkInfo);
      }
    }

    throw new CardNotFoundException(cardHandle);
  }
}
