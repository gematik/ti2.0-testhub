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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.config.VsdmConfig;
import de.gematik.ti20.simsvc.server.exception.ErrorCase;
import de.gematik.ti20.simsvc.server.exception.VsdmErrorException;
import de.gematik.ti20.simsvc.server.exception.ZetaErrorException;
import de.gematik.ti20.simsvc.server.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VsdmControllerV1Test {

  private String VALID_USER_INFO =
      Base64.getEncoder()
          .encodeToString(
              """
            {
              "subject": "subject",
              "commonName": "commonName",
              "identifier": "1-SMC-B-Testkarte--883110000168762",
              "professionOID": "1.2.276.0.76.4.50"
            }
                """
                  .getBytes());

  @Mock private VsdmService vsdmService;
  @Mock private FhirService fhirService;
  @Mock private ChecksumService checksumService;
  @Mock private EtagService etagService;
  private UserInfoValidationService userInfoValidationService;

  @Mock private HttpServletRequest request;

  private VsdmControllerV1 vsdmController;

  @BeforeEach
  void setUp() throws Exception {
    VsdmConfig vsdmConfig = new VsdmConfig();
    vsdmConfig.setIknr("109500969");
    vsdmConfig.setValidKvnrPrefix("X1234");
    vsdmConfig.setInvalidKvnrPrefix("X4321");
    vsdmConfig.setUnknownKvnrPrefix("X9");
    vsdmConfig.setValidProfileVersionMapping(Map.of("1.1", "1.1.0"));

    userInfoValidationService = new UserInfoValidationService();
    userInfoValidationService.init();

    vsdmController =
        new VsdmControllerV1(
            vsdmConfig,
            vsdmService,
            fhirService,
            checksumService,
            etagService,
            userInfoValidationService);

    request = mock(HttpServletRequest.class);
    lenient().when(request.getHeader("zeta-popp-token-content")).thenReturn("mock-popp-token");
    lenient().when(request.getHeader("zeta-user-info")).thenReturn("mock-user-info");
    lenient().when(request.getHeader("if-none-match")).thenReturn("\"0\"");
  }

  private String makePoppTokenContentCoded(final String kvnr, final String iknr) {
    String poppTokenContent =
        String.format(
            """
                {
                    "actorId": "1-SMC-B-Testkarte--883110000168762",
                    "actorProfessionOid": "1.2.276.0.76.4.50",
                    "at": 1773397230,
                    "insurerId": "%1$s",
                    "iss": "https://popp.example.com",
                    "patientId": "%2$s",
                    "patientProofTime": 1773397230,
                    "proofMethod": "ehc-practitioner-trustedchannel",
                    "version": "1.0.0"
              }
          """,
            iknr, kvnr);
    String poppTokenContentCoded = Base64.getEncoder().encodeToString(poppTokenContent.getBytes());
    return poppTokenContentCoded;
  }

  @Test
  void testVsdmbundle_Success() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String responseBody = "{\"resourceType\":\"Bundle\"}";
    Resource mockResource = new Bundle();
    String userInfo = VALID_USER_INFO;
    String etag = "\"0\"";
    String profileVersion = "1.1";

    when(etagService.checkEtag(kvnr, etag)).thenReturn(false);
    when(vsdmService.readVsd(kvnr)).thenReturn(mockResource);
    when(fhirService.encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class)))
        .thenReturn(responseBody);
    when(request.getHeader("if-none-match")).thenReturn(etag);

    ResponseEntity<?> response =
        vsdmController.vsdmbundle(poppTokenContentCoded, userInfo, etag, profileVersion, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(responseBody, response.getBody());

    verify(etagService).checkEtag(kvnr, etag);
    verify(vsdmService).readVsd(kvnr);
    verify(fhirService).encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class));
    verify(checksumService).addChecksumHeader(eq(kvnr), any(HttpHeaders.class));
    verify(etagService).addEtagHeader(eq(kvnr), eq(responseBody), any(HttpHeaders.class));
  }

  @Test
  void testVsdmbundle_ChecksRequestContentType() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String responseBody = "{\"resourceType\":\"Bundle\"}";
    Resource mockResource = new Bundle();
    String userInfo = VALID_USER_INFO;
    String etag = "\"0\"";
    String profileVersion = "1.1";

    // Mock request headers to contain specific content-type
    when(etagService.checkEtag(kvnr, etag)).thenReturn(false);
    when(vsdmService.readVsd(kvnr)).thenReturn(mockResource);
    when(fhirService.encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class)))
        .thenReturn(responseBody);
    when(request.getHeader("if-none-match")).thenReturn(etag);

    ResponseEntity<?> response =
        vsdmController.vsdmbundle(poppTokenContentCoded, userInfo, etag, profileVersion, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(responseBody, response.getBody());

    // Verify that the request was called with the expected content-type
    verify(etagService).checkEtag(kvnr, etag);
    verify(vsdmService).readVsd(kvnr);
    verify(fhirService).encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class));
  }

  @Test
  void testVsdmbundle_UsesUtf8Encoding() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String responseBodyWithUmlaut =
        "{\"resourceType\":\"Bundle\",\"name\":\"Müller\",\"city\":\"München\"}";
    Resource mockResource = new Bundle();
    String userInfo = VALID_USER_INFO;
    String etag = "\"0\"";
    String profileVersion = "1.1";

    when(etagService.checkEtag(kvnr, etag)).thenReturn(false);
    when(vsdmService.readVsd(kvnr)).thenReturn(mockResource);
    when(fhirService.encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class)))
        .thenReturn(responseBodyWithUmlaut);
    when(request.getHeader("if-none-match")).thenReturn(etag);

    ResponseEntity<?> response =
        vsdmController.vsdmbundle(poppTokenContentCoded, userInfo, etag, profileVersion, request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(responseBodyWithUmlaut, response.getBody());

    // Verify UTF-8 characters are preserved
    String responseString = (String) response.getBody();
    assertTrue(responseString.contains("Müller"));
    assertTrue(responseString.contains("München"));

    verify(checksumService).addChecksumHeader(eq(kvnr), any(HttpHeaders.class));
    verify(etagService).addEtagHeader(eq(kvnr), eq(responseBodyWithUmlaut), any(HttpHeaders.class));
  }

  @Test
  void testVsdmbundle_NotModified() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String userInfo = VALID_USER_INFO;
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(etagService.checkEtag(kvnr, etag)).thenReturn(true);
    when(checksumService.calculateChecksum(kvnr)).thenReturn("PZ");
    when(request.getHeader("if-none-match")).thenReturn(etag);

    ResponseEntity<?> response =
        vsdmController.vsdmbundle(poppTokenContentCoded, userInfo, etag, profileVersion, request);

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());

    assertNotNull(response.getHeaders().get(HttpHeaders.ETAG));
    assertEquals(1, response.getHeaders().get(HttpHeaders.ETAG).size());
    assertEquals(etag, response.getHeaders().get(HttpHeaders.ETAG).getFirst());

    assertNotNull(response.getHeaders().get(ChecksumService.HEADER_NAME));
    assertEquals(1, response.getHeaders().get(ChecksumService.HEADER_NAME).size());
    assertEquals("PZ", response.getHeaders().get(ChecksumService.HEADER_NAME).getFirst());

    assertNull(response.getBody());

    verify(etagService).checkEtag(kvnr, etag);
    verify(vsdmService, never()).readVsd(kvnr);
    verify(fhirService, never()).encodeResponse(any(), any(), any());
    verify(checksumService, never()).addChecksumHeader(any(), any());
    verify(etagService, never()).addEtagHeader(any(), any(), any());
  }

  @Test
  void testVsdmbundle_InvalidPoppToken_Base64EncodingWrong() {
    String poppTokenContentCoded = "INVALID";

    String userInfo = "mock-user-info";
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("zeta-popp-token-content")).thenReturn("INVALID");
    when(request.getHeader("if-none-match")).thenReturn(etag);

    ZetaErrorException exception =
        assertThrows(
            ZetaErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals("ERROR_HEADER_USERINFO", exception.getErrorCase().getBdeReference());
  }

  @Test
  void testVsdmbundle_InvalidPoppToken_MissingFieldsInClaims() {
    String poppTokenContentMissingFields =
        """
      {
          "actorId": "1-SMC-B-Testkarte--883110000168762",
          "actorProfessionOid": "1.2.276."
        }
      """;
    String poppTokenContentCoded =
        Base64.getEncoder().encodeToString(poppTokenContentMissingFields.getBytes());

    String userInfo = "mock-user-info";
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("if-none-match")).thenReturn(etag);

    ZetaErrorException exception =
        assertThrows(
            ZetaErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals("ERROR_HEADER_USERINFO", exception.getErrorCase().getBdeReference());
  }

  @Test
  void testVsdmbundle_MissingPoppToken() {
    String poppTokenContentCoded = null;

    String userInfo = "mock-user-info";
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(poppTokenContentCoded);

    ZetaErrorException exception =
        assertThrows(
            ZetaErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals("MISSING_HEADER_POPP", exception.getErrorCase().getBdeReference());
  }

  @Test
  void testVsdmBundle_InvalidPoppToken() {
    String poppTokenContentCoded = "INVALID";

    String userInfo = VALID_USER_INFO;
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(poppTokenContentCoded);
    when(request.getHeader("zeta-user-info")).thenReturn(userInfo);
    when(request.getHeader("if-none-match")).thenReturn(etag);

    ZetaErrorException exception =
        assertThrows(
            ZetaErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals("ERROR_HEADER_POPPTOKEN", exception.getErrorCase().getBdeReference());
  }

  @Test
  void testVsdmbundle_InvalidUserInfo_Base64EncodingWrong() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);
    String userInfo = "INVALID";
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("if-none-match")).thenReturn(etag);

    ZetaErrorException exception =
        assertThrows(
            ZetaErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals("ERROR_HEADER_USERINFO", exception.getErrorCase().getBdeReference());
  }

  @Test
  void testVsdmbundle_InvalidUserInfo_MissingFieldsInClaims() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String userInfoMissingFields =
        """
          {
              "foo": "bar"
            }
          """;
    String userInfo = Base64.getEncoder().encodeToString(userInfoMissingFields.getBytes());

    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("if-none-match")).thenReturn(etag);

    ZetaErrorException exception =
        assertThrows(
            ZetaErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals("ERROR_HEADER_USERINFO", exception.getErrorCase().getBdeReference());
  }

  @Test
  void testVsdmbundle_MissingUserInfo() {
    String kvnr = "X123456789";
    String iknr = "9876543210";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("zeta-user-info")).thenReturn(null);

    ZetaErrorException exception =
        assertThrows(
            ZetaErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, null, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals("MISSING_HEADER_USERINFO", exception.getErrorCase().getBdeReference());
  }

  @Test
  void testVsdmbundle_UnknownIK() {
    String kvnr = "X123456789";
    String iknr = "987654321";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String userInfo = VALID_USER_INFO;
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("if-none-match")).thenReturn(etag);

    VsdmErrorException exception =
        assertThrows(
            VsdmErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals(ErrorCase.VSDSERVICE_UNKNOWN_IK, exception.getErrorCase());
  }

  @Test
  void testVsdmbundle_InvalidIK() {
    String kvnr = "X123456789";
    String iknr = "9876543210";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String userInfo = VALID_USER_INFO;
    String etag = "\"123456789\"";
    String profileVersion = "1.1";

    when(request.getHeader("if-none-match")).thenReturn(etag);

    VsdmErrorException exception =
        assertThrows(
            VsdmErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, profileVersion, request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals(ErrorCase.VSDSERVICE_INVALID_IK, exception.getErrorCase());
  }

  @Test
  void testVsdmbundle_MissingProfileVersion() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String userInfo = VALID_USER_INFO;
    String etag = "\"123456789\"";

    when(request.getHeader("if-none-match")).thenReturn(etag);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> vsdmController.vsdmbundle(poppTokenContentCoded, userInfo, etag, null, request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("VSDSERVICE_MISSING_PROFILE_VERSION", exception.getReason());
  }

  @Test
  void testVsdmbundle_InvalidProfileVersion() {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String poppTokenContentCoded = makePoppTokenContentCoded(kvnr, iknr);

    String userInfo = VALID_USER_INFO;
    String etag = "\"123456789\"";

    when(request.getHeader("if-none-match")).thenReturn(etag);

    VsdmErrorException exception =
        assertThrows(
            VsdmErrorException.class,
            () ->
                vsdmController.vsdmbundle(
                    poppTokenContentCoded, userInfo, etag, "unknown", request));

    assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getErrorCase().getHttpCode());
    assertEquals(ErrorCase.VSDSERVICE_INVALID_PROFILE_VERSION, exception.getErrorCase());
  }
}
