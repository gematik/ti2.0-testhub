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

import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.simsvc.client.service.VsdmClientService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/client/config")
public class ConfigController {

  private final VsdmClientService vsdmClientService;

  public ConfigController(@Autowired VsdmClientService vsdmClientService) {
    this.vsdmClientService = vsdmClientService;
  }

  @PutMapping("/terminal")
  public ResponseEntity<String> setTerminalConnectionConfigs(
      @RequestBody List<CardTerminalConnectionConfig> terminalConnectionConfigs) {

    log.debug(
        "Received terminal connection configs: {}",
        terminalConnectionConfigs.stream().map(CardTerminalConnectionConfig::getName).toList());

    try {
      this.vsdmClientService.setTerminalConnectionConfigs(terminalConnectionConfigs);
    } catch (final Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }

    return ResponseEntity.ok().build();
  }

  @GetMapping("/terminal")
  public ResponseEntity<List<CardTerminalConnectionConfig>> getTerminalConnectionConfigs() {
    try {
      return ResponseEntity.ok(this.vsdmClientService.getTerminalConnectionConfigs());
    } catch (final Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }
}
