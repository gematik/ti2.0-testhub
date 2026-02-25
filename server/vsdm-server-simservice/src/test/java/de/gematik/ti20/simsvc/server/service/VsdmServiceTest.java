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

import de.gematik.ti20.simsvc.server.config.VsdmConfig;
import de.gematik.ti20.simsvc.server.model.PoppToken;
import de.gematik.ti20.simsvc.server.repository.TestDataRepository;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import java.util.Optional;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VsdmServiceTest {

  @Mock private TokenService tokenService;

  @Mock private TestDataRepository testDataRepository;

  @Mock private PoppToken poppToken;

  private VsdmService vsdmService;

  @BeforeEach
  void setUp() {
    final VsdmConfig vsdmConfig = new VsdmConfig();
    vsdmConfig.setIknr("109500969");
    vsdmConfig.setValidKvnrPrefix("X1234");
    vsdmConfig.setInvalidKvnrPrefix("X4321");
    vsdmConfig.setUnknownKvnrPrefix("X9");

    vsdmService = new VsdmService(vsdmConfig, tokenService, testDataRepository);
  }

  @Test
  void testReadKVNR_ValidToken_DataFromTestdata() throws Exception {
    String expectedKvnr = "X123456789";
    String iknr = "109500969";
    String tokenHeader = "valid.token.content";

    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(expectedKvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);

    String result = vsdmService.readKVNR(tokenHeader);

    assertEquals(expectedKvnr, result);
    verify(tokenService).parsePoppToken(tokenHeader);
    verify(poppToken).getClaimValue("patientId");
  }

  @Test
  void testReadKVNR_InvalidToken() {
    String tokenHeader = "invalid.token";

    when(tokenService.parsePoppToken(tokenHeader)).thenThrow(new RuntimeException("Invalid token"));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readKVNR(tokenHeader));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Invalid POPP token", exception.getReason());
  }

  @Test
  void testReadKVNR_NoTokenHeader() {
    when(tokenService.parsePoppToken(null)).thenThrow(new RuntimeException("No token"));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readKVNR(null));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Invalid POPP token", exception.getReason());
  }

  @Test
  void testReadVsd_FromTestdata_Success() throws Exception {
    final String kvnr = "N430140916";
    final String iknr = "109500969";
    final String tokenHeader = "valid.token";

    final VsdmPatient mockPatient = mock(VsdmPatient.class);
    when(mockPatient.getResourceType()).thenReturn(ResourceType.Patient);
    when(testDataRepository.patientByKvnr(kvnr)).thenReturn(Optional.of(mockPatient));

    // GIVEN a config
    final VsdmConfig vsdmConfig = new VsdmConfig();
    vsdmConfig.setIknr("109500969");
    vsdmConfig.setValidKvnrPrefix("X1234");
    vsdmConfig.setInvalidKvnrPrefix("X4321");
    vsdmConfig.setUnknownKvnrPrefix("X9");
    // AND the VsdmService is configured
    vsdmService = new VsdmService(vsdmConfig, tokenService, testDataRepository);

    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);

    vsdmService.readVsd(tokenHeader);
    verify(testDataRepository).patientByKvnr(kvnr);
  }

  void testSuccessful(final String kvnr, final String iknr) throws Exception {

    String tokenHeader = "valid.token";

    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);

    Resource result = vsdmService.readVsd(tokenHeader);

    assertNotNull(result);
    assertInstanceOf(VsdmBundle.class, result);

    VsdmBundle vsdmBundle = (VsdmBundle) result;
    assertEquals(3, vsdmBundle.getEntry().size());

    Resource resource = vsdmBundle.getEntryFirstRep().getResource();
    assertNotNull(resource);
    assertInstanceOf(VsdmPatient.class, resource);

    VsdmPatient patient = (VsdmPatient) resource;
    assertEquals(kvnr, patient.getIdentifierFirstRep().getValue());

    HumanName name = patient.getNameFirstRep();
    assertEquals("family-name-" + kvnr, name.getFamily());
    assertEquals("given-name-" + kvnr, name.getGiven().getFirst().getValue());

    verify(testDataRepository).patientByKvnr(kvnr);
  }

  @Test
  void testReadVsd_Synthetic_Success() throws Exception {
    testSuccessful("X123456789", "109500969");
  }

  @Test
  void testReadVsd_Synthetic_Success_Letter() throws Exception {
    testSuccessful("X12345678X", "109500969");
  }

  private void testErrorCase(final String kvnr, final String iknr, final String expectedReason)
      throws Exception {
    final String tokenHeader = "valid.token";

    when(tokenService.parsePoppToken(tokenHeader)).thenReturn(poppToken);
    when(poppToken.getClaimValue("patientId")).thenReturn(kvnr);
    when(poppToken.getClaimValue("insurerId")).thenReturn(iknr);

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> vsdmService.readVsd(tokenHeader));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals(expectedReason, exception.getReason());
  }

  @Test
  void testReadVsd_Unknown_IKNR() throws Exception {
    testErrorCase("X123456789", "987654321", "VSDSERVICE_UNKNOWN_IK");
  }

  @Test
  void testReadVsd_Invalid_IKNR() throws Exception {
    testErrorCase("X123456789", "9876543210", "VSDSERVICE_INVALID_IK");
  }

  @Test
  void testReadVsd_Invalid_KVNR_Syntax() throws Exception {
    testErrorCase("123456789X", "987654321", "VSDSERVICE_INVALID_KVNR");
  }

  @Test
  void testReadVsd_Invalid_KVNR_Prefix() throws Exception {
    testErrorCase("X899999999", "109500969", "VSDSERVICE_INVALID_KVNR");
  }

  @Test
  void testReadVsd_PatientRecordNotFound() throws Exception {
    testErrorCase("X432156789", "109500969", "VSDSERVICE_PATIENT_RECORD_NOT_FOUND");
  }
}
