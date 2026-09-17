/*-
 * #%L
 * VSDM Client Simulator Service
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.card.CardTerminalConnectionConfig;
import de.gematik.ti20.simsvc.client.card.EgkInfo;
import de.gematik.ti20.simsvc.client.card.SmcbInfo;
import de.gematik.ti20.simsvc.client.exception.CardTerminalException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class CardTerminalServiceTest {

  private CardTerminalService service;
  private OkHttpClient httpClient;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    service = spy(new CardTerminalService());
    httpClient = mock(OkHttpClient.class);
    objectMapper = mock(ObjectMapper.class);
    ReflectionTestUtils.setField(service, "httpClient", httpClient);
    ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
  }

  @Test
  void keepsTerminalConfigs() {
    CardTerminalConnectionConfig config =
        new CardTerminalConnectionConfig("terminal", "http://example");

    service.setTerminalConnectionConfigs(List.of(config));

    assertThat(service.getTerminalConnectionConfigs()).containsExactly(config);
  }

  @Test
  void ignoresNullTerminalConfigs() {
    service.setTerminalConnectionConfigs(List.of());
    service.setTerminalConnectionConfigs(null);

    assertThat(service.getTerminalConnectionConfigs()).isEmpty();
  }

  @Test
  void mapsAttachedCardsFromConfiguredTerminals() throws Exception {
    CardTerminalConnectionConfig config =
        new CardTerminalConnectionConfig("terminal", "http://example");
    service.setTerminalConnectionConfigs(List.of(config));

    Call call = mock(Call.class);
    when(httpClient.newCall(any(Request.class))).thenReturn(call);
    when(call.execute())
        .thenReturn(
            okHttpResponse("[{\"cardHandle\":\"card-1\",\"cardType\":\"EGK\",\"slotId\":1}]"));
    when(objectMapper.readValue(any(String.class), eq(Map[].class)))
        .thenReturn(new Map[] {Map.of("cardHandle", "card-1", "cardType", "EGK", "slotId", 1)});

    List<AttachedCard> cards = service.getAttachedCards();

    assertThat(cards).hasSize(1);
    assertThat(cards.getFirst().getId()).isEqualTo("card-1");
    assertThat(cards.getFirst().getCardType()).isEqualTo("EGK");
    assertThat(cards.getFirst().getSlotId()).isEqualTo(1);
    assertThat(cards.getFirst().getTerminalUrl()).isEqualTo("http://example");
  }

  @Test
  void failsWhenAvailableCardsCannotBeLoaded() throws Exception {
    CardTerminalConnectionConfig config =
        new CardTerminalConnectionConfig("terminal", "http://example");
    service.setTerminalConnectionConfigs(List.of(config));

    Call call = mock(Call.class);
    when(httpClient.newCall(any(Request.class))).thenReturn(call);
    when(call.execute()).thenThrow(new IOException("down"));

    assertThatThrownBy(service::getAttachedCards)
        .isInstanceOfSatisfying(
            CardTerminalException.class,
            exception ->
                assertThat(exception.getMessage())
                    .isEqualTo("Failed to get available cards. Status code: down"));
  }

  @Test
  void failsWhenNoSmcbIsAttached() throws Exception {
    CardTerminalConnectionConfig config =
        new CardTerminalConnectionConfig("terminal", "http://example");
    service.setTerminalConnectionConfigs(List.of(config));

    Call call = mock(Call.class);
    when(httpClient.newCall(any(Request.class))).thenReturn(call);
    when(call.execute())
        .thenReturn(
            okHttpResponse("[{\"cardHandle\":\"card-1\",\"cardType\":\"EGK\",\"slotId\":1}]"));
    when(objectMapper.readValue(any(String.class), eq(Map[].class)))
        .thenReturn(new Map[] {Map.of("cardHandle", "card-1", "cardType", "EGK", "slotId", 1)});

    assertThatThrownBy(service::getSmcbInfo)
        .isInstanceOfSatisfying(
            CardTerminalException.class,
            exception -> assertThat(exception.getMessage()).isEqualTo("no smc-b found"));
  }

  @Test
  void returnsAttachedCardForMatchingSlot() throws Exception {
    AttachedCard firstCard = mock(AttachedCard.class);
    when(firstCard.getSlotId()).thenReturn(1);
    AttachedCard secondCard = mock(AttachedCard.class);
    when(secondCard.getSlotId()).thenReturn(2);
    when(secondCard.getId()).thenReturn("card-2");
    when(service.getAttachedCards()).thenReturn(List.of(firstCard, secondCard));

    AttachedCard result = service.getAttachedCard("terminal", 2);

    assertThat(result).isSameAs(secondCard);
  }

  @Test
  void failsWhenNoCardExistsForRequestedSlot() throws Exception {
    AttachedCard card = mock(AttachedCard.class);
    when(card.getSlotId()).thenReturn(1);
    when(service.getAttachedCards()).thenReturn(List.of(card));

    assertThatThrownBy(() -> service.getAttachedCard("terminal", 2))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> {
              assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(exception.getReason()).isEqualTo("No card found in slot 2");
            });
  }

  @Test
  void failsWhenAttachedCardsCannotBeLoadedForRequestedSlot() throws Exception {
    when(service.getAttachedCards()).thenThrow(new CardTerminalException("boom"));

    assertThatThrownBy(() -> service.getAttachedCard("terminal", 1))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception ->
                assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void returnsEgkInfoForAttachedCard() throws Exception {
    AttachedCard attachedCard =
        AttachedCard.from(
            "http://terminal", Map.of("cardHandle", "card-1", "cardType", "EGK", "slotId", 1));
    Call call = mock(Call.class);
    when(httpClient.newCall(any(Request.class))).thenReturn(call);
    when(call.execute())
        .thenReturn(
            okHttpResponse(
                "{\"kvnr\":\"X123\",\"iknr\":\"IK1\",\"firstName\":\"Max\",\"lastName\":\"Mustermann\",\"valid\":\"true\"}"));
    when(objectMapper.readValue(any(String.class), eq(EgkInfo.class)))
        .thenReturn(new EgkInfo("X123", "IK1", "Max", "Mustermann", "true"));

    EgkInfo result = service.getEgkInfo(attachedCard);

    assertThat(result.getKvnr()).isEqualTo("X123");
    assertThat(result.getIknr()).isEqualTo("IK1");
    assertThat(result.getFirstName()).isEqualTo("Max");
    assertThat(result.getLastName()).isEqualTo("Mustermann");
    assertThat(result.getValid()).isTrue();
  }

  @Test
  void failsWhenEgkInfoCannotBeLoaded() throws Exception {
    AttachedCard attachedCard =
        AttachedCard.from(
            "http://terminal", Map.of("cardHandle", "card-1", "cardType", "EGK", "slotId", 1));
    Call call = mock(Call.class);
    when(httpClient.newCall(any(Request.class))).thenReturn(call);
    when(call.execute()).thenThrow(new IOException("broken"));

    assertThatThrownBy(() -> service.getEgkInfo(attachedCard))
        .isInstanceOfSatisfying(
            CardTerminalException.class,
            exception ->
                assertThat(exception.getMessage()).isEqualTo("Failed to get EGK info: broken"));
  }

  @Test
  void returnsSmcbInfoForFirstMatchingSmcbCard() throws Exception {
    CardTerminalConnectionConfig config =
        new CardTerminalConnectionConfig("terminal", "http://example");
    service.setTerminalConnectionConfigs(List.of(config));

    Call call = mock(Call.class);
    when(httpClient.newCall(any(Request.class))).thenReturn(call);
    when(call.execute())
        .thenReturn(
            okHttpResponse(
                "[{\"cardHandle\":\"card-1\",\"cardType\":\"EGK\",\"slotId\":1},{\"cardHandle\":\"card-2\",\"cardType\":\"SMCB\",\"slotId\":2}]"))
        .thenReturn(
            okHttpResponse("{\"telematikId\":\"telematik-1\",\"professionOid\":\"profession-1\"}"));
    when(objectMapper.readValue(any(String.class), eq(Map[].class)))
        .thenReturn(
            new Map[] {
              Map.of("cardHandle", "card-1", "cardType", "EGK", "slotId", 1),
              Map.of("cardHandle", "card-2", "cardType", "SMCB", "slotId", 2)
            });
    when(objectMapper.readValue(any(String.class), eq(SmcbInfo.class)))
        .thenReturn(new SmcbInfo("telematik-1", "profession-1"));

    SmcbInfo result = service.getSmcbInfo();

    assertThat(result.getTelematikId()).isEqualTo("telematik-1");
    assertThat(result.getProfessionOid()).isEqualTo("profession-1");
  }

  private static Response okHttpResponse(String body) {
    return new Response.Builder()
        .request(new Request.Builder().url("http://localhost").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(ResponseBody.create(body, null))
        .build();
  }
}
