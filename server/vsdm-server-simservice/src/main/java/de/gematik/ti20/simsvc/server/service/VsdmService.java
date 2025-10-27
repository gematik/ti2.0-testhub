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

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.test.testdata.exceptions.NoSuchTestDataException;
import de.gematik.ti20.simsvc.server.config.VsdmConfig;
import de.gematik.ti20.simsvc.server.model.PoppToken;
import de.gematik.ti20.simsvc.server.repository.TestDataRepository;
import de.gematik.ti20.vsdm.fhir.builder.VsdmBundleBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmCoverageBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPatientBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPayorOrganizationBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.fhir.def.VsdmCoverage;
import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import de.gematik.ti20.vsdm.fhir.def.VsdmPayorOrganization;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class VsdmService {

  private final VsdmConfig vsdmConfig;

  private final TokenService tokenService;
  private final TestDataRepository data;

  public VsdmService(
      final VsdmConfig vsdmConfig,
      TokenService tokenService,
      final TestDataRepository dataRepository) {
    this.vsdmConfig = vsdmConfig;
    this.tokenService = tokenService;
    this.data = dataRepository;
  }

  public String readKVNR(final HttpServletRequest request) {
    String kvnr;
    try {
      final PoppToken poppToken =
          this.tokenService.parsePoppToken(request.getHeader("zeta-popp-token-content"));

      kvnr = poppToken.getClaimValue("patientId");
      log.debug("2. ReadVsd for KVNR {}", kvnr);

      final String iknr = poppToken.getClaimValue("insurerId");
      if (!iknr.equals(vsdmConfig.getIknr())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VSDSERVICE_INVALID_IK");
      }
    } catch (final Exception ex) {
      if (ex instanceof ResponseStatusException) {
        throw (ResponseStatusException) ex;
      } else {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid POPP token");
      }
    }

    return kvnr;
  }

  public Resource readVsd(final HttpServletRequest request) {
    log.debug("Reading vsd resource");

    final long start = System.currentTimeMillis();
    log.debug("1. ReadVsd called");

    final String kvnr = readKVNR(request);

    if ("X1234567890".equals(kvnr)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "VSDSERVICE_PATIENT_RECORD_NOT_FOUND");
    }
    final RbelElement patientData =
        data.findElementByKeyValue("persondata.kvnr", kvnr)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "VSDSERVICE_INVALID_KVNR"));

    log.debug("3. Found data for KVNR {}", kvnr);

    final VsdmPatient patient = toPatient(patientData);
    final VsdmPayorOrganization payorOrganization = mockPayorOrganization();
    final VsdmCoverage coverage = mockCoverage(patient, payorOrganization);

    final VsdmBundle vsdmBundle =
        VsdmBundleBuilder.create()
            .addEntry(patient)
            .addEntry(payorOrganization)
            .addEntry(coverage)
            .build();

    log.debug("4. ReadVsd for KVNR {} took {}ms", kvnr, System.currentTimeMillis() - start);

    return vsdmBundle;
  }

  private VsdmPatient toPatient(final RbelElement patientElement) {

    var pd = patientElement.findElement("$.persondata");
    if (pd.isEmpty()) {
      throw new NoSuchTestDataException("personData is not set: " + patientElement.toString());
    }

    final List<Address> addresses = new ArrayList<>();
    var adrPost = pd.get().findElement("$.address.post");
    if (adrPost.isPresent()) {
      addresses.add(
          new Address()
              .setCountry(data.getStringFor(adrPost.get(), "$.country").orElse(""))
              .setCity(data.getStringFor(adrPost.get(), "$.city").orElse(""))
              .setPostalCode(data.getStringFor(adrPost.get(), "$.zip").orElse(""))
              .addLine(data.getStringFor(adrPost.get(), "$.line1").orElse(""))
              .addLine(data.getStringFor(adrPost.get(), "$.line2").orElse(""))
              .setType(Address.AddressType.POSTAL));
    }

    final VsdmPatient patient =
        VsdmPatientBuilder.create()
            .withNames(
                data.getStringFor(pd.get(), "$.name.family").orElse(""),
                data.getStringFor(pd.get(), "$.name.given").orElse(""))
            .withKvnr(data.getStringFor(pd.get(), "$.kvnr").orElse(""))
            .withBirthDate(data.getDateFor(pd.get(), "$.birthdate").orElse(null))
            .build();

    patient.setAddress(addresses);

    return patient;
  }

  private VsdmPayorOrganization mockPayorOrganization() {
    final VsdmPayorOrganization payorOrganization =
        VsdmPayorOrganizationBuilder.create()
            .iknr("107723372")
            .name("Test GKV Krankenkasse")
            .build();

    return payorOrganization;
  }

  private VsdmCoverage mockCoverage(VsdmPatient patient, VsdmPayorOrganization payorOrganization) {
    final VsdmCoverage coverage =
        VsdmCoverageBuilder.create()
            .withStatus("active")
            .withPayor("https://gematik.de/fhir/Organization/" + payorOrganization.getId())
            .withBeneficiary("https://gematik.de/fhir/Patient/" + patient.getId())
            .build();

    return coverage;
  }
}
