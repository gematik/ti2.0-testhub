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
package de.gematik.ti20.simsvc.server.repository;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"vsdm.path-to-test-data=src/test/resources/vsdmservice/patient"})
class TestDataRepositoryTest {

  @Autowired private TestDataRepository testDataRepository;

  @Test
  void thatAPatientIsReturned() {
    final Optional<VsdmPatient> vsdmPatient = testDataRepository.patientByKvnr("N430140916");
    assertThat(vsdmPatient)
        .hasValueSatisfying(
            patient -> {
              assertThat(patient.getName()).isNotEmpty();
              assertThat(patient.getBirthDate()).isEqualTo("1935-06-22");
              assertThat(patient.getAddress()).hasSize(1);
            });
  }

  @Test
  void thatKvnrsAreReturned() {
    final Set<String> kvnrs = testDataRepository.findAvailableKvnrs();
    assertThat(kvnrs).hasSizeGreaterThan(1);
  }
}
