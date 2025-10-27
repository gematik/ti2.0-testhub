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
package de.gematik.ti20.client.card.terminal.connector.signature;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.connector.ConnectorAttachedCard;
import de.gematik.ti20.client.card.terminal.connector.ConnectorClient;
import java.util.Base64;
import javax.xml.soap.*;
import javax.xml.ws.Dispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SignatureService for the Connector. This service communicates with the
 * Connector's AuthSignatureService using SOAP and specifically the ExternalAuthenticate operation
 * from AuthSignatureService for signing. According to gematik specifications, ExternalAuthenticate
 * is preferred for signing operations.
 */
public class ConnectorSignatureService implements SignatureService {

  private static final Logger log = LoggerFactory.getLogger(ConnectorSignatureService.class);

  private static final String NAMESPACE_AUTHSIGNATURESERVICE =
      "http://ws.gematik.de/conn/AuthSignatureService/v7.5";
  private static final String CONTEXT_PREFIX = "http://ws.gematik.de/conn/v7.0/ConnectorContext";

  private final ConnectorClient client;
  private final Dispatch<SOAPMessage> dispatch;
  private final String mandantId;
  private final String clientSystemId;
  private final String workplaceId;
  private final String userId;

  /**
   * Constructs a new ConnectorSignatureService.
   *
   * @param client the Connector client
   * @param dispatch the SOAP dispatch
   * @param mandantId the mandant ID
   * @param clientSystemId the client system ID
   * @param workplaceId the workplace ID
   * @param userId the user ID
   */
  public ConnectorSignatureService(
      ConnectorClient client,
      Dispatch<SOAPMessage> dispatch,
      String mandantId,
      String clientSystemId,
      String workplaceId,
      String userId) {

    this.client = client;
    this.dispatch = dispatch;
    this.mandantId = mandantId;
    this.clientSystemId = clientSystemId;
    this.workplaceId = workplaceId;
    this.userId = userId;
  }

  /** {@inheritDoc} */
  @Override
  public byte[] sign(AttachedCard card, byte[] data, SignOptions options)
      throws CardTerminalException {

    if (!(card instanceof ConnectorAttachedCard)) {
      throw new CardTerminalException("Card is not a Connector card");
    }

    ConnectorAttachedCard connectorCard = (ConnectorAttachedCard) card;
    String cardHandle = connectorCard.getCardHandle();

    try {
      log.debug(
          "Signing data with card: {} using ExternalAuthenticate operation with options: algorithm={}, type={}, key={}",
          card.getId(),
          options.getHashAlgorithm(),
          options.getSignatureType(),
          options.getKeyReference());

      // Convert SignOptions.HashAlgorithm to Connector HashAlgorithm
      HashAlgorithm connectorHashAlgorithm =
          convertToConnectorHashAlgorithm(options.getHashAlgorithm());

      // Create SOAP message for ExternalAuthenticate operation
      SOAPMessage requestMessage =
          createExternalAuthenticateRequest(cardHandle, data, connectorHashAlgorithm);

      // Send to Connector
      SOAPMessage responseMessage = dispatch.invoke(requestMessage);

      // Process response
      return processExternalAuthenticateResponse(responseMessage);

    } catch (Exception e) {
      throw new CardTerminalException(
          "Error during signature creation with ExternalAuthenticate", e);
    }
  }

  /**
   * Converts a SignOptions.HashAlgorithm to the equivalent Connector-specific HashAlgorithm.
   *
   * @param algorithm the generic algorithm to convert
   * @return the equivalent Connector-specific algorithm
   */
  private HashAlgorithm convertToConnectorHashAlgorithm(SignOptions.HashAlgorithm algorithm) {
    if (algorithm == null) {
      return HashAlgorithm.SHA256; // Default
    }

    switch (algorithm) {
      case SHA384:
        return HashAlgorithm.SHA384;
      case SHA512:
        return HashAlgorithm.SHA512;
      case SHA256:
      default:
        return HashAlgorithm.SHA256;
    }
  }

  /**
   * Converts a Connector HashAlgorithm to the equivalent SignOptions.HashAlgorithm.
   *
   * @param algorithm the Connector-specific algorithm to convert
   * @return the equivalent generic algorithm
   */
  private SignOptions.HashAlgorithm convertToSignOptionsHashAlgorithm(HashAlgorithm algorithm) {
    if (algorithm == null) {
      return SignOptions.HashAlgorithm.SHA256; // Default
    }

    switch (algorithm) {
      case SHA384:
        return SignOptions.HashAlgorithm.SHA384;
      case SHA512:
        return SignOptions.HashAlgorithm.SHA512;
      case SHA256:
      default:
        return SignOptions.HashAlgorithm.SHA256;
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getSignatureAlgorithm(HashAlgorithm hashAlgorithm) {
    return hashAlgorithm.getSignatureAlgorithm();
  }

  /**
   * Creates a SOAP message for the ExternalAuthenticate operation from AuthSignatureService.
   *
   * @param cardHandle the card handle
   * @param data the data to sign
   * @param hashAlgorithm the hash algorithm
   * @return the created SOAP message
   * @throws SOAPException if message creation fails
   * @throws Exception if any other error occurs
   */
  private SOAPMessage createExternalAuthenticateRequest(
      String cardHandle, byte[] data, HashAlgorithm hashAlgorithm) throws Exception {

    // Create SOAP message
    MessageFactory messageFactory = MessageFactory.newInstance();
    SOAPMessage soapMessage = messageFactory.createMessage();
    SOAPPart soapPart = soapMessage.getSOAPPart();
    SOAPEnvelope envelope = soapPart.getEnvelope();
    SOAPBody body = envelope.getBody();

    // Set namespaces
    envelope.addNamespaceDeclaration("ns", NAMESPACE_AUTHSIGNATURESERVICE);

    // Create ExternalAuthenticate operation element from AuthSignatureService
    SOAPElement externalAuthOperation = body.addChildElement("ExternalAuthenticate", "ns");

    // Add context
    addContext(externalAuthOperation);

    // Add CardHandle
    SOAPElement cardHandleElem = externalAuthOperation.addChildElement("CardHandle", "ns");
    cardHandleElem.addTextNode(cardHandle);

    // Add Binary Data to sign
    SOAPElement dataElem = externalAuthOperation.addChildElement("SignatureObject", "ns");
    String base64Data = Base64.getEncoder().encodeToString(data);
    dataElem.addTextNode(base64Data);

    soapMessage.saveChanges();

    return soapMessage;
  }

  /**
   * Processes the SOAP response from the ExternalAuthenticate operation.
   *
   * @param soapResponse the SOAP response
   * @return the signature bytes
   * @throws Exception if processing fails
   */
  private byte[] processExternalAuthenticateResponse(SOAPMessage soapResponse) throws Exception {
    SOAPBody body = soapResponse.getSOAPBody();

    // Check for fault
    if (body.hasFault()) {
      SOAPFault fault = body.getFault();
      String faultString = fault.getFaultString();
      throw new Exception("SOAP Fault: " + faultString);
    }

    // Get the ExternalAuthenticateResponse element
    SOAPElement responseElem = (SOAPElement) body.getChildElements().next();

    // Get the ExternalAuthenticateReturn element containing the signature
    SOAPElement returnElem = (SOAPElement) responseElem.getChildElements().next();

    // Get the signature value in Base64 format
    String base64Signature = returnElem.getValue();

    log.debug("Received signature from AuthSignatureService.ExternalAuthenticate");

    // Decode and return
    return Base64.getDecoder().decode(base64Signature);
  }

  /**
   * Adds the context information to the SOAP element.
   *
   * @param parent the parent SOAP element
   * @throws SOAPException if adding the context fails
   */
  private void addContext(SOAPElement parent) throws SOAPException {
    SOAPElement contextElem = parent.addChildElement("Context", "ns");

    SOAPElement mandantElem = contextElem.addChildElement("MandantId", "ns");
    mandantElem.addTextNode(mandantId);

    SOAPElement clientSystemElem = contextElem.addChildElement("ClientSystemId", "ns");
    clientSystemElem.addTextNode(clientSystemId);

    SOAPElement workplaceElem = contextElem.addChildElement("WorkplaceId", "ns");
    workplaceElem.addTextNode(workplaceId);

    SOAPElement userElem = contextElem.addChildElement("UserId", "ns");
    userElem.addTextNode(userId);
  }
}
