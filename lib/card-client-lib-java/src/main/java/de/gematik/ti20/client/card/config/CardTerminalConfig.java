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
import de.gematik.ti20.client.card.terminal.CardTerminalV1;
import de.gematik.ti20.client.card.terminal.CardTerminalV1.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardTerminalConfig {

  private final String vendorId;
  private final String productId;
  private final String productName;
  private final String serialNumber;
  private final CardTerminalV1.Type type;
  private final Map<String, String> connection = new HashMap<>();
  private List<TerminalSlotConfig> slots = new ArrayList<>();

  @JsonCreator
  public CardTerminalConfig(
      @JsonProperty("vendorId") String vendorId,
      @JsonProperty("productId") String productId,
      @JsonProperty("productName") String productName,
      @JsonProperty("serialNumber") String serialNumber,
      @JsonProperty("type") CardTerminalV1.Type type) {

    this.vendorId = vendorId;
    this.productId = productId;
    this.serialNumber = serialNumber;
    this.productName = productName;
    this.type = type;
  }

  public String getVendorId() {
    return vendorId;
  }

  public String getProductId() {
    return productId;
  }

  public String getSerialNumber() {
    return serialNumber;
  }

  public String getProductName() {
    return productName;
  }

  public Type getType() {
    return type;
  }

  public List<TerminalSlotConfig> getSlots() {
    return slots;
  }

  public void setConnection(Map<String, String> connection) {
    this.connection.clear();
    this.connection.putAll(connection);
  }

  public Map<String, String> getConnection() {
    return connection;
  }

  public void setSlots(List<TerminalSlotConfig> slots) {
    this.slots = slots;
  }

  public void addSlot(TerminalSlotConfig slot) {
    slots.add(slot);
  }
}
