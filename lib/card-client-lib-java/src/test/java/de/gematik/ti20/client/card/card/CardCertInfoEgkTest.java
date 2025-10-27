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
package de.gematik.ti20.client.card.card;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardCertInfoEgkTest {

  @Test
  void testDefaultConstructor() {
    CardCertInfoEgk info = new CardCertInfoEgk();
    assertEquals(CardType.EGK, info.getCardType());
    assertNull(info.getKvnr());
    assertNull(info.getIknr());
    assertNull(info.getPatientName());
    assertNull(info.getFirstName());
    assertNull(info.getLastName());
  }

  @Test
  void testParameterizedConstructorAndGetters() {
    CardCertInfoEgk info =
        new CardCertInfoEgk("kvnr123", "iknr456", "Max Mustermann", "Max", "Mustermann");
    assertEquals(CardType.EGK, info.getCardType());
    assertEquals("kvnr123", info.getKvnr());
    assertEquals("iknr456", info.getIknr());
    assertEquals("Max Mustermann", info.getPatientName());
    assertEquals("Max", info.getFirstName());
    assertEquals("Mustermann", info.getLastName());
  }

  @Test
  void testSetters() {
    CardCertInfoEgk info = new CardCertInfoEgk();
    info.setKvnr("kvnr999");
    info.setIknr("iknr888");
    info.setPatientName("Erika Musterfrau");
    info.setFirstName("Erika");
    info.setLastName("Musterfrau");

    assertEquals("kvnr999", info.getKvnr());
    assertEquals("iknr888", info.getIknr());
    assertEquals("Erika Musterfrau", info.getPatientName());
    assertEquals("Erika", info.getFirstName());
    assertEquals("Musterfrau", info.getLastName());
  }
}
