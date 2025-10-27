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
package de.gematik.ti20.simsvc.client.model.apdu;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApduScenarioTest {

  @Test
  void testConstructorAndGetters() {
    ApduStep step1 = mock(ApduStep.class);
    ApduStep step2 = mock(ApduStep.class);
    ApduScenario scenario = new ApduScenario("TestScenario", List.of(step1, step2));

    assertEquals("TestScenario", scenario.getName());
    assertEquals(2, scenario.getSteps().size());
    assertEquals(2, scenario.getStepCount());
  }

  @Test
  void testAddStep() {
    ApduScenario scenario = new ApduScenario("AddStepTest");
    ApduStep step = mock(ApduStep.class);
    scenario.addStep(step);

    assertEquals(1, scenario.getStepCount());
    assertTrue(scenario.getSteps().contains(step));
  }

  @Test
  void testFindStepByName() {
    ApduStep step1 = mock(ApduStep.class);
    when(step1.getName()).thenReturn("step1");
    ApduStep step2 = mock(ApduStep.class);
    when(step2.getName()).thenReturn("step2");

    ApduScenario scenario = new ApduScenario("FindStep", List.of(step1, step2));
    assertEquals(step2, scenario.findStepByName("step2"));
    assertNull(scenario.findStepByName("notfound"));
  }

  @Test
  void testToString() {
    ApduScenario scenario = new ApduScenario("ToStringTest");
    assertTrue(scenario.toString().contains("name='ToStringTest'"));
  }
}
