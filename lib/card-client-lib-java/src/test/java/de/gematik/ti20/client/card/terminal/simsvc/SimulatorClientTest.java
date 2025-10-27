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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.terminal.CardTerminal;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimulatorClientTest {

  private SimulatorClient client;
  private OkHttpClient httpClientMock;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    client = new SimulatorClient("http://localhost:1234/");
    objectMapper = new ObjectMapper();
    // Reflection zum Austauschen des httpClient durch Mock
    try {
      var field = SimulatorClient.class.getDeclaredField("httpClient");
      field.setAccessible(true);
      httpClientMock = mock(OkHttpClient.class);
      field.set(client, httpClientMock);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void testGetAvailableCards_success() throws Exception {
    String json =
        "[{\"cardHandle\":\"1\",\"type\":\"SIMULATOR\",\"terminal\":{\"type\":\"SIMULATOR\"}}]";
    Response response = mockResponse(200, json);
    Call call = mock(Call.class);
    when(call.execute()).thenReturn(response);
    when(httpClientMock.newCall(any())).thenReturn(call);

    List<AttachedCardInfo> cards = client.getAvailableCards();
    assertEquals(1, cards.size());
    assertEquals("1", cards.get(0).getId());
  }

  @Test
  void testGetAvailableCards_failure() throws Exception {
    Response response = mockResponse(500, "Fehler");
    Call call = mock(Call.class);
    when(call.execute()).thenReturn(response);
    when(httpClientMock.newCall(any())).thenReturn(call);

    assertThrows(IOException.class, () -> client.getAvailableCards());
  }

  @Test
  void testConnectToCard_success() throws Exception {
    AttachedCard card = mock(AttachedCard.class);
    CardTerminal terminal = mock(CardTerminal.class);
    when(card.getId()).thenReturn("1");
    when(card.getTerminal()).thenReturn(terminal);

    String json = "{\"cardHandle\":\"h1\",\"atr\":\"00\",\"protocol\":\"T=1\",\"exclusive\":true}";
    Response response = mockResponse(200, json);
    Call call = mock(Call.class);
    when(call.execute()).thenReturn(response);
    when(httpClientMock.newCall(any())).thenReturn(call);

    CardConnectionInfo info = client.connectToCard(card);
    assertEquals("h1", info.getCardHandle());
  }

  @Test
  void testDisconnectFromCard_success() throws Exception {
    AttachedCard card = mock(AttachedCard.class);
    CardTerminal terminal = mock(CardTerminal.class);
    when(card.getId()).thenReturn("1");
    when(card.getTerminal()).thenReturn(terminal);

    Response response = mockResponse(200, "OK");
    Call call = mock(Call.class);
    when(call.execute()).thenReturn(response);
    when(httpClientMock.newCall(any())).thenReturn(call);

    assertDoesNotThrow(() -> client.disconnectFromCard(card));
  }

  @Test
  void testTransmitApdu_success() throws Exception {
    String apduJson = "{\"statusWord\":\"9000\",\"statusMessage\":\"OK\",\"data\":\"DEADBEEF\"}";
    Response response = mockResponse(200, apduJson);
    Call call = mock(Call.class);
    when(call.execute()).thenReturn(response);
    when(httpClientMock.newCall(any())).thenReturn(call);

    SimulatorClient.ApduRequest req = new SimulatorClient.ApduRequest("00A40400");
    SimulatorClient.ApduResponse apduResp = client.transmitApdu("1", req);
    assertTrue(apduResp.isSuccessful());
    assertArrayEquals(
        new byte[] {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}, apduResp.getData());
  }

  @Test
  void testTransmitApdu_failure() throws Exception {
    String apduJson = "{\"statusWord\":\"6A82\",\"statusMessage\":\"Not found\",\"data\":\"\"}";
    Response response = mockResponse(200, apduJson);
    Call call = mock(Call.class);
    when(call.execute()).thenReturn(response);
    when(httpClientMock.newCall(any())).thenReturn(call);

    SimulatorClient.ApduRequest req = new SimulatorClient.ApduRequest("00A40400");
    assertThrows(IOException.class, () -> client.transmitApdu("1", req));
  }

  @Test
  void testSignData_success() throws Exception {
    String signature = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});
    String signJson =
        "{\"signature\":\"" + signature + "\",\"algorithm\":\"algo\",\"certificate\":\"cert\"}";
    Response response = mockResponse(200, signJson);
    Call call = mock(Call.class);
    when(call.execute()).thenReturn(response);
    when(httpClientMock.newCall(any())).thenReturn(call);

    SignOptions options = mock(SignOptions.class);
    when(options.getHashAlgorithm()).thenReturn(SignOptions.HashAlgorithm.SHA256);
    when(options.getSignatureType()).thenReturn(SignOptions.SignatureType.RSA);
    when(options.getKeyReference()).thenReturn(null);

    byte[] result = client.signData("1", new byte[] {0x01}, options);
    assertArrayEquals(new byte[] {1, 2, 3}, result);
  }

  // Hilfsmethode für Mock-Response
  private Response mockResponse(int code, String body) {
    ResponseBody responseBody = ResponseBody.create(body, MediaType.get("application/json"));
    return new Response.Builder()
        .request(new Request.Builder().url("http://localhost/").build())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("")
        .body(responseBody)
        .build();
  }
}
