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
import de.gematik.ti20.client.card.terminal.CardTerminalV1;
import de.gematik.ti20.client.card.terminal.TerminalSlotV1;
import java.util.*;
import org.junit.jupiter.api.Test;

class CardTerminalConfigTest {

  @Test
  void testConstructorAndGetters() {
    CardTerminalConfig config =
        new CardTerminalConfig("V1", "P1", "ProdName", "SN123", CardTerminalV1.Type.SIMULATOR);
    assertEquals("V1", config.getVendorId());
    assertEquals("P1", config.getProductId());
    assertEquals("ProdName", config.getProductName());
    assertEquals("SN123", config.getSerialNumber());
    assertEquals(CardTerminalV1.Type.SIMULATOR, config.getType());
    assertNotNull(config.getConnection());
    assertNotNull(config.getSlots());
  }

  @Test
  void testSetAndGetConnection() {
    CardTerminalConfig config =
        new CardTerminalConfig("V2", "P2", "Prod2", "SN456", CardTerminalV1.Type.SIMULATOR);
    Map<String, String> conn = new HashMap<>();
    conn.put("port", "USB1");
    conn.put("baud", "9600");
    config.setConnection(conn);
    assertEquals(2, config.getConnection().size());
    assertEquals("USB1", config.getConnection().get("port"));
    assertEquals("9600", config.getConnection().get("baud"));
  }

  @Test
  void testSetAndGetSlots() {
    CardTerminalConfig config =
        new CardTerminalConfig("V3", "P3", "Prod3", "SN789", CardTerminalV1.Type.SIMULATOR);
    TerminalSlotConfig slot1 = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "slot1");
    TerminalSlotConfig slot2 = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "slot2");
    List<TerminalSlotConfig> slots = Arrays.asList(slot1, slot2);
    config.setSlots(slots);
    assertEquals(2, config.getSlots().size());
    assertEquals("Slot slot1", config.getSlots().get(0).getName());
  }

  @Test
  void testAddSlot() {
    CardTerminalConfig config =
        new CardTerminalConfig("V4", "P4", "Prod4", "SN000", CardTerminalV1.Type.SIMULATOR);
    TerminalSlotConfig slot = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "slotX");
    config.addSlot(slot);
    assertEquals(1, config.getSlots().size());
    assertEquals("Slot slotX", config.getSlots().get(0).getName());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    CardTerminalConfig config =
        new CardTerminalConfig("V5", "P5", "Prod5", "SN555", CardTerminalV1.Type.SIMULATOR);
    config.setConnection(Collections.singletonMap("interface", "USB"));
    config.addSlot(new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "slotA"));

    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(config);
    System.out.println(json);
    assertTrue(json.contains("\"vendorId\":\"V5\""));
    assertTrue(json.contains("\"type\":\"SIMULATOR\""));

    json =
        "{\"vendorId\":\"V5\",\"productId\":\"P5\",\"productName\":\"Prod5\",\"serialNumber\":\"SN555\",\"type\":\"simulator\"}";
    CardTerminalConfig deserialized = mapper.readValue(json, CardTerminalConfig.class);
    assertEquals("V5", deserialized.getVendorId());
    assertEquals("P5", deserialized.getProductId());
    assertEquals("Prod5", deserialized.getProductName());
    assertEquals("SN555", deserialized.getSerialNumber());
    assertEquals(CardTerminalV1.Type.SIMULATOR, deserialized.getType());
  }
}
