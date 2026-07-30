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

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.simsvc.client.service.popp.PoppClientAdapter;
import de.gematik.ti20.simsvc.client.service.popp.PoppToken;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromCacheStrategy;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromInjectedStrategy;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromMockedStrategy;
import de.gematik.ti20.simsvc.client.service.popp.PoppTokenFromServiceStrategy;
import de.gematik.ti20.vsdm.fhir.builder.VsdmBundleBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPatientBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import io.ktor.client.plugins.ClientRequestException;
import io.ktor.client.plugins.ServerResponseException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class VsdmClientService {

  public static final String HEADER_VSDM_PZ = "vsdm-pz";
  public static final String HEADER_ETAG = "etag";

  private final CardTerminalService cardTerminalService;
  private final VsdmDataRepository vsdmDataRepository;
  private final ZetaSdkClientAdapter vsdmZetaClient;

  private final PoppTokenFromInjectedStrategy poppTokenFromInjected;
  private final PoppTokenFromMockedStrategy poppTokenFromMocked;
  private final PoppTokenFromCacheStrategy poppTokenFromCache;
  private final PoppTokenFromServiceStrategy poppTokenFromService;

  @Getter private List<CardTerminalConnectionConfig> terminalConnectionConfigs;

  private final FhirService fhirService;

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
    this.fhirService = fhirService;

    this.vsdmDataRepository = vsdmDataRepository;

    this.terminalConnectionConfigs = new ArrayList<>();
    this.vsdmZetaClient = vsdmZetaClient;

    this.poppTokenFromInjected = new PoppTokenFromInjectedStrategy();
    this.poppTokenFromMocked =
        new PoppTokenFromMockedStrategy(
            vsdmClientConfig, cardTerminalService, mockPoppTokenService, poppTokenRepository);
    this.poppTokenFromCache = new PoppTokenFromCacheStrategy(poppTokenRepository);
    this.poppTokenFromService =
        new PoppTokenFromServiceStrategy(poppClientAdapter, poppTokenRepository);
  }

  public ResponseEntity<String> read(
      final String terminalId,
      final int egkSlotId,
      final String virtualCard,
      final boolean isFhirXml,
      final String poppTokenInjected,
      final String ifNoneMatch,
      final String profileVersion) {
    log.info(
        "read initiated with terminalId = {}, egkSlotId={}, if-none-match={}, poppTokenInjected={}, profileVersion={}",
        terminalId,
        egkSlotId,
        ifNoneMatch,
        poppTokenInjected != null,
        profileVersion);
    final AttachedCard attachedCard =
        poppTokenInjected != null ? null : getAttachedCard(terminalId, egkSlotId);

    final PoppToken poppToken =
        requestPoppToken(poppTokenInjected, terminalId, egkSlotId, attachedCard, virtualCard);
    log.debug("Received PoPP token: {}", poppToken.value());

    final ResponseEntity<String> vsd =
        requestVsd(
            terminalId, egkSlotId, attachedCard, poppToken, ifNoneMatch, isFhirXml, profileVersion);
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

    final PoppToken poppToken =
        maybePoppToken.orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve PoPP token"));

    return poppToken;
  }

  public AttachedCard getAttachedCard(final String terminalId, final Integer slotId) {
    log.debug("Getting attached card for terminal ID: {}, slot ID: {}", terminalId, slotId);

    List<? extends AttachedCard> cards = null;

    try {
      cards = cardTerminalService.getAttachedCards();
    } catch (final Exception e) {
      log.error("Error getting attached EGK cards from terminal", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }

    final AttachedCard attachedCard =
        cards.stream()
            .filter(card -> ((SimulatorAttachedCard) card).getSlotId().equals(slotId))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No card found in slot " + slotId));

    log.debug("Using card with ID: {}", attachedCard.getId());

    return attachedCard;
  }

  // vsdm handling

  protected ResponseEntity<String> requestVsd(
      final String terminal,
      final int egkSlotId,
      final AttachedCard attachedCard,
      final PoppToken poppToken,
      final String ifNoneMatch,
      final boolean isFhirXml,
      final String profileVersion) {

    if (attachedCard != null) {
      final VsdmCachedValue vsdmCachedValue =
          vsdmDataRepository.get(terminal, egkSlotId, attachedCard.getId());

      if (vsdmCachedValue != null) {
        return ResponseEntity.status(HttpStatus.OK)
            .header(HEADER_VSDM_PZ, vsdmCachedValue.pruefziffer())
            .header(HEADER_ETAG, vsdmCachedValue.etag())
            .body(vsdmCachedValue.vsdmData());
      }
    }

    try {
      final String traceId = MDC.get("traceId");
      final ZetaSdkClientAdapter.RequestParameters requestParameters =
          new ZetaSdkClientAdapter.RequestParameters(
              traceId, poppToken.value(), isFhirXml, ifNoneMatch);
      final String baseUrl = "vsdservice/v1/vsdmbundle";
      final String url =
          profileVersion != null ? baseUrl + "?profileVersion=" + profileVersion : baseUrl;
      final ZetaSdkClientAdapter.Response responseFromServer =
          vsdmZetaClient.httpGet(url, requestParameters);

      responseFromServer
          .headers()
          .forEach((key, value) -> log.debug("Header from VSDM response: {}: {}", key, value));

      final boolean isNotModified =
          responseFromServer.statusCode().isSameCodeAs(HttpStatus.NOT_MODIFIED);
      if (!responseFromServer.statusCode().is2xxSuccessful() && !isNotModified) {
        return ResponseEntity.status(responseFromServer.statusCode())
            .headers(copyApplicableHeaders(responseFromServer))
            .body(responseFromServer.body());
      }

      if (isNotModified) {
        return handleNotModified(terminal, egkSlotId, attachedCard, responseFromServer);
      }

      final HttpHeaders responseHeaders = copyApplicableHeaders(responseFromServer);
      final String responseToCaller = responseFromServer.body();

      if (responseToCaller == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Could not parse valid FHIR response");
      }

      // Only cache if attachedCard is available
      if (attachedCard != null) {
        vsdmDataRepository.put(
            terminal,
            egkSlotId,
            attachedCard.getId(),
            new VsdmCachedValue(
                responseHeaders.getETag(),
                responseHeaders.getFirst(HEADER_VSDM_PZ),
                responseToCaller));
      }

      return ResponseEntity.status(HttpStatus.OK).headers(responseHeaders).body(responseToCaller);

    } catch (final ClientRequestException e) {
      final int responseStatus = e.getResponse().getStatus().getValue();
      return ResponseEntity.status(responseStatus).body(e.getMessage());
    } catch (final ServerResponseException e) {
      log.error("Error while connecting to VSDM server: {}", e.getMessage(), e);

      if (attachedCard == null) {
        // No fallback available when using provided token
        throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
      }
      // Fallback to card data only if attachedCard is available
      try {
        final String responseToCaller = loadTruncatedDataFromCard(attachedCard);
        if (responseToCaller == null) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.OK).body(responseToCaller);
      } catch (final CardTerminalException cardEx) {
        log.error("Error while loading truncated data from card: {}", cardEx.getMessage(), cardEx);
        throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Thread interrupted while requesting VsdBundle with token", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    } catch (final Exception e) {
      log.error("Error on requesting VsdBundle with token", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }
  }

  /** Process a 304 from the VSDM backend and update the cache accordingly. */
  private @NotNull ResponseEntity<String> handleNotModified(
      final String terminal,
      final Integer egkSlotId,
      final AttachedCard attachedCard,
      final ZetaSdkClientAdapter.Response responseFromServer) {
    final HttpHeaders responseHeaders = copyApplicableHeaders(responseFromServer);
    final String etagHeader = responseFromServer.headers().get(HEADER_ETAG);
    Objects.requireNonNull(
        etagHeader, "'%s' header must be set by VSDM backend on 304".formatted(HEADER_ETAG));

    final String checkDigitHeader =
        responseFromServer.headers().get(HEADER_VSDM_PZ) != null
            ? responseFromServer.headers().get(HEADER_VSDM_PZ)
            : responseFromServer.headers().get(HEADER_VSDM_PZ.toLowerCase());
    Objects.requireNonNull(
        checkDigitHeader,
        "'%s' header must be set by VSDM backend on 304".formatted(HEADER_VSDM_PZ));

    if (attachedCard != null) {
      final VsdmCachedValue cachedValue =
          vsdmDataRepository.get(terminal, egkSlotId, attachedCard.getId());
      final VsdmCachedValue updatedCacheValue;
      if (cachedValue == null) {
        updatedCacheValue = new VsdmCachedValue(etagHeader, checkDigitHeader, "");
      } else {
        updatedCacheValue = cachedValue.copyWith(etagHeader, checkDigitHeader);
      }
      vsdmDataRepository.put(terminal, egkSlotId, attachedCard.getId(), updatedCacheValue);
    }

    return ResponseEntity.status(HttpStatus.NOT_MODIFIED).headers(responseHeaders).build();
  }

  public String loadTruncatedDataFromCard(final AttachedCard attachedCard)
      throws CardTerminalException {
    final EgkInfo egkInfo = cardTerminalService.getEgkInfo(attachedCard);

    if (!egkInfo.getValid()) {
      return null;
    }

    // send 401, falls nicht valid
    final VsdmBundle truncatedDataBundle =
        VsdmBundleBuilder.create()
            .addEntry(
                VsdmPatientBuilder.create()
                    .withKvnr(egkInfo.getKvnr())
                    .withNames(egkInfo.getLastName(), egkInfo.getFirstName())
                    .build())
            .build();

    return fhirService.encodeResponse(truncatedDataBundle, EncodingType.JSON);
  }

  private HttpHeaders copyApplicableHeaders(
      final ZetaSdkClientAdapter.Response responseFromServer) {
    final HttpHeaders responseHeaders = new HttpHeaders();
    responseFromServer
        .headers()
        .forEach(
            (key, value) -> {
              if (key.equalsIgnoreCase(HEADER_VSDM_PZ)
                  || key.equalsIgnoreCase(HEADER_ETAG)
                  || key.equalsIgnoreCase("Content-Type")
                  || key.equalsIgnoreCase("Content-Length")) {
                responseHeaders.put(key, List.of(value));
              }
            });

    return responseHeaders;
  }

  public void setTerminalConnectionConfigs(final List<CardTerminalConnectionConfig> configs) {
    log.debug("Setting terminal connection configs: ", terminalConnectionConfigs);

    terminalConnectionConfigs = configs;
    cardTerminalService.setTerminalConnectionConfigs(terminalConnectionConfigs);
  }
}
