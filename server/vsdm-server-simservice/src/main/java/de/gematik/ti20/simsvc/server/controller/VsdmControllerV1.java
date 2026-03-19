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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.server.config.VsdmConfig;
import de.gematik.ti20.simsvc.server.exception.ErrorCase;
import de.gematik.ti20.simsvc.server.model.PoppTokenContent;
import de.gematik.ti20.simsvc.server.service.ChecksumService;
import de.gematik.ti20.simsvc.server.service.EtagService;
import de.gematik.ti20.simsvc.server.service.FhirService;
import de.gematik.ti20.simsvc.server.service.VsdmService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.sparql.function.library.leviathan.root;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/vsdservice/v1")
public class VsdmControllerV1 {

  private final VsdmConfig vsdmConfig;
  private final VsdmService vsdmService;
  private final FhirService fhirService;
  private final ChecksumService checksumService;
  private final EtagService etagService;

  private static final Pattern VALID_IKNR_PATTERN = Pattern.compile("^[0-9]{9}$");
  private static final Pattern VALID_KVNR_PATTERN = Pattern.compile("^[A-Z][0-9]{8}[A-Z,0-9]$");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public VsdmControllerV1(
      @Autowired VsdmConfig vsdmConfig,
      @Autowired VsdmService vsdmService,
      @Autowired FhirService fhirService,
      @Autowired ChecksumService checksumService,
      @Autowired EtagService etagService) {
    this.vsdmConfig = vsdmConfig;
    this.vsdmService = vsdmService;
    this.fhirService = fhirService;
    this.checksumService = checksumService;
    this.etagService = etagService;
  }

  @GetMapping(value = "/vsdmbundle", produces = "application/fhir+json")
  public ResponseEntity<?> vsdmbundle(
      @RequestHeader(value = "zeta-popp-token-content", required = false)
          final String poppTokenContentCoded,
      @RequestHeader(value = "zeta-user-info", required = false, defaultValue = "mock-user-info")
          final String _userInfo,
      @RequestHeader(value = "if-none-match", required = false, defaultValue = "0")
          final String ifNoneMatch,
      final HttpServletRequest request) {
    log.info("Received request for readVsd");
    validateHeaders(request);

    final HttpHeaders responseHeaders = new HttpHeaders();

    final PoppTokenContent poppTokenContent = parsePoppTokenContent(poppTokenContentCoded);
    final String kvnr = poppTokenContent.getPatientId();

    if (etagService.checkEtag(kvnr, ifNoneMatch)) {
      responseHeaders.set(HttpHeaders.ETAG, ifNoneMatch);
      responseHeaders.set(ChecksumService.HEADER_NAME, checksumService.calculateChecksum(kvnr));
      return new ResponseEntity<>(responseHeaders, HttpStatus.NOT_MODIFIED);
    }

    final Resource fhirResourceOut = vsdmService.readVsd(kvnr);
    final String responseBody =
        fhirService.encodeResponse(fhirResourceOut, request, responseHeaders);

    log.debug("Response for readVsd: {}", responseBody);

    checksumService.addChecksumHeader(kvnr, responseHeaders);
    etagService.addEtagHeader(kvnr, responseBody, responseHeaders);

    responseHeaders.add("Content-Type", "application/fhir+json");
    return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.OK);
  }

  private void validateHeaders(final HttpServletRequest request) {
    if (request.getHeader("zeta-popp-token-content") == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-popp-token-content"));
    }
    if (request.getHeader("zeta-user-info") == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-user-info"));
    }
    if (request.getHeader("if-none-match") == null) {
      throw new ResponseStatusException(
          HttpStatus.PRECONDITION_REQUIRED,
          ErrorCase.VSDSERVICE_INVALID_PATIENT_RECORD_VERSION.getBdeReference());
    }
  }

  private String checkAndGetKvnr(final JsonNode claims) {
    final String kvnr = claims.path("patientId").asText(null);
    if (kvnr == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-popp-token-content"));
    }
    if (!VALID_KVNR_PATTERN.matcher(kvnr).matches()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, ErrorCase.VSDSERVICE_INVALID_KVNR.getBdeReference());
    }

    if (kvnr.startsWith(vsdmConfig.getUnknownKvnrPrefix())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, ErrorCase.VSDSERVICE_UNKNOWN_KVNR.getBdeReference());
    }

    return kvnr;
  }

  private String checkAndGetIknr(final JsonNode claims) {
    final String iknr = claims.path("insurerId").asText(null);
    if (iknr == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-popp-token-content"));
    }
    if (!VALID_IKNR_PATTERN.matcher(iknr).matches()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, ErrorCase.VSDSERVICE_INVALID_IK.getBdeReference());
    }
    if (!iknr.equals(vsdmConfig.getIknr())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, ErrorCase.VSDSERVICE_UNKNOWN_IK.getBdeReference());
    }

    return iknr;
  }

  private PoppTokenContent parsePoppTokenContent(final String poppTokenContent) {
    try {
      final byte[] decoded = Base64.getDecoder().decode(poppTokenContent);
      final String json = new String(decoded, StandardCharsets.UTF_8);
      final JsonNode root = OBJECT_MAPPER.readTree(json);

      final String insurerId = checkAndGetIknr(root);
      final String patientId = checkAndGetKvnr(root);

      return new PoppTokenContent(insurerId, patientId);
    } catch (final IllegalArgumentException e) {
      // Base64 decoding failed
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-popp-token-content"));
    } catch (final ResponseStatusException e) {
      throw e;
    } catch (final Exception e) {
      // JSON parsing or other unexpected errors
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER.getBdeReference());
    }
  }
}
