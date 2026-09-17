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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    ResponseEntity<String> mockResponse = ResponseEntity.ok("Success");
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, null, isFhirXml, null, ifNoneMatch, profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<?> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, null, isFhirXml, profileVersion, null, ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
  }

  @Test
  void testVirtualCardSet() {
    String ifNoneMatch = "\"etag123\"";
    boolean isFhirXml = true;

    ResponseEntity<String> mockResponse = ResponseEntity.ok("Success");
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, virtualCard, isFhirXml, null, ifNoneMatch, profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<?> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, virtualCard, isFhirXml, profileVersion, null, ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
  }

  @Test
  void testReadVsd_DefaultIsFhirXml() {
    String ifNoneMatch = "\"etag123\"";

    ResponseEntity<String> mockResponse = ResponseEntity.ok("Success");
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, null, false, null, ifNoneMatch, profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, null, false, profileVersion, null, ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
  }

  @Test
  void testReadVsd_QuotesUnquotedIfNoneMatch() {
    String poppToken = "token123";
    String ifNoneMatch = "etag123";
    String quotedIfNoneMatch = "\"etag123\"";
    ResponseEntity<String> mockResponse = ResponseEntity.ok("Quoted");

    when(mockVsdmClientService.read(
            terminalId, egkSlotId, null, true, poppToken, quotedIfNoneMatch, profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, null, true, profileVersion, poppToken, ifNoneMatch);

    assertEquals("Quoted", response.getBody());
    verify(mockVsdmClientService)
        .read(terminalId, egkSlotId, null, true, poppToken, quotedIfNoneMatch, profileVersion);
  }

  @Test
  void testReadVsd_LeavesNullIfNoneMatchUntouched() {
    ResponseEntity<String> mockResponse = ResponseEntity.ok("NullValue");
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, virtualCard, false, "poppToken", null, profileVersion))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, virtualCard, false, profileVersion, "poppToken", null);

    assertEquals("NullValue", response.getBody());
    verify(mockVsdmClientService)
        .read(terminalId, egkSlotId, virtualCard, false, "poppToken", null, profileVersion);
  }
}
