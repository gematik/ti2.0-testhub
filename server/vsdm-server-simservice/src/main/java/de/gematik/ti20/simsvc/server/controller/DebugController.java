/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.ti20.simsvc.server.controller;

import de.gematik.ti20.simsvc.server.repository.TestDataRepository;
import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
public class DebugController {

  @Nonnull private final TestDataRepository testDataRepository;

  public DebugController(@Nonnull final TestDataRepository testDataRepository) {
    this.testDataRepository = testDataRepository;
  }

  @GetMapping("patients")
  @Nonnull
  public List<VsdmPatient> getPatientByKvnr(
      @Nullable @RequestParam(name = "kvnr", required = false) final String kvnr) {
    if (kvnr == null || kvnr.isBlank()) {
      return listAvailableKvnrs().stream()
          .map(testDataRepository::patientByKvnr)
          .flatMap(Optional::stream)
          .toList();
    }

    return testDataRepository.patientByKvnr(kvnr).map(List::of).orElse(List.of());
  }

  @GetMapping("kvnrs")
  @Nonnull
  public Set<String> listAvailableKvnrs() {
    return testDataRepository.findAvailableKvnrs();
  }
}
