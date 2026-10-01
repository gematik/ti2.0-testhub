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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.exception.VsdmServerException;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.simsvc.client.service.ZetaSdkClientAdapter;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VsdmDataFromServiceStrategyTest {

  @Mock private VsdmDataRepository vsdmDataRepository;
  @Mock private ZetaSdkClientAdapter vsdmZetaClient;
  @Mock private AttachedCard attachedCard;

  private VsdmDataFromServiceStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new VsdmDataFromServiceStrategy(vsdmDataRepository, vsdmZetaClient);
    MDC.put("traceId", "trace-123");
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void shouldReturnAndCacheVsdmDataFromService() throws InterruptedException {
    final ZetaSdkClientAdapter.Response response =
        new ZetaSdkClientAdapter.Response(
            HttpStatus.OK,
            Map.of(
                "ETag",
                "etag-123",
                "VSDM-PZ",
                "pruefziffer-456",
                "Content-Type",
                "application/fhir+json"),
            "vsdm-data");
    when(attachedCard.getId()).thenReturn("card-123");
    when(vsdmZetaClient.httpGet(eq("vsdservice/v1/vsdmbundle?profileVersion=1.1"), any()))
        .thenReturn(response);

    final Optional<VsdmReadResult> result =
        strategy.get(
            "terminal-1", 2, attachedCard, false, false, "popp-token", "previous-etag", "1.1");

    assertThat(result)
        .contains(
            new VsdmReadResult(
                HttpStatus.OK,
                "etag-123",
                "pruefziffer-456",
                "vsdm-data",
                "application/fhir+json"));
    verify(vsdmDataRepository)
        .put(
            "terminal-1",
            2,
            "card-123",
            new VsdmCachedValue("etag-123", "pruefziffer-456", "vsdm-data"));

    final ArgumentCaptor<ZetaSdkClientAdapter.RequestParameters> parametersCaptor =
        ArgumentCaptor.forClass(ZetaSdkClientAdapter.RequestParameters.class);
    verify(vsdmZetaClient)
        .httpGet(eq("vsdservice/v1/vsdmbundle?profileVersion=1.1"), parametersCaptor.capture());
    assertThat(parametersCaptor.getValue())
        .isEqualTo(
            new ZetaSdkClientAdapter.RequestParameters(
                "trace-123", "popp-token", false, false, "previous-etag"));
  }

  @Test
  void shouldForwardSkipPoppTokenHeader() throws InterruptedException {
    final ZetaSdkClientAdapter.Response response =
        new ZetaSdkClientAdapter.Response(HttpStatus.OK, Map.of("ETag", "etag-123"), "vsdm-data");
    when(vsdmZetaClient.httpGet(eq("vsdservice/v1/vsdmbundle"), any())).thenReturn(response);

    strategy.get("terminal-1", 2, null, false, true, "popp-token", null, null);

    final ArgumentCaptor<ZetaSdkClientAdapter.RequestParameters> parametersCaptor =
        ArgumentCaptor.forClass(ZetaSdkClientAdapter.RequestParameters.class);
    verify(vsdmZetaClient).httpGet(eq("vsdservice/v1/vsdmbundle"), parametersCaptor.capture());
    assertThat(parametersCaptor.getValue().skipPoppTokenHeader()).isTrue();
  }

  @Test
  void shouldUpdateCachedHeadersWhenServiceReturnsNotModified() throws InterruptedException {
    final VsdmCachedValue cachedValue =
        new VsdmCachedValue("old-etag", "old-pruefziffer", "cached-vsdm-data");
    final ZetaSdkClientAdapter.Response response =
        new ZetaSdkClientAdapter.Response(
            HttpStatus.NOT_MODIFIED, Map.of("etag", "new-etag", "vsdm-pz", "new-pruefziffer"), "");
    when(attachedCard.getId()).thenReturn("card-123");
    when(vsdmDataRepository.get("terminal-1", 2, "card-123")).thenReturn(cachedValue);
    when(vsdmZetaClient.httpGet(eq("vsdservice/v1/vsdmbundle"), any())).thenReturn(response);

    final Optional<VsdmReadResult> result =
        strategy.get("terminal-1", 2, attachedCard, false, false, "popp-token", "old-etag", null);

    assertThat(result)
        .contains(
            new VsdmReadResult(HttpStatus.NOT_MODIFIED, "new-etag", "new-pruefziffer", null, null));
    verify(vsdmDataRepository)
        .put(
            "terminal-1",
            2,
            "card-123",
            new VsdmCachedValue("new-etag", "new-pruefziffer", "cached-vsdm-data"));
  }

  @Test
  void shouldThrowVsdmServerExceptionForUnsuccessfulResponse() throws InterruptedException {
    final ZetaSdkClientAdapter.Response response =
        new ZetaSdkClientAdapter.Response(
            HttpStatus.BAD_GATEWAY, Map.of("error-header", "error-value"), "backend error");
    when(vsdmZetaClient.httpGet(eq("vsdservice/v1/vsdmbundle"), any())).thenReturn(response);

    final VsdmServerException exception =
        assertThrows(
            VsdmServerException.class,
            () -> strategy.get("terminal-1", 2, null, false, false, "popp-token", null, null));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(exception.getHeaders()).containsEntry("error-header", "error-value");
    assertThat(exception.getResponseBody()).isEqualTo("backend error");
    verifyNoInteractions(vsdmDataRepository);
  }

  @Test
  void shouldThrowInternalServerErrorWhenResponseBodyIsMissing() throws InterruptedException {
    final ZetaSdkClientAdapter.Response response =
        new ZetaSdkClientAdapter.Response(HttpStatus.OK, Map.of(), null);
    when(vsdmZetaClient.httpGet(eq("vsdservice/v1/vsdmbundle"), any())).thenReturn(response);

    final ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> strategy.get("terminal-1", 2, null, false, false, "popp-token", null, null));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(exception.getReason()).contains("Could not parse valid FHIR response");
    verifyNoInteractions(vsdmDataRepository);
  }
}
