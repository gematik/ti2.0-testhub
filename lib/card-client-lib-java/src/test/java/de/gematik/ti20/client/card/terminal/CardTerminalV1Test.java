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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.card.config.CardTerminalConfig;
import de.gematik.ti20.client.card.config.TerminalSlotConfig;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardTerminalV1Test {
  @Mock private CardTerminalConfig config;

  @Mock private OkHttpClient client;

  @Mock private CardTerminalV1.CardTerminalEventHandler eventHandler;

  @Test
  void thatGetSlotWorks() {
    final TerminalSlotConfig expected = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "id");
    when(config.getSlots()).thenReturn(List.of(expected));

    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);

    final TerminalSlotV1 actual = terminalV1.getSlot("id");
    assertThat(actual).isNotNull();
  }

  @Test
  void thatGetSlotReturnsNullForUnknownSlotId() {
    when(config.getSlots()).thenReturn(List.of());

    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);

    final TerminalSlotV1 actual = terminalV1.getSlot("unknown");
    assertThat(actual).isNull();
  }

  @Test
  void thatConnectWorks() {
    final TerminalSlotConfig expected = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "id");
    when(config.getSlots()).thenReturn(List.of(expected));
    when(config.getConnection()).thenReturn(Map.of("url", "http://example.com"));

    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);

    terminalV1.connect("id", eventHandler);

    final TerminalSlotV1 id = terminalV1.getSlot("id");
    assertThat(id).isNotNull();
  }

  @Test
  void thatConnectRaisesExceptionForUnknownSlot() {
    when(config.getSlots()).thenReturn(List.of());

    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> terminalV1.connect("id", eventHandler));
  }

  @Test
  void thatDisconnectWorks() {
    final TerminalSlotConfig expected = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "id");
    when(config.getSlots()).thenReturn(List.of(expected));
    when(config.getConnection()).thenReturn(Map.of("url", "http://example.com"));

    // Call connect so that the slot is written
    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);
    terminalV1.connect("id", eventHandler);
    // Ensure slot is present
    final TerminalSlotV1 id = terminalV1.getSlot("id");
    assertThat(id).isNotNull();

    // Then assert disconnect works
    assertThatNoException().isThrownBy(() -> terminalV1.disconnect("id"));
  }

  @Test
  void thatDisconnectRaisesExceptionForUnknownSlot() {
    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> terminalV1.disconnect("unknown"));
  }

  @Test
  void thatOnCardInsertedIsProxied() {
    final TerminalSlotConfig expected = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "id");
    when(config.getSlots()).thenReturn(List.of(expected));
    when(config.getConnection()).thenReturn(Map.of("url", "http://example.com"));

    // Call connect so the eventHandler is set
    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);
    terminalV1.connect("id", eventHandler);

    // Then assert onCardInserted works
    terminalV1.onCardInserted(mock(TerminalSlotJsonV1.class));

    verify(eventHandler, times(1)).onCardInserted(any());
  }

  @Test
  void thatOnCardRemovedIsProxied() {
    final TerminalSlotConfig expected = new TerminalSlotConfig(TerminalSlotV1.Type.JSON, "id");
    when(config.getSlots()).thenReturn(List.of(expected));
    when(config.getConnection()).thenReturn(Map.of("url", "http://example.com"));

    // Call connect so the eventHandler is set
    final CardTerminalV1 terminalV1 = new CardTerminalV1(config, client);
    terminalV1.connect("id", eventHandler);

    // Then assert onCardInserted works
    terminalV1.onCardRemoved(mock(TerminalSlotJsonV1.class));

    verify(eventHandler, times(1)).onCardRemoved(any());
  }
}
