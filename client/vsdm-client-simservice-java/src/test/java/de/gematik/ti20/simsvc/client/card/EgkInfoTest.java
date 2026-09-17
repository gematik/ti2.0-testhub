/*-
 * #%L
 * VSDM Client Simulator Service
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EgkInfoTest {

  @Test
  void shouldMapAllFieldsAndSetValidToTrue() {
    final EgkInfo egkInfo = new EgkInfo("X123456789", "IK123456", "Max", "Mustermann", "true");

    assertEquals("X123456789", egkInfo.getKvnr());
    assertEquals("IK123456", egkInfo.getIknr());
    assertEquals("Max", egkInfo.getFirstName());
    assertEquals("Mustermann", egkInfo.getLastName());
    assertTrue(egkInfo.getValid());
  }

  @Test
  void shouldSetValidToFalseForNonTrueValues() {
    final EgkInfo egkInfo = new EgkInfo("X123456789", "IK123456", "Max", "Mustermann", "false");

    assertFalse(egkInfo.getValid());
  }

  @Test
  void shouldParseMapWithAdditionalAttributes() throws Exception {
    final ObjectMapper objectMapper = new ObjectMapper();
    final Map<String, Object> values =
        Map.of(
            "kvnr", "X123456789",
            "iknr", "IK123456",
            "firstName", "Max",
            "lastName", "Mustermann",
            "valid", "true",
            "additionalAttribute", "ignored");

    final EgkInfo egkInfo = objectMapper.convertValue(values, EgkInfo.class);

    assertEquals("X123456789", egkInfo.getKvnr());
    assertEquals("IK123456", egkInfo.getIknr());
    assertEquals("Max", egkInfo.getFirstName());
    assertEquals("Mustermann", egkInfo.getLastName());
    assertTrue(egkInfo.getValid());
  }
}
