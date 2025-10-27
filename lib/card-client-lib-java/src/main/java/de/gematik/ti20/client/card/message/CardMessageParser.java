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
package de.gematik.ti20.client.card.message;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.client.card.exception.CardMessageException;
import java.nio.charset.StandardCharsets;

public class CardMessageParser {

  private static final ObjectMapper jsonConverter =
      new ObjectMapper()
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  public static CardMessage parse(byte[] message) {
    // TODO: parse binary messages
    return parse(new String(message, StandardCharsets.UTF_8));
  }

  public static CardMessage parse(String message) {
    if (message == null || message.isBlank()) {
      return new CardMessage("");
    }

    switch (message.charAt(0)) {
      case '{':
      case '[':
        return parseFromJson(message);
    }

    return new CardMessage(message);
  }

  private static CardMessage parseFromJson(String json) {
    // TODO: parse dependent on type

    return new CardMessage(json);
  }

  public static CardCommand parseCommandFromJson(String json) throws CardMessageException {
    try {
      return jsonConverter.readValue(json, CardCommand.class);
    } catch (Exception e) {
      throw new CardMessageException(
          "parse", "Failed to parse CardCommand from JSON", e.getMessage());
    }
  }

  public static CardResult parseResultFromJson(String json) throws CardMessageException {
    try {
      return jsonConverter.readValue(json, CardResult.class);
    } catch (Exception e) {
      throw new CardMessageException(
          "parse", "Failed to parse CardResult from JSON", e.getMessage());
    }
  }
}
