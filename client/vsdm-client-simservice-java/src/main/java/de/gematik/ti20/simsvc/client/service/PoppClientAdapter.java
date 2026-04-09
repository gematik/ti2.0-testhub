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

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.service.PoppClientService;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import de.gematik.ti20.simsvc.client.service.dto.PoppClientRequest;
import de.gematik.ti20.simsvc.client.service.dto.PoppClientResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
public class PoppClientAdapter {
  private final PoppClientService poppClientService;
  private final WebClient webClient;

  public PoppClientAdapter(PoppClientService poppClientService, WebClient webClient) {
    this.poppClientService = poppClientService;
    this.webClient = webClient;
  }

  public List<? extends AttachedCard> getAttachedCards() throws CardTerminalException {
    return poppClientService.getAttachedCards();
  }

  public String getPoppToken(
      AttachedCard attachedCard, Integer smcbSlotId, VsdmClientService vsdmClientService)
      throws PoppClientException {
    log.info(
        "============ Starting PoPP token session for card with tokentype={} and URL={}",
        getPoppClientConfig().getTokenType(),
        getPoppClientConfig().getUrlPoppServerHttp(attachedCard));
    PoppClientRequest poppRequestPayload =
        new PoppClientRequest(getPoppClientConfig().getTokenType().getType(), null);

    PoppClientResponse response =
        webClient
            .post()
            .uri(getPoppClientConfig().getUrlPoppServerHttp(attachedCard))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(poppRequestPayload)
            .retrieve()
            .bodyToMono(PoppClientResponse.class)
            .block();

    if (response == null
        || response.status()
            != de.gematik.ti20.client.popp.controller.PoppClientResponseStatus.OK) {
      log.error(
          "Failed to retrieve PoPP token. Response: {}",
          response != null ? response.toString() : "null");
      throw new PoppClientException("POPP_TOKEN_SESSION_ERROR");
    }
    log.info("Successfully retrieved PoPP token: {}", response.token());

    return response.token();
  }

  public EgkInfo getEgkInfo(AttachedCard attachedCard) throws CardTerminalException {
    return poppClientService.getEgkInfo(attachedCard);
  }

  public ZetaClientService getZetaClientService() {
    return this.poppClientService.getZetaClientService();
  }

  public PoppClientConfig getPoppClientConfig() {
    return poppClientService.getPoppClientConfig();
  }
}
