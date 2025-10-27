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
package de.gematik.ti20.client.card.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.gematik.ti20.client.card.terminal.TerminalSlotV1;

public class TerminalSlotConfig {

  private String name;
  private final TerminalSlotV1.Type type;
  private String slotId;

  @JsonCreator
  public TerminalSlotConfig(
      @JsonProperty("type") TerminalSlotV1.Type slotType, @JsonProperty("slotId") String slotId) {
    this.type = slotType;
    this.slotId = slotId;
  }

  public String getName() {
    if (name == null || name.isBlank()) {
      return "Slot " + slotId;
    }
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TerminalSlotV1.Type getType() {
    return type;
  }

  public String getSlotId() {
    return slotId;
  }

  //  public void setUrl(String url) {
  //    this.url = url;
  //  }
  //
  //  public TerminalSlotConfig url(String url) {
  //    this.setUrl(url);
  //    return this;
  //  }
  //
  //  public String getUrl() {
  //    return this.url;
  //  }

  //  @JsonIgnore
  //  public String getUrlSimulator() {
  //    return getUrl() + "/cardreader/slot/" + getSlotId();
  //  }

}
