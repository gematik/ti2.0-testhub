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

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class for creating SOAP dispatchers. */
public class SoapDispatcher {

  private static final Logger log = LoggerFactory.getLogger(SoapDispatcher.class);

  private static final String NAMESPACE_URI = "http://ws.gematik.de/conn/CardService/v6.0";
  private static final String SERVICE_NAME = "CardService";
  private static final String PORT_NAME = "CardServicePort";

  /**
   * Creates a dispatch object for sending SOAP messages to the specified endpoint.
   *
   * @param endpointAddress the endpoint address
   * @return the created dispatch object
   */
  public static Dispatch<SOAPMessage> createDispatch(String endpointAddress) {
    log.debug("Creating SOAP dispatch for endpoint {}", endpointAddress);

    QName serviceName = new QName(NAMESPACE_URI, SERVICE_NAME);
    QName portName = new QName(NAMESPACE_URI, PORT_NAME);

    Service service = Service.create(serviceName);
    service.addPort(portName, "http://www.w3.org/2003/05/soap/bindings/HTTP/", endpointAddress);

    return service.createDispatch(portName, SOAPMessage.class, Service.Mode.MESSAGE);
  }
}
