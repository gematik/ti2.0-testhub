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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.connector.signature.SignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectorCardConnectionTest {

  @Mock private ConnectorAttachedCard card;
  @Mock private ConnectorCardTerminal terminal;

  private ConnectorCardConnection connection;

  @BeforeEach
  public void setup() {
    // use spy so that we can mock `connection.isConnected()`
    connection = spy(new ConnectorCardConnection(card, "connection-1", terminal));
  }

  @Test
  void thatTransmitRaisesExceptionIfDisconnected() {
    when(connection.isConnected()).thenReturn(false);
    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> connection.transmit(new byte[] {}));
  }

  @Test
  void thatTransmitProxiesTransmitCall() throws Exception {
    final String connectionHandle = "connection-1";
    final byte[] apdu = new byte[] {0, 1, 0};
    final byte[] response = new byte[] {1, 0, 1};
    when(terminal.transmitAPDU(connectionHandle, apdu)).thenReturn(response);
    final byte[] result = connection.transmit(apdu);

    assertThat(result).isEqualTo(response);
    verify(terminal).transmitAPDU(connectionHandle, apdu);
  }

  @Test
  void thatTransmitRaisesExceptionOnTerminalException() throws Exception {
    when(terminal.transmitAPDU(any(), any())).thenThrow(new RuntimeException("Transmit failed"));

    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> connection.transmit(new byte[] {}));
  }

  @Test
  void thatSignRaisesExceptionIfDisconnected() {
    when(connection.isConnected()).thenReturn(false);
    assertThatExceptionOfType(CardTerminalException.class)
        .isThrownBy(() -> connection.sign(new byte[] {}, new SignOptions()));
  }

  @Test
  void thatSignWorksAsExpected() throws CardTerminalException {
    final SignatureService mockSignatureService = mock(SignatureService.class);
    final byte[] expectedSignedData = {1};
    when(mockSignatureService.sign(any(), any(), any())).thenReturn(expectedSignedData);
    when(terminal.getSignatureService()).thenReturn(mockSignatureService);

    final byte[] data = new byte[] {};
    final SignOptions options = new SignOptions(SignOptions.HashAlgorithm.SHA256);

    final byte[] actualSignedData = connection.sign(data, options);

    assertThat(actualSignedData).isEqualTo(expectedSignedData);

    verify(mockSignatureService, times(1)).sign(eq(card), eq(data), eq(options));
  }

  @Test
  void thatSignUsesFallback() throws CardTerminalException {
    final byte[] successResponse = new byte[] {0, 0, (byte) 0x90, 0x00};
    when(terminal.transmitAPDU(any(), any())).thenReturn(successResponse);

    // force fallback sign procedure
    when(terminal.getSignatureService()).thenThrow(new RuntimeException("use fallback"));

    // this test only exercises the various calls to static methods, but testing a correct
    // implementation is not done currently
    connection.sign(
        new byte[] {},
        new SignOptions(
            SignOptions.HashAlgorithm.SHA256, SignOptions.SignatureType.ECDSA, "some-ref"));
  }

  @Test
  void thatDisconnectIsANoOpIfDisconnected() throws Exception {
    when(connection.isConnected()).thenReturn(false);

    connection.disconnect();

    verify(terminal, never()).disconnect(any());
  }

  @Test
  void thatDisconnectRaisesNoExceptionOnTerminalException() throws Exception {
    doThrow(new RuntimeException("Disconnect failed")).when(terminal).disconnect(any());
    assertThatNoException().isThrownBy(() -> connection.disconnect());
  }

  @Test
  void thatDisconnectProxiesDisconnectCall() throws Exception {
    connection.disconnect();

    verify(terminal).disconnect(any());
    assertThat(connection.isConnected()).isFalse();
  }
}
