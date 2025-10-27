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

class CardDataEgkTest {

  @Test
  void testConstructorAndGetters() {
    CardDataEgk egk = new CardDataEgk("123456", "2026-12", "A123456789");
    assertEquals(CardData.Type.EGK, egk.getType());
    assertEquals("123456", egk.getNumber());
    assertEquals("2026-12", egk.getExpires());
    assertEquals("A123456789", egk.getKvnr());
    assertNotNull(egk.getPersonalData());
    assertNotNull(egk.getInsuranceData());
  }

  @Test
  void testPersonalDataSettersAndGetters() {
    CardDataEgk.PersonalData pd = new CardDataEgk.PersonalData();
    pd.setName("Max Mustermann");
    pd.setBirthDate("1980-01-01");
    pd.setGender("M");
    pd.setAddress("Musterstraße 1");

    assertEquals("Max Mustermann", pd.getName());
    assertEquals("1980-01-01", pd.getBirthDate());
    assertEquals("M", pd.getGender());
    assertEquals("Musterstraße 1", pd.getAddress());
  }

  @Test
  void testInsuranceDataSettersAndGetters() {
    CardDataEgk.InsuranceData id = new CardDataEgk.InsuranceData();
    id.setInsuranceName("AOK");
    id.setInsuredStatus("active");

    assertEquals("AOK", id.getInsuranceName());
    assertEquals("active", id.getInsuredStatus());
  }

  @Test
  void testJsonSerializationAndDeserialization() throws Exception {
    CardDataEgk egk = new CardDataEgk("987654", "2025-01", "K987654321");
    egk.getPersonalData().setName("Erika Musterfrau");
    egk.getPersonalData().setBirthDate("1975-05-05");
    egk.getPersonalData().setGender("F");
    egk.getPersonalData().setAddress("Beispielweg 2");
    egk.getInsuranceData().setInsuranceName("TK");
    egk.getInsuranceData().setInsuredStatus("inactive");

    String json = egk.toJson();
    assertTrue(json.contains("\"kvnr\":\"K987654321\""));
    assertTrue(json.contains("\"name\":\"Erika Musterfrau\""));
    assertTrue(json.contains("\"insuranceName\":\"TK\""));

    ObjectMapper mapper = new ObjectMapper();
    CardDataEgk deserialized = mapper.readValue(json, CardDataEgk.class);
    assertEquals("987654", deserialized.getNumber());
    assertEquals("2025-01", deserialized.getExpires());
    assertEquals("K987654321", deserialized.getKvnr());
    assertEquals("Erika Musterfrau", deserialized.getPersonalData().getName());
    assertEquals("TK", deserialized.getInsuranceData().getInsuranceName());
  }
}
