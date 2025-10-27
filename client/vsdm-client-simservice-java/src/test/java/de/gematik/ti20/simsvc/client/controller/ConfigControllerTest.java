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
package de.gematik.ti20.simsvc.client.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.simsvc.client.service.VsdmClientService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class ConfigControllerTest {

  @Mock private VsdmClientService mockVsdmClientService;
  private ConfigController configController;

  @BeforeEach
  void setUp() {
    mockVsdmClientService = mock(VsdmClientService.class);
    configController = new ConfigController(mockVsdmClientService);
  }

  @Test
  void testSetTerminalConnectionConfigs_Success() {
    List<CardTerminalConnectionConfig> configs =
        List.of(new SimulatorConnectionConfig("Terminal1", "URL1"));

    doNothing().when(mockVsdmClientService).setTerminalConnectionConfigs(configs);

    ResponseEntity<String> response = configController.setTerminalConnectionConfigs(configs);

    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
  }

  @Test
  void testSetTerminalConnectionConfigs_Failure() {
    List<CardTerminalConnectionConfig> configs =
        List.of(new SimulatorConnectionConfig("Terminal1", "URL1"));

    doThrow(new RuntimeException("Invalid Config"))
        .when(mockVsdmClientService)
        .setTerminalConnectionConfigs(configs);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> configController.setTerminalConnectionConfigs(configs));

    assertEquals(400, exception.getStatusCode().value());
    assertEquals("Invalid Config", exception.getReason());
  }

  @Test
  void testGetTerminalConnectionConfigs_Success() {
    List<CardTerminalConnectionConfig> configs =
        List.of(new SimulatorConnectionConfig("Terminal1", "URL1"));

    when(mockVsdmClientService.getTerminalConnectionConfigs()).thenReturn(configs);

    ResponseEntity<List<CardTerminalConnectionConfig>> response =
        configController.getTerminalConnectionConfigs();

    assertNotNull(response);
    assertEquals(200, response.getStatusCodeValue());
    assertEquals(configs, response.getBody());
  }

  @Test
  void testGetTerminalConnectionConfigs_Failure() {
    when(mockVsdmClientService.getTerminalConnectionConfigs())
        .thenThrow(new RuntimeException("Fetch Error"));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> configController.getTerminalConnectionConfigs());

    assertEquals(400, exception.getStatusCode().value());
    assertEquals("Fetch Error", exception.getReason());
  }
}
