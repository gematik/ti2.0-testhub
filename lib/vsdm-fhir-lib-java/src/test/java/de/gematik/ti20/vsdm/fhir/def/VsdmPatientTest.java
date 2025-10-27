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
import static org.mockito.Mockito.*;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmPatientTest {

  @Test
  void testConstructor() {
    VsdmPatient patient = new VsdmPatient();

    assertNotNull(patient);
  }

  @Test
  void testInheritance() {
    VsdmPatient patient = new VsdmPatient();

    assertTrue(patient instanceof Patient);
  }

  @Test
  void testResourceDefAnnotation() {
    ResourceDef annotation = VsdmPatient.class.getAnnotation(ResourceDef.class);

    assertNotNull(annotation);
    assertEquals(
        "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMPatient", annotation.profile());
  }

  @Test
  void testGetResourceType() {
    VsdmPatient patient = new VsdmPatient();

    assertEquals("Patient", patient.getResourceType().name());
  }

  @Test
  void testCanAddName() {
    VsdmPatient patient = new VsdmPatient();
    HumanName name = new HumanName();
    name.setFamily("Mustermann");
    name.addGiven("Max");

    patient.addName(name);

    assertEquals(1, patient.getName().size());
    assertEquals("Mustermann", patient.getName().get(0).getFamily());
    assertEquals("Max", patient.getName().get(0).getGiven().get(0).getValue());
  }

  @Test
  void testEmptyPatient() {
    VsdmPatient patient = new VsdmPatient();

    assertTrue(patient.getName().isEmpty());
    assertTrue(patient.getIdentifier().isEmpty());
    assertNull(patient.getBirthDate());
  }

  @Test
  void testHasDefaultConstructor() throws NoSuchMethodException {
    var constructor = VsdmPatient.class.getDeclaredConstructor();

    assertNotNull(constructor);
    assertEquals(0, constructor.getParameterCount());
  }

  @Test
  void testSerialVersionUID() throws NoSuchFieldException {
    var field = VsdmPatient.class.getDeclaredField("serialVersionUID");

    assertNotNull(field);
    assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()));
  }
}
