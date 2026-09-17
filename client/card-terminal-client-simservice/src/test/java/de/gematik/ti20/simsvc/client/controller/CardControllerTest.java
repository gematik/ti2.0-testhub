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

import de.gematik.ti20.simsvc.client.exception.CardNotFoundException;
import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import de.gematik.ti20.simsvc.client.service.EgkInfoService;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import de.gematik.ti20.simsvc.client.service.SmcBInfoService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CardControllerTest {

  private SlotManager slotManager;
  private SmcBInfoService smcBInfoService;
  private EgkInfoService egkInfoService;
  private CardController controller;

  @BeforeEach
  void setUp() {
    slotManager = mock(SlotManager.class);
    smcBInfoService = mock(SmcBInfoService.class);
    egkInfoService = mock(EgkInfoService.class);
    controller = new CardController(slotManager, smcBInfoService, egkInfoService);
  }

  @Test
  void listCards_returnsCardHandles() {
    List<CardHandleDto> handles =
        List.of(
            new CardHandleDto("id1", "EGK", 1, "label1"),
            new CardHandleDto("id2", "EGK", 2, "label2"));
    when(slotManager.listAllCards()).thenReturn(handles);

    ResponseEntity<List<CardHandleDto>> response = controller.listCards();
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(handles, response.getBody());
  }

  @Test
  void getSmcBInfo_returnsInfo() {
    final String cardHandle = "cardHandle";
    final VirtualCardImageData imageData = mock(VirtualCardImageData.class);
    final SmcBInfoDto info = new SmcBInfoDto();
    when(smcBInfoService.extractSmcBInfo(imageData)).thenReturn(info);
    when(slotManager.findCardByHandle(cardHandle)).thenReturn(imageData);

    ResponseEntity<SmcBInfoDto> response = controller.getSmcBInfo(cardHandle);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(info, response.getBody());
  }

  @Test
  void getEgkInfo_returnsInfo() {
    final String cardHandle = "cardHandle";
    final VirtualCardImageData imageData = mock(VirtualCardImageData.class);
    final EgkInfoDto info = new EgkInfoDto();
    when(slotManager.findCardByHandle(cardHandle)).thenReturn(imageData);
    when(egkInfoService.extractEgkInfo(imageData)).thenReturn(info);

    ResponseEntity<EgkInfoDto> response = controller.getEgkInfo(cardHandle);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(info, response.getBody());
  }

  @Test
  void getEgkInfo_cardNotFound_returnsNotFound() {
    final String cardHandle = "cardHandle";
    when(slotManager.findCardByHandle(cardHandle)).thenReturn(null);

    CardNotFoundException exception =
        assertThrows(CardNotFoundException.class, () -> controller.getEgkInfo(cardHandle));

    assertEquals("cardHandle", exception.getCardId());
  }
}
