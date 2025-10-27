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
package de.gematik.ti20.simsvc.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.ti20.simsvc.server.config.VsdmConfig;
import de.gematik.ti20.simsvc.server.model.PoppToken;
import de.gematik.ti20.simsvc.server.repository.TestDataRepository;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VsdmServiceTest {

  @Mock private TokenService tokenService;

  @Mock private TestDataRepository testDataRepository;

  @Mock private HttpServletRequest request;

  @Mock private PoppToken poppToken;

  @Mock private RbelElement patientElement;

  @Mock private RbelElement personDataElement;

  @Mock private RbelElement addressElement;

  private VsdmService vsdmService;

  @BeforeEach
  void setUp() {
    VsdmConfig vsdmConfig = new VsdmConfig();
    vsdmConfig.setIknr("109500969"); // Set a valid iknr for testing

    vsdmService = new VsdmService(vsdmConfig, tokenService, testDataRepository);
  }

  @Test
  void testReadKVNR_ValidToken() throws Exception {
    String expectedKvnr = "X123456789";
    String iknr = "109500969";
    String tokenHeader = "valid.token.content";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(expectedKvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);

    String result = vsdmService.readKVNR(request);

    assertEquals(expectedKvnr, result);
    verify(tokenService).parsePoppToken(tokenHeader);
    verify(poppToken).getClaimValue("patientId");
  }

  @Test
  void testReadKVNR_InvalidToken() {
    String tokenHeader = "invalid.token";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenThrow(new RuntimeException("Invalid token"));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readKVNR(request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Invalid POPP token", exception.getReason());
  }

  @Test
  void testReadKVNR_NoTokenHeader() {
    when(request.getHeader("zeta-popp-token-content")).thenReturn(null);
    when(tokenService.parsePoppToken(null)).thenThrow(new RuntimeException("No token"));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readKVNR(request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Invalid POPP token", exception.getReason());
  }

  @Test
  void testReadVsd_Success() throws Exception {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String tokenHeader = "valid.token";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);
    when(testDataRepository.findElementByKeyValue("persondata.kvnr", kvnr))
        .thenReturn(Optional.of(patientElement));
    when(patientElement.findElement("$.persondata")).thenReturn(Optional.of(personDataElement));
    when(personDataElement.findElement("$.address.post")).thenReturn(Optional.of(addressElement));

    // Mock patient data
    when(testDataRepository.getStringFor(personDataElement, "$.name.family"))
        .thenReturn(Optional.of("Mustermann"));
    when(testDataRepository.getStringFor(personDataElement, "$.name.given"))
        .thenReturn(Optional.of("Max"));
    when(testDataRepository.getStringFor(personDataElement, "$.kvnr"))
        .thenReturn(Optional.of(kvnr));
    when(testDataRepository.getDateFor(personDataElement, "$.birthdate"))
        .thenReturn(Optional.of(new Date()));

    // Mock address data
    when(testDataRepository.getStringFor(addressElement, "$.country"))
        .thenReturn(Optional.of("Deutschland"));
    when(testDataRepository.getStringFor(addressElement, "$.city"))
        .thenReturn(Optional.of("Berlin"));
    when(testDataRepository.getStringFor(addressElement, "$.zip")).thenReturn(Optional.of("12345"));
    when(testDataRepository.getStringFor(addressElement, "$.line1"))
        .thenReturn(Optional.of("Musterstraße 1"));
    when(testDataRepository.getStringFor(addressElement, "$.line2")).thenReturn(Optional.of(""));

    Resource result = vsdmService.readVsd(request);

    assertNotNull(result);
    assertTrue(result instanceof VsdmBundle);
    verify(testDataRepository).findElementByKeyValue("persondata.kvnr", kvnr);
  }

  @Test
  void testReadVsd_PatientNotFound() throws Exception {
    String kvnr = "X123456789";
    String iknr = "UnknownIKNR";
    String tokenHeader = "valid.token";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readVsd(request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("VSDSERVICE_INVALID_IK", exception.getReason());
  }

  @Test
  void testReadVsd_InvalidIKNR() throws Exception {
    String kvnr = "X999999999";
    String iknr = "109500969";
    String tokenHeader = "valid.token";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);
    when(testDataRepository.findElementByKeyValue("persondata.kvnr", kvnr))
        .thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readVsd(request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("VSDSERVICE_INVALID_KVNR", exception.getReason());
  }

  @Test
  void testReadVsd_UnknownIKNR() throws Exception {
    String kvnr = "X1234567890";
    String iknr = "109500969";
    String tokenHeader = "valid.token";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readVsd(request));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("VSDSERVICE_PATIENT_RECORD_NOT_FOUND", exception.getReason());
  }

  @Test
  void testToPatient_MissingPersonData() {
    assertThrows(ResponseStatusException.class, () -> vsdmService.readVsd(request));
  }

  @Test
  void testReadVsd_WithoutAddress() throws Exception {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String tokenHeader = "valid.token";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);
    when(testDataRepository.findElementByKeyValue("persondata.kvnr", kvnr))
        .thenReturn(Optional.of(patientElement));
    when(patientElement.findElement("$.persondata")).thenReturn(Optional.of(personDataElement));
    when(personDataElement.findElement("$.address.post")).thenReturn(Optional.empty());

    // Mock patient data
    when(testDataRepository.getStringFor(personDataElement, "$.name.family"))
        .thenReturn(Optional.of("Mustermann"));
    when(testDataRepository.getStringFor(personDataElement, "$.name.given"))
        .thenReturn(Optional.of("Max"));
    when(testDataRepository.getStringFor(personDataElement, "$.kvnr"))
        .thenReturn(Optional.of(kvnr));
    when(testDataRepository.getDateFor(personDataElement, "$.birthdate"))
        .thenReturn(Optional.of(new Date()));

    Resource result = vsdmService.readVsd(request);

    assertNotNull(result);
    assertTrue(result instanceof VsdmBundle);
  }

  @Test
  void testReadVsd_EmptyOptionalFields() throws Exception {
    String kvnr = "X123456789";
    String iknr = "109500969";
    String tokenHeader = "valid.token";

    when(request.getHeader("zeta-popp-token-content")).thenReturn(tokenHeader);
    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);
    when(testDataRepository.findElementByKeyValue("persondata.kvnr", kvnr))
        .thenReturn(Optional.of(patientElement));
    when(patientElement.findElement("$.persondata")).thenReturn(Optional.of(personDataElement));
    when(personDataElement.findElement("$.address.post")).thenReturn(Optional.of(addressElement));

    // Mock empty optional fields
    when(testDataRepository.getStringFor(personDataElement, "$.name.family"))
        .thenReturn(Optional.empty());
    when(testDataRepository.getStringFor(personDataElement, "$.name.given"))
        .thenReturn(Optional.empty());
    when(testDataRepository.getStringFor(personDataElement, "$.kvnr")).thenReturn(Optional.empty());
    when(testDataRepository.getDateFor(personDataElement, "$.birthdate"))
        .thenReturn(Optional.empty());

    when(testDataRepository.getStringFor(addressElement, "$.country")).thenReturn(Optional.empty());
    when(testDataRepository.getStringFor(addressElement, "$.city")).thenReturn(Optional.empty());
    when(testDataRepository.getStringFor(addressElement, "$.zip")).thenReturn(Optional.empty());
    when(testDataRepository.getStringFor(addressElement, "$.line1")).thenReturn(Optional.empty());
    when(testDataRepository.getStringFor(addressElement, "$.line2")).thenReturn(Optional.empty());

    Resource result = vsdmService.readVsd(request);

    assertNotNull(result);
    assertTrue(result instanceof VsdmBundle);
  }

  @Test
  void testConstructor() {
    assertNotNull(vsdmService);
  }
}
