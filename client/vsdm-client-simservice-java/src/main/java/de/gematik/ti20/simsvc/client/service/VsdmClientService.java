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

import com.google.common.base.Strings;
import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.rest.fd.MediaType;
import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.message.TokenMessage;
import de.gematik.ti20.client.popp.service.PoppTokenSession;
import de.gematik.ti20.client.popp.service.PoppTokenSessionEventHandler;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.vsdm.fhir.builder.VsdmBundleBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPatientBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import io.ktor.client.plugins.ClientRequestException;
import io.ktor.client.plugins.ServerResponseException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
public class VsdmClientService implements PoppTokenSessionEventHandler {

  public static final String HEADER_VSDM_PZ = "VSDM-Pz";
  public static final String HEADER_ETAG = "etag";

  private final PoppClientAdapter poppClientAdapter;
  private final VsdmClientConfig vsdmClientConfig;

  private final ZetaClientService poppZetaClient;
  private final MockPoppTokenService mockPoppTokenService;

  private final PoppTokenRepository poppTokenRepository;
  private final VsdmDataRepository vsdmDataRepository;
  private final ZetaSdkClientAdapter vsdmZetaClient;

  private List<CardTerminalConnectionConfig> terminalConnectionConfigs;

  private final FhirService fhirService;

  private final Map<String, CompletableFuture<TokenMessage>> tokenFutures =
      new ConcurrentHashMap<>();

  public VsdmClientService(
      final VsdmClientConfig vsdmClientConfig,
      final MockPoppTokenService mockPoppTokenService,
      final PoppClientAdapter poppClientAdapter,
      final FhirService fhirService,
      final PoppTokenRepository poppTokenRepository,
      final VsdmDataRepository vsdmDataRepository,
      final ZetaSdkClientAdapter vsdmZetaClient) {

    this.vsdmClientConfig = vsdmClientConfig;

    this.poppZetaClient = poppClientAdapter.getZetaClientService();
    this.mockPoppTokenService = mockPoppTokenService;

    this.poppClientAdapter = poppClientAdapter;
    this.fhirService = fhirService;

    this.poppTokenRepository = poppTokenRepository;
    this.vsdmDataRepository = vsdmDataRepository;

    this.terminalConnectionConfigs = new ArrayList<>();
    this.vsdmZetaClient = vsdmZetaClient;
  }

  public ResponseEntity<String> read(
      final String terminalId,
      final int egkSlotId,
      final int smcbSlotId,
      final boolean isFhirXml,
      final String poppTokenInjected,
      final String ifNoneMatch) {
    log.info(
        "read initiated with terminalId = {}, egkSlotId={}, smcBSlotId = {}, if-none-match={}, poppTokenInjected={}",
        terminalId,
        egkSlotId,
        smcbSlotId,
        ifNoneMatch,
        poppTokenInjected != null);

    final AttachedCard attachedCard;

    if (poppTokenInjected != null && !poppTokenInjected.isEmpty()) {
      log.debug("Using provided PoPP token, skipping Popp-Service call");
      attachedCard = null;
      // poppToken = poppTokenInjected;
    } else {
      attachedCard = getAttachedCard(terminalId, egkSlotId);
      // poppToken = requestPoppToken(terminalId, egkSlotId, smcbSlotId, attachedCard);
    }

    final String poppToken =
        Optional.ofNullable(poppTokenInjected)
            .orElseGet(() -> requestPoppToken(terminalId, egkSlotId, smcbSlotId, attachedCard));
    log.debug("Received PoPP token: {}", poppToken);

    final ResponseEntity<String> vsd =
        requestVsd(terminalId, egkSlotId, attachedCard, poppToken, ifNoneMatch, isFhirXml);
    log.debug("Received VSD: {}", vsd);

    return vsd;
  }

  public AttachedCard getAttachedCard(final String terminalId, final Integer slotId) {
    log.debug("Getting attached card for terminal ID: {}, slot ID: {}", terminalId, slotId);

    List<? extends AttachedCard> cards = null;

    try {
      cards = poppClientAdapter.getAttachedCards();
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

  protected String requestPoppToken(
      final String terminalId,
      final int egkSlotId,
      final int smcbSlotId,
      final AttachedCard attachedCard) {
    log.info("Requesting PoPP token for attached card: {}", attachedCard.getId());

    if (vsdmClientConfig.isUseMockPoppToken()) {
      log.info("Load mocked PoPP token");
      final String mockPoppToken = loadMockPoppToken(vsdmClientConfig, attachedCard);
      poppTokenRepository.put(terminalId, egkSlotId, attachedCard.getId(), mockPoppToken);
      return mockPoppToken;
    }

    final String poppTokenFromRepository =
        poppTokenRepository.get(terminalId, egkSlotId, attachedCard.getId());

    if (poppTokenFromRepository != null) {
      log.debug("PoPP token found in repository: {}", poppTokenFromRepository);
      return poppTokenFromRepository;
    }

    try {
      final String poppTokenFromService =
          poppClientAdapter.getPoppToken(attachedCard, smcbSlotId, this);
      log.debug("Received PoPP token from popp service: {}", poppTokenFromService);
      poppTokenRepository.put(terminalId, egkSlotId, attachedCard.getId(), poppTokenFromService);

      return poppTokenFromService;
    } catch (final PoppClientException e) {
      log.error("Error starting PoppTokenSession with card {}", attachedCard.getId(), e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    } catch (final Exception e) {
      log.error(
          "Error on waiting for completing of PoppTokenSession with card {} ",
          attachedCard.getId(),
          e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }
  }

  protected ResponseEntity<String> requestVsd(
      final String terminal,
      final int egkSlotId,
      final AttachedCard attachedCard,
      final String poppToken,
      final String ifNoneMatch,
      final boolean isFhirXml) {

    // Skip cache if attachedCard is null (when poppToken is provided externally)
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
          new ZetaSdkClientAdapter.RequestParameters(traceId, poppToken, isFhirXml, ifNoneMatch);
      final ZetaSdkClientAdapter.Response responseFromServer =
          vsdmZetaClient.httpGet("vsdservice/v1/vsdmbundle", requestParameters);

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
      final String responseToCaller = encodeVsdmBundle(isFhirXml, responseFromServer.body());

      if (responseToCaller == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Could not parse valid FHIR response");
      }
      responseHeaders.put("Content-Type", List.of(MediaType.FHIR_JSON.asString()));

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

      // Fallback to card data only if attachedCard is available
      if (attachedCard != null) {
        try {
          final String responseToCaller = loadTruncatedDataFromCard(attachedCard);
          if (responseToCaller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
          }
          return ResponseEntity.status(HttpStatus.OK).body(responseToCaller);
        } catch (final CardTerminalException cardEx) {
          log.error(
              "Error while loading truncated data from card: {}", cardEx.getMessage(), cardEx);
          throw new ResponseStatusException(
              HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
        }
      } else {
        // No fallback available when using provided token
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

    // Only update cache if attachedCard is available
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

  private String encodeVsdmBundle(final boolean isFhirXml, final String body) {
    if (Strings.isNullOrEmpty(body)) {
      return null;
    }

    final VsdmBundle vsdmBundle =
        fhirService.parseString(body, isFhirXml ? "xml" : "json", VsdmBundle.class);
    return fhirService.encodeResponse(vsdmBundle, isFhirXml ? EncodingType.XML : EncodingType.JSON);
  }

  public String loadTruncatedDataFromCard(final AttachedCard attachedCard)
      throws CardTerminalException {
    final EgkInfo egkInfo = poppClientAdapter.getEgkInfo(attachedCard);

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

  private String loadMockPoppToken(final VsdmClientConfig config, final AttachedCard attachedCard) {
    try {
      final EgkInfo egkInfo = poppClientAdapter.getEgkInfo(attachedCard);
      return mockPoppTokenService.requestPoppToken(config, egkInfo.getIknr(), egkInfo.getKvnr());
    } catch (final CardTerminalException cardEx) {
      return null;
    }
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

  public List<CardTerminalConnectionConfig> getTerminalConnectionConfigs() {
    return terminalConnectionConfigs;
  }

  public void setTerminalConnectionConfigs(final List<CardTerminalConnectionConfig> configs) {
    log.debug("Setting terminal connection configs: ", terminalConnectionConfigs);

    terminalConnectionConfigs = configs;

    poppZetaClient.getZetaClientConfig().setTerminalConnectionConfigs(configs);
    poppClientAdapter.getPoppClientConfig().setTerminalConnectionConfigs(configs);
  }

  @Override
  public void onConnectedToTerminalSlot(final PoppTokenSession pts) {
    log.debug("onConnectedToTerminalSlot: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onDisconnectedFromTerminalSlot(final PoppTokenSession pts) {
    log.debug("onDisconnectedFromTerminalSlot: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onCardInserted(final PoppTokenSession pts) {
    log.debug("onCardInserted: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onCardRemoved(final PoppTokenSession pts) {
    log.debug("onCardRemoved: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onCardPairedToServer(final PoppTokenSession pts) {
    log.debug("onCardPairedToServer: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onConnectedToServer(final PoppTokenSession pts) {
    log.debug("onConnectedToServer: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onDisconnectedFromServer(final PoppTokenSession pts) {
    log.debug("onDisconnectedFromServer: {}", pts.getAttachedCard().getId());
  }

  @Override
  public void onError(PoppTokenSession poppTokenSession, PoppClientException e) {
    log.error(
        "Error in PoppTokenSession for card {}: {}",
        poppTokenSession.getAttachedCard().getId(),
        e.getMessage(),
        e);

    final String cardId = poppTokenSession.getAttachedCard().getId();
    final CompletableFuture<TokenMessage> poppTokenFuture = tokenFutures.remove(cardId);

    if (poppTokenFuture != null) {
      poppTokenFuture.completeExceptionally(e);
    }
  }

  @Override
  public void onReceivedPoppToken(PoppTokenSession poppTokenSession, TokenMessage tokenMessage) {
    log.debug("onReceivedPoppToken: {}", poppTokenSession.getAttachedCard().getId());

    final String cardId = poppTokenSession.getAttachedCard().getId();
    final CompletableFuture<TokenMessage> poppTokenFuture = tokenFutures.remove(cardId);

    if (poppTokenFuture != null) {
      poppTokenFuture.complete(tokenMessage);
    }
  }

  @Override
  public void onFinished(PoppTokenSession poppTokenSession) {
    log.debug("onFinished: {}", poppTokenSession.getAttachedCard().getId());
  }
}
