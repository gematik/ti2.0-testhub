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

class CardDataSmcbTest {

  @Test
  void testConstructorAndGetters() {
    CardDataSmcb smcb = new CardDataSmcb("SMCB123", "2031-07", "Klinik X", "123456789");
    assertEquals(CardData.Type.SMCB, smcb.getType());
    assertEquals("SMCB123", smcb.getNumber());
    assertEquals("2031-07", smcb.getExpires());
    assertEquals("Klinik X", smcb.getInstitutionName());
    assertEquals("123456789", smcb.getBsnr());
    assertNotNull(smcb.getSecurityData());
  }

  @Test
  void testSecurityDataSettersAndGetters() {
    CardDataSmcb.SecurityData sec = new CardDataSmcb.SecurityData();
    sec.setPrivateKey("privKey");
    sec.setAuthCertificate("certData");
    assertEquals("privKey", sec.getPrivateKey());
    assertEquals("certData", sec.getAuthCertificate());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    CardDataSmcb smcb = new CardDataSmcb("SMCB999", "2028-12", "Praxis Y", "987654321");
    smcb.getSecurityData().setPrivateKey("key123");
    smcb.getSecurityData().setAuthCertificate("cert456");

    String json = smcb.toJson();
    assertTrue(json.contains("\"institutionName\":\"Praxis Y\""));
    assertTrue(json.contains("\"bsnr\":\"987654321\""));

    ObjectMapper mapper = new ObjectMapper();
    CardDataSmcb deserialized = mapper.readValue(json, CardDataSmcb.class);
    assertEquals("SMCB999", deserialized.getNumber());
    assertEquals("2028-12", deserialized.getExpires());
    assertEquals("Praxis Y", deserialized.getInstitutionName());
    assertEquals("987654321", deserialized.getBsnr());
    assertNotNull(deserialized.getSecurityData());
  }
}
