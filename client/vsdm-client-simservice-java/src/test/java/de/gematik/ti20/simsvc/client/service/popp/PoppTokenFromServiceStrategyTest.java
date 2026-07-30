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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PoppTokenFromServiceStrategyTest {

  @Mock private PoppClientAdapter poppClientAdapter;
  @Mock private PoppTokenRepository poppTokenRepository;
  @Mock private AttachedCard attachedCard;

  private PoppTokenFromServiceStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new PoppTokenFromServiceStrategy(poppClientAdapter, poppTokenRepository);
  }

  @Test
  void shouldReturnPoppTokenAndCacheItWhenServiceReturnsToken() {
    String terminalId = "terminal-1";
    Integer egkSlotId = 1;
    String cardId = "card-1";
    String virtualCard = "virtual-card-1";
    String serviceToken = "service-token";

    when(attachedCard.getId()).thenReturn(cardId);
    when(poppClientAdapter.getPoppToken(attachedCard, virtualCard)).thenReturn(serviceToken);

    Optional<PoppToken> result = strategy.get(terminalId, egkSlotId, attachedCard, virtualCard);

    assertTrue(result.isPresent());
    assertEquals(serviceToken, result.get().value());
    verify(poppTokenRepository).put(terminalId, egkSlotId, cardId, serviceToken);
  }

  @Test
  void shouldReturnEmptyAndCacheNullWhenServiceReturnsNull() {
    String terminalId = "terminal-1";
    Integer egkSlotId = 1;
    String cardId = "card-1";

    when(attachedCard.getId()).thenReturn(cardId);
    when(poppClientAdapter.getPoppToken(attachedCard, null)).thenReturn(null);

    Optional<PoppToken> result = strategy.get(terminalId, egkSlotId, attachedCard, null);

    assertTrue(result.isEmpty());
    verify(poppTokenRepository).put(terminalId, egkSlotId, cardId, null);
  }

  @Test
  void shouldReturnEmptyWhenServiceThrowsRetryableException() {
    when(poppClientAdapter.getPoppToken(attachedCard, null))
        .thenThrow(new RuntimeException("Websocket client is not connected"));

    Optional<PoppToken> result = strategy.get("terminal-1", 1, attachedCard, null);

    assertTrue(result.isEmpty());
    verifyNoInteractions(poppTokenRepository);
  }

  @Test
  void shouldReturnEmptyWhenServiceThrowsRetryable5xxResponseException() {
    WebClientResponseException responseException =
        WebClientResponseException.create(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            HttpHeaders.EMPTY,
            "".getBytes(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8);

    when(poppClientAdapter.getPoppToken(attachedCard, null)).thenThrow(responseException);

    Optional<PoppToken> result = strategy.get("terminal-1", 1, attachedCard, null);

    assertTrue(result.isEmpty());
    verifyNoInteractions(poppTokenRepository);
  }

  @Test
  void shouldThrowResponseStatusExceptionWhenExceptionIsNotRetryable() {
    when(poppClientAdapter.getPoppToken(attachedCard, null))
        .thenThrow(new RuntimeException("boom"));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> strategy.get("terminal-1", 1, attachedCard, null));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    assertEquals("boom", ex.getReason());
  }
}
