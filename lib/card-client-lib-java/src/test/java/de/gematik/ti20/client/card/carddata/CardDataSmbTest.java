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
package de.gematik.ti20.client.card.carddata;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CardDataSmbTest {

  @Test
  void testConstructorAndGetters() {
    CardDataSmb smb = new CardDataSmb("SMB123", "2030-12");
    assertEquals(CardData.Type.SMB, smb.getType());
    assertEquals("SMB123", smb.getNumber());
    assertEquals("2030-12", smb.getExpires());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    CardDataSmb smb = new CardDataSmb("SMB999", "2028-05");
    String json = smb.toJson();
    assertTrue(json.contains("\"type\":\"SMB\""));
    assertTrue(json.contains("\"number\":\"SMB999\""));
    assertTrue(json.contains("\"expires\":\"2028-05\""));

    ObjectMapper mapper = new ObjectMapper();
    CardDataSmb deserialized = mapper.readValue(json, CardDataSmb.class);
    assertEquals("SMB999", deserialized.getNumber());
    assertEquals("2028-05", deserialized.getExpires());
    assertEquals(CardData.Type.SMB, deserialized.getType());
  }
}
