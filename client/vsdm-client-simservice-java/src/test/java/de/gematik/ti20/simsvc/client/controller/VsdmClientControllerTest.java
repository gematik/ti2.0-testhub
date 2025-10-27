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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.service.VsdmClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class VsdmClientControllerTest {

  private VsdmClientService mockVsdmClientService;
  private VsdmClientController vsdmClientController;

  @BeforeEach
  void setUp() {
    mockVsdmClientService = mock(VsdmClientService.class);
    vsdmClientController = new VsdmClientController(mockVsdmClientService);
  }

  @Test
  void testReadVsd_Success() {
    String terminalId = "terminalId";
    Integer egkSlotId = 1;
    Integer smcbSlotId = 2;
    String ifNoneMatch = "etag123";
    boolean isFhirXml = true;
    boolean forceUpdate = false;

    ResponseEntity<String> mockResponse = ResponseEntity.ok("Success");
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, smcbSlotId, isFhirXml, forceUpdate, null, ifNoneMatch))
        .thenReturn(mockResponse);

    ResponseEntity<?> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, smcbSlotId, isFhirXml, forceUpdate, null, ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
  }

  @Test
  void testReadVsd_DefaultIsFhirXml() {
    String terminalId = "terminal123";
    Integer egkSlotId = 1;
    Integer smcbSlotId = 1;
    String ifNoneMatch = "etag123";
    boolean forceUpdate = false;

    ResponseEntity<String> mockResponse = ResponseEntity.ok("Success");
    when(mockVsdmClientService.read(
            terminalId, egkSlotId, smcbSlotId, false, forceUpdate, null, ifNoneMatch))
        .thenReturn(mockResponse);

    ResponseEntity<?> response =
        vsdmClientController.readVsd(
            terminalId, egkSlotId, smcbSlotId, null, forceUpdate, null, ifNoneMatch);

    assertNotNull(response);
    assertEquals(200, response.getStatusCode().value());
    assertEquals("Success", response.getBody());
  }
}
