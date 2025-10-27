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
package de.gematik.ti20.client.popp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.terminal.CardTerminal;
import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PoppClientServiceTest {

  private PoppClientConfig poppClientConfig;
  private ZetaClientService zetaClientService;
  private CardTerminalService cardTerminalService;
  private PoppClientService poppClientService;

  @BeforeEach
  void setUp() {
    poppClientConfig = mock(PoppClientConfig.class);
    zetaClientService = mock(ZetaClientService.class);
    cardTerminalService = mock(CardTerminalService.class);
    poppClientService =
        new PoppClientService(poppClientConfig, zetaClientService, cardTerminalService);
  }

  @Test
  void testGetPoppClientConfigAndZetaClientService() {
    assertEquals(poppClientConfig, poppClientService.getPoppClientConfig());
    assertEquals(zetaClientService, poppClientService.getZetaClientService());
  }

  @Test
  void testGetAttachedCardsFiltersEgk() throws Exception {
    AttachedCard egk = mock(AttachedCard.class);
    when(egk.isEgk()).thenReturn(true);
    AttachedCard notEgk = mock(AttachedCard.class);
    when(notEgk.isEgk()).thenReturn(false);

    when(cardTerminalService.getAttachedCards()).thenReturn((List) Arrays.asList(egk, notEgk));

    List<? extends AttachedCard> result = poppClientService.getAttachedCards();
    assertEquals(1, result.size());
    assertTrue(result.contains(egk));
    assertFalse(result.contains(notEgk));
  }

  @Test
  void testStartPoppTokenSessionWithNullCardThrows() {
    assertThrows(
        PoppClientException.class,
        () ->
            poppClientService.startPoppTokenSession(
                null, 0, mock(PoppTokenSessionEventHandler.class)));
  }

  @Test
  void testStartPoppTokenSessionWithNonEgkThrows() {
    AttachedCard card = mock(AttachedCard.class);
    when(card.isEgk()).thenReturn(false);
    assertThrows(
        PoppClientException.class,
        () ->
            poppClientService.startPoppTokenSession(
                card, 0, mock(PoppTokenSessionEventHandler.class)));
  }

  @Test
  void testStartPoppTokenSessionCreatesAndStartsSession() throws Exception {
    CardTerminal cardTerminal = mock(CardTerminal.class);
    when(cardTerminal.getName()).thenReturn("terminal-1");

    AttachedCard card = mock(AttachedCard.class);
    when(card.getTerminal()).thenReturn(cardTerminal);
    when(card.isEgk()).thenReturn(true);
    when(card.getId()).thenReturn("card-1");

    PoppTokenSessionEventHandler handler = mock(PoppTokenSessionEventHandler.class);

    // Session wird im Service erzeugt, wir mocken Konstruktor nicht, aber prüfen keine Exception
    assertDoesNotThrow(() -> poppClientService.startPoppTokenSession(card, 0, handler));
  }
}
