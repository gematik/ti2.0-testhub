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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.service.VsdmClientService;
import de.gematik.ti20.simsvc.client.service.vsdm.VsdmReadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

class VsdmClientControllerTest {

  private VsdmClientService mockVsdmClientService;
  private VsdmClientController vsdmClientController;

  private final String terminalId = "terminalId";
  private final Integer egkSlotId = 1;
  private final String virtualCard = "virtualCard";
  private final String profileVersion = "1.1";

  @BeforeEach
  void setUp() {
    mockVsdmClientService = mock(VsdmClientService.class);
    vsdmClientController = new VsdmClientController(mockVsdmClientService);
  }

  @Test
  void testReadVsd_Success() {
    String ifNoneMatch = "\"etag123\"";
    boolean isFhirXml = true;
    boolean skipPoppTokenHeader = false;

    VsdmReadResult mockResponse =
        new VsdmReadResult(HttpStatus.OK, "\"etag123\"", "pz123", "Success", null);
    when(mockVsdmClientService.read(
            terminalId,
            egkSlotId,
            null,
            isFhirXml,
            skipPoppTokenHeader,
            null,
            ifNoneMatch,
            profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<?> response =
        vsdmClientController.readVsd(
            terminalId,
            egkSlotId,
            null,
            isFhirXml,
            skipPoppTokenHeader,
            profileVersion,
            null,
            ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
    assertEquals("\"etag123\"", response.getHeaders().getETag());
    assertEquals("pz123", response.getHeaders().getFirst("vsdm-pz"));
  }

  @Test
  void testVirtualCardSet() {
    String ifNoneMatch = "\"etag123\"";
    boolean isFhirXml = true;
    boolean skipPoppTokenHeader = false;

    VsdmReadResult mockResponse = new VsdmReadResult(HttpStatus.OK, null, null, "Success", null);
    when(mockVsdmClientService.read(
            terminalId,
            egkSlotId,
            virtualCard,
            isFhirXml,
            skipPoppTokenHeader,
            null,
            ifNoneMatch,
            profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<?> response =
        vsdmClientController.readVsd(
            terminalId,
            egkSlotId,
            virtualCard,
            isFhirXml,
            skipPoppTokenHeader,
            profileVersion,
            null,
            ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
  }

  @Test
  void testReadVsd_DefaultIsFhirXml() {
    String ifNoneMatch = "\"etag123\"";
    boolean skipPoppTokenHeader = false;

    VsdmReadResult mockResponse = new VsdmReadResult(HttpStatus.OK, null, null, "Success", null);
    when(mockVsdmClientService.read(
            terminalId,
            egkSlotId,
            null,
            false,
            skipPoppTokenHeader,
            null,
            ifNoneMatch,
            profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId,
            egkSlotId,
            null,
            false,
            skipPoppTokenHeader,
            profileVersion,
            null,
            ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
  }

  @Test
  void testReadVsd_QuotesUnquotedIfNoneMatch() {
    String poppToken = "token123";
    String ifNoneMatch = "etag123";
    String quotedIfNoneMatch = "\"etag123\"";
    boolean skipPoppTokenHeader = false;
    VsdmReadResult mockResponse = new VsdmReadResult(HttpStatus.OK, null, null, "Quoted", null);

    when(mockVsdmClientService.read(
            terminalId,
            egkSlotId,
            null,
            true,
            skipPoppTokenHeader,
            poppToken,
            quotedIfNoneMatch,
            profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId,
            egkSlotId,
            null,
            true,
            skipPoppTokenHeader,
            profileVersion,
            poppToken,
            ifNoneMatch);

    assertEquals("Quoted", response.getBody());
    verify(mockVsdmClientService)
        .read(
            terminalId,
            egkSlotId,
            null,
            true,
            skipPoppTokenHeader,
            poppToken,
            quotedIfNoneMatch,
            profileVersion);
  }

  @Test
  void testReadVsd_LeavesNullIfNoneMatchUntouched() {
    boolean skipPoppTokenHeader = false;
    VsdmReadResult mockResponse = new VsdmReadResult(HttpStatus.OK, null, null, "NullValue", null);
    when(mockVsdmClientService.read(
            terminalId,
            egkSlotId,
            virtualCard,
            false,
            skipPoppTokenHeader,
            "poppToken",
            null,
            profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId,
            egkSlotId,
            virtualCard,
            false,
            skipPoppTokenHeader,
            profileVersion,
            "poppToken",
            null);

    assertEquals("NullValue", response.getBody());
    verify(mockVsdmClientService)
        .read(
            terminalId,
            egkSlotId,
            virtualCard,
            false,
            skipPoppTokenHeader,
            "poppToken",
            null,
            profileVersion);
  }

  @Test
  void testReadVsd_ForwardsSkipPoppTokenHeaderFlagWhenTrue() {
    String ifNoneMatch = "\"etag123\"";
    boolean skipPoppTokenHeader = true;
    VsdmReadResult mockResponse = new VsdmReadResult(HttpStatus.OK, null, null, "Success", null);

    when(mockVsdmClientService.read(
            terminalId,
            egkSlotId,
            virtualCard,
            false,
            skipPoppTokenHeader,
            "poppToken",
            ifNoneMatch,
            profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId,
            egkSlotId,
            virtualCard,
            false,
            skipPoppTokenHeader,
            profileVersion,
            "poppToken",
            ifNoneMatch);

    assertEquals("Success", response.getBody());
    verify(mockVsdmClientService)
        .read(
            terminalId,
            egkSlotId,
            virtualCard,
            false,
            skipPoppTokenHeader,
            "poppToken",
            ifNoneMatch,
            profileVersion);
  }

  @Test
  void testReadVsd_NotModifiedBuildsHeaderOnlyResponse() {
    boolean skipPoppTokenHeader = false;
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, null, false, skipPoppTokenHeader, null, null, profileVersion))
        .thenReturn(
            new VsdmReadResult(HttpStatus.NOT_MODIFIED, "\"etag123\"", "pz123", null, null));

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, null, false, skipPoppTokenHeader, profileVersion, null, null);

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
    assertNull(response.getBody());
    assertEquals("\"etag123\"", response.getHeaders().getETag());
    assertEquals("pz123", response.getHeaders().getFirst("vsdm-pz"));
  }

  @Test
  void testReadVsd_UsesContentTypeFromServiceResult() {
    boolean skipPoppTokenHeader = false;
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, null, true, skipPoppTokenHeader, null, null, profileVersion))
        .thenReturn(
            new VsdmReadResult(
                HttpStatus.OK, null, null, "<Bundle/>", MediaType.APPLICATION_XML_VALUE));

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, null, true, skipPoppTokenHeader, profileVersion, null, null);

    assertEquals(MediaType.APPLICATION_XML, response.getHeaders().getContentType());
    assertEquals("<Bundle/>", response.getBody());
  }
}
