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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardTerminalServiceTest {

  private CardTerminalService service;
  private CardTerminalConnectionConfig config;
  private CardTerminal terminal;

  @BeforeEach
  void setUp() {
    config = mock(CardTerminalConnectionConfig.class);
    when(config.getType()).thenReturn(CardTerminalType.PCSC);
    when(config.getName()).thenReturn("TestTerminal");
    service = new CardTerminalService(new ArrayList<>(List.of(config)));
  }

  @Test
  void testGetTerminalConnectionConfigs() {
    List<CardTerminalConnectionConfig> configs = service.getTerminalConnectionConfigs();
    assertEquals(1, configs.size());
    assertEquals(config, configs.get(0));
  }

  @Test
  void testAddTerminalConnectionConfig() {
    CardTerminalConnectionConfig newConfig = mock(CardTerminalConnectionConfig.class);
    service.addTerminalConnectionConfig(newConfig);
    assertTrue(service.getTerminalConnectionConfigs().contains(newConfig));
  }

  @Test
  void testAddTerminalConnectionConfig_null() {
    int sizeBefore = service.getTerminalConnectionConfigs().size();
    service.addTerminalConnectionConfig(null);
    assertEquals(sizeBefore, service.getTerminalConnectionConfigs().size());
  }

  @Test
  void testSetTerminalConnectionConfigs() {
    CardTerminalConnectionConfig newConfig = mock(CardTerminalConnectionConfig.class);
    List<CardTerminalConnectionConfig> newList = List.of(newConfig);
    service.setTerminalConnectionConfigs(newList);
    assertEquals(1, service.getTerminalConnectionConfigs().size());
    assertEquals(newConfig, service.getTerminalConnectionConfigs().get(0));
  }

  @Test
  void testSetTerminalConnectionConfigs_null() {
    List<CardTerminalConnectionConfig> before =
        new ArrayList<>(service.getTerminalConnectionConfigs());
    service.setTerminalConnectionConfigs(null);
    assertEquals(before, service.getTerminalConnectionConfigs());
  }

  @Test
  void testGetAvailableTerminals_emptyConfig() {
    CardTerminalService emptyService = new CardTerminalService(new ArrayList<>());
    List<CardTerminal> result = emptyService.getAvailableTerminals();
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetAvailableTerminals_terminalCreationFails() {
    CardTerminalConnectionConfig badConfig = mock(CardTerminalConnectionConfig.class);
    when(badConfig.getType()).thenReturn(CardTerminalType.valueOf("PCSC"));
    when(badConfig.getName()).thenReturn("bad");
    CardTerminalService s = spy(new CardTerminalService(List.of(badConfig)));
    try {
      doThrow(new CardTerminalException("fail")).when(s).createTerminal(badConfig);
    } catch (CardTerminalException ignored) {
    }
    List<CardTerminal> result = s.getAvailableTerminals();
    assertTrue(result.isEmpty());
  }

  @Test
  void testCreateTerminal_nullConfig() {
    assertThrows(
        CardTerminalException.class,
        () -> {
          service
              .getClass()
              .getDeclaredMethod("createTerminal", CardTerminalConnectionConfig.class)
              .setAccessible(true);
          try {
            service
                .getClass()
                .getDeclaredMethod("createTerminal", CardTerminalConnectionConfig.class)
                .invoke(service, (Object) null);
          } catch (Exception e) {
            if (e.getCause() instanceof CardTerminalException)
              throw (CardTerminalException) e.getCause();
            throw new RuntimeException(e);
          }
        });
  }

  @Test
  void testCreateTerminal_unsupportedType() {
    CardTerminalConnectionConfig unknownConfig = mock(CardTerminalConnectionConfig.class);
    when(unknownConfig.getType()).thenReturn(null);
    when(unknownConfig.getName()).thenReturn("unknown");
    assertThrows(
        CardTerminalException.class,
        () -> {
          service
              .getClass()
              .getDeclaredMethod("createTerminal", CardTerminalConnectionConfig.class)
              .setAccessible(true);
          try {
            service
                .getClass()
                .getDeclaredMethod("createTerminal", CardTerminalConnectionConfig.class)
                .invoke(service, unknownConfig);
          } catch (Exception e) {
            if (e.getCause() instanceof CardTerminalException)
              throw (CardTerminalException) e.getCause();
            throw new RuntimeException(e);
          }
        });
  }
}
