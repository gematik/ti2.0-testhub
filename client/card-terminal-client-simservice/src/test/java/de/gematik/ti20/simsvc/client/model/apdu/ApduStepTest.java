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

import java.util.List;
import org.junit.jupiter.api.Test;

class ApduStepTest {

  @Test
  void testConstructorAndGetters() {
    List<String> expected = List.of("9000", "6A82");
    ApduStep step = new ApduStep("Select", "Select file", "00A4040002AABB", expected);

    assertEquals("Select", step.getName());
    assertEquals("Select file", step.getDescription());
    assertEquals("00A4040002AABB", step.getCommandApdu());
    assertEquals(expected, step.getExpectedStatusWords());
  }

  @Test
  void testDefaultStatusWordConstructor() {
    ApduStep step = new ApduStep("Read", "Read binary", "00B0000001");
    assertEquals(List.of("9000"), step.getExpectedStatusWords());
  }

  @Test
  void testIsStatusWordExpected() {
    ApduStep step = new ApduStep("Test", "desc", "00A4", List.of("9000", "6A82"));
    assertTrue(step.isStatusWordExpected("9000"));
    assertTrue(step.isStatusWordExpected("6a82"));
    assertFalse(step.isStatusWordExpected("6A84"));
  }

  @Test
  void testParseCommand() {
    ApduStep step = new ApduStep("Parse", "desc", "00A40400");
    ApduCommand cmd = step.parseCommand();
    assertEquals((byte) 0x00, cmd.getCla());
    assertEquals((byte) 0xA4, cmd.getIns());
  }

  @Test
  void testToString() {
    ApduStep step = new ApduStep("ToString", "desc", "00A4");
    String str = step.toString();
    assertTrue(str.contains("name='ToString'"));
    assertTrue(str.contains("commandApdu='00A4'"));
  }
}
