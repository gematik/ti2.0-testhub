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

import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.simsvc.client.service.popp.PoppClientAdapter;
import de.gematik.ti20.simsvc.client.service.popp.PoppToken;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromCacheStrategy;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromInjectedStrategy;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromMockedStrategy;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromServiceStrategy;
import de.gematik.ti20.simsvc.client.service.vsdm.VsdmDataFromCacheStrategy;
import de.gematik.ti20.simsvc.client.service.vsdm.VsdmDataFromCardStrategy;
import de.gematik.ti20.simsvc.client.service.vsdm.VsdmDataFromServiceStrategy;
import de.gematik.ti20.simsvc.client.service.vsdm.VsdmReadResult;
import java.net.HttpURLConnection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class VsdmClientService {

  private final CardTerminalService cardTerminalService;
  private final VsdmDataRepository vsdmDataRepository;
  private final ZetaSdkClientAdapter vsdmZetaClient;

  private final PoppTokenFromInjectedStrategy poppTokenFromInjected;
  private final PoppTokenFromMockedStrategy poppTokenFromMocked;
  private final PoppTokenFromCacheStrategy poppTokenFromCache;
  private final PoppTokenFromServiceStrategy poppTokenFromService;

  private final VsdmDataFromCacheStrategy vsdmDataFromCacheStrategy;
  private final VsdmDataFromServiceStrategy vsdmDataFromServiceStrategy;
  private final VsdmDataFromCardStrategy vsdmDataFromCardStrategy;

  public VsdmClientService(
      final VsdmClientConfig vsdmClientConfig,
      final MockPoppTokenService mockPoppTokenService,
      final CardTerminalService cardTerminalService,
      final PoppClientAdapter poppClientAdapter,
      final FhirService fhirService,
      final PoppTokenRepository poppTokenRepository,
      final VsdmDataRepository vsdmDataRepository,
      final ZetaSdkClientAdapter vsdmZetaClient) {

    this.cardTerminalService = cardTerminalService;

    this.vsdmDataRepository = vsdmDataRepository;

    this.vsdmZetaClient = vsdmZetaClient;

    this.poppTokenFromInjected = new PoppTokenFromInjectedStrategy();
    this.poppTokenFromMocked =
        new PoppTokenFromMockedStrategy(
            vsdmClientConfig, cardTerminalService, mockPoppTokenService, poppTokenRepository);
    this.poppTokenFromCache = new PoppTokenFromCacheStrategy(poppTokenRepository);
    this.poppTokenFromService =
        new PoppTokenFromServiceStrategy(poppClientAdapter, poppTokenRepository);

    this.vsdmDataFromCacheStrategy = new VsdmDataFromCacheStrategy(vsdmDataRepository);
    this.vsdmDataFromServiceStrategy =
        new VsdmDataFromServiceStrategy(vsdmDataRepository, vsdmZetaClient);
    this.vsdmDataFromCardStrategy = new VsdmDataFromCardStrategy(cardTerminalService, fhirService);
  }

  public VsdmReadResult read(
      final String terminalId,
      final int egkSlotId,
      final String virtualCard,
      final boolean isFhirXml,
      final boolean skipPoppTokenHeader,
      final String poppTokenInjected,
      final String ifNoneMatch,
      final String profileVersion) {
    log.info(
        "read initiated with terminalId = {}, egkSlotId={}, if-none-match={}, skipPoppTokenHeader={}, poppTokenInjected={}, profileVersion={}",
        terminalId,
        egkSlotId,
        ifNoneMatch,
        skipPoppTokenHeader,
        poppTokenInjected != null,
        profileVersion);

    final AttachedCard attachedCard =
        poppTokenInjected != null
            ? null
            : cardTerminalService.getAttachedCard(terminalId, egkSlotId);

    final PoppToken poppToken =
        requestPoppToken(poppTokenInjected, terminalId, egkSlotId, attachedCard, virtualCard);
    log.debug("Received PoPP token: {}", poppToken.value());

    final VsdmReadResult vsd =
        requestVsd(
            terminalId,
            egkSlotId,
            attachedCard,
            poppToken,
            ifNoneMatch,
            isFhirXml,
            skipPoppTokenHeader,
            profileVersion);
    log.debug("Received VSD: {}", vsd);

    return vsd;
  }

  protected PoppToken requestPoppToken(
      final String poppTokenInjected,
      final String terminalId,
      final int egkSlotId,
      final AttachedCard attachedCard,
      final String virtualCard) {

    log.info(
        "Requesting PoPP token for attached card: {}",
        attachedCard != null ? attachedCard.getId() : "none");

    final Optional<PoppToken> maybePoppToken =
        poppTokenFromInjected
            .get(poppTokenInjected)
            .or(() -> poppTokenFromMocked.get(terminalId, egkSlotId, attachedCard))
            .or(() -> poppTokenFromCache.get(terminalId, egkSlotId, attachedCard))
            .or(() -> poppTokenFromService.get(terminalId, egkSlotId, attachedCard, virtualCard))
            .or(() -> poppTokenFromService.get(terminalId, egkSlotId, attachedCard, virtualCard))
            .or(() -> poppTokenFromService.get(terminalId, egkSlotId, attachedCard, virtualCard));

    return maybePoppToken.orElseThrow(
        () ->
            new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve PoPP token"));
  }

  protected VsdmReadResult requestVsd(
      final String terminal,
      final int egkSlotId,
      final AttachedCard attachedCard,
      final PoppToken poppToken,
      final String ifNoneMatch,
      final boolean isFhirXml,
      final boolean skipPoppTokenHeader,
      final String profileVersion) {

    final Optional<VsdmReadResult> maybeVsdmReadResult =
        vsdmDataFromCacheStrategy
            .get(attachedCard, terminal, egkSlotId)
            .or(
                () ->
                    vsdmDataFromServiceStrategy.get(
                        terminal,
                        egkSlotId,
                        attachedCard,
                        isFhirXml,
                        skipPoppTokenHeader,
                        poppToken.value(),
                        ifNoneMatch,
                        profileVersion))
            .or(() -> vsdmDataFromCardStrategy.get(attachedCard));

    return maybeVsdmReadResult.orElseThrow(
        () ->
            new ResponseStatusException(
                HttpURLConnection.HTTP_INTERNAL_ERROR, "Could not retrieve VSDM data", null));
  }
}
