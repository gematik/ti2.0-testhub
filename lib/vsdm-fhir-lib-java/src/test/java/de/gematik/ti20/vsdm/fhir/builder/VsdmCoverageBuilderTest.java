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

import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmCoverage;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmCoverageBuilderTest {

  @Test
  void testCreateBuilder() {
    VsdmCoverageBuilder builder = VsdmCoverageBuilder.create();

    assertNotNull(builder);
  }

  @Test
  void testInheritance() {
    VsdmCoverageBuilder builder = VsdmCoverageBuilder.create();

    assertTrue(builder instanceof ResourceBuilder);
  }

  @Test
  void testBuildReturnsVsdmCoverage() {
    VsdmCoverage coverage = VsdmCoverageBuilder.create().build();

    assertNotNull(coverage);
    assertTrue(coverage instanceof VsdmCoverage);
  }

  @Test
  void testBuildCreatesNewInstance() {
    VsdmCoverageBuilder builder = VsdmCoverageBuilder.create();

    VsdmCoverage coverage1 = builder.build();
    VsdmCoverage coverage2 = builder.build();

    assertNotSame(coverage1, coverage2);
  }

  @Test
  void testPrivateConstructor() {
    var constructors = VsdmCoverageBuilder.class.getDeclaredConstructors();

    assertEquals(1, constructors.length);
    assertTrue(java.lang.reflect.Modifier.isPrivate(constructors[0].getModifiers()));
    assertEquals(0, constructors[0].getParameterCount());
  }

  @Test
  void testFactoryMethodIsStatic() throws NoSuchMethodException {
    var method = VsdmCoverageBuilder.class.getDeclaredMethod("create");

    assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    assertEquals(VsdmCoverageBuilder.class, method.getReturnType());
  }

  @Test
  void testBuildMethodOverride() throws NoSuchMethodException {
    var method = VsdmCoverageBuilder.class.getDeclaredMethod("build");

    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    assertEquals(VsdmCoverage.class, method.getReturnType());
  }

  @Test
  void testChainedCalls() {
    VsdmCoverageBuilder builder = VsdmCoverageBuilder.create();

    assertNotNull(builder);

    VsdmCoverage coverage = builder.build();
    assertNotNull(coverage);
  }

  @Test
  void testGenericTypeParameters() {
    VsdmCoverageBuilder builder = VsdmCoverageBuilder.create();

    var superclass = builder.getClass().getGenericSuperclass();
    assertNotNull(superclass);
    assertTrue(superclass.getTypeName().contains("VsdmCoverage"));
    assertTrue(superclass.getTypeName().contains("VsdmCoverageBuilder"));
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.vsdm.fhir.builder", VsdmCoverageBuilder.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(VsdmCoverageBuilder.class.getModifiers()));
  }

  @Test
  void testClassIsFinal() {
    assertFalse(java.lang.reflect.Modifier.isFinal(VsdmCoverageBuilder.class.getModifiers()));
  }

  @Test
  void testWithStatusSetsStatus() {
    VsdmCoverageBuilder builder = VsdmCoverageBuilder.create();

    VsdmCoverageBuilder result = builder.withStatus("active");

    assertSame(builder, result);
    assertEquals("active", builder.status);
  }

  @Test
  void testWithPayorSetsPayor() {
    VsdmCoverageBuilder builder = VsdmCoverageBuilder.create();

    VsdmCoverageBuilder result = builder.withPayor("Test Insurance");

    assertSame(builder, result);
    assertEquals("Test Insurance", builder.payor);
  }

  @Test
  void testBuildWithStatusAndPayor() {
    VsdmCoverage coverage =
        VsdmCoverageBuilder.create().withStatus("active").withPayor("Test Insurance").build();

    assertNotNull(coverage);
    assertEquals(Coverage.CoverageStatus.ACTIVE, coverage.getStatus());
    assertEquals(1, coverage.getPayor().size());
    assertEquals("Test Insurance", coverage.getPayor().get(0).getDisplay());
  }

  @Test
  void testChainedMethodCalls() {
    VsdmCoverageBuilder builder =
        VsdmCoverageBuilder.create().withStatus("cancelled").withPayor("Another Insurance");

    assertEquals("cancelled", builder.status);
    assertEquals("Another Insurance", builder.payor);
  }

  @Test
  void testBuildSetsCorrectCoverageStatus() {
    VsdmCoverage coverage = VsdmCoverageBuilder.create().withStatus("cancelled").build();

    assertEquals(Coverage.CoverageStatus.CANCELLED, coverage.getStatus());
  }

  @Test
  void testBuildWithAllFields() {
    VsdmCoverage coverage =
        VsdmCoverageBuilder.create()
            .withStatus("active")
            .withPayor("Test Insurance")
            .withBeneficiary("Max Mustermann")
            .build();

    assertNotNull(coverage);
    assertEquals(Coverage.CoverageStatus.ACTIVE, coverage.getStatus());
    assertNotNull(coverage.getType());
    assertNotNull(coverage.getPeriod());

    assertEquals(1, coverage.getPayor().size());
    assertEquals("Test Insurance", coverage.getPayor().get(0).getDisplay());

    assertNotNull(coverage.getBeneficiary());
    assertEquals("Max Mustermann", coverage.getBeneficiary().getReference());
  }
}
