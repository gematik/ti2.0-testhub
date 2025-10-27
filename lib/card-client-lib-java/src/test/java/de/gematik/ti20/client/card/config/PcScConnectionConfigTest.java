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

class PcScConnectionConfigTest {

  @Test
  void testConstructorAndGetters() {
    PcScConnectionConfig config = new PcScConnectionConfig("PCSC-Test", "Reader 1");
    assertEquals("PCSC-Test", config.getName());
    assertEquals(CardTerminalType.PCSC, config.getType());
    assertEquals("Reader 1", config.getReaderName());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    PcScConnectionConfig config = new PcScConnectionConfig("JsonName", "USB-Reader");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(config);
    assertTrue(json.contains("\"name\":\"JsonName\""));
    assertTrue(json.contains("\"readerName\":\"USB-Reader\""));
    assertTrue(json.contains("\"type\":\"PCSC\""));

    CardTerminalConnectionConfig deserialized =
        mapper.readValue(json, CardTerminalConnectionConfig.class);
    assertTrue(deserialized instanceof PcScConnectionConfig);
    PcScConnectionConfig pcsc = (PcScConnectionConfig) deserialized;
    assertEquals("JsonName", pcsc.getName());
    assertEquals("USB-Reader", pcsc.getReaderName());
    assertEquals(CardTerminalType.PCSC, pcsc.getType());
  }
}
