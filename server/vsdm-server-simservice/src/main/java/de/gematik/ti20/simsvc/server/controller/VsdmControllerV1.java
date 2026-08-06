/*-
 * #%L
 * VSDM Server Simservice
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
package de.gematik.ti20.simsvc.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import de.gematik.ti20.simsvc.server.config.VsdmConfig;
import de.gematik.ti20.simsvc.server.exception.ErrorCase;
import de.gematik.ti20.simsvc.server.exception.VsdmErrorException;
import de.gematik.ti20.simsvc.server.exception.ZetaErrorException;
import de.gematik.ti20.simsvc.server.model.PoppTokenContent;
import de.gematik.ti20.simsvc.server.service.ChecksumService;
import de.gematik.ti20.simsvc.server.service.EtagService;
import de.gematik.ti20.simsvc.server.service.FhirService;
import de.gematik.ti20.simsvc.server.service.UserInfoValidationService;
import de.gematik.ti20.simsvc.server.service.VsdmService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
  private final UserInfoValidationService userInfoValidationService;

  private static final Pattern VALID_IKNR_PATTERN = Pattern.compile("^[0-9]{9}$");
  private static final Pattern VALID_KVNR_PATTERN = Pattern.compile("^[A-Z][0-9]{8}[A-Z,0-9]$");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public VsdmControllerV1(
      @Autowired VsdmConfig vsdmConfig,
      @Autowired VsdmService vsdmService,
      @Autowired FhirService fhirService,
      @Autowired ChecksumService checksumService,
      @Autowired EtagService etagService,
      @Autowired UserInfoValidationService userInfoValidationService) {
    this.vsdmConfig = vsdmConfig;
    this.vsdmService = vsdmService;
    this.fhirService = fhirService;
    this.checksumService = checksumService;
    this.etagService = etagService;
    this.userInfoValidationService = userInfoValidationService;
  }

  @GetMapping(
      value = "/vsdmbundle",
      produces = {"application/fhir+json", "application/fhir+xml"})
  public ResponseEntity<?> vsdmbundle(
      @RequestHeader(value = "zeta-popp-token-content", required = false)
          final String poppTokenContentCoded,
      @RequestHeader(value = "zeta-user-info", required = false) final String userInfo,
      @RequestHeader(value = "if-none-match", required = false, defaultValue = "\"0\"")
          final String ifNoneMatch,
      @RequestParam(value = "profileVersion", required = false) final String profileVersion,
      final HttpServletRequest request) {
    log.info(
        "Received request for readVsd zeta-popp-token-content: {}, zeta-user-info: {}, if-none-match: {}",
        poppTokenContentCoded,
        userInfo,
        ifNoneMatch);
    validateHeaders(request);
    userInfoValidationService.validateUserInfo(userInfo);

    final HttpHeaders responseHeaders = new HttpHeaders();

    final PoppTokenContent poppTokenContent = parsePoppTokenContent(poppTokenContentCoded);
    final String kvnr = poppTokenContent.getPatientId();

    if (Strings.isNullOrEmpty(profileVersion)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "VSDSERVICE_MISSING_PROFILE_VERSION");
    }

    if (!vsdmConfig.getValidProfileVersionMapping().containsKey(profileVersion)) {
      throw new VsdmErrorException(
          ErrorCase.VSDSERVICE_INVALID_PROFILE_VERSION, Map.of("version", profileVersion));
    }

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

    return new ResponseEntity<>(responseBody, responseHeaders, HttpStatus.OK);
  }

  private void validateHeaders(final HttpServletRequest request) {
    if (request.getHeader("zeta-popp-token-content") == null) {
      throw new ZetaErrorException(ErrorCase.MISSING_HEADER_POPP);
    }
    if (request.getHeader("zeta-user-info") == null) {
      throw new ZetaErrorException(ErrorCase.MISSING_HEADER_USERINFO);
    }
    if (request.getHeader("if-none-match") == null) {
      throw new VsdmErrorException(ErrorCase.VSDSERVICE_MISSING_PATIENT_RECORD_VERSION);
    }
    if (!isQuoted(request.getHeader("if-none-match"))) {
      throw new VsdmErrorException(
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER, Map.of("header", "if-none-match"));
    }
  }

  private boolean isQuoted(final String s) {
    return s != null && s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"");
  }

  private String checkAndGetKvnr(final JsonNode claims) {
    final String kvnr = claims.path("patientId").asText(null);
    final String iknr = claims.path("insurerId").asText(null);
    if (kvnr == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-popp-token-content"));
    }
    if (!VALID_KVNR_PATTERN.matcher(kvnr).matches()) {
      throw new VsdmErrorException(ErrorCase.VSDSERVICE_INVALID_KVNR, Map.of("kvnr", kvnr));
    }

    if (kvnr.startsWith(vsdmConfig.getUnknownKvnrPrefix())) {
      throw new VsdmErrorException(
          ErrorCase.VSDSERVICE_UNKNOWN_KVNR, Map.of("kvnr", kvnr, "ik", iknr));
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
      throw new VsdmErrorException(ErrorCase.VSDSERVICE_INVALID_IK, Map.of("ik", iknr));
    }
    if (!iknr.equals(vsdmConfig.getIknr())) {
      throw new VsdmErrorException(ErrorCase.VSDSERVICE_UNKNOWN_IK, Map.of("ik", iknr));
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
    } catch (final ResponseStatusException | VsdmErrorException e) {
      throw e;
    } catch (final Exception e) {
      // Base64 decoding failed, JSON parsing or other unexpected errors
      throw new ZetaErrorException(ErrorCase.ERROR_HEADER_POPPTOKEN);
    }
  }
}
