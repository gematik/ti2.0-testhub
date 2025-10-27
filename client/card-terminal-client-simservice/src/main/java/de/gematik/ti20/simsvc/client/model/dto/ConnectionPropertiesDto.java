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
package de.gematik.ti20.simsvc.client.model.dto;

/**
 * Data Transfer Object (DTO) for connection properties. Contains information about a connection to
 * a card.
 */
public class ConnectionPropertiesDto {

  private String cardHandle;
  private String atr;
  private String protocol;
  private boolean exclusive;

  /** Default constructor. */
  public ConnectionPropertiesDto() {}

  /**
   * Constructor with all fields.
   *
   * @param cardHandle Card handle for the connection
   * @param atr ATR (Answer to Reset) of the card
   * @param protocol Protocol used for the connection
   * @param exclusive Whether the connection is exclusive
   */
  public ConnectionPropertiesDto(
      String cardHandle, String atr, String protocol, boolean exclusive) {
    this.cardHandle = cardHandle;
    this.atr = atr;
    this.protocol = protocol;
    this.exclusive = exclusive;
  }

  /**
   * Get the card handle.
   *
   * @return Card handle
   */
  public String getCardHandle() {
    return cardHandle;
  }

  /**
   * Set the card handle.
   *
   * @param cardHandle Card handle
   */
  public void setCardHandle(String cardHandle) {
    this.cardHandle = cardHandle;
  }

  /**
   * Get the ATR (Answer to Reset).
   *
   * @return ATR as a hex string
   */
  public String getAtr() {
    return atr;
  }

  /**
   * Set the ATR (Answer to Reset).
   *
   * @param atr ATR as a hex string
   */
  public void setAtr(String atr) {
    this.atr = atr;
  }

  /**
   * Get the protocol.
   *
   * @return Protocol name
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * Set the protocol.
   *
   * @param protocol Protocol name
   */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /**
   * Check if the connection is exclusive.
   *
   * @return true if exclusive, false otherwise
   */
  public boolean isExclusive() {
    return exclusive;
  }

  /**
   * Set whether the connection is exclusive.
   *
   * @param exclusive Exclusive flag
   */
  public void setExclusive(boolean exclusive) {
    this.exclusive = exclusive;
  }
}
