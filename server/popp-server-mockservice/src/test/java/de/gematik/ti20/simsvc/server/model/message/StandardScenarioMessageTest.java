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
package de.gematik.ti20.simsvc.server.model.message;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class StandardScenarioMessageTest {

  @Test
  void testDefaultConstructorSetsTypeAndVersion() {
    StandardScenarioMessage msg = new StandardScenarioMessage();
    assertEquals("StandardScenario", msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertNull(msg.getClientSessionId());
    assertEquals(0, msg.getSequenceCounter());
    assertEquals(0, msg.getTimeSpan());
    assertNull(msg.getSteps());
  }

  @Test
  void testConstructorWithFields() {
    List<ScenarioStep> steps = Arrays.asList(new ScenarioStep(), new ScenarioStep());
    StandardScenarioMessage msg = new StandardScenarioMessage("session123", 5, 10, steps);
    assertEquals("StandardScenario", msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertEquals("session123", msg.getClientSessionId());
    assertEquals(5, msg.getSequenceCounter());
    assertEquals(10, msg.getTimeSpan());
    assertEquals(steps, msg.getSteps());
  }

  @Test
  void testSettersAndGetters() {
    StandardScenarioMessage msg = new StandardScenarioMessage();
    msg.setClientSessionId("abc");
    msg.setSequenceCounter(42);
    msg.setTimeSpan(99);
    List<ScenarioStep> steps = Arrays.asList(new ScenarioStep());
    msg.setSteps(steps);
    msg.setVersion("2.0.0");
    assertEquals("abc", msg.getClientSessionId());
    assertEquals(42, msg.getSequenceCounter());
    assertEquals(99, msg.getTimeSpan());
    assertEquals(steps, msg.getSteps());
    assertEquals("2.0.0", msg.getVersion());
  }
}
