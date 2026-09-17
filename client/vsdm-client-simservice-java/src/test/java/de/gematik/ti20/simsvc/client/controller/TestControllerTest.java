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
package de.gematik.ti20.simsvc.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.simsvc.client.service.CardTerminalService;
import de.gematik.ti20.simsvc.client.service.VsdmClientService;
import de.gematik.ti20.simsvc.client.util.StorageInterceptor;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TestControllerTest {

  private PoppTokenRepository poppTokenRepository;
  private VsdmDataRepository vsdmDataRepository;
  private VsdmClientService vsdmClientService;
  private VsdmClientConfig vsdmClientConfig;
  private StorageInterceptor storageInterceptor;
  private CardTerminalService cardTerminalService;

  private TestController testController;

  @BeforeEach
  void setUp() {
    poppTokenRepository = mock(PoppTokenRepository.class);
    vsdmDataRepository = mock(VsdmDataRepository.class);
    vsdmClientService = mock(VsdmClientService.class);
    vsdmClientConfig = new VsdmClientConfig();
    storageInterceptor = new StorageInterceptor();
    cardTerminalService = mock(CardTerminalService.class);

    testController =
        new TestController(
            poppTokenRepository,
            vsdmDataRepository,
            vsdmClientService,
            vsdmClientConfig,
            storageInterceptor,
            cardTerminalService);
  }

  @Test
  void shouldReturnPoppTokenFromRepository() {
    when(poppTokenRepository.get("terminal-1", 1, "card-1")).thenReturn("token-123");

    String response = testController.getPoppToken("terminal-1", 1, "card-1");

    assertEquals("token-123", response);
  }

  @Test
  void shouldClearPoppTokenCache() {
    testController.clearPoppTokenCache();

    verify(poppTokenRepository).clear();
  }

  @Test
  void shouldReturnVsdmDataFromRepository() {
    VsdmCachedValue cachedValue = new VsdmCachedValue("etag", "pruefziffer", "vsdmData");
    when(vsdmDataRepository.get("terminal-1", 2, "card-2")).thenReturn(cachedValue);

    VsdmCachedValue response = testController.getVsdmData("terminal-1", 2, "card-2");

    assertEquals(cachedValue, response);
  }

  @Test
  void shouldClearVsdmDataCache() {
    testController.clearVsdmDataCache();

    verify(vsdmDataRepository).clear();
  }

  @Test
  void shouldReturnUnauthorizedWhenNoEgkDataIsAvailable() throws Exception {
    AttachedCard attachedCard =
        AttachedCard.from(
            "http://terminal", Map.of("cardHandle", "card-1", "cardType", "EGK", "slotId", 3));
    when(cardTerminalService.getAttachedCard("terminal-1", 3)).thenReturn(attachedCard);
    when(vsdmClientService.loadTruncatedDataFromCard(attachedCard)).thenReturn(null);

    ResponseEntity<String> response = testController.readEgk("terminal-1", 3);

    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void shouldReturnEgkDataWhenAvailable() throws Exception {
    AttachedCard attachedCard =
        AttachedCard.from(
            "http://terminal", Map.of("cardHandle", "card-1", "cardType", "EGK", "slotId", 4));
    when(cardTerminalService.getAttachedCard("terminal-1", 4)).thenReturn(attachedCard);
    when(vsdmClientService.loadTruncatedDataFromCard(attachedCard)).thenReturn("egk-data");

    ResponseEntity<String> response = testController.readEgk("terminal-1", 4);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("egk-data", response.getBody());
  }

  @Test
  void shouldReturnZetaDataFromStorageInterceptor() {
    storageInterceptor.getCache().put("key", "value");
    vsdmClientConfig.setInterceptStorage(true);

    ResponseEntity<Map<String, String>> response = testController.readZetaData();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(Map.of("key", "value"), response.getBody());
  }
}
