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

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.simsvc.client.service.VsdmClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/client/test")
public class TestController {

  private final PoppTokenRepository poppTokenRepository;
  private final VsdmDataRepository vsdmDataRepository;

  private final VsdmClientService vsdmClientService;

  public TestController(
      final @Autowired PoppTokenRepository poppTokenRepository,
      final @Autowired VsdmDataRepository vsdmDataRepository,
      final @Autowired VsdmClientService vsdmClientService) {
    this.poppTokenRepository = poppTokenRepository;
    this.vsdmDataRepository = vsdmDataRepository;
    this.vsdmClientService = vsdmClientService;
  }

  @GetMapping("/poppToken")
  public String getPoppToken(
      @RequestParam final String terminalId,
      @RequestParam final Integer slotId,
      @RequestParam final String cardId) {
    log.info(
        "getPoppToken called with terminalId: {}, slotId: {}, cardId: {}",
        terminalId,
        slotId,
        cardId);

    return poppTokenRepository.get(terminalId, slotId, cardId);
  }

  @GetMapping("/vsdmData")
  public VsdmCachedValue getVsdmData(
      @RequestParam final String terminalId,
      @RequestParam final Integer slotId,
      @RequestParam final String cardId) {
    return vsdmDataRepository.get(terminalId, slotId, cardId);
  }

  @GetMapping("/readEgk")
  public ResponseEntity<String> readEgk(
      @RequestParam final String terminalId, @RequestParam final Integer egkSlotId)
      throws CardTerminalException {

    final AttachedCard attachedCard = vsdmClientService.getAttachedCard(terminalId, egkSlotId);
    final String egkData = vsdmClientService.loadTruncatedDataFromCard(attachedCard);

    if (egkData == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok(egkData);
  }
}
