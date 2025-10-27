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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;

public class CardCommand extends CardMessage {

  // TODO: this must be adjusted to the implementation of the real card commands
  public enum Type {
    GET_CARD_DATA,
    SIGN;

    @JsonValue
    public String toValue() {
      return switch (this) {
        case GET_CARD_DATA -> "getCardData";
        case SIGN -> "sign";
      };
    }

    @JsonCreator
    public static Type fromValue(String value) {
      return switch (value) {
        case "getCardData" -> GET_CARD_DATA;
        case "sign" -> SIGN;
        default -> throw new IllegalArgumentException("Invalid value: " + value);
      };
    }
  }

  private Type type;
  private Map<String, String> args = new HashMap<>();

  public CardCommand(Type type) {
    super();
    this.type = type;
  }

  @JsonCreator
  public CardCommand(
      @JsonProperty("type") Type type, @JsonProperty("args") Map<String, String> args) {
    super();
    this.type = type;
    if (args != null) {
      this.args = args;
    }
  }

  public CardCommand addArg(String key, String value) {
    args.put(key, value);
    return this;
  }

  public CardCommand addArg(String key, Object value) {
    if (value != null) {
      args.put(key, value.toString());
    }
    return this;
  }

  public String getArg(String key) {
    return args.get(key);
  }

  public Type getType() {
    return type;
  }

  @JsonInclude(Include.NON_EMPTY)
  public Map<String, String> getArgs() {
    return args;
  }
}
