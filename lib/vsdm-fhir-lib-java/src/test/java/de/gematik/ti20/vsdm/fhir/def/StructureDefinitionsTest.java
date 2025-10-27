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
package de.gematik.ti20.vsdm.fhir.def;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.bbriccs.fhir.coding.WithStructureDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StructureDefinitionsTest {

  @Test
  void testEnumValues() {
    StructureDefinitions[] values = StructureDefinitions.values();

    assertEquals(4, values.length);
    assertEquals(StructureDefinitions.VSDM_BUNDLE, values[0]);
    assertEquals(StructureDefinitions.VSDM_PATIENT, values[1]);
    assertEquals(StructureDefinitions.VSDM_COVERAGE, values[2]);
  }

  @Test
  void testVsdmBundleCanonicalUrl() {
    assertEquals(
        "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle",
        StructureDefinitions.VSDM_BUNDLE.getCanonicalUrl());
  }

  @Test
  void testVsdmPatientCanonicalUrl() {
    assertEquals(
        "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMPatient",
        StructureDefinitions.VSDM_PATIENT.getCanonicalUrl());
  }

  @Test
  void testVsdmCoverageCanonicalUrl() {
    assertEquals(
        "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMCoverage",
        StructureDefinitions.VSDM_COVERAGE.getCanonicalUrl());
  }

  @Test
  void testImplementsWithStructureDefinition() {
    assertTrue(StructureDefinitions.VSDM_BUNDLE instanceof WithStructureDefinition);
    assertTrue(StructureDefinitions.VSDM_PATIENT instanceof WithStructureDefinition);
    assertTrue(StructureDefinitions.VSDM_COVERAGE instanceof WithStructureDefinition);
  }

  @Test
  void testValueOf() {
    assertEquals(StructureDefinitions.VSDM_BUNDLE, StructureDefinitions.valueOf("VSDM_BUNDLE"));
    assertEquals(StructureDefinitions.VSDM_PATIENT, StructureDefinitions.valueOf("VSDM_PATIENT"));
    assertEquals(StructureDefinitions.VSDM_COVERAGE, StructureDefinitions.valueOf("VSDM_COVERAGE"));
  }

  @Test
  void testValueOfInvalidName() {
    assertThrows(
        IllegalArgumentException.class, () -> StructureDefinitions.valueOf("INVALID_NAME"));
  }

  @Test
  void testCanonicalUrlsAreNotNull() {
    for (StructureDefinitions definition : StructureDefinitions.values()) {
      assertNotNull(definition.getCanonicalUrl());
      assertFalse(definition.getCanonicalUrl().isEmpty());
    }
  }

  @Test
  void testCanonicalUrlsAreUnique() {
    StructureDefinitions[] values = StructureDefinitions.values();

    for (int i = 0; i < values.length; i++) {
      for (int j = i + 1; j < values.length; j++) {
        assertNotEquals(values[i].getCanonicalUrl(), values[j].getCanonicalUrl());
      }
    }
  }
}
