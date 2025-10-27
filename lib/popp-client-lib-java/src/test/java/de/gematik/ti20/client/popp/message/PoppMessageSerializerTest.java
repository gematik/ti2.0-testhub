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
package de.gematik.ti20.client.popp.message;

import static org.junit.jupiter.api.Assertions.*;

import java.security.InvalidParameterException;
import org.junit.jupiter.api.Test;

class PoppMessageSerializerTest {

  @Test
  void testToJsonAndFromJsonWithErrorMessage() {
    ErrorMessage msg = new ErrorMessage("404", "Not Found");
    String json = PoppMessageSerializer.toJson(msg);
    assertTrue(json.contains("\"errorCode\":\"404\""));
    assertTrue(json.contains("\"errorDetail\":\"Not Found\""));
    assertTrue(json.contains("\"type\":\"Error\""));

    BasePoppMessage deserialized = PoppMessageSerializer.fromJson(json);
    assertTrue(deserialized instanceof ErrorMessage);
    ErrorMessage err = (ErrorMessage) deserialized;
    assertEquals("404", err.getErrorCode());
    assertEquals("Not Found", err.getErrorDetail());
    assertEquals("Error", err.getType());
  }

  @Test
  void testFromJsonWithUnknownType() {
    String json = "{\"type\":\"Unbekannt\",\"foo\":\"bar\"}";
    InvalidParameterException ex =
        assertThrows(InvalidParameterException.class, () -> PoppMessageSerializer.fromJson(json));
    assertTrue(ex.getMessage().contains("Failed to parse from JSON"));
  }

  @Test
  void testFromJsonWithInvalidJson() {
    String json = "{invalid json}";
    InvalidParameterException ex =
        assertThrows(InvalidParameterException.class, () -> PoppMessageSerializer.fromJson(json));
    assertTrue(ex.getMessage().contains("Failed to parse from JSON"));
  }
}
