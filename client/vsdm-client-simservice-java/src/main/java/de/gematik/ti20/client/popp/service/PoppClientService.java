/*-
 * #%L
 * PoPP Client Library
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
package de.gematik.ti20.client.popp.service;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoppClientService {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Getter private final PoppClientConfig poppClientConfig;
  @Getter private final ZetaClientService zetaClientService;
  private final CardTerminalService cardTerminalService;
  private final Map<String, PoppTokenSession> poppTokenSessions = new ConcurrentHashMap<>();

  public PoppClientService(
      final PoppClientConfig poppClientConfig,
      final ZetaClientService zetaClientService,
      final CardTerminalService cardTerminalService) {
    this.poppClientConfig = poppClientConfig;
    this.zetaClientService = zetaClientService;
    this.cardTerminalService = cardTerminalService;
  }

  public PoppClientService(
      final PoppClientConfig poppClientConfig, final ZetaClientService zetaClientService) {
    this(
        poppClientConfig,
        zetaClientService,
        new CardTerminalService(poppClientConfig.getTerminalConnectionConfigs()));
  }

  /** Returns the list of currently attached cards that are relevant for POPP processes (EGK) */
  public synchronized List<? extends AttachedCard> getAttachedCards() throws CardTerminalException {
    return cardTerminalService.getAttachedCards().stream().filter(AttachedCard::isEgk).toList();
  }

  public EgkInfo getEgkInfo(final AttachedCard attachedCard) throws CardTerminalException {
    return cardTerminalService.getEgkInfo(attachedCard);
  }

  /** Starts the session for requesting a new PoPP Token */
  public synchronized void startPoppTokenSession(
      final AttachedCard card,
      final Integer smcbSlotId,
      final PoppTokenSessionEventHandler eventHandler)
      throws PoppClientException {

    if (card == null || !card.isEgk()) {
      throw new PoppClientException("The provided card is either null or not a valid EGK.");
    }

    final String cardId = card.getId();
    final PoppTokenSession existingPoppTokenSession = poppTokenSessions.get(cardId);
    if (existingPoppTokenSession != null && existingPoppTokenSession.isPaired()) {
      log.warn(
          "Unfinished PoPP token session for card {} is already paired but will be closed now",
          existingPoppTokenSession.getAttachedCard().getId());
      existingPoppTokenSession.finish();
      poppTokenSessions.remove(cardId);
    }

    final PoppTokenSession poppTokenSession = new PoppTokenSession(card, eventHandler, this);
    poppTokenSessions.put(cardId, poppTokenSession);

    poppTokenSession.start(smcbSlotId);
  }
}
