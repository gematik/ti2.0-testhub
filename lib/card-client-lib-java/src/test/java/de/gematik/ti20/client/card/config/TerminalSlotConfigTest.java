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
package de.gematik.ti20.client.card.config;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.client.card.terminal.TerminalSlotV1;
import org.junit.jupiter.api.Test;

class TerminalSlotConfigTest {

  @Test
  void testConstructorAndGetters() {
    TerminalSlotConfig config = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "1");
    assertEquals(TerminalSlotV1.Type.JSON, config.getType());
    assertEquals("1", config.getSlotId());
    // Name ist nicht gesetzt, sollte Default liefern
    assertEquals("Slot 1", config.getName());
  }

  @Test
  void testSetNameAndGetName() {
    TerminalSlotConfig config = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "2");
    config.setName("eGK-Slot");
    assertEquals("eGK-Slot", config.getName());
  }

  @Test
  void testGetNameWithBlankName() {
    TerminalSlotConfig config = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "3");
    config.setName("   ");
    assertEquals("Slot 3", config.getName());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    TerminalSlotConfig config = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "4");
    config.setName("HBA-Slot");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(config);
    assertTrue(json.contains("\"type\":\"json\""));
    assertTrue(json.contains("\"slotId\":\"4\""));

    TerminalSlotConfig deserialized = mapper.readValue(json, TerminalSlotConfig.class);
    assertEquals(TerminalSlotV1.Type.JSON, deserialized.getType());
    assertEquals("4", deserialized.getSlotId());
    assertEquals("HBA-Slot", deserialized.getName());
  }
}
