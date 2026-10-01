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

import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.exception.VsdmServerException;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.simsvc.client.service.ZetaSdkClientAdapter;
import io.ktor.client.plugins.ClientRequestException;
import io.ktor.client.plugins.ServerResponseException;
import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class VsdmDataFromServiceStrategy {

  private static final String HEADER_VSDM_PZ = "vsdm-pz";
  private static final String HEADER_ETAG = "etag";

  private final VsdmDataRepository vsdmDataRepository;
  private final ZetaSdkClientAdapter vsdmZetaClient;

  public VsdmDataFromServiceStrategy(
      final VsdmDataRepository vsdmDataRepository, final ZetaSdkClientAdapter vsdmZetaClient) {
    this.vsdmDataRepository = vsdmDataRepository;
    this.vsdmZetaClient = vsdmZetaClient;
  }

  public Optional<VsdmReadResult> get(
      final String terminalId,
      final int egkSlotId,
      final AttachedCard attachedCard,
      final boolean isFhirXml,
      final boolean skipPoppTokenHeader,
      final String poppToken,
      final String ifNoneMatch,
      final String profileVersion) {
    try {
      final String traceId = MDC.get("traceId");
      final ZetaSdkClientAdapter.RequestParameters requestParameters =
          new ZetaSdkClientAdapter.RequestParameters(
              traceId, poppToken, isFhirXml, skipPoppTokenHeader, ifNoneMatch);
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
        throw new VsdmServerException(
            responseFromServer.statusCode(),
            responseFromServer.headers(),
            responseFromServer.body());
      }

      if (isNotModified) {
        return Optional.of(
            handleNotModified(terminalId, egkSlotId, attachedCard, responseFromServer));
      }

      final String responseToCaller = responseFromServer.body();

      if (responseToCaller == null) {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "Could not parse valid FHIR response");
      }

      final String etag = getHeaderValue(responseFromServer, HEADER_ETAG);
      final String pruefziffer = getHeaderValue(responseFromServer, HEADER_VSDM_PZ);
      final String contentType = getHeaderValue(responseFromServer, HttpHeaders.CONTENT_TYPE);

      maybeUpdateCache(
          terminalId,
          egkSlotId,
          attachedCard,
          new VsdmCachedValue(etag, pruefziffer, responseToCaller));

      return Optional.of(
          new VsdmReadResult(HttpStatus.OK, etag, pruefziffer, responseToCaller, contentType));
    } catch (final ClientRequestException e) {
      final int responseStatus = e.getResponse().getStatus().getValue();
      throw new ResponseStatusException(HttpStatus.valueOf(responseStatus), e.getMessage(), e);
    } catch (final VsdmServerException e) {
      throw e;
    } catch (final ServerResponseException e) {
      log.error("Error while connecting to VSDM server: {}", e.getMessage(), e);
      // pass through, try card next
      return Optional.empty();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Thread interrupted while requesting VsdBundle with token", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    } catch (final Exception e) {
      log.error("Error on requesting VsdmBundle with token", e);
      throw new ResponseStatusException(HttpURLConnection.HTTP_INTERNAL_ERROR, e.getMessage(), e);
    }
  }

  private VsdmReadResult handleNotModified(
      final String terminalId,
      final Integer egkSlotId,
      final AttachedCard attachedCard,
      final ZetaSdkClientAdapter.Response responseFromServer) {
    final String etagHeader =
        Objects.requireNonNull(
            getHeaderValue(responseFromServer, HEADER_ETAG),
            "'%s' header must be set by VSDM backend on 304".formatted(HEADER_ETAG));

    final String checkDigitHeader =
        Objects.requireNonNull(
            getHeaderValue(responseFromServer, HEADER_VSDM_PZ),
            "'%s' header must be set by VSDM backend on 304".formatted(HEADER_VSDM_PZ));

    if (attachedCard != null) {
      final VsdmCachedValue cachedValue =
          vsdmDataRepository.get(terminalId, egkSlotId, attachedCard.getId());
      final VsdmCachedValue updatedCacheValue;
      if (cachedValue == null) {
        updatedCacheValue = new VsdmCachedValue(etagHeader, checkDigitHeader, "");
      } else {
        updatedCacheValue = cachedValue.copyWith(etagHeader, checkDigitHeader);
      }
      maybeUpdateCache(terminalId, egkSlotId, attachedCard, updatedCacheValue);
    }

    return new VsdmReadResult(HttpStatus.NOT_MODIFIED, etagHeader, checkDigitHeader, null, null);
  }

  private void maybeUpdateCache(
      final String terminalId,
      final Integer egkSlotId,
      final AttachedCard attachedCard,
      final VsdmCachedValue updatedCacheValue) {
    // Only cache if attachedCard is available
    if (attachedCard != null) {
      vsdmDataRepository.put(terminalId, egkSlotId, attachedCard.getId(), updatedCacheValue);
    }
  }

  private String getHeaderValue(
      final ZetaSdkClientAdapter.Response responseFromServer, final String headerName) {
    return responseFromServer.headers().entrySet().stream()
        .filter(entry -> entry.getKey().equalsIgnoreCase(headerName))
        .map(java.util.Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }
}
