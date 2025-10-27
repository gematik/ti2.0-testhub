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
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmOperationOutcomeTest {

  @Test
  void testConstructor() {
    VsdmOperationOutcome outcome = new VsdmOperationOutcome();

    assertNotNull(outcome);
  }

  @Test
  void testInheritanceFromOperationOutcome() {
    VsdmOperationOutcome outcome = new VsdmOperationOutcome();

    assertTrue(outcome instanceof OperationOutcome);
  }

  @Test
  void testResourceDefAnnotation() {
    ResourceDef annotation = VsdmOperationOutcome.class.getAnnotation(ResourceDef.class);

    assertNotNull(annotation);
    assertEquals(
        "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMOperationOutcome",
        annotation.profile());
  }

  @Test
  void testCanAddIssue() {
    VsdmOperationOutcome outcome = new VsdmOperationOutcome();
    OperationOutcome.OperationOutcomeIssueComponent issue =
        new OperationOutcome.OperationOutcomeIssueComponent();
    issue.setSeverity(OperationOutcome.IssueSeverity.ERROR);
    issue.setCode(OperationOutcome.IssueType.PROCESSING);

    outcome.addIssue(issue);

    assertEquals(1, outcome.getIssue().size());
    assertEquals(OperationOutcome.IssueSeverity.ERROR, outcome.getIssue().get(0).getSeverity());
    assertEquals(OperationOutcome.IssueType.PROCESSING, outcome.getIssue().get(0).getCode());
  }

  @Test
  void testResourceType() {
    VsdmOperationOutcome outcome = new VsdmOperationOutcome();

    assertEquals("OperationOutcome", outcome.getResourceType().name());
  }

  @Test
  void testSerialVersionUID() throws NoSuchFieldException {
    var field = VsdmOperationOutcome.class.getDeclaredField("serialVersionUID");

    assertNotNull(field);
    assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()));
    assertEquals(long.class, field.getType());
  }

  @Test
  void testHasDefaultConstructor() throws NoSuchMethodException {
    var constructor = VsdmOperationOutcome.class.getDeclaredConstructor();

    assertNotNull(constructor);
    assertEquals(0, constructor.getParameterCount());
    assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
  }

  @Test
  void testCanSetMultipleIssues() {
    VsdmOperationOutcome outcome = new VsdmOperationOutcome();
    OperationOutcome.OperationOutcomeIssueComponent issue1 =
        new OperationOutcome.OperationOutcomeIssueComponent();
    issue1.setSeverity(OperationOutcome.IssueSeverity.ERROR);
    OperationOutcome.OperationOutcomeIssueComponent issue2 =
        new OperationOutcome.OperationOutcomeIssueComponent();
    issue2.setSeverity(OperationOutcome.IssueSeverity.WARNING);

    outcome.addIssue(issue1);
    outcome.addIssue(issue2);

    assertEquals(2, outcome.getIssue().size());
    assertEquals(OperationOutcome.IssueSeverity.ERROR, outcome.getIssue().get(0).getSeverity());
    assertEquals(OperationOutcome.IssueSeverity.WARNING, outcome.getIssue().get(1).getSeverity());
  }

  @Test
  void testCanSetId() {
    VsdmOperationOutcome outcome = new VsdmOperationOutcome();
    String testId = "test-id-123";

    outcome.setId(testId);

    assertEquals(testId, outcome.getId());
  }
}
