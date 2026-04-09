/*-
 * #%L
 * VSDM Server Simservice
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
package de.gematik.ti20.simsvc.server.repository;

import de.gematik.test.testdata.TestDataManager;
import de.gematik.test.testdata.exceptions.NoSuchTestDataException;
import de.gematik.test.testdata.model.Patient;
import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Provides access to the data stored in the test data folder. For initialization see {@link
 * de.gematik.ti20.simsvc.server.config.TestDataConfiguration}.
 */
@Repository
public class TestDataRepository {

  private final TestDataManager testDataManager;

  @Autowired
  public TestDataRepository(final TestDataManager testDataManager) {
    this.testDataManager = testDataManager;
  }

  @Nonnull
  public Optional<VsdmPatient> patientByKvnr(@Nonnull final String kvnr) {
    try {
      final String byKvnr = "$.patients.[?(@..kvnr == '%s')]".formatted(kvnr);
      final Patient patient = testDataManager.getPatient(byKvnr);
      return Optional.of(VsdmPatient.from(patient));
    } catch (final NoSuchTestDataException e) {
      return Optional.empty();
    }
  }

  @Nonnull
  public Set<String> findAvailableKvnrs() {
    return Set.copyOf(testDataManager.getClassesFromList("$.patients..kvnr", String.class));
  }
}
