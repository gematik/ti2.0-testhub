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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CardMessage {

  @JsonProperty("data")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private byte[] data = null;

  protected static final ObjectMapper jsonMapper = new ObjectMapper();

  public CardMessage() {}

  public CardMessage(final byte[] data) {
    this.data = data;
  }

  public CardMessage(final String text) {
    this.data = text.getBytes(StandardCharsets.UTF_8);
  }

  public CardMessage(final Object object) throws IOException {
    this.data = jsonMapper.writeValueAsBytes(object);
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  @JsonIgnore
  public String getText() {
    return new String(data, StandardCharsets.UTF_8);
  }

  @JsonIgnore
  public String getJson() throws JsonProcessingException {
    return jsonMapper.writeValueAsString(this);
  }

  @JsonIgnore
  public Map<?, ?> getMapFromJson() throws IOException {
    return jsonMapper.readValue(data, Map.class);
  }

  @JsonIgnore
  public <T> T getObjectFromJson(Class<T> clazz) throws IOException {
    return jsonMapper.readValue(data, clazz);
  }
}
