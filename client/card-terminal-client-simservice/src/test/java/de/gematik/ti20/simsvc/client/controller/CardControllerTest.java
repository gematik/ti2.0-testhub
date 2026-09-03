/*-
 * #%L
 * Card Terminal Simulator
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
package de.gematik.ti20.simsvc.client.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.dto.*;
import de.gematik.ti20.simsvc.client.service.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CardControllerTest {

  private CardManager cardManager;
  private SmcBInfoService smcBInfoService;
  private EgkInfoService egkInfoService;
  private CardController controller;

  @BeforeEach
  void setUp() {
    cardManager = mock(CardManager.class);
    smcBInfoService = mock(SmcBInfoService.class);
    egkInfoService = mock(EgkInfoService.class);
    controller = new CardController(cardManager, smcBInfoService, egkInfoService);
  }

  @Test
  void listCards_returnsCardHandles() {
    List<CardHandleDto> handles =
        List.of(
            new CardHandleDto("id1", "EGK", 1, "label1"),
            new CardHandleDto("id2", "EGK", 2, "label2"));
    when(cardManager.listAllCards()).thenReturn(handles);

    ResponseEntity<List<CardHandleDto>> response = controller.listCards();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(handles, response.getBody());
  }

  @Test
  void getSmcBInfo_returnsInfo() {
    SmcBInfoDto info = new SmcBInfoDto();
    when(smcBInfoService.extractSmcBInfo("h")).thenReturn(info);

    ResponseEntity<SmcBInfoDto> response = controller.getSmcBInfo("h");
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(info, response.getBody());
  }

  @Test
  void getEgkInfo_cardNotFound_returnsNotFound() {
    when(cardManager.findCardByHandle("h")).thenReturn(null);

    ResponseEntity<?> response = controller.getEgkInfo("h");
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertTrue(((Map<?, ?>) response.getBody()).containsKey("error"));
  }
}
