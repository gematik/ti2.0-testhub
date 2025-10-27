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

/** Configuration for Connector card terminals (TI 2.0). */
public class ConnectorConnectionConfig extends CardTerminalConnectionConfig {

  private final String endpointAddress;
  private final String mandantId;
  private final String clientSystemId;
  private final String workplaceId;
  private final String userId;

  /**
   * Constructs a new Connector terminal configuration.
   *
   * @param name the name of the terminal configuration
   * @param endpointAddress the endpoint address of the Connector
   * @param mandantId the mandant ID
   * @param clientSystemId the client system ID
   * @param workplaceId the workplace ID
   * @param userId the user ID
   */
  @JsonCreator
  public ConnectorConnectionConfig(
      @JsonProperty("name") final String name,
      @JsonProperty("endpointAddress") final String endpointAddress,
      @JsonProperty("mandantId") final String mandantId,
      @JsonProperty("clientSystemId") final String clientSystemId,
      @JsonProperty("workplaceId") final String workplaceId,
      @JsonProperty("userId") final String userId) {
    super(name, CardTerminalType.CONNECTOR);
    this.endpointAddress = endpointAddress;
    this.mandantId = mandantId;
    this.clientSystemId = clientSystemId;
    this.workplaceId = workplaceId;
    this.userId = userId;
  }

  /**
   * Returns the endpoint address of the Connector.
   *
   * @return the endpoint address
   */
  public String getEndpointAddress() {
    return endpointAddress;
  }

  /**
   * Returns the mandant ID.
   *
   * @return the mandant ID
   */
  public String getMandantId() {
    return mandantId;
  }

  /**
   * Returns the client system ID.
   *
   * @return the client system ID
   */
  public String getClientSystemId() {
    return clientSystemId;
  }

  /**
   * Returns the workplace ID.
   *
   * @return the workplace ID
   */
  public String getWorkplaceId() {
    return workplaceId;
  }

  /**
   * Returns the user ID.
   *
   * @return the user ID
   */
  public String getUserId() {
    return userId;
  }
}
