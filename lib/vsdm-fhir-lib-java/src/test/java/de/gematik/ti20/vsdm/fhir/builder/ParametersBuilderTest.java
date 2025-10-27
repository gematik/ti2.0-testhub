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

import de.gematik.bbriccs.fhir.coding.WithStructureDefinition;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParametersBuilderTest {

  @Mock private WithStructureDefinition<?> mockDefinition;

  @Mock private CanonicalType mockCanonicalType;

  @Test
  void testCreateBuilder() {
    ParametersBuilder builder = ParametersBuilder.create(mockDefinition);

    assertNotNull(builder);
  }

  @Test
  void testSetStringType() {
    StringType stringType = new StringType("test");

    ParametersBuilder builder = ParametersBuilder.create(mockDefinition).set("param1", stringType);

    assertNotNull(builder);
  }

  @Test
  void testSetStringValue() {
    ParametersBuilder builder = ParametersBuilder.create(mockDefinition).set("param1", "testValue");

    assertNotNull(builder);
  }

  @Test
  void testSetBooleanValue() {
    ParametersBuilder builder = ParametersBuilder.create(mockDefinition).set("param1", true);

    assertNotNull(builder);
  }

  @Test
  void testBuildWithNoParameters() {
    when(mockDefinition.asCanonicalType()).thenReturn(mockCanonicalType);

    Parameters parameters = ParametersBuilder.create(mockDefinition).build();

    assertNotNull(parameters);
    assertTrue(parameters.getParameter().isEmpty());
  }

  @Test
  void testBuildWithStringParameter() {
    when(mockDefinition.asCanonicalType()).thenReturn(mockCanonicalType);

    Parameters parameters =
        ParametersBuilder.create(mockDefinition).set("testParam", "testValue").build();

    assertNotNull(parameters);
    assertEquals(1, parameters.getParameter().size());
    assertEquals("testParam", parameters.getParameter().get(0).getName());
    assertEquals(
        "testValue", ((StringType) parameters.getParameter().get(0).getValue()).getValue());
  }

  @Test
  void testBuildWithBooleanParameter() {
    when(mockDefinition.asCanonicalType()).thenReturn(mockCanonicalType);

    Parameters parameters = ParametersBuilder.create(mockDefinition).set("boolParam", true).build();

    assertNotNull(parameters);
    assertEquals(1, parameters.getParameter().size());
    assertEquals("boolParam", parameters.getParameter().get(0).getName());
    assertTrue(((BooleanType) parameters.getParameter().get(0).getValue()).getValue());
  }

  @Test
  void testBuildWithMultipleParameters() {
    when(mockDefinition.asCanonicalType()).thenReturn(mockCanonicalType);

    Parameters parameters =
        ParametersBuilder.create(mockDefinition)
            .set("param1", "value1")
            .set("param2", true)
            .set("param3", "value3")
            .build();

    assertNotNull(parameters);
    assertEquals(3, parameters.getParameter().size());
  }

  @Test
  void testChainedCalls() {
    when(mockDefinition.asCanonicalType()).thenReturn(mockCanonicalType);

    ParametersBuilder builder =
        ParametersBuilder.create(mockDefinition).set("param1", "value1").set("param2", true);

    assertNotNull(builder);

    Parameters parameters = builder.build();
    assertNotNull(parameters);
    assertEquals(2, parameters.getParameter().size());
  }

  @Test
  void testOverwriteParameter() {
    when(mockDefinition.asCanonicalType()).thenReturn(mockCanonicalType);

    Parameters parameters =
        ParametersBuilder.create(mockDefinition)
            .set("param1", "oldValue")
            .set("param1", "newValue")
            .build();

    assertNotNull(parameters);
    assertEquals(1, parameters.getParameter().size());
    assertEquals("newValue", ((StringType) parameters.getParameter().get(0).getValue()).getValue());
  }
}
