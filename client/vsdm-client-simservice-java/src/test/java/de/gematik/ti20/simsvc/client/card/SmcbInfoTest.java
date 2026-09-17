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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SmcbInfoTest {

  @Test
  void shouldMapAllFields() {
    final SmcbInfo smcbInfo = new SmcbInfo("1-2.3.4.5.6", "1.2.276.0.76.4.54");

    assertEquals("1-2.3.4.5.6", smcbInfo.getTelematikId());
    assertEquals("1.2.276.0.76.4.54", smcbInfo.getProfessionOid());
  }

  @Test
  void shouldParseMapWithAdditionalAttributes() throws Exception {
    final ObjectMapper objectMapper = new ObjectMapper();
    final Map<String, Object> values =
        Map.of(
            "telematikId", "1-2.3.4.5.6",
            "professionOid", "1.2.276.0.76.4.54",
            "additionalAttribute", "ignored");

    final SmcbInfo smcbInfo = objectMapper.convertValue(values, SmcbInfo.class);

    assertEquals("1-2.3.4.5.6", smcbInfo.getTelematikId());
    assertEquals("1.2.276.0.76.4.54", smcbInfo.getProfessionOid());
  }
}
