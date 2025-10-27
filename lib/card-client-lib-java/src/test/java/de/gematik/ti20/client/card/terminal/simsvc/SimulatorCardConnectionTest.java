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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.card.CardCertInfo;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulatorCardConnectionTest {

  private SimulatorCardTerminal terminal;
  private SimulatorClient client;
  private SimulatorAttachedCard card;
  private CardConnectionInfo cci;
  private SimulatorCardConnection connection;

  @BeforeEach
  void setUp() {
    terminal = mock(SimulatorCardTerminal.class);
    client = mock(SimulatorClient.class);
    card = mock(SimulatorAttachedCard.class);
    cci = mock(CardConnectionInfo.class);

    when(terminal.getClient()).thenReturn(client);
    when(card.getId()).thenReturn("cardId");

    connection = new SimulatorCardConnection(card, terminal, cci);
  }

  @Test
  void testGetCardConnectionInfo() {
    assertEquals(cci, connection.getCardConnectionInfo());
  }

  @Test
  void testTransmit_success() throws Exception {
    byte[] command = {0x00, 0x01};
    byte[] responseBytes = {0x10, 0x20};
    SimulatorClient.ApduResponse response = mock(SimulatorClient.ApduResponse.class);
    when(client.transmitApdu(eq("cardId"), any())).thenReturn(response);
    when(response.getResponse()).thenReturn(responseBytes);

    byte[] result = connection.transmit(command);
    assertArrayEquals(responseBytes, result);
  }

  @Test
  void testTransmit_throwsException() throws Exception {
    when(client.transmitApdu(any(), any())).thenThrow(new IOException("IO"));
    assertThrows(CardTerminalException.class, () -> connection.transmit(new byte[] {0x00}));
  }

  @Test
  void testGetCertInfo_success() throws Exception {
    CardCertInfo certInfo = mock(CardCertInfo.class);
    when(client.getCertInfo("cardId")).thenReturn(certInfo);

    assertEquals(certInfo, connection.getCertInfo());
  }

  @Test
  void testGetCertInfo_throwsException() throws Exception {
    when(client.getCertInfo(any())).thenThrow(new IOException("IO"));
    assertThrows(CardTerminalException.class, () -> connection.getCertInfo());
  }

  @Test
  void testSign_success() throws Exception {
    byte[] data = {0x01, 0x02};
    byte[] signed = {0x03, 0x04};
    SignOptions options = mock(SignOptions.class);
    when(client.signData("cardId", data, options)).thenReturn(signed);

    assertArrayEquals(signed, connection.sign(data, options));
  }

  @Test
  void testSign_throwsException() throws Exception {
    when(client.signData(any(), any(), any())).thenThrow(new IOException("IO"));
    assertThrows(
        CardTerminalException.class,
        () -> connection.sign(new byte[] {0x01}, mock(SignOptions.class)));
  }

  @Test
  void testDisconnect_success() throws Exception {
    doNothing().when(client).disconnectFromCard(card);
    connection.disconnect();
    assertFalse(connection.isConnected());
  }

  @Test
  void testDisconnect_logsOnException() throws Exception {
    doThrow(new RuntimeException("fail")).when(client).disconnectFromCard(card);
    connection.disconnect();
    assertFalse(connection.isConnected());
  }
}
