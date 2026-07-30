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

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import java.net.HttpURLConnection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class PoppTokenFromServiceStrategy {
  private static final String POPP_WEBSOCKET_NOT_CONNECTED = "Websocket client is not connected";
  private static final String POPP_PREMATURE_CLOSE =
      "Connection prematurely closed BEFORE response";

  private final PoppClientAdapter poppClientAdapter;
  private final PoppTokenRepository poppTokenRepository;

  public PoppTokenFromServiceStrategy(
      final PoppClientAdapter poppClientAdapter, final PoppTokenRepository poppTokenRepository) {
    this.poppClientAdapter = poppClientAdapter;
    this.poppTokenRepository = poppTokenRepository;
  }

  public Optional<PoppToken> get(
      final String terminalId,
      final Integer egkSlotId,
      final AttachedCard attachedCard,
      final String virtualCard) {
    try {
      final String poppTokenFromService = poppClientAdapter.getPoppToken(attachedCard, virtualCard);
      log.debug("Received PoPP token from popp service: {}", poppTokenFromService);
      poppTokenRepository.put(terminalId, egkSlotId, attachedCard.getId(), poppTokenFromService);

      return Optional.ofNullable(poppTokenFromService).map(PoppToken::new);
    } catch (final Exception e) {
      if (!isRetryablePoppTokenException(e)) {
        throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
      }
    }
    return Optional.empty();
  }

  private boolean isRetryablePoppTokenException(final Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof WebClientRequestException) {
        return true;
      }
      if (current instanceof WebClientResponseException responseException
          && responseException.getStatusCode().is5xxServerError()) {
        return true;
      }

      final String message = current.getMessage();
      if (message != null
          && (message.contains(POPP_WEBSOCKET_NOT_CONNECTED)
              || message.contains(POPP_PREMATURE_CLOSE))) {
        return true;
      }

      current = current.getCause();
    }
    return false;
  }
}
