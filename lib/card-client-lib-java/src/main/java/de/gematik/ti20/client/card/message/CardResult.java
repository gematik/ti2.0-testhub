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
import de.gematik.ti20.client.card.exception.CardMessageException;

public class CardResult extends CardMessage {

  public enum Type {
    ERROR,
    RESULT,
    EVENT,
    CARD_DATA;

    @JsonCreator
    public static Type fromValue(String value) {
      if (value == null) {
        throw new IllegalArgumentException("Type cannot be null");
      }
      return switch (value.toLowerCase()) {
        case "error" -> ERROR;
        case "result" -> RESULT;
        case "card_data" -> CARD_DATA;
        case "event" -> EVENT;
        default -> throw new IllegalArgumentException("Invalid type value: " + value);
      };
    }

    @JsonValue
    public String toValue() {
      return name().toLowerCase();
    }
  }

  public enum Event {
    CARD_INSERTED,
    CARD_REMOVED;

    @JsonValue
    public String toValue() {
      return switch (this) {
        case CARD_INSERTED -> "cardInserted";
        case CARD_REMOVED -> "cardRemoved";
      };
    }

    @JsonCreator
    public static Event fromValue(String value) {
      return switch (value) {
        case "cardInserted" -> CARD_INSERTED;
        case "cardRemoved" -> CARD_REMOVED;
        default -> throw new IllegalArgumentException("Invalid event value: " + value);
      };
    }
  }

  private Type type;
  private Object result;
  private Event event;
  private CardMessageException error;

  public CardResult(Type type) {
    super();
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  public CardResult result(Object result) {
    this.result = result;
    return this;
  }

  @JsonInclude(Include.NON_EMPTY)
  public Object getResult() {
    return result;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public CardResult event(Event event) {
    this.event = event;
    return this;
  }

  @JsonInclude(Include.NON_EMPTY)
  public Event getEvent() {
    return event;
  }

  public void setError(CardMessageException error) {
    this.error = error;
  }

  public CardResult error(CardMessageException error) {
    this.error = error;
    return this;
  }

  @JsonInclude(Include.NON_EMPTY)
  public CardMessageException getError() {
    return error;
  }

  public boolean isError() {
    return type == Type.ERROR;
  }

  public static CardResult newResult(Object result) {
    return new CardResult(Type.RESULT).result(result);
  }

  public static CardResult newEvent(Event event) {
    return new CardResult(Type.EVENT).event(event);
  }

  public static CardResult newError(CardMessageException error) {
    return new CardResult(Type.ERROR).error(error);
  }

  public static CardResult newError(String type, String message) {
    return newError(new CardMessageException(type, message));
  }

  public static CardResult newError(String type, String message, String details) {
    return newError(new CardMessageException(type, message, details));
  }

  @JsonCreator
  public static CardResult create(
      @JsonProperty("type") Type type,
      @JsonProperty("error") CardMessageException error,
      @JsonProperty("result") Object result,
      @JsonProperty("event") Event event) {
    CardResult cardResult = new CardResult(type);
    cardResult.setError(error);
    cardResult.setResult(result);
    cardResult.setEvent(event);
    return cardResult;
  }
}
