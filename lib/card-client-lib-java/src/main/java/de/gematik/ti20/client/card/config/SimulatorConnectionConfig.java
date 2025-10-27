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
import de.gematik.ti20.client.card.terminal.CardTerminalType;

/** Configuration for simulated card terminals. */
public class SimulatorConnectionConfig extends CardTerminalConnectionConfig {

  private final String url;

  /**
   * Constructs a new simulator terminal configuration.
   *
   * @param name the name of the terminal configuration
   * @param url URL
   */
  @JsonCreator
  public SimulatorConnectionConfig(
      @JsonProperty("name") final String name, @JsonProperty("url") final String url) {
    super(name, CardTerminalType.SIMSVC);
    this.url = url;
  }

  public String getUrl() {
    return url;
  }
}
