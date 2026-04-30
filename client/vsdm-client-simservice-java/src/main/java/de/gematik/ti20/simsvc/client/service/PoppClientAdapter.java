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
import de.gematik.ti20.simsvc.client.config.PoppClientConfig;
import de.gematik.ti20.simsvc.client.service.dto.PoppClientRequest;
import de.gematik.ti20.simsvc.client.service.dto.PoppClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
public class PoppClientAdapter {

  private final PoppClientConfig poppClientConfig;

  private final WebClient webClient;

  public PoppClientAdapter(final PoppClientConfig poppClientConfig, final WebClient webClient) {
    this.poppClientConfig = poppClientConfig;
    this.webClient = webClient;
  }

  public String getPoppToken(final AttachedCard attachedCard) {
    log.info(
        "============ Starting PoPP token session for card with tokentype={} and URL={}",
        poppClientConfig.getTokenType(),
        poppClientConfig.getUrlPoppServerHttp(attachedCard));
    PoppClientRequest poppRequestPayload =
        new PoppClientRequest(poppClientConfig.getTokenType().getType(), null);

    PoppClientResponse response =
        webClient
            .post()
            .uri(poppClientConfig.getUrlPoppServerHttp(attachedCard))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(poppRequestPayload)
            .retrieve()
            .bodyToMono(PoppClientResponse.class)
            .block();

    log.info("Successfully retrieved PoPP token: {}", response.token());

    return response.token();
  }
}
