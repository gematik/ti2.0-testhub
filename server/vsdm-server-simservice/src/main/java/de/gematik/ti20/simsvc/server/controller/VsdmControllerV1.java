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
package de.gematik.ti20.simsvc.server.controller;

import de.gematik.ti20.simsvc.server.exception.ErrorCase;
import de.gematik.ti20.simsvc.server.service.ChecksumService;
import de.gematik.ti20.simsvc.server.service.EtagService;
import de.gematik.ti20.simsvc.server.service.FhirService;
import de.gematik.ti20.simsvc.server.service.VsdmService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/vsdservice/v1")
public class VsdmControllerV1 {

  private final VsdmService vsdmService;
  private final FhirService fhirService;
  private final ChecksumService checksumService;
  private final EtagService etagService;

  public VsdmControllerV1(
      @Autowired VsdmService vsdmService,
      @Autowired FhirService fhirService,
      @Autowired ChecksumService checksumService,
      @Autowired EtagService etagService) {
    this.vsdmService = vsdmService;
    this.fhirService = fhirService;
    this.checksumService = checksumService;
    this.etagService = etagService;
  }

  @GetMapping(value = "/vsdmbundle", produces = "application/fhir+json")
  public ResponseEntity<?> vsdmbundle(final HttpServletRequest request) {
    log.debug("Received request for readVsd");
    validateHeaders(request);

    final HttpHeaders responseHeaders = new HttpHeaders();

    final String kvnr = vsdmService.readKVNR(request);

    if (etagService.checkEtag(kvnr, request)) {
      return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_MODIFIED);
    }

    final Resource fhirResourceOut = vsdmService.readVsd(request);
    final String responseBody =
        fhirService.encodeResponse(fhirResourceOut, request, responseHeaders);

    log.debug("Response for readVsd: {}", responseBody);

    checksumService.addChecksumHeader(responseBody, responseHeaders);
    etagService.addEtagHeader(kvnr, responseBody, responseHeaders);

    responseHeaders.add("Content-Type", "application/fhir+json");
    return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.OK);
  }

  private void validateHeaders(final HttpServletRequest request) {
    if (request.getHeader("zeta-popp-token-content") == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, ErrorCase.VSDSERVICE_MISSING_OR_INVALID_HEADER.getBdeReference());
    }
    if (request.getHeader("zeta-user-info") == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, ErrorCase.VSDSERVICE_MISSING_OR_INVALID_HEADER.getBdeReference());
    }
    if (request.getHeader("if-none-match") == null) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_REQUIRED,
          ErrorCase.VSDSERVICE_INVALID_PATIENT_RECORD_VERSION.getBdeReference());
    }
  }
}
