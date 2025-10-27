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
package de.gematik.ti20.client.popp.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BasePoppMessageTypeTest {

  @Test
  void testGetValueAndGetClazz() {
    assertEquals("Error", BasePoppMessageType.ERROR.getValue());
    assertEquals(ErrorMessage.class, BasePoppMessageType.ERROR.getClazz());

    assertEquals("StartMessage", BasePoppMessageType.START.getValue());
    assertEquals(StartMessage.class, BasePoppMessageType.START.getClazz());

    assertEquals("Token", BasePoppMessageType.TOKEN.getValue());
    assertEquals(TokenMessage.class, BasePoppMessageType.TOKEN.getClazz());

    assertEquals("StandardScenario", BasePoppMessageType.STANDARD_SCENARIO.getValue());
    assertEquals(StandardScenarioMessage.class, BasePoppMessageType.STANDARD_SCENARIO.getClazz());

    assertEquals("ScenarioResponse", BasePoppMessageType.SCENARIO_RESPONSE.getValue());
    assertEquals(ScenarioResponseMessage.class, BasePoppMessageType.SCENARIO_RESPONSE.getClazz());
  }

  @Test
  void testGetClassForTypeValid() {
    assertEquals(ErrorMessage.class, BasePoppMessageType.getClassForType("Error"));
    assertEquals(StartMessage.class, BasePoppMessageType.getClassForType("StartMessage"));
    assertEquals(TokenMessage.class, BasePoppMessageType.getClassForType("Token"));
    assertEquals(
        StandardScenarioMessage.class, BasePoppMessageType.getClassForType("StandardScenario"));
    assertEquals(
        ScenarioResponseMessage.class, BasePoppMessageType.getClassForType("ScenarioResponse"));
  }

  @Test
  void testGetClassForTypeInvalid() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class, () -> BasePoppMessageType.getClassForType("Unbekannt"));
    assertTrue(ex.getMessage().contains("Unknown BasePoppMessageType"));
  }
}
