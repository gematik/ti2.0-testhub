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
package de.gematik.ti20.simsvc.client.model.card;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardTypeTest {

  @Test
  void testEnumValues() {
    assertEquals(CardType.EGK, CardType.valueOf("EGK"));
    assertEquals(CardType.EHC, CardType.valueOf("EHC"));
    assertEquals(CardType.HBA, CardType.valueOf("HBA"));
    assertEquals(CardType.HPC, CardType.valueOf("HPC"));
    assertEquals(CardType.HPIC, CardType.valueOf("HPIC"));
    assertEquals(CardType.SMCB, CardType.valueOf("SMCB"));
  }
}
