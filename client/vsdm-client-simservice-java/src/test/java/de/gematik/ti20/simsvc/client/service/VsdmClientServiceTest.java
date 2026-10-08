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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.card.EgkInfo;
import de.gematik.ti20.simsvc.client.card.SmcbInfo;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.exception.CardTerminalException;
import de.gematik.ti20.simsvc.client.exception.VsdmServerException;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.simsvc.client.service.popp.PoppClientAdapter;
import de.gematik.ti20.simsvc.client.service.popp.PoppToken;
import de.gematik.ti20.simsvc.client.service.vsdm.VsdmReadResult;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import io.ktor.client.plugins.ClientRequestException;
import io.ktor.client.plugins.ServerResponseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class VsdmClientServiceTest {

  private VsdmClientService vsdmClientService;

  private VsdmClientConfig vsdmClientConfig;

  private ZetaSdkClientAdapter mockZetaSdkAdapter;
  private PoppClientAdapter mockPoppClientAdapter;
  private MockPoppTokenService mockPoppTokenService;
  private CardTerminalService mockCardTerminalService;
  private FhirService mockFhirService;
  private PoppTokenRepository mockPoppTokenRepository;
  private VsdmDataRepository mockVsdmDataRepository;

  private EgkInfo mockEgkInfo;
  private AttachedCard mockEgkCard;

  private final String terminalId = "terminal1";
  private final int egkSlotId = 1;
  private final String virtualCard = "virtualCard";

  private final String cardId = "card1";
  private final PoppToken poppToken = new PoppToken("token123");
  private final String profileVersion = "1.1";

  @BeforeEach
  void setUp() throws Exception {
    vsdmClientConfig = new VsdmClientConfig();
    vsdmClientConfig.setResourceServerUrl("http://localhost:8080");
    vsdmClientConfig.setUseMockPoppToken(false);
    vsdmClientConfig.setPoppTokenGeneratorUrl("poppTokenGeneratorUrl");

    mockPoppClientAdapter = mock(PoppClientAdapter.class);
    mockCardTerminalService = mock(CardTerminalService.class);

    mockEgkCard = mock(AttachedCard.class);
    when(mockEgkCard.getSlotId()).thenReturn(1);
    when(mockEgkCard.getId()).thenReturn("card1");
    mockEgkInfo = mock(EgkInfo.class);
    when(mockCardTerminalService.getEgkInfo(any())).thenReturn(mockEgkInfo);
    when(mockCardTerminalService.getAttachedCards()).thenReturn((List) Arrays.asList(mockEgkCard));

    mockPoppTokenService = mock(MockPoppTokenService.class);
    when(mockPoppTokenService.requestPoppToken(
            vsdmClientConfig, "iknr", "kvnr", "actorId", "actorProfId"))
        .thenReturn("mocked-token");

    mockFhirService = mock(FhirService.class);

    mockPoppTokenRepository = mock(PoppTokenRepository.class);
    when(mockPoppTokenRepository.get(anyString(), anyInt(), anyString())).thenReturn(null);

    mockVsdmDataRepository = mock(VsdmDataRepository.class);
    when(mockVsdmDataRepository.get(anyString(), anyInt(), anyString())).thenReturn(null);

    mockEgkCard = mock(AttachedCard.class);
    when(mockEgkCard.getSlotId()).thenReturn(1);
    when(mockEgkCard.getId()).thenReturn("card1");

    mockZetaSdkAdapter = mock(ZetaSdkClientAdapter.class);

    vsdmClientService =
        new VsdmClientService(
            vsdmClientConfig,
            mockPoppTokenService,
            mockCardTerminalService,
            mockPoppClientAdapter,
            mockFhirService,
            mockPoppTokenRepository,
            mockVsdmDataRepository,
            mockZetaSdkAdapter);
  }

  @Nested
  class PoppTokenHandling {

    @Test
    void testRequestPoppToken_FromRepository() {
      String expectedToken = "cached-token";
      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(expectedToken);

      String result =
          vsdmClientService
              .requestPoppToken(null, terminalId, egkSlotId, mockEgkCard, null)
              .value();

      assertEquals(expectedToken, result);
      verify(mockPoppTokenRepository).get(terminalId, egkSlotId, "card1");
      verify(mockPoppClientAdapter, never()).getPoppToken(any());
    }

    @Test
    void testRequestPoppToken_FromService() {
      String expectedToken = "service-token";

      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(null);

      when(mockPoppClientAdapter.getPoppToken(any())).thenReturn(expectedToken);

      String result =
          vsdmClientService
              .requestPoppToken(null, terminalId, egkSlotId, mockEgkCard, null)
              .value();

      assertEquals(expectedToken, result);
      verify(mockPoppClientAdapter).getPoppToken(null);
      verify(mockPoppTokenRepository).put(terminalId, egkSlotId, "card1", expectedToken);
    }

    @Test
    void testRequestPoppToken_RetriesTransientPoppFailure() {
      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(null);
      when(mockPoppClientAdapter.getPoppToken(any()))
          .thenThrow(new RuntimeException("Websocket client is not connected"))
          .thenReturn("service-token");

      String result =
          vsdmClientService
              .requestPoppToken(null, terminalId, egkSlotId, mockEgkCard, null)
              .value();

      assertEquals("service-token", result);
      verify(mockPoppClientAdapter, times(2)).getPoppToken(null);
      verify(mockPoppTokenRepository).put(terminalId, egkSlotId, "card1", "service-token");
    }

    @Test
    void testRequestPoppToken_DoesNotRetryNonTransientPoppFailure() {
      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(null);
      when(mockPoppClientAdapter.getPoppToken(any())).thenThrow(new RuntimeException("boom"));

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () ->
                  vsdmClientService.requestPoppToken(
                      null, terminalId, egkSlotId, mockEgkCard, null));

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
      verify(mockPoppClientAdapter, times(1)).getPoppToken(null);
      verify(mockPoppTokenRepository, never()).put(anyString(), anyInt(), anyString(), anyString());
    }

    @Nested
    class MockedPoppToken {

      @Test
      void testRequestPoppToken_FromPoppTokenGenerator() throws Exception {
        String expectedToken = "mocked-token";

        vsdmClientConfig = new VsdmClientConfig();
        vsdmClientConfig.setResourceServerUrl("http://localhost:8080");
        vsdmClientConfig.setUseMockPoppToken(true);
        vsdmClientConfig.setPoppTokenGeneratorUrl("poppTokenGeneratorUrl");

        vsdmClientService =
            new VsdmClientService(
                vsdmClientConfig,
                mockPoppTokenService,
                mockCardTerminalService,
                mockPoppClientAdapter,
                mockFhirService,
                mockPoppTokenRepository,
                mockVsdmDataRepository,
                mockZetaSdkAdapter);

        when(mockCardTerminalService.getEgkInfo(any()))
            .thenReturn(new EgkInfo("kvnr", "iknr", "actual-first", "actual-last", "true"));
        when(mockCardTerminalService.getSmcbInfo())
            .thenReturn(new SmcbInfo("telematikId", "professionOid"));

        when(mockPoppTokenService.requestPoppToken(
                vsdmClientConfig, "iknr", "kvnr", "telematikId", "professionOid"))
            .thenReturn(expectedToken);

        String result =
            vsdmClientService
                .requestPoppToken(null, terminalId, egkSlotId, mockEgkCard, null)
                .value();

        assertEquals(expectedToken, result);

        verify(mockPoppTokenRepository, never()).get(any(), any(), any());
        verify(mockPoppClientAdapter, never()).getPoppToken(any());
      }
    }

    @Test
    void readUsesInjectedTokenWithoutLoadingAttachedCard() throws InterruptedException {
      final ZetaSdkClientAdapter.Response mockResponse =
          new ZetaSdkClientAdapter.Response(
              HttpStatus.OK, Map.of(), "{\"resourceType\":\"Bundle\"}");
      when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

      final VsdmReadResult response =
          vsdmClientService.read(
              terminalId,
              egkSlotId,
              virtualCard,
              false,
              false,
              "injected-token",
              null,
              profileVersion);

      assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.value()).isEqualTo("{\"resourceType\":\"Bundle\"}");
      verify(mockCardTerminalService, never()).getAttachedCard(anyString(), anyInt());
      ArgumentCaptor<ZetaSdkClientAdapter.RequestParameters> parametersCaptor =
          ArgumentCaptor.forClass(ZetaSdkClientAdapter.RequestParameters.class);
      verify(mockZetaSdkAdapter).httpGet(anyString(), parametersCaptor.capture());
      assertThat(parametersCaptor.getValue().poppToken()).isEqualTo("injected-token");
      assertThat(parametersCaptor.getValue().skipPoppTokenHeader()).isFalse();
    }

    @Test
    void readSkipsPoppHeaderWhenConfigured() throws InterruptedException {
      final ZetaSdkClientAdapter.Response mockResponse =
          new ZetaSdkClientAdapter.Response(
              HttpStatus.OK, Map.of(), "{\"resourceType\":\"Bundle\"}");
      when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

      final VsdmReadResult response =
          vsdmClientService.read(
              terminalId,
              egkSlotId,
              virtualCard,
              false,
              true,
              "injected-token",
              null,
              profileVersion);

      assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.value()).isEqualTo("{\"resourceType\":\"Bundle\"}");
      ArgumentCaptor<ZetaSdkClientAdapter.RequestParameters> parametersCaptor =
          ArgumentCaptor.forClass(ZetaSdkClientAdapter.RequestParameters.class);
      verify(mockZetaSdkAdapter).httpGet(anyString(), parametersCaptor.capture());
      assertThat(parametersCaptor.getValue().poppToken()).isEqualTo("injected-token");
      assertThat(parametersCaptor.getValue().skipPoppTokenHeader()).isTrue();
    }

    @Test
    void readLoadsAttachedCardWhenNoInjectedTokenIsProvided() {
      when(mockCardTerminalService.getAttachedCard(terminalId, egkSlotId)).thenReturn(mockEgkCard);
      when(mockPoppTokenRepository.get(terminalId, egkSlotId, cardId)).thenReturn("cached-token");
      when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId))
          .thenReturn(new VsdmCachedValue("etag", "pz", "cached-vsd"));

      final VsdmReadResult response =
          vsdmClientService.read(
              terminalId, egkSlotId, virtualCard, false, false, null, null, profileVersion);

      assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.value()).isEqualTo("cached-vsd");
      assertThat(response.etag()).isEqualTo("etag");
      assertThat(response.pruefziffer()).isEqualTo("pz");
      verify(mockCardTerminalService).getAttachedCard(terminalId, egkSlotId);
    }

    @Nested
    class RequestVSD {

      @Test
      @SneakyThrows
      void testRequestVsd_SuccessfulServerResponse() {
        when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

        final ZetaSdkClientAdapter.Response mockResponse =
            new ZetaSdkClientAdapter.Response(
                HttpStatus.OK,
                Map.of(
                    "etag", "new-etag",
                    "vsdm-pz", "new-pz",
                    "Content-Type", "application/fhir+json"),
                """
                {"resourceType":"Bundle"}\
                """);

        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        VsdmReadResult response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, mockEgkCard, poppToken, null, false, false, profileVersion);

        assertEquals(HttpStatus.OK, response.statusCode());
        assertEquals("{\"resourceType\":\"Bundle\"}", response.value());
        assertEquals("new-etag", response.etag());
        assertEquals("new-pz", response.pruefziffer());
        assertEquals("application/fhir+json", response.contentType());
        verify(mockVsdmDataRepository)
            .put(eq(terminalId), eq(egkSlotId), eq(cardId), any(VsdmCachedValue.class));
        verify(mockCardTerminalService, never()).getAttachedCards();
      }

      @Test
      @SneakyThrows
      void testRequestVsd_WithXmlFormat() {
        when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

        final ZetaSdkClientAdapter.Response mockResponse =
            new ZetaSdkClientAdapter.Response(
                HttpStatus.OK,
                new HashMap<>(),
                """
                <Bundle xmlns="http://hl7.org/fhir"></Bundle>\
                """);
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        VsdmReadResult response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, mockEgkCard, poppToken, null, true, false, profileVersion);

        assertEquals(HttpStatus.OK, response.statusCode());
        assertEquals("<Bundle xmlns=\"http://hl7.org/fhir\"></Bundle>", response.value());

        ArgumentCaptor<ZetaSdkClientAdapter.RequestParameters> requestCaptor =
            ArgumentCaptor.forClass(ZetaSdkClientAdapter.RequestParameters.class);
        verify(mockZetaSdkAdapter).httpGet(anyString(), requestCaptor.capture());
        assertTrue(requestCaptor.getValue().isFhirXml());
      }

      @Test
      @SneakyThrows
      void testRequestVsd_ServerError() {
        vsdmClientService =
            new VsdmClientService(
                vsdmClientConfig,
                mockPoppTokenService,
                mockCardTerminalService,
                mockPoppClientAdapter,
                new FhirService(),
                mockPoppTokenRepository,
                mockVsdmDataRepository,
                mockZetaSdkAdapter);

        when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

        final ZetaSdkClientAdapter.Response mockResponse =
            new ZetaSdkClientAdapter.Response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                new HashMap<>(),
                "{\"resourceType\":\"Bundle\",\"id\":\"9f8a388d-c6ba-47d3-a644-34750542d1a0\",\"meta\":{\"profile\":[\"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle\"]},\"identifier\":{\"system\":\"urn:ietf:rfc:3986\",\"value\":\"urn:uuid:9f8a388d-c6ba-47d3-a644-34750542d1a0\"},\"type\":\"document\",\"timestamp\":\"2025-08-21T14:15:33.402+02:00\",\"entry\":[{\"fullUrl\":\"https://gematik.de/fhir/OperationOutcome/70237e55-ec26-4ee9-8b8d-1e5cc7f0af26\",\"resource\":{\"resourceType\":\"OperationOutcome\",\"id\":\"70237e55-ec26-4ee9-8b8d-1e5cc7f0af26\",\"meta\":{\"profile\":[\"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMOperationOutcome\"]},\"issue\":[{\"severity\":\"fatal\",\"code\":\"invalid\",\"details\":{\"coding\":[{\"code\":\"VSDSERVICE_INTERNAL_SERVER_ERROR\",\"display\":\"Unerwarteter"
                    + " interner Fehler des Fachdienstes VSDM. \"}],\"text\":\"Unerwarteter"
                    + " interner Fehler des Fachdienstes VSDM. \"}}]}}]}");

        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        VsdmServerException exception =
            assertThrows(
                VsdmServerException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        mockEgkCard,
                        poppToken,
                        null,
                        false,
                        false,
                        profileVersion));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getResponseBody().contains("\"resourceType\":\"OperationOutcome\""));
        assertThat(exception.getHeaders()).isEmpty();
      }

      @Test
      @SneakyThrows
      void testRequestVsd_ServerErrorPreservesHeaders() {
        when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

        final Map<String, String> responseHeaders =
            Map.of(
                "etag", "\"etag-2\"", "vsdm-pz", "pz-2", "Content-Type", "application/fhir+json");
        final ZetaSdkClientAdapter.Response mockResponse =
            new ZetaSdkClientAdapter.Response(
                HttpStatus.BAD_GATEWAY, responseHeaders, "{\"resourceType\":\"OperationOutcome\"}");

        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        VsdmServerException exception =
            assertThrows(
                VsdmServerException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        mockEgkCard,
                        poppToken,
                        null,
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.getHeaders()).containsAllEntriesOf(responseHeaders);
        assertThat(exception.getResponseBody())
            .isEqualTo("{\"resourceType\":\"OperationOutcome\"}");
      }

      @Test
      @SneakyThrows
      void testRequestVsd_Success() {
        ZetaSdkClientAdapter.Response mockResponse =
            new ZetaSdkClientAdapter.Response(
                HttpStatus.OK,
                new HashMap<>(),
                """
                {"resourceType":"Bundle"}\
                """);
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        VsdmReadResult response =
            vsdmClientService.requestVsd(
                terminalId,
                egkSlotId,
                mockEgkCard,
                poppToken,
                "etag123",
                false,
                false,
                profileVersion);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.statusCode());
        assertEquals("{\"resourceType\":\"Bundle\"}", response.value());
      }

      @Test
      @SneakyThrows
      void testRequestVsd_ServerUnreachable() {
        // kotlin is accessing deeply nested info when creating an instance, we avoid that pain by
        // mocking
        final ServerResponseException serverResponseException = mock(ServerResponseException.class);
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenThrow(serverResponseException);

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        "terminalId",
                        egkSlotId,
                        mockEgkCard,
                        poppToken,
                        "etag123",
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      }

      @Test
      void thatEmptyStringForNoneMatchHeaderChecksCache() {
        // GIVEN a repository with a cached value
        final VsdmCachedValue cachedValue = mock(VsdmCachedValue.class);
        when(cachedValue.vsdmData()).thenReturn("The Data");
        when(mockVsdmDataRepository.get(any(), any(), anyString())).thenReturn(cachedValue);

        // WHEN the client requests data from cache
        final VsdmReadResult response =
            vsdmClientService.requestVsd(
                "terminal", 1, mockEgkCard, poppToken, "", false, false, profileVersion);

        // THEN the repository was accessed
        verify(mockVsdmDataRepository, times(1)).get("terminal", 1, mockEgkCard.getId());

        // AND the response matches
        assertThat(response.value()).isEqualTo("The Data");
      }

      @Test
      void thatMissingIfNoneMatchHeaderIsSendToServer() throws InterruptedException {
        final ZetaSdkClientAdapter.Response mockResponse =
            new ZetaSdkClientAdapter.Response(
                HttpStatus.OK,
                new HashMap<>(),
                """
                {"resourceType":"Bundle"}\
                """);
        when(mockZetaSdkAdapter.httpGet(any(), any())).thenReturn(mockResponse);
        final VsdmBundle mockBundle = mock(VsdmBundle.class);
        when(mockFhirService.parseString(anyString(), eq("json"), eq(VsdmBundle.class)))
            .thenReturn(mockBundle);
        when(mockFhirService.encodeResponse(mockBundle, EncodingType.JSON))
            .thenReturn("encoded response");

        // WHEN the client requests data from cache
        vsdmClientService.requestVsd(
            "terminal", 1, mockEgkCard, poppToken, null, false, false, profileVersion);

        // AND a request to the VSDM backend sent without header
        ArgumentCaptor<ZetaSdkClientAdapter.RequestParameters> parametersCaptor =
            ArgumentCaptor.forClass(ZetaSdkClientAdapter.RequestParameters.class);
        verify(mockZetaSdkAdapter).httpGet(any(), parametersCaptor.capture());
        assertThat(parametersCaptor.getValue().ifNoneMatch()).isNull();
      }

      @Test
      void that304WorksWithoutExistingCache() throws InterruptedException {
        // GIVEN a no cached entry exists
        when(mockVsdmDataRepository.get(terminalId, 1, mockEgkCard.getId())).thenReturn(null);

        final Map<String, String> responseHeaders =
            Map.of(
                "etag", "etag",
                "vsdm-pz", "ziffer-1");

        // AND the VSDM backend returns 304
        when(mockZetaSdkAdapter.httpGet(any(), any()))
            .thenReturn(
                new ZetaSdkClientAdapter.Response(HttpStatus.NOT_MODIFIED, responseHeaders, ""));

        // WHEN we request data
        final VsdmReadResult response =
            vsdmClientService.requestVsd(
                terminalId,
                egkSlotId,
                mockEgkCard,
                poppToken,
                "etag",
                false,
                false,
                profileVersion);

        // THEN we update the cache with expected values
        final VsdmCachedValue expectedCacheValue = new VsdmCachedValue("etag", "ziffer-1", "");
        verify(mockVsdmDataRepository, times(1))
            .put(terminalId, 1, mockEgkCard.getId(), expectedCacheValue);
        // AND return 304
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(response.etag()).isEqualTo("etag");
        assertThat(response.pruefziffer()).isEqualTo("ziffer-1");
      }

      @Test
      void that304WithLowercaseHeaderWorksWithoutAttachedCard() throws InterruptedException {
        when(mockZetaSdkAdapter.httpGet(any(), any()))
            .thenReturn(
                new ZetaSdkClientAdapter.Response(
                    HttpStatus.NOT_MODIFIED,
                    Map.of("etag", "etag", "vsdm-pz", "lowercase-pz"),
                    ""));

        final VsdmReadResult response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, null, poppToken, "etag", false, false, profileVersion);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(response.etag()).isEqualTo("etag");
        assertThat(response.pruefziffer()).isEqualTo("lowercase-pz");
        verify(mockVsdmDataRepository, never()).put(anyString(), anyInt(), anyString(), any());
      }

      @Test
      void thatRequestVsdWithProvidedTokenDoesNotCacheResponse() throws InterruptedException {
        final ZetaSdkClientAdapter.Response mockResponse =
            new ZetaSdkClientAdapter.Response(
                HttpStatus.OK,
                Map.of("etag", "etag-1", "vsdm-pz", "pz-1"),
                "{\"resourceType\":\"Bundle\"}");
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        final VsdmReadResult response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, null, poppToken, null, false, false, profileVersion);

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.value()).isEqualTo("{\"resourceType\":\"Bundle\"}");
        verify(mockVsdmDataRepository, never()).put(anyString(), anyInt(), anyString(), any());
      }

      @Test
      void testRequestVsd_ThrowsWhenResponseBodyIsNull() throws InterruptedException {
        when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);
        when(mockZetaSdkAdapter.httpGet(anyString(), any()))
            .thenReturn(
                new ZetaSdkClientAdapter.Response(
                    HttpStatus.OK, Map.of("etag", "etag-1", "vsdm-pz", "pz-1"), null));

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        mockEgkCard,
                        poppToken,
                        null,
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getReason()).contains("Could not parse valid FHIR response");
      }

      @Test
      void testRequestVsd_MapsClientRequestExceptionStatus() throws Exception {
        ClientRequestException clientRequestException = mock(ClientRequestException.class);
        io.ktor.client.statement.HttpResponse response =
            mock(io.ktor.client.statement.HttpResponse.class);
        io.ktor.http.HttpStatusCode statusCode = mock(io.ktor.http.HttpStatusCode.class);
        when(clientRequestException.getResponse()).thenReturn(response);
        when(response.getStatus()).thenReturn(statusCode);
        when(statusCode.getValue()).thenReturn(400);
        when(clientRequestException.getMessage()).thenReturn("bad request");
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenThrow(clientRequestException);

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        null,
                        poppToken,
                        null,
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getReason()).isEqualTo("bad request");
      }

      @Test
      void testRequestVsd_ThrowsOnGenericException() throws InterruptedException {
        when(mockZetaSdkAdapter.httpGet(anyString(), any()))
            .thenThrow(new RuntimeException("boom"));

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        null,
                        poppToken,
                        null,
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getReason()).isEqualTo("boom");
      }

      @Test
      void that304WithoutEtagHeaderThrows() throws InterruptedException {
        when(mockZetaSdkAdapter.httpGet(anyString(), any()))
            .thenReturn(
                new ZetaSdkClientAdapter.Response(
                    HttpStatus.NOT_MODIFIED, Map.of("vsdm-pz", "pz-1"), ""));

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        mockEgkCard,
                        poppToken,
                        "etag",
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getReason())
            .contains("'etag' header must be set by VSDM backend on 304");
      }

      @Test
      void thatServerResponseFallbackWithoutAttachedCardThrows() throws InterruptedException {
        final ServerResponseException serverResponseException = mock(ServerResponseException.class);
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenThrow(serverResponseException);

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        null,
                        poppToken,
                        "etag",
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      }

      @Test
      void thatServerResponseFallbackReturnsUnauthorizedWhenCardDataInvalid()
          throws InterruptedException, CardTerminalException {
        when(mockCardTerminalService.getEgkInfo(mockEgkCard))
            .thenReturn(new EgkInfo("kvnr", "iknr", "first", "last", "false"));
        final ServerResponseException serverResponseException = mock(ServerResponseException.class);
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenThrow(serverResponseException);

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        mockEgkCard,
                        poppToken,
                        "etag",
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
      }

      @Test
      void thatServerResponseFallbackThrowsWhenCardLoadingFails() throws Exception {
        when(mockCardTerminalService.getEgkInfo(mockEgkCard))
            .thenThrow(new CardTerminalException("card broken"));
        final ServerResponseException serverResponseException = mock(ServerResponseException.class);
        when(serverResponseException.getMessage()).thenReturn("server down");
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenThrow(serverResponseException);

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () ->
                    vsdmClientService.requestVsd(
                        terminalId,
                        egkSlotId,
                        mockEgkCard,
                        poppToken,
                        "etag",
                        false,
                        false,
                        profileVersion));

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }
}
