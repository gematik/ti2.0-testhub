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
package de.gematik.ti20.client.card.terminal;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.ti20.client.card.config.CardTerminalConfig;
import de.gematik.ti20.client.card.config.TerminalSlotConfig;
import de.gematik.ti20.client.card.exception.CardTerminalSlotException;
import de.gematik.ti20.client.card.message.CardMessage;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TerminalSlotJsonV1Test {
  @Mock private CardTerminalV1 terminal;

  @Mock private TerminalSlotConfig config;

  @Mock private CardTerminalConfig cardTerminalConfig;

  @Mock private OkHttpClient okHttpClient;

  private TerminalSlotJsonV1 terminalSlotJsonV1;

  @BeforeEach
  public void setup() {
    terminalSlotJsonV1 = new TerminalSlotJsonV1(config, terminal, okHttpClient);
  }

  @Test
  void thatConnectingWorks() {
    when(terminal.getConfig()).thenReturn(cardTerminalConfig);

    when(cardTerminalConfig.getConnection()).thenReturn(Map.of("url", "http://example.com"));
    when(config.getSlotId()).thenReturn("slot-1");

    terminalSlotJsonV1.connect();

    verify(okHttpClient, times(1)).newWebSocket(any(), any());
  }

  @Test
  void thatDisconnectingWorks() {
    terminalSlotJsonV1.disconnect();
  }

  @Test
  void thatSendRaisesExceptionForJSONError() throws JsonProcessingException {
    final CardMessage mock = mock(CardMessage.class);
    when(mock.getJson()).thenThrow(mock(JsonProcessingException.class));
    assertThatExceptionOfType(CardTerminalSlotException.class)
        .isThrownBy(() -> terminalSlotJsonV1.send(mock));
  }
}
