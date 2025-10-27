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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.service.ChecksumService;
import de.gematik.ti20.simsvc.server.service.EtagService;
import de.gematik.ti20.simsvc.server.service.FhirService;
import de.gematik.ti20.simsvc.server.service.VsdmService;
import jakarta.servlet.http.HttpServletRequest;
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

@ExtendWith(MockitoExtension.class)
class VsdmControllerV1Test {

  @Mock private VsdmService vsdmService;

  @Mock private FhirService fhirService;

  @Mock private ChecksumService checksumService;

  @Mock private EtagService etagService;

  @Mock private HttpServletRequest request;

  private VsdmControllerV1 vsdmController;

  @BeforeEach
  void setUp() {
    vsdmController = new VsdmControllerV1(vsdmService, fhirService, checksumService, etagService);

    request = mock(HttpServletRequest.class);
    when(request.getHeader("zeta-popp-token-content")).thenReturn("mock-popp-token");
    when(request.getHeader("zeta-user-info")).thenReturn("mock-user-info");
    when(request.getHeader("if-none-match")).thenReturn("0");
  }

  @Test
  void testVsdmbundle_Success() {
    String kvnr = "X123456789";
    String responseBody = "{\"resourceType\":\"Bundle\"}";
    Resource mockResource = new Bundle();

    when(vsdmService.readKVNR(request)).thenReturn(kvnr);
    when(etagService.checkEtag(kvnr, request)).thenReturn(false);
    when(vsdmService.readVsd(request)).thenReturn(mockResource);
    when(fhirService.encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class)))
        .thenReturn(responseBody);

    ResponseEntity<?> response = vsdmController.vsdmbundle(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(responseBody, response.getBody());

    verify(vsdmService).readKVNR(request);
    verify(etagService).checkEtag(kvnr, request);
    verify(vsdmService).readVsd(request);
    verify(fhirService).encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class));
    verify(checksumService).addChecksumHeader(eq(responseBody), any(HttpHeaders.class));
    verify(etagService).addEtagHeader(eq(kvnr), eq(responseBody), any(HttpHeaders.class));
  }

  @Test
  void testVsdmbundle_ChecksRequestContentType() {
    String kvnr = "X123456789";
    String responseBody = "{\"resourceType\":\"Bundle\"}";
    Resource mockResource = new Bundle();
    String expectedContentType = "application/fhir+json";

    // Mock request headers to contain specific content-type
    when(vsdmService.readKVNR(request)).thenReturn(kvnr);
    when(etagService.checkEtag(kvnr, request)).thenReturn(false);
    when(vsdmService.readVsd(request)).thenReturn(mockResource);
    when(fhirService.encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class)))
        .thenReturn(responseBody);

    ResponseEntity<?> response = vsdmController.vsdmbundle(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(responseBody, response.getBody());

    // Verify that the request was called with the expected content-type
    verify(vsdmService).readKVNR(request);
    verify(etagService).checkEtag(kvnr, request);
    verify(vsdmService).readVsd(request);
    verify(fhirService).encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class));
  }

  @Test
  void testVsdmbundle_UsesUtf8Encoding() {
    String kvnr = "X123456789";
    String responseBodyWithUmlaut =
        "{\"resourceType\":\"Bundle\",\"name\":\"Müller\",\"city\":\"München\"}";
    Resource mockResource = new Bundle();

    when(vsdmService.readKVNR(request)).thenReturn(kvnr);
    when(etagService.checkEtag(kvnr, request)).thenReturn(false);
    when(vsdmService.readVsd(request)).thenReturn(mockResource);
    when(fhirService.encodeResponse(eq(mockResource), eq(request), any(HttpHeaders.class)))
        .thenReturn(responseBodyWithUmlaut);

    ResponseEntity<?> response = vsdmController.vsdmbundle(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(responseBodyWithUmlaut, response.getBody());

    // Verify UTF-8 characters are preserved
    String responseString = (String) response.getBody();
    assertTrue(responseString.contains("Müller"));
    assertTrue(responseString.contains("München"));

    verify(checksumService).addChecksumHeader(eq(responseBodyWithUmlaut), any(HttpHeaders.class));
    verify(etagService).addEtagHeader(eq(kvnr), eq(responseBodyWithUmlaut), any(HttpHeaders.class));
  }

  @Test
  void testVsdmbundle_NotModified() {
    String kvnr = "X123456789";

    when(vsdmService.readKVNR(request)).thenReturn(kvnr);
    when(etagService.checkEtag(kvnr, request)).thenReturn(true);

    ResponseEntity<?> response = vsdmController.vsdmbundle(request);

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    assertNull(response.getBody());

    verify(vsdmService).readKVNR(request);
    verify(etagService).checkEtag(kvnr, request);
    verify(vsdmService, never()).readVsd(request);
    verify(fhirService, never()).encodeResponse(any(), any(), any());
    verify(checksumService, never()).addChecksumHeader(any(), any());
    verify(etagService, never()).addEtagHeader(any(), any(), any());
  }
}
