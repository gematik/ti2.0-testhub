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
package de.gematik.ti20.client.card.terminal.connector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Factory for creating Connector client instances. */
public class ConnectorClientFactory {

  private static final Logger log = LoggerFactory.getLogger(ConnectorClientFactory.class);

  /**
   * Creates a new Connector client.
   *
   * @param endpointAddress the endpoint address of the Connector
   * @param mandantId the mandant ID
   * @param clientSystemId the client system ID
   * @param workplaceId the workplace ID
   * @param userId the user ID
   * @return the created Connector client
   * @throws Exception if client creation fails
   */
  public static ConnectorClient createClient(
      String endpointAddress,
      String mandantId,
      String clientSystemId,
      String workplaceId,
      String userId)
      throws Exception {

    log.debug("Creating Connector client for endpoint {}", endpointAddress);

    return new ConnectorClient(
        endpointAddress,
        mandantId,
        clientSystemId,
        workplaceId,
        userId,
        SoapDispatcher.createDispatch(endpointAddress));
  }
}
