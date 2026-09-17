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
package de.gematik.ti20.simsvc.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.card.CardTerminalConnectionConfig;
import de.gematik.ti20.simsvc.client.card.EgkInfo;
import de.gematik.ti20.simsvc.client.card.SmcbInfo;
import de.gematik.ti20.simsvc.client.exception.CardTerminalException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class CardTerminalService {

  private final List<CardTerminalConnectionConfig> connectionConfigs = new ArrayList<>();

  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  public CardTerminalService() {
    this.httpClient = new OkHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  public List<CardTerminalConnectionConfig> getTerminalConnectionConfigs() {
    return connectionConfigs;
  }

  public void setTerminalConnectionConfigs(final List<CardTerminalConnectionConfig> configs) {
    if (configs != null) {
      connectionConfigs.clear();
      connectionConfigs.addAll(configs);
    } else {
      log.warn("Attempted to add null terminal connection configuration");
    }
  }

  /**
   * Returns a list of all cards currently attached to all available terminals based on the provided
   * configurations.
   *
   * @return list of attached cards
   */
  public List<AttachedCard> getAttachedCards() throws CardTerminalException {
    final List<AttachedCard> cards = new ArrayList<>();
    for (var connectionConfig : connectionConfigs) {
      cards.addAll(getAttachedCards(connectionConfig));
    }

    return cards;
  }

  private List<AttachedCard> getAttachedCards(final CardTerminalConnectionConfig connectionConfig)
      throws CardTerminalException {
    log.info("Getting available cards from CardSimulator at {}", connectionConfig.getUrl());
    final Request request =
        new Request.Builder().url(connectionConfig.getUrl() + "/cards/").get().build();

    log.debug("Sending request to get available cards");
    final Call call = httpClient.newCall(request);
    try {
      final Response response = call.execute();
      final String responseBody = response.body().string();
      log.debug("Received response for available cards: {}", responseBody);

      final Map<String, Object>[] cardValues = objectMapper.readValue(responseBody, Map[].class);
      final List<AttachedCard> cards = new ArrayList<>();
      for (final Map<String, Object> cardValue : cardValues) {
        cards.add(AttachedCard.from(connectionConfig.getUrl(), cardValue));
      }

      return cards;
    } catch (final Exception e) {
      throw new CardTerminalException(
          "Failed to get available cards. Status code: " + e.getMessage());
    }
  }

  public AttachedCard getAttachedCard(final String terminalId, final Integer slotId) {
    log.debug("Getting attached card for terminal ID: {}, slot ID: {}", terminalId, slotId);

    List<? extends AttachedCard> cards = null;

    try {
      cards = getAttachedCards();
    } catch (final Exception e) {
      log.error("Error getting attached EGK cards from terminal", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }

    final AttachedCard attachedCard =
        cards.stream()
            .filter(card -> card.getSlotId().equals(slotId))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No card found in slot " + slotId));

    log.debug("Using card with ID: {}", attachedCard.getId());

    return attachedCard;
  }

  public EgkInfo getEgkInfo(final AttachedCard attachedCard) throws CardTerminalException {
    return getCardInfoDto(attachedCard, "/egk-info", EgkInfo.class);
  }

  public SmcbInfo getSmcbInfo() throws CardTerminalException {
    final List<AttachedCard> cards = getAttachedCards();
    final AttachedCard attachedCard =
        cards.stream()
            .filter(card -> "SMCB".equalsIgnoreCase(card.getCardType()))
            .findFirst()
            .orElseThrow(() -> new CardTerminalException("no smc-b found"));
    return getCardInfoDto(attachedCard, "/smc-b-info", SmcbInfo.class);
  }

  private <T> T getCardInfoDto(
      final AttachedCard attachedCard, final String url, final Class<T> dtoType)
      throws CardTerminalException {
    final Request request =
        new Request.Builder()
            .url(attachedCard.getTerminalUrl() + "/cards/" + attachedCard.getId() + url)
            .get()
            .build();

    log.debug(
        "Getting card info for card: {} on terminal: {}",
        attachedCard.getId(),
        attachedCard.getTerminalUrl());
    try {
      final Response response = httpClient.newCall(request).execute();

      final String responseBody = response.body().string();
      log.debug("Received card info: {}", responseBody);

      return objectMapper.readValue(responseBody, dtoType);
    } catch (final Exception e) {
      throw new CardTerminalException("Failed to get EGK info: " + e.getMessage());
    }
  }
}
