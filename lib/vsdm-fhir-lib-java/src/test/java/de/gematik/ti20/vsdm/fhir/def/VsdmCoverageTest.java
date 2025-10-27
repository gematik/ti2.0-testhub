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

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmCoverageTest {

  @Test
  void testConstructor() {
    VsdmCoverage coverage = new VsdmCoverage();

    assertNotNull(coverage);
  }

  @Test
  void testInheritanceFromCoverage() {
    VsdmCoverage coverage = new VsdmCoverage();

    assertTrue(coverage instanceof Coverage);
  }

  @Test
  void testResourceDefAnnotation() {
    ResourceDef annotation = VsdmCoverage.class.getAnnotation(ResourceDef.class);

    assertNotNull(annotation);
    assertEquals(
        "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMCoverageGKV", annotation.profile());
  }

  @Test
  void testCanSetIdentifier() {
    VsdmCoverage coverage = new VsdmCoverage();
    Identifier identifier = new Identifier();
    identifier.setValue("test-identifier");

    coverage.addIdentifier(identifier);

    assertEquals(1, coverage.getIdentifier().size());
    assertEquals("test-identifier", coverage.getIdentifier().get(0).getValue());
  }

  @Test
  void testCanSetBeneficiary() {
    VsdmCoverage coverage = new VsdmCoverage();
    Reference beneficiary = new Reference("Patient/123");

    coverage.setBeneficiary(beneficiary);

    assertEquals("Patient/123", coverage.getBeneficiary().getReference());
  }

  @Test
  void testCanSetPayor() {
    VsdmCoverage coverage = new VsdmCoverage();
    Reference payor = new Reference("Organization/456");

    coverage.addPayor(payor);

    assertEquals(1, coverage.getPayor().size());
    assertEquals("Organization/456", coverage.getPayor().get(0).getReference());
  }

  @Test
  void testCanSetStatus() {
    VsdmCoverage coverage = new VsdmCoverage();

    coverage.setStatus(Coverage.CoverageStatus.ACTIVE);

    assertEquals(Coverage.CoverageStatus.ACTIVE, coverage.getStatus());
  }

  @Test
  void testSerialVersionUID() throws NoSuchFieldException {
    var field = VsdmCoverage.class.getDeclaredField("serialVersionUID");

    assertNotNull(field);
    assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()));
    assertEquals(long.class, field.getType());
  }

  @Test
  void testSerialVersionUIDValue() throws NoSuchFieldException, IllegalAccessException {
    var field = VsdmCoverage.class.getDeclaredField("serialVersionUID");
    field.setAccessible(true);

    assertEquals(-1234567890123456789L, field.get(null));
  }

  @Test
  void testResourceType() {
    VsdmCoverage coverage = new VsdmCoverage();

    assertEquals("Coverage", coverage.getResourceType().name());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(VsdmCoverage.class.getModifiers()));
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.vsdm.fhir.def", VsdmCoverage.class.getPackage().getName());
  }

  @Test
  void testConstructorAccessibility() {
    var constructors = VsdmCoverage.class.getDeclaredConstructors();

    assertEquals(1, constructors.length);
    assertTrue(java.lang.reflect.Modifier.isPublic(constructors[0].getModifiers()));
  }

  @Test
  void testHasDefaultConstructor() throws NoSuchMethodException {
    var constructor = VsdmCoverage.class.getDeclaredConstructor();

    assertNotNull(constructor);
    assertEquals(0, constructor.getParameterCount());
  }
}
