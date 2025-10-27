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

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.xml.soap.*;
import javax.xml.ws.Dispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client for communicating with a Connector using TI 2.0 SOAP protocols. */
public class ConnectorClient {

  private static final Logger log = LoggerFactory.getLogger(ConnectorClient.class);
  private static final String NAMESPACE_CTK = "http://ws.gematik.de/conn/CTK";
  private static final String NAMESPACE_CARDSERVICE = "http://ws.gematik.de/conn/CardService";
  private static final String NAMESPACE_CARDTERMINAL =
      "http://ws.gematik.de/conn/CardTerminalService";
  private static final String NAMESPACE_EVENTSERVICE = "http://ws.gematik.de/conn/EventService";

  private final String endpointAddress;
  private final Dispatch<SOAPMessage> dispatch;
  private final String contextValueMandant;
  private final String contextValueClientSystem;
  private final String contextValueWorkplace;
  private final String contextValueUser;

  /**
   * Creates a new Connector client.
   *
   * @param endpointAddress the endpoint address of the Connector
   * @param mandantId the mandant ID
   * @param clientSystemId the client system ID
   * @param workplaceId the workplace ID
   * @param userId the user ID
   * @param dispatch the SOAP dispatch
   */
  public ConnectorClient(
      String endpointAddress,
      String mandantId,
      String clientSystemId,
      String workplaceId,
      String userId,
      Dispatch<SOAPMessage> dispatch) {

    this.endpointAddress = endpointAddress;
    this.dispatch = dispatch;
    this.contextValueMandant = mandantId;
    this.contextValueClientSystem = clientSystemId;
    this.contextValueWorkplace = workplaceId;
    this.contextValueUser = userId;
  }

  /**
   * Returns the available card terminals.
   *
   * @return the card terminals
   * @throws Exception if an error occurs
   */
  public String[] getCardTerminals() throws Exception {
    SOAPMessage request = createSOAPMessage();
    SOAPBody body = request.getSOAPBody();

    SOAPElement getCardTerminals =
        body.addChildElement("GetCardTerminals", "", NAMESPACE_CARDTERMINAL);

    SOAPElement ctxID = createContextElement(request, body);
    body.addChildElement(ctxID);

    // Output request for debugging
    if (log.isDebugEnabled()) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      request.writeTo(out);
      log.debug("SOAP Request: {}", out.toString());
    }

    SOAPMessage response = dispatch.invoke(request);

    // Output response for debugging
    if (log.isDebugEnabled()) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      response.writeTo(out);
      log.debug("SOAP Response: {}", out.toString());
    }

    SOAPBody responseBody = response.getSOAPBody();
    SOAPElement responseElement = (SOAPElement) responseBody.getChildElements().next();

    // Extract card terminal IDs
    String[] cardTerminals = {};
    if (responseElement.getChildElements().hasNext()) {
      SOAPElement terminalIdsElement = (SOAPElement) responseElement.getChildElements().next();
      if (terminalIdsElement.getChildElements().hasNext()) {
        // Build array from terminal IDs
        java.util.List<String> terminalIds = new java.util.ArrayList<>();
        java.util.Iterator<?> it = terminalIdsElement.getChildElements();
        while (it.hasNext()) {
          SOAPElement element = (SOAPElement) it.next();
          terminalIds.add(element.getValue());
        }
        cardTerminals = terminalIds.toArray(new String[0]);
      }
    }

    return cardTerminals;
  }

  /**
   * Returns the available cards.
   *
   * @return the card handles
   * @throws Exception if an error occurs
   */
  public String[] getCards() throws Exception {
    SOAPMessage request = createSOAPMessage();
    SOAPBody body = request.getSOAPBody();

    SOAPElement getCards = body.addChildElement("GetCards", "", NAMESPACE_CARDSERVICE);

    SOAPElement ctxID = createContextElement(request, body);
    body.addChildElement(ctxID);

    SOAPMessage response = dispatch.invoke(request);

    SOAPBody responseBody = response.getSOAPBody();
    SOAPElement responseElement = (SOAPElement) responseBody.getChildElements().next();

    // Extract card handles
    String[] cardHandles = {};
    if (responseElement.getChildElements().hasNext()) {
      SOAPElement cardHandlesElement = (SOAPElement) responseElement.getChildElements().next();
      if (cardHandlesElement.getChildElements().hasNext()) {
        // Build array from card handles
        java.util.List<String> handles = new java.util.ArrayList<>();
        java.util.Iterator<?> it = cardHandlesElement.getChildElements();
        while (it.hasNext()) {
          SOAPElement element = (SOAPElement) it.next();
          handles.add(element.getValue());
        }
        cardHandles = handles.toArray(new String[0]);
      }
    }

    return cardHandles;
  }

  /**
   * Returns the card type.
   *
   * @param cardHandle the card handle
   * @return the card type
   * @throws Exception if an error occurs
   */
  public String getCardType(String cardHandle) throws Exception {
    SOAPMessage request = createSOAPMessage();
    SOAPBody body = request.getSOAPBody();

    SOAPElement getCardDetails = body.addChildElement("GetCardDetails", "", NAMESPACE_CARDSERVICE);

    SOAPElement ctxID = createContextElement(request, body);
    body.addChildElement(ctxID);

    SOAPElement cardHandle_element = body.addChildElement("CardHandle", "", NAMESPACE_CARDSERVICE);
    cardHandle_element.addTextNode(cardHandle);
    body.addChildElement(cardHandle_element);

    SOAPMessage response = dispatch.invoke(request);

    SOAPBody responseBody = response.getSOAPBody();
    SOAPElement responseElement = (SOAPElement) responseBody.getChildElements().next();

    // Extract card type
    String cardType = "UNKNOWN";
    if (responseElement.getChildElements().hasNext()) {
      SOAPElement detailsElement = (SOAPElement) responseElement.getChildElements().next();
      java.util.Iterator<?> it = detailsElement.getChildElements();
      while (it.hasNext()) {
        SOAPElement element = (SOAPElement) it.next();
        if (element.getLocalName().equals("CardType")) {
          cardType = element.getValue();
          break;
        }
      }
    }

    return cardType;
  }

  /**
   * Establishes a connection to a card.
   *
   * @param cardHandle the card handle
   * @return the connection handle
   * @throws Exception if an error occurs
   */
  public String connect(String cardHandle) throws Exception {
    SOAPMessage request = createSOAPMessage();
    SOAPBody body = request.getSOAPBody();

    SOAPElement connectCard = body.addChildElement("ConnectCard", "", NAMESPACE_CARDSERVICE);

    SOAPElement ctxID = createContextElement(request, body);
    body.addChildElement(ctxID);

    SOAPElement cardHandle_element = body.addChildElement("CardHandle", "", NAMESPACE_CARDSERVICE);
    cardHandle_element.addTextNode(cardHandle);
    body.addChildElement(cardHandle_element);

    SOAPMessage response = dispatch.invoke(request);

    SOAPBody responseBody = response.getSOAPBody();
    SOAPElement responseElement = (SOAPElement) responseBody.getChildElements().next();

    // Extract connection handle
    String connectionHandle = null;
    if (responseElement.getChildElements().hasNext()) {
      SOAPElement connectionHandleElement = (SOAPElement) responseElement.getChildElements().next();
      connectionHandle = connectionHandleElement.getValue();
    }

    if (connectionHandle == null) {
      throw new Exception("Failed to establish card connection");
    }

    return connectionHandle;
  }

  /**
   * Transmits an APDU to a card.
   *
   * @param connectionHandle the connection handle
   * @param apdu the APDU
   * @return the response APDU
   * @throws Exception if an error occurs
   */
  public byte[] transmit(String connectionHandle, byte[] apdu) throws Exception {
    SOAPMessage request = createSOAPMessage();
    SOAPBody body = request.getSOAPBody();

    SOAPElement transmitCard = body.addChildElement("TransmitCard", "", NAMESPACE_CARDSERVICE);

    SOAPElement ctxID = createContextElement(request, body);
    body.addChildElement(ctxID);

    SOAPElement connectionHandle_element =
        body.addChildElement("ConnectionHandle", "", NAMESPACE_CARDSERVICE);
    connectionHandle_element.addTextNode(connectionHandle);
    body.addChildElement(connectionHandle_element);

    SOAPElement command = body.addChildElement("Command", "", NAMESPACE_CARDSERVICE);
    command.addTextNode(javax.xml.bind.DatatypeConverter.printHexBinary(apdu));
    body.addChildElement(command);

    SOAPMessage response = dispatch.invoke(request);

    SOAPBody responseBody = response.getSOAPBody();
    SOAPElement responseElement = (SOAPElement) responseBody.getChildElements().next();

    // Extract response APDU
    byte[] responseApdu = new byte[0];
    if (responseElement.getChildElements().hasNext()) {
      SOAPElement responseApduElement = (SOAPElement) responseElement.getChildElements().next();
      String responseApduHex = responseApduElement.getValue();
      responseApdu = javax.xml.bind.DatatypeConverter.parseHexBinary(responseApduHex);
    }

    return responseApdu;
  }

  /**
   * Disconnects a card connection.
   *
   * @param connectionHandle the connection handle
   * @throws Exception if an error occurs
   */
  public void disconnect(String connectionHandle) throws Exception {
    SOAPMessage request = createSOAPMessage();
    SOAPBody body = request.getSOAPBody();

    SOAPElement disconnectCard = body.addChildElement("DisconnectCard", "", NAMESPACE_CARDSERVICE);

    SOAPElement ctxID = createContextElement(request, body);
    body.addChildElement(ctxID);

    SOAPElement connectionHandle_element =
        body.addChildElement("ConnectionHandle", "", NAMESPACE_CARDSERVICE);
    connectionHandle_element.addTextNode(connectionHandle);
    body.addChildElement(connectionHandle_element);

    dispatch.invoke(request);
  }

  /** Closes the client. */
  public void close() {
    // Nothing to do
  }

  /**
   * Creates a new SOAP message.
   *
   * @return the SOAP message
   * @throws Exception if an error occurs
   */
  private SOAPMessage createSOAPMessage() throws Exception {
    MessageFactory factory = MessageFactory.newInstance();
    SOAPMessage message = factory.createMessage();

    // Add necessary headers
    SOAPHeader header = message.getSOAPHeader();
    SOAPElement action =
        header.addChildElement("Action", "wsa", "http://www.w3.org/2005/08/addressing");
    action.addTextNode("http://ws.gematik.de/conn/ServiceDirectory/v3.1#GetResourceInformation");

    SOAPElement messageID =
        header.addChildElement("MessageID", "wsa", "http://www.w3.org/2005/08/addressing");
    messageID.addTextNode("urn:uuid:" + UUID.randomUUID().toString());

    SOAPElement to = header.addChildElement("To", "wsa", "http://www.w3.org/2005/08/addressing");
    to.addTextNode(endpointAddress);

    return message;
  }

  /**
   * Creates a context element for SOAP requests.
   *
   * @param request the SOAP message
   * @param body the SOAP body
   * @return the context element
   * @throws Exception if an error occurs
   */
  private SOAPElement createContextElement(SOAPMessage request, SOAPBody body) throws Exception {
    // Create Context element with all required IDs
    SOAPElement context = body.addChildElement("Context", "", NAMESPACE_CTK);

    if (contextValueMandant != null && !contextValueMandant.isEmpty()) {
      SOAPElement mandantID = context.addChildElement("MandantID", "", NAMESPACE_CTK);
      mandantID.addTextNode(contextValueMandant);
      context.addChildElement(mandantID);
    }

    if (contextValueClientSystem != null && !contextValueClientSystem.isEmpty()) {
      SOAPElement clientSystemID = context.addChildElement("ClientSystemID", "", NAMESPACE_CTK);
      clientSystemID.addTextNode(contextValueClientSystem);
      context.addChildElement(clientSystemID);
    }

    if (contextValueWorkplace != null && !contextValueWorkplace.isEmpty()) {
      SOAPElement workplaceID = context.addChildElement("WorkplaceID", "", NAMESPACE_CTK);
      workplaceID.addTextNode(contextValueWorkplace);
      context.addChildElement(workplaceID);
    }

    if (contextValueUser != null && !contextValueUser.isEmpty()) {
      SOAPElement userID = context.addChildElement("UserID", "", NAMESPACE_CTK);
      userID.addTextNode(contextValueUser);
      context.addChildElement(userID);
    }

    return context;
  }
}
