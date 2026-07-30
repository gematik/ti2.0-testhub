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
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PoppTokenFromCacheStrategyTest {

  @Mock private PoppTokenRepository poppTokenRepository;
  @Mock private AttachedCard attachedCard;

  private PoppTokenFromCacheStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new PoppTokenFromCacheStrategy(poppTokenRepository);
  }

  @Test
  void shouldReturnEmptyWhenRepositoryHasNoToken() {
    String terminalId = "terminal-1";
    Integer egkSlotId = 2;
    String cardId = "card-123";

    when(attachedCard.getId()).thenReturn(cardId);
    when(poppTokenRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

    Optional<PoppToken> result = strategy.get(terminalId, egkSlotId, attachedCard);

    assertTrue(result.isEmpty());
    verify(poppTokenRepository).get(terminalId, egkSlotId, cardId);
  }

  @Test
  void shouldReturnPoppTokenWhenRepositoryContainsToken() {
    String terminalId = "terminal-1";
    Integer egkSlotId = 2;
    String cardId = "card-123";
    String cachedToken = "cached-popp-token";

    when(attachedCard.getId()).thenReturn(cardId);
    when(poppTokenRepository.get(terminalId, egkSlotId, cardId)).thenReturn(cachedToken);

    Optional<PoppToken> result = strategy.get(terminalId, egkSlotId, attachedCard);

    assertTrue(result.isPresent());
    assertEquals(cachedToken, result.get().value());
    verify(poppTokenRepository).get(terminalId, egkSlotId, cardId);
  }
}
