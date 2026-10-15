/*-
 * #%L
 * erp-testsuite
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
package de.gematik.ti20.erp.popp;

import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PoppClientSerializationTest {

  private final ObjectMapper objectWriter = new ObjectMapper();

  @Test
  void shouldDeserializePoppClientResponse() {
    val poppClientResponseJson =
        """
        {
          "status" : "OK",
          "token" : "eyJraWQiOiIwMDEyMDA1OTAwMDEwMDAxIiwidHlwIjoidm5kLnRlbGVtYXRpay5wb3BwK2p3dCIsImFsZyI6IkVTMjU2In0.eyJ2ZXJzaW9uIjoiMS4wLjAiLCJwYXRpZW50UHJvb2ZUaW1lIjoxNzg4MzUzOTE2LCJhY3RvcklkIjoiMy1TTUMtQi1UZXN0a2FydGUtLTg4MzExMDAwMDE2Mzk3MyIsImFjdG9yUHJvZmVzc2lvbk9pZCI6IjEuMi4yNzYuMC43Ni40LjU0IiwicHJvb2ZNZXRob2QiOiJlaGMtcHJhY3RpdGlvbmVyLXRydXN0ZWRjaGFubmVsIiwicGF0aWVudElkIjoiQTE0NzkwNTMzNyIsImluc3VyZXJJZCI6IjEwMTU2MDAwMCIsImlhdCI6MTc4ODM1MzkxNywiaXNzIjoiaHR0cHM6Ly9wb3BwLmRldi5wb3Bwc2VydmljZS5kZSJ9.44RKyH8ZQpGZwr9V3PpKCWNkpQEaTseDwpqGDM1FiNz2JUHEgmb8sw0SElPkk83MQDwgsalhUwPAB8wtDAjKCw"
        }
        """;

    val response =
        assertDoesNotThrow(
            () -> objectWriter.readValue(poppClientResponseJson, PoppClientResponse.class));
    assertEquals("OK", response.status());
    assertTrue(response.token().startsWith("eyJraWQiOiIw"));
  }

  @ParameterizedTest
  @EnumSource(PoppClientCommunicationType.class)
  void shouldSerializePoppClientRequestBody(PoppClientCommunicationType type) {
    val requestBody = new PoppClientRequestBody(type);
    val json = assertDoesNotThrow(() -> objectWriter.writeValueAsString(requestBody));

    val expectedTypeValue = type.getLabel();
    assertTrue(json.contains(format("\"communicationType\":\"{0}\"", expectedTypeValue)));
  }
}
