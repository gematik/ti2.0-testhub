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
package de.gematik.ti20.client.card.terminal.simsvc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Class representing card connection information from the CardSimulator API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardConnectionInfo {

  @JsonProperty("cardHandle")
  private String cardHandle;

  @JsonProperty("atr")
  private String ATR;

  @JsonProperty("protocol")
  private String protocol;

  @JsonProperty("exclusive")
  private boolean exclusive;

  /** Default constructor for JSON deserialization. */
  public CardConnectionInfo() {}

  /**
   * Constructs a new CardConnectionInfo.
   *
   * @param handle the card handle
   * @param atr Answer to Reset of the card
   * @param protocol Protocol
   * @param exclusive Is the connection exclusive?
   */
  @JsonCreator
  public CardConnectionInfo(
      @JsonProperty("cardHandle") String handle,
      @JsonProperty("atr") String atr,
      @JsonProperty("protocol") String protocol,
      @JsonProperty("exclusive") boolean exclusive) {
    this.cardHandle = handle;
    this.ATR = atr;
    this.protocol = protocol;
    this.exclusive = exclusive;
  }

  public String getCardHandle() {
    return cardHandle;
  }

  public String getAtr() {
    return ATR;
  }

  public String getProtocol() {
    return protocol;
  }

  public boolean isExclusive() {
    return exclusive;
  }
}
