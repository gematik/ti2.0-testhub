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

import de.gematik.ti20.simsvc.server.config.VsdmConfig;
import de.gematik.ti20.simsvc.server.repository.TestDataRepository;
import de.gematik.ti20.vsdm.fhir.builder.VsdmBundleBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmCoverageBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPatientBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPayorOrganizationBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.fhir.def.VsdmCoverage;
import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import de.gematik.ti20.vsdm.fhir.def.VsdmPayorOrganization;
import java.util.*;
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
  private final TestDataRepository data;

  public VsdmService(final VsdmConfig vsdmConfig, final TestDataRepository data) {
    this.vsdmConfig = vsdmConfig;
    this.data = data;
  }

  public Resource readVsd(final String kvnr) {
    log.debug("Reading vsd resource");

    final VsdmPatient patient = getPatient(kvnr);
    final VsdmPayorOrganization payorOrganization = mockPayorOrganization();
    final VsdmCoverage coverage = mockCoverage(kvnr, patient, payorOrganization);

    final VsdmBundle vsdmBundle =
        VsdmBundleBuilder.create()
            .addEntry(patient)
            .addEntry(payorOrganization)
            .addEntry(coverage)
            .build();

    return vsdmBundle;
  }

  private VsdmPatient getPatient(final String kvnr) {
    return data.patientByKvnr(kvnr).orElseGet(() -> syntheticPatient(kvnr));
  }

  private VsdmPatient syntheticPatient(final String kvnr) {
    if (kvnr.startsWith(vsdmConfig.getInvalidKvnrPrefix())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "VSDSERVICE_PATIENT_RECORD_NOT_FOUND");
    }

    if (!kvnr.startsWith(vsdmConfig.getValidKvnrPrefix())) {
      log.error("Invalid KVNR prefix {}, {}", kvnr, vsdmConfig.getValidKvnrPrefix());

      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "VSDSERVICE_INVALID_KVNR");
    }

    final VsdmPatient patient =
        VsdmPatientBuilder.create()
            .withKvnr(kvnr)
            .withNames("family-name-" + kvnr, "given-name-" + kvnr)
            .withBirthDate("1980-01-01")
            .build();

    final List<Address> addresses = new ArrayList<>();
    addresses.add(
        new Address()
            .setCountry("DE")
            .setCity("Berlin")
            .setPostalCode("12345")
            .addLine("")
            .setType(Address.AddressType.POSTAL));

    patient.setAddress(addresses);
    return patient;
  }

  private VsdmPayorOrganization mockPayorOrganization() {
    final VsdmPayorOrganization payorOrganization =
        VsdmPayorOrganizationBuilder.create()
            .iknr(vsdmConfig.getIknr())
            .name("Test GKV Krankenkasse")
            .build();

    return payorOrganization;
  }

  private VsdmCoverage mockCoverage(
      final String kvnr, final VsdmPatient patient, final VsdmPayorOrganization payorOrganization) {
    final VsdmCoverage coverage =
        VsdmCoverageBuilder.create()
            .withStatus("active")
            .withPayor("https://gematik.de/fhir/Organization/" + payorOrganization.getId())
            .withBeneficiary("https://gematik.de/fhir/Patient/" + patient.getId())
            .withKvnr(kvnr)
            .build();

    return coverage;
  }
}
