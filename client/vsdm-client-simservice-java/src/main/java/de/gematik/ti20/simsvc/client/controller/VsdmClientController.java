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
  public ResponseEntity<?> readVsd(
      @RequestParam final String terminalId,
      @RequestParam final Integer egkSlotId,
      @RequestParam final Integer smcBSlotId,
      @RequestParam(required = false) final Boolean isFhirXml,
      @RequestParam(required = false) final Boolean forceUpdate,
      @RequestHeader(name = "poppToken", required = false) final String poppToken,
      @RequestHeader(name = "If-None-Match", required = false) final String ifNoneMatch) {
    log.info(
        "readVsd initiated with terminalId = {}, egkSlotId={}, smcBSlotId = {}, if-none-match={}",
        terminalId,
        egkSlotId,
        smcBSlotId,
        ifNoneMatch);

    final boolean isFhirXmlValue = (isFhirXml != null) ? isFhirXml : false;
    final boolean forceUpdateValue = (forceUpdate != null) ? forceUpdate : false;

    return vsdmClientService.read(
        terminalId,
        egkSlotId,
        smcBSlotId,
        isFhirXmlValue,
        forceUpdateValue,
        poppToken,
        ifNoneMatch);
  }
}
