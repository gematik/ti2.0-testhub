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

/** Configuration for USB card terminals. */
public class PcScConnectionConfig extends CardTerminalConnectionConfig {

  /** The name of the card reader. */
  private final String readerName;

  /**
   * Constructs a new USB terminal configuration.
   *
   * @param name the name of the terminal configuration
   * @param readerName the name of the USB card reader
   */
  @JsonCreator
  public PcScConnectionConfig(
      @JsonProperty("name") final String name, @JsonProperty("readerName") String readerName) {
    super(name, CardTerminalType.PCSC);
    this.readerName = readerName;
  }

  /**
   * Returns the name of the USB card reader.
   *
   * @return the reader name
   */
  public String getReaderName() {
    return readerName;
  }
}
