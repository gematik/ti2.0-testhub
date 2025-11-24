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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardType;
import de.gematik.ti20.client.card.config.ConnectorConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalType;
import de.gematik.ti20.client.card.terminal.connector.signature.SignatureService;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import java.util.List;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorCardTerminalTest {
  @Mock private ConnectorConnectionConfig config;
  @Mock private Dispatch<SOAPMessage> dispatcher;

  @Mock private ConnectorClient client;

  @Mock private SignatureService signatureService;

  @InjectMocks private ConnectorCardTerminal cardTerminal;

  @Test
  void thatGetAttachedCardsReturnCards() throws Exception {
    when(client.getCards()).thenReturn(new String[] {"card-handle-1"});
    when(client.getCardType(any())).thenReturn("EGK");

    final List<AttachedCard> attachedCards = cardTerminal.getAttachedCards();
    assertThat(attachedCards).hasSize(1);
    assertThat(attachedCards.getFirst())
        .satisfies(
            card -> {
              assertThat(card.getTerminal()).isSameAs(cardTerminal);
              assertThat(card.getId()).endsWith("card-handle-1");
              assertThat(card.getType()).isEqualTo(CardType.EGK);
              assertThat(card.getInfo()).isEqualTo("Type: EGK, Handle: card-handle-1");
            });
  }

  @Test
  void thatGetAttachedCardsCanReturnEmptyList() throws Exception {
    when(client.getCards()).thenReturn(new String[] {});

    final List<AttachedCard> attachedCards = cardTerminal.getAttachedCards();
    assertThat(attachedCards).isEmpty();
  }

  @Test
  void thatGetAttachedCardsRaisesExceptionOnClientError() throws Exception {
    when(client.getCards()).thenReturn(new String[] {"card-handle-1"});
    when(client.getCardType(any())).thenThrow(new RuntimeException("Any exception"));
    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> cardTerminal.getAttachedCards());
  }

  @Test
  void thatGetSignatureServiceRaisesExceptionForMissingService() {
    // Create a terminal with null signature service
    final ConnectorCardTerminal terminal =
        new ConnectorCardTerminal(
            "test-terminal", CardTerminalType.CONNECTOR, config, dispatcher, client, null);

    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(terminal::getSignatureService);
  }

  @Test
  void thatGetSignatureServiceReturnsService() throws CardTerminalException {
    final SignatureService service = cardTerminal.getSignatureService();
    assertThat(service).isSameAs(signatureService);
  }

  @Test
  void thatDisconnectRaisesExceptionWhenDisconnectFails() throws Exception {
    final String connectionHandle = "connection-handle-1";
    doThrow(new RuntimeException("Disconnect failed")).when(client).disconnect(connectionHandle);

    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> cardTerminal.disconnect(connectionHandle));
  }

  @Test
  void thatDisconnectForwardsToClient() throws Exception {
    final String connectionHandle = "connection-handle-1";
    cardTerminal.disconnect(connectionHandle);
    verify(client).disconnect(connectionHandle);
  }

  @Test
  void thatTransmitAPDURaisesExceptionOnClientError() throws Exception {
    final String connectionHandle = "connection-handle-1";
    final byte[] apdu = new byte[] {};
    when(client.transmit(connectionHandle, apdu))
        .thenThrow(new RuntimeException("Transmit failed"));

    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> cardTerminal.transmitAPDU(connectionHandle, apdu));
  }

  @Test
  void thatTransmitAPDUProxiesToClient() throws Exception {
    final String connectionHandle = "connection-handle-1";
    final byte[] apdu = new byte[] {0, 1, 0};
    final byte[] response = new byte[] {1, 0, 1};
    when(client.transmit(connectionHandle, apdu)).thenReturn(response);

    final byte[] result = cardTerminal.transmitAPDU(connectionHandle, apdu);

    assertThat(result).isEqualTo(response);
    verify(client).transmit(connectionHandle, apdu);
  }

  @Test
  void thatConnectFailsIfCardIsNotConnectorAttachedCard() {
    final AttachedCard nonConnectorCard = mock(SimulatorAttachedCard.class);
    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> cardTerminal.connect(nonConnectorCard));
  }

  @Test
  void thatConnectProxiesToClient() throws Exception {
    final String cardHandle = "card-handle-1";
    final String connectionHandle = "connection-handle-1";
    final ConnectorAttachedCard card =
        new ConnectorAttachedCard("test-id", CardType.EGK, cardTerminal, cardHandle);
    when(client.connect(cardHandle)).thenReturn(connectionHandle);

    cardTerminal.connect(card);

    verify(client).connect(cardHandle);
  }

  @Test
  void thatConnectRaisesExceptionOnClientError() throws Exception {
    final String cardHandle = "card-handle-1";
    final ConnectorAttachedCard card =
        new ConnectorAttachedCard("test-id", CardType.EGK, cardTerminal, cardHandle);
    when(client.connect(cardHandle)).thenThrow(new RuntimeException("Connect failed"));

    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> cardTerminal.connect(card));
  }
}
