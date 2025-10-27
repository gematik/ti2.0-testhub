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
import de.gematik.ti20.client.card.terminal.CardTerminalType;
import org.junit.jupiter.api.Test;

class SimulatorConnectionConfigTest {

  @Test
  void testConstructorAndGetters() {
    SimulatorConnectionConfig config =
        new SimulatorConnectionConfig("SimName", "http://localhost:1234");
    assertEquals("SimName", config.getName());
    assertEquals(CardTerminalType.SIMSVC, config.getType());
    assertEquals("http://localhost:1234", config.getUrl());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    SimulatorConnectionConfig config =
        new SimulatorConnectionConfig("SimJson", "http://simulator:5678");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(config);
    assertTrue(json.contains("\"name\":\"SimJson\""));
    assertTrue(json.contains("\"url\":\"http://simulator:5678\""));
    assertTrue(
        json.contains("\"type\":\"SIMSVC\"") || json.contains("\"type\":\"SIMSVC\"".toLowerCase()));

    CardTerminalConnectionConfig deserialized =
        mapper.readValue(json, CardTerminalConnectionConfig.class);
    assertTrue(deserialized instanceof SimulatorConnectionConfig);
    SimulatorConnectionConfig sim = (SimulatorConnectionConfig) deserialized;
    assertEquals("SimJson", sim.getName());
    assertEquals("http://simulator:5678", sim.getUrl());
    assertEquals(CardTerminalType.SIMSVC, sim.getType());
  }
}
