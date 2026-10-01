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
package de.gematik.ti20.simsvc.client.service.vsdm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class VsdmDataFromCacheStrategyTest {

  @Mock private VsdmDataRepository vsdmDataRepository;
  @Mock private AttachedCard attachedCard;

  private VsdmDataFromCacheStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new VsdmDataFromCacheStrategy(vsdmDataRepository);
  }

  @Test
  void shouldReturnCachedVsdmData() {
    final String terminalId = "terminal-1";
    final Integer egkSlotId = 2;
    final String cardId = "card-123";
    final VsdmCachedValue cachedValue =
        new VsdmCachedValue("etag-123", "pruefziffer-456", "vsdm-data");
    when(attachedCard.getId()).thenReturn(cardId);
    when(vsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(cachedValue);

    final Optional<VsdmReadResult> result = strategy.get(attachedCard, terminalId, egkSlotId);

    assertThat(result)
        .contains(
            new VsdmReadResult(
                HttpStatus.OK,
                cachedValue.etag(),
                cachedValue.pruefziffer(),
                cachedValue.vsdmData(),
                null));
    verify(vsdmDataRepository).get(terminalId, egkSlotId, cardId);
  }

  @Test
  void shouldReturnEmptyWhenRepositoryContainsNoVsdmData() {
    final String terminalId = "terminal-1";
    final Integer egkSlotId = 2;
    final String cardId = "card-123";
    when(attachedCard.getId()).thenReturn(cardId);
    when(vsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

    final Optional<VsdmReadResult> result = strategy.get(attachedCard, terminalId, egkSlotId);

    assertThat(result).isEmpty();
    verify(vsdmDataRepository).get(terminalId, egkSlotId, cardId);
  }

  @Test
  void shouldReturnEmptyWhenNoCardIsAttached() {
    final Optional<VsdmReadResult> result = strategy.get(null, "terminal-1", 2);

    assertThat(result).isEmpty();
    verifyNoInteractions(vsdmDataRepository);
  }
}
