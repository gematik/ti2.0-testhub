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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorClientTest {

  @Mock private Dispatch<SOAPMessage> dispatcher;
  private ConnectorClient connectorClient;

  @BeforeEach
  public void setup() {
    this.connectorClient =
        new ConnectorClient("endpoint", "mandant", "clientSystem", "workplace", "user", dispatcher);
  }

  @Test
  void thatCardsCanBeRetrieved() throws Exception {
    final SOAPElement cardHandle = mock(SOAPElement.class);
    when(cardHandle.getValue()).thenReturn("card-handle");

    final SOAPElement cardHandleElement = mockSoapElement(iteratorOf(cardHandle));

    final SOAPElement cardContainer = mockSoapElement(iteratorOf(cardHandleElement));

    final SOAPBody mockBody = mock(SOAPBody.class);
    final Iterator<Node> iterator = iteratorOf(cardContainer);
    when(mockBody.getChildElements()).thenReturn(iterator);
    final SOAPMessage mockResponse = mock(SOAPMessage.class);
    when(mockResponse.getSOAPBody()).thenReturn(mockBody);
    when(dispatcher.invoke(any())).thenReturn(mockResponse);

    final ConnectorClient connectorClient =
        new ConnectorClient("endpoint", "mandant", "clientSystem", "workplace", "user", dispatcher);
    final String[] cards = connectorClient.getCards();
    assertThat(cards).containsOnly("card-handle");
  }

  @Test
  void thatMissingCardsDoNotRaiseException() throws Exception {
    final SOAPElement cardHandleElement = mockSoapElement(Collections.emptyIterator());

    final SOAPElement cardContainer = mockSoapElement(iteratorOf(cardHandleElement));

    final SOAPBody mockBody = mock(SOAPBody.class);
    final Iterator<Node> iterator = iteratorOf(cardContainer);
    when(mockBody.getChildElements()).thenReturn(iterator);
    final SOAPMessage mockResponse = mock(SOAPMessage.class);
    when(mockResponse.getSOAPBody()).thenReturn(mockBody);
    when(dispatcher.invoke(any())).thenReturn(mockResponse);

    final ConnectorClient connectorClient =
        new ConnectorClient("endpoint", "mandant", "clientSystem", "workplace", "user", dispatcher);
    final String[] cards = connectorClient.getCards();
    assertThat(cards).isEmpty();
  }

  @Test
  void thatGetCardTypeReturnsUnknownForMissingCardType() throws Exception {
    final SOAPElement cardContainer = mockSoapElement(Collections.emptyIterator());
    final SOAPBody mockBody = mock(SOAPBody.class);
    when(mockBody.getChildElements()).thenReturn(iteratorOf(cardContainer));
    final SOAPMessage mockResponse = mock(SOAPMessage.class);
    when(mockResponse.getSOAPBody()).thenReturn(mockBody);
    when(dispatcher.invoke(any())).thenReturn(mockResponse);

    final ConnectorClient connectorClient =
        new ConnectorClient("endpoint", "mandant", "clientSystem", "workplace", "user", dispatcher);
    final String cards = connectorClient.getCardType("card-handle");
    assertThat(cards).isEqualTo("UNKNOWN");
  }

  @Test
  void thatGetCardTypeReturnsTypeFromResponse() throws Exception {
    final SOAPElement element = mock(SOAPElement.class);
    when(element.getLocalName()).thenReturn("CardType");
    when(element.getValue()).thenReturn("the-card-type");

    final SOAPElement detailsElement = mockSoapElement(iteratorOf(element));

    final SOAPElement responseElement = mockSoapElement(iteratorOf(detailsElement));

    final SOAPBody mockBody = mock(SOAPBody.class);
    when(mockBody.getChildElements()).thenReturn(iteratorOf(responseElement));
    final SOAPMessage mockResponse = mock(SOAPMessage.class);
    when(mockResponse.getSOAPBody()).thenReturn(mockBody);
    when(dispatcher.invoke(any())).thenReturn(mockResponse);

    final ConnectorClient connectorClient =
        new ConnectorClient("endpoint", "mandant", "clientSystem", "workplace", "user", dispatcher);
    final String cards = connectorClient.getCardType("card-handle");
    assertThat(cards).isEqualTo("the-card-type");
  }

  @Test
  void thatConnectWorks() throws Exception {
    // verify the connection request

    final SOAPElement connectionHandleElement = mock(SOAPElement.class);
    when(connectionHandleElement.getValue()).thenReturn("the-handle");
    final SOAPElement responseElement = mockSoapElement(iteratorOf(connectionHandleElement));
    final SOAPBody mockBody = mock(SOAPBody.class);
    when(mockBody.getChildElements()).thenReturn(iteratorOf(responseElement));
    // return the handle
    final SOAPMessage mockResponse = mock(SOAPMessage.class);
    when(mockResponse.getSOAPBody()).thenReturn(mockBody);
    when(dispatcher.invoke(any())).thenReturn(mockResponse);

    final String connect = connectorClient.connect("the-card-handle");
    assertThat(connect).isEqualTo("the-handle");
  }

  @Test
  void thatDisconnectWorks() throws Exception {
    connectorClient.disconnect("the-card-handle");

    ArgumentCaptor<SOAPMessage> requestCaptor = ArgumentCaptor.forClass(SOAPMessage.class);
    verify(dispatcher, times(1)).invoke(requestCaptor.capture());

    final SOAPMessage request = requestCaptor.getValue();
    final Iterator<Node> commandElements =
        request
            .getSOAPBody()
            .getChildElements(
                QName.valueOf("{http://ws.gematik.de/conn/CardService}DisconnectCard"));
    assertThat(commandElements).hasNext();
    final Iterator<Node> connectionHandles =
        request
            .getSOAPBody()
            .getChildElements(
                QName.valueOf("{http://ws.gematik.de/conn/CardService}ConnectionHandle"));
    final Node connectionHandle = connectionHandles.next();
    assertThat(connectionHandle.getValue()).isEqualTo("the-card-handle");
    final Iterator<Node> contextElements =
        request
            .getSOAPBody()
            .getChildElements(QName.valueOf("{http://ws.gematik.de/conn/CTK}Context"));
    assertThat(contextElements).hasNext();
  }

  @Test
  void thatTransmittingWorks() throws Exception {
    final byte[] expectedResponse = new byte[] {0, 1, 0};
    final byte[] requestData = new byte[] {1, 0, 0};

    // GIVEN a SOAP message to the connector
    final SOAPElement payloadElement = mock(SOAPElement.class);
    when(payloadElement.getValue()).thenReturn(DatatypeConverter.printHexBinary(expectedResponse));
    final SOAPElement responseElement = mockSoapElement(iteratorOf(payloadElement));

    final SOAPBody mockBody = mock(SOAPBody.class);
    when(mockBody.getChildElements()).thenReturn(iteratorOf(responseElement));

    final SOAPMessage mock = mock(SOAPMessage.class);
    when(mock.getSOAPBody()).thenReturn(mockBody);
    when(dispatcher.invoke(any())).thenReturn(mock);

    // WHEN the request is send
    final byte[] actualResponse = connectorClient.transmit("the-card-handle", requestData);

    // THEN the response was parsed as expected
    assertThat(actualResponse).isEqualTo(expectedResponse);

    // AND the SOAP message constructed by the client matches our expectation
    ArgumentCaptor<SOAPMessage> requestCaptor = ArgumentCaptor.forClass(SOAPMessage.class);
    verify(dispatcher, times(1)).invoke(requestCaptor.capture());

    final SOAPMessage requestMessage = requestCaptor.getValue();
    final Iterator<Node> commandElements =
        requestMessage
            .getSOAPBody()
            .getChildElements(QName.valueOf("{http://ws.gematik.de/conn/CardService}TransmitCard"));
    assertThat(commandElements).hasNext();
    final Iterator<Node> payloadHandles =
        requestMessage
            .getSOAPBody()
            .getChildElements(QName.valueOf("{http://ws.gematik.de/conn/CardService}Command"));
    final Node payloadHandle = payloadHandles.next();
    assertThat(payloadHandle.getValue()).isEqualTo(DatatypeConverter.printHexBinary(requestData));
  }

  private static @NotNull SOAPElement mockSoapElement(final Iterator<Node> childElements) {
    final SOAPElement detailsElement = mock(SOAPElement.class);
    when(detailsElement.getChildElements()).thenReturn(childElements);
    return detailsElement;
  }

  private static @NotNull Iterator<Node> iteratorOf(final SOAPElement detailsElement) {
    return List.<Node>of(detailsElement).iterator();
  }
}
