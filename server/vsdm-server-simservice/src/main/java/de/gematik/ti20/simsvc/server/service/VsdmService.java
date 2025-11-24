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
  private final TokenService tokenService;
  private final TestDataRepository data;

  public VsdmService(
      final VsdmConfig vsdmConfig, final TokenService tokenService, final TestDataRepository data) {
    this.vsdmConfig = vsdmConfig;
    this.tokenService = tokenService;
    this.data = data;
  }

  public String readKVNR(final String poppTokenFromRequest) {
    String kvnr;
    try {
      final PoppToken poppToken = this.tokenService.parsePoppToken(poppTokenFromRequest);

      kvnr = poppToken.getClaimValue("patientId");

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

  public Resource readVsd(final String poppToken) {
    log.debug("Reading vsd resource");

    final String kvnr = readKVNR(poppToken);

    final VsdmPatient patient = getPatient(kvnr);
    final VsdmPayorOrganization payorOrganization = mockPayorOrganization();
    final VsdmCoverage coverage = mockCoverage(patient, payorOrganization);

    final VsdmBundle vsdmBundle =
        VsdmBundleBuilder.create()
            .addEntry(patient)
            .addEntry(payorOrganization)
            .addEntry(coverage)
            .build();

    return vsdmBundle;
  }

  private VsdmPatient getPatient(final String kvnr) {
    final VsdmPatient patientFromTestdata = getPatientFromTestdata(kvnr);
    if (patientFromTestdata != null) {
      return patientFromTestdata;
    }

    return syntheticPatient(kvnr);
  }

  private VsdmPatient getPatientFromTestdata(final String kvnr) {
    final Optional<RbelElement> patientElement =
        data.findElementByKeyValue("persondata.kvnr", kvnr);
    if (patientElement.isEmpty()) {
      return null;
    }
    var pd = patientElement.get().findElement("$.persondata");
    if (pd.isEmpty()) {
      throw new NoSuchTestDataException("personData is not set: " + patientElement.get());
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
            .withBirthDate(new GregorianCalendar(1980, Calendar.JANUARY, 1).getTime())
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
