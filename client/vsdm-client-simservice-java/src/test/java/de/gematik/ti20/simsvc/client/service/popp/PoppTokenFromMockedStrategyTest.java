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
package de.gematik.ti20.simsvc.client.service.popp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.service.MockPoppTokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PoppTokenFromMockedStrategyTest {

  @Mock private VsdmClientConfig vsdmClientConfig;
  @Mock private CardTerminalService cardTerminalService;
  @Mock private MockPoppTokenService mockPoppTokenService;
  @Mock private PoppTokenRepository poppTokenRepository;
  @Mock private AttachedCard attachedCard;
  @Mock private EgkInfo egkInfo;

  private PoppTokenFromMockedStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy =
        new PoppTokenFromMockedStrategy(
            vsdmClientConfig, cardTerminalService, mockPoppTokenService, poppTokenRepository);
  }

  @Test
  void shouldReturnEmptyWhenMockPoppTokenIsDisabled() {
    when(vsdmClientConfig.isUseMockPoppToken()).thenReturn(false);

    Optional<PoppToken> result = strategy.get("terminal-1", 1, attachedCard);

    assertTrue(result.isEmpty());
    verifyNoInteractions(cardTerminalService, mockPoppTokenService, poppTokenRepository);
  }

  @Test
  void shouldRequestAndCacheMockPoppTokenWhenEnabled() throws Exception {
    String terminalId = "terminal-1";
    Integer egkSlotId = 1;
    String cardId = "card-1";
    String iknr = "iknr-1";
    String kvnr = "kvnr-1";
    String mockedToken = "mocked-popp-token";

    when(vsdmClientConfig.isUseMockPoppToken()).thenReturn(true);
    when(attachedCard.getId()).thenReturn(cardId);
    when(cardTerminalService.getEgkInfo(attachedCard)).thenReturn(egkInfo);
    when(egkInfo.getIknr()).thenReturn(iknr);
    when(egkInfo.getKvnr()).thenReturn(kvnr);
    when(mockPoppTokenService.requestPoppToken(vsdmClientConfig, iknr, kvnr))
        .thenReturn(mockedToken);

    Optional<PoppToken> result = strategy.get(terminalId, egkSlotId, attachedCard);

    assertTrue(result.isPresent());
    assertEquals(mockedToken, result.get().value());
    verify(poppTokenRepository).put(terminalId, egkSlotId, cardId, mockedToken);
  }

  @Test
  void shouldReturnEmptyWhenEgkInfoCannotBeLoaded() throws Exception {
    String terminalId = "terminal-1";
    Integer egkSlotId = 1;
    String cardId = "card-1";

    when(vsdmClientConfig.isUseMockPoppToken()).thenReturn(true);
    when(attachedCard.getId()).thenReturn(cardId);
    when(cardTerminalService.getEgkInfo(attachedCard)).thenThrow(new CardTerminalException("boom"));

    Optional<PoppToken> result = strategy.get(terminalId, egkSlotId, attachedCard);

    assertTrue(result.isEmpty());
    verifyNoInteractions(mockPoppTokenService);
    verify(poppTokenRepository).put(terminalId, egkSlotId, cardId, null);
  }
}
