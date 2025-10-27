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
package de.gematik.ti20.client.card.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

public class CardMessageException extends Exception {

  @Serial private static final long serialVersionUID = 5774576985610889272L;

  private static final ObjectMapper jsonConverter = new ObjectMapper();

  private final String type;
  private String details = null;

  public CardMessageException(String type, String message) {
    super(message);
    this.type = type;
  }

  public CardMessageException(String type, String message, String details) {
    this(type, message);
    this.details = details;
  }

  public String getType() {
    return type;
  }

  public String getDetails() {
    return details;
  }

  @JsonValue
  public Map<String, String> toJson() throws JsonProcessingException {
    Map<String, String> data = new HashMap<>();
    data.put("type", type);
    data.put("message", getMessage());
    if (details != null) {
      data.put("details", details);
    }
    return data;
  }

  @JsonCreator
  public static CardMessageException fromJson(
      @JsonProperty("type") String type,
      @JsonProperty("message") String message,
      @JsonProperty("details") String details) {
    return new CardMessageException(type, message, details);
  }
}
