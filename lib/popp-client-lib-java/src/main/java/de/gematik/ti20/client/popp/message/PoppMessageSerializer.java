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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.InvalidParameterException;

public class PoppMessageSerializer {

  private static final ObjectMapper jsonConverter =
      new ObjectMapper()
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  public static BasePoppMessage fromJson(final String json) {
    try {
      JsonNode jsonNode = jsonConverter.readTree(json);
      String type = jsonNode.get("type").asText();

      Class<? extends BasePoppMessage> messageClass = BasePoppMessageType.getClassForType(type);
      return jsonConverter.treeToValue(jsonNode, messageClass);

    } catch (final Exception e) {
      throw new InvalidParameterException("Failed to parse from JSON: " + e.getMessage());
    }
  }

  public static String toJson(final Object obj) {
    try {
      return jsonConverter.writeValueAsString(obj);
    } catch (final Exception e) {
      throw new InvalidParameterException(
          "Failed to serialize "
              + obj.getClass().getCanonicalName()
              + " to JSON: "
              + e.getMessage());
    }
  }
}
