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
package de.gematik.ti20.vsdm.fhir.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import java.util.Date;
import org.hl7.fhir.r4.model.Address;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmPatientBuilderTest {

  @Test
  void testCreateBuilder() {
    VsdmPatientBuilder builder = VsdmPatientBuilder.create();

    assertNotNull(builder);
  }

  @Test
  void testInheritance() {
    VsdmPatientBuilder builder = VsdmPatientBuilder.create();

    assertTrue(builder instanceof ResourceBuilder);
  }

  @Test
  void testWithNames() {
    VsdmPatientBuilder builder = VsdmPatientBuilder.create().withNames("Mustermann", "Max");

    assertNotNull(builder);
    assertEquals("Mustermann", builder.nameFamily);
    assertEquals(1, builder.namesGiven.size());
    assertEquals("Max", builder.namesGiven.get(0));
  }

  @Test
  void testWithNamesMultipleGiven() {
    VsdmPatientBuilder builder = VsdmPatientBuilder.create().withNames("Mustermann", "Max Peter");

    assertNotNull(builder);
    assertEquals("Mustermann", builder.nameFamily);
    assertEquals(2, builder.namesGiven.size());
    assertEquals("Max", builder.namesGiven.get(0));
    assertEquals("Peter", builder.namesGiven.get(1));
  }

  @Test
  void testWithNamesNullGiven() {
    VsdmPatientBuilder builder = VsdmPatientBuilder.create().withNames("Mustermann", null);

    assertNotNull(builder);
    assertEquals("Mustermann", builder.nameFamily);
    assertTrue(builder.namesGiven.isEmpty());
  }

  @Test
  void testWithKvnr() {
    VsdmPatientBuilder builder = VsdmPatientBuilder.create().withKvnr("X123456789");

    assertNotNull(builder);
    assertEquals("X123456789", builder.kvnr);
  }

  @Test
  void testWithBirthDate() {
    Date birthDate = new Date();
    VsdmPatientBuilder builder = VsdmPatientBuilder.create().withBirthDate(birthDate);

    assertNotNull(builder);
    assertEquals(birthDate, builder.birthDate);
  }

  @Test
  void testAddAddress() {
    Address address = new Address();
    address.setCity("Berlin");

    VsdmPatientBuilder builder = VsdmPatientBuilder.create().addAddress(address);

    assertNotNull(builder);
    assertEquals(1, builder.addresses.size());
    assertEquals("Berlin", builder.addresses.get(0).getCity());
  }

  @Test
  void testAddMultipleAddresses() {
    Address address1 = new Address().setCity("Berlin");
    Address address2 = new Address().setCity("Hamburg");

    VsdmPatientBuilder builder =
        VsdmPatientBuilder.create().addAddress(address1).addAddress(address2);

    assertNotNull(builder);
    assertEquals(2, builder.addresses.size());
    assertEquals("Berlin", builder.addresses.get(0).getCity());
    assertEquals("Hamburg", builder.addresses.get(1).getCity());
  }

  @Test
  void testBuildMinimalPatient() {
    VsdmPatient patient =
        VsdmPatientBuilder.create().withNames("Mustermann", "Max").withKvnr("X123456789").build();

    assertNotNull(patient);
    assertEquals(1, patient.getName().size());
    assertEquals("Mustermann", patient.getName().get(0).getFamily());
    assertEquals("Max", patient.getName().get(0).getGiven().get(0).getValue());
    assertEquals(1, patient.getIdentifier().size());
    assertEquals("X123456789", patient.getIdentifier().get(0).getValue());
    assertEquals("http://fhir.de/sid/gkv/kvid-10", patient.getIdentifier().get(0).getSystem());
  }

  @Test
  void testBuildCompletePatient() {
    Date birthDate = new Date();
    Address address = new Address().setCity("Berlin");

    VsdmPatient patient =
        VsdmPatientBuilder.create()
            .withNames("Mustermann", "Max Peter")
            .withKvnr("X123456789")
            .withBirthDate(birthDate)
            .addAddress(address)
            .build();

    assertNotNull(patient);
    assertEquals("Mustermann", patient.getName().get(0).getFamily());
    assertEquals(2, patient.getName().get(0).getGiven().size());
    assertEquals("Max", patient.getName().get(0).getGiven().get(0).getValue());
    assertEquals("Peter", patient.getName().get(0).getGiven().get(1).getValue());
    assertEquals("X123456789", patient.getIdentifier().get(0).getValue());
    assertEquals(birthDate, patient.getBirthDate());
    assertEquals(1, patient.getAddress().size());
    assertEquals("Berlin", patient.getAddress().get(0).getCity());
  }

  @Test
  void testChainedCalls() {
    Date birthDate = new Date();
    Address address = new Address().setCity("Berlin");

    VsdmPatient patient =
        VsdmPatientBuilder.create()
            .withNames("Mustermann", "Max")
            .withKvnr("X123456789")
            .withBirthDate(birthDate)
            .addAddress(address)
            .build();

    assertNotNull(patient);
    assertEquals("Mustermann", patient.getName().get(0).getFamily());
    assertEquals("X123456789", patient.getIdentifier().get(0).getValue());
    assertEquals(birthDate, patient.getBirthDate());
    assertEquals(1, patient.getAddress().size());
  }

  @Test
  void testResourceDefAnnotationUsage() {
    VsdmPatient patient =
        VsdmPatientBuilder.create().withNames("Mustermann", "Max").withKvnr("X123456789").build();

    ResourceDef annotation = VsdmPatient.class.getAnnotation(ResourceDef.class);
    assertNotNull(annotation);
    assertNotNull(patient);
  }

  @Test
  void testIdentifierSystem() {
    VsdmPatient patient =
        VsdmPatientBuilder.create().withNames("Mustermann", "Max").withKvnr("X123456789").build();

    assertEquals("http://fhir.de/sid/gkv/kvid-10", patient.getIdentifier().get(0).getSystem());
  }

  @Test
  void testEmptyGivenNameHandling() {
    VsdmPatient patient =
        VsdmPatientBuilder.create().withNames("Mustermann", "").withKvnr("X123456789").build();

    assertNotNull(patient);
    assertEquals("Mustermann", patient.getName().get(0).getFamily());
    assertEquals(1, patient.getName().get(0).getGiven().size());
    assertEquals("", patient.getName().get(0).getGiven().get(0).getValue());
  }
}
