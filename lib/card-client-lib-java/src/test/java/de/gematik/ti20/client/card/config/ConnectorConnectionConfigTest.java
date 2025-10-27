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

class ConnectorConnectionConfigTest {

  @Test
  void testConstructorAndGetters() {
    ConnectorConnectionConfig config =
        new ConnectorConnectionConfig(
            "TestName", "https://endpoint", "mandant1", "clientSys1", "workplace1", "user1");
    assertEquals("TestName", config.getName());
    assertEquals(CardTerminalType.CONNECTOR, config.getType());
    assertEquals("https://endpoint", config.getEndpointAddress());
    assertEquals("mandant1", config.getMandantId());
    assertEquals("clientSys1", config.getClientSystemId());
    assertEquals("workplace1", config.getWorkplaceId());
    assertEquals("user1", config.getUserId());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    ConnectorConnectionConfig config =
        new ConnectorConnectionConfig(
            "JsonName", "https://ep", "mand2", "clientSys2", "workplace2", "user2");
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(config);
    assertTrue(json.contains("\"name\":\"JsonName\""));
    assertTrue(json.contains("\"endpointAddress\":\"https://ep\""));
    assertTrue(json.contains("\"type\":\"CONNECTOR\""));

    CardTerminalConnectionConfig deserialized =
        mapper.readValue(json, CardTerminalConnectionConfig.class);
    assertTrue(deserialized instanceof ConnectorConnectionConfig);
    ConnectorConnectionConfig cc = (ConnectorConnectionConfig) deserialized;
    assertEquals("JsonName", cc.getName());
    assertEquals("https://ep", cc.getEndpointAddress());
    assertEquals("mand2", cc.getMandantId());
    assertEquals("clientSys2", cc.getClientSystemId());
    assertEquals("workplace2", cc.getWorkplaceId());
    assertEquals("user2", cc.getUserId());
    assertEquals(CardTerminalType.CONNECTOR, cc.getType());
  }
}
