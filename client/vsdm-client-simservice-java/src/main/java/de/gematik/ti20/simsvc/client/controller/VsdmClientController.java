/*-
 * #%L
 * VSDM Client Simulator Service
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

import de.gematik.ti20.simsvc.client.service.VsdmClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/client/vsdm")
public class VsdmClientController {

  private final VsdmClientService vsdmClientService;

  public VsdmClientController(@Autowired VsdmClientService vsdmClientService) {
    this.vsdmClientService = vsdmClientService;
  }

  @GetMapping("/vsd")
  public ResponseEntity<String> readVsd(
      @RequestParam final String terminalId,
      @RequestParam final int egkSlotId,
      @RequestParam(required = false) final String virtualCard,
      @RequestParam(defaultValue = "false") final boolean isFhirXml,
      @RequestParam(required = false) final String profileVersion,
      @RequestHeader(name = "poppToken", required = false) final String poppToken,
      @RequestHeader(name = "If-None-Match", required = false) final String ifNoneMatch) {
    log.info(
        "readVsd initiated with terminalId = {}, egkSlotId={}, if-none-match={}, profileVersion={}",
        terminalId,
        egkSlotId,
        ifNoneMatch,
        profileVersion);

    return vsdmClientService.read(
        terminalId,
        egkSlotId,
        virtualCard,
        isFhirXml,
        poppToken,
        quoteIfNotQuoted(ifNoneMatch),
        profileVersion);
  }

  private static String quoteIfNotQuoted(final String input) {
    if (input == null) {
      return null;
    }

    if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
      return input; // bereits korrekt gequoted
    }

    return "\"" + input + "\"";
  }
}
