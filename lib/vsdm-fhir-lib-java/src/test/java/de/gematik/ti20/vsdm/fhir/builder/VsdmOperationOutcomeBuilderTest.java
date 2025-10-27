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
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmOperationOutcomeBuilderTest {

  @Test
  void testCreateBuilder() {
    VsdmOperationOutcomeBuilder builder = VsdmOperationOutcomeBuilder.create();

    assertNotNull(builder);
  }

  @Test
  void testInheritance() {
    VsdmOperationOutcomeBuilder builder = VsdmOperationOutcomeBuilder.create();

    assertTrue(builder instanceof ResourceBuilder);
  }

  @Test
  void testWithCode() {
    VsdmOperationOutcomeBuilder builder = VsdmOperationOutcomeBuilder.create().withCode("404");

    assertNotNull(builder);
  }

  @Test
  void testWithText() {
    VsdmOperationOutcomeBuilder builder =
        VsdmOperationOutcomeBuilder.create().withText("Error message");

    assertNotNull(builder);
  }

  @Test
  void testBuildReturnsVsdmOperationOutcome() {
    VsdmOperationOutcome outcome = VsdmOperationOutcomeBuilder.create().build();

    assertNotNull(outcome);
    assertTrue(outcome instanceof VsdmOperationOutcome);
  }

  @Test
  void testBuildWithText() {
    String testText = "Test error message";
    VsdmOperationOutcome outcome = VsdmOperationOutcomeBuilder.create().withText(testText).build();

    assertNotNull(outcome);
    assertEquals(1, outcome.getIssue().size());
    assertEquals(testText, outcome.getIssue().get(0).getDetails().getText());
    assertEquals(testText, outcome.getIssue().get(0).getDetails().getCoding().get(0).getDisplay());
  }

  @Test
  void testBuildWithAllParameters() {
    String testCode = "500";
    String testText = "Internal server error";
    VsdmOperationOutcome outcome =
        VsdmOperationOutcomeBuilder.create().withCode(testCode).withText(testText).build();

    assertNotNull(outcome);
    assertEquals(1, outcome.getIssue().size());
    var issue = outcome.getIssue().get(0);
    assertEquals(OperationOutcome.IssueSeverity.FATAL, issue.getSeverity());
    assertEquals(OperationOutcome.IssueType.INVALID, issue.getCode());
    assertEquals(testText, issue.getDetails().getText());
    assertEquals(testText, issue.getDetails().getCoding().get(0).getDisplay());
  }

  @Test
  void testBuildCreatesNewInstance() {
    VsdmOperationOutcomeBuilder builder = VsdmOperationOutcomeBuilder.create();

    VsdmOperationOutcome outcome1 = builder.build();
    VsdmOperationOutcome outcome2 = builder.build();

    assertNotSame(outcome1, outcome2);
  }

  @Test
  void testChainedCalls() {
    VsdmOperationOutcome outcome =
        VsdmOperationOutcomeBuilder.create().withCode("400").withText("Bad request").build();

    assertNotNull(outcome);
    assertEquals(1, outcome.getIssue().size());
  }

  @Test
  void testPrivateConstructor() {
    var constructors = VsdmOperationOutcomeBuilder.class.getDeclaredConstructors();

    assertEquals(1, constructors.length);
    assertTrue(java.lang.reflect.Modifier.isPrivate(constructors[0].getModifiers()));
    assertEquals(0, constructors[0].getParameterCount());
  }

  @Test
  void testFactoryMethodIsStatic() throws NoSuchMethodException {
    var method = VsdmOperationOutcomeBuilder.class.getDeclaredMethod("create");

    assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    assertEquals(VsdmOperationOutcomeBuilder.class, method.getReturnType());
  }

  @Test
  void testBuildMethodOverride() throws NoSuchMethodException {
    var method = VsdmOperationOutcomeBuilder.class.getDeclaredMethod("build");

    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    assertEquals(VsdmOperationOutcome.class, method.getReturnType());
  }

  @Test
  void testDefaultIssueSeverityAndCode() {
    VsdmOperationOutcome outcome = VsdmOperationOutcomeBuilder.create().build();

    assertEquals(1, outcome.getIssue().size());
    assertEquals(OperationOutcome.IssueSeverity.FATAL, outcome.getIssue().get(0).getSeverity());
    assertEquals(OperationOutcome.IssueType.INVALID, outcome.getIssue().get(0).getCode());
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.vsdm.fhir.builder",
        VsdmOperationOutcomeBuilder.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(
        java.lang.reflect.Modifier.isPublic(VsdmOperationOutcomeBuilder.class.getModifiers()));
  }
}
