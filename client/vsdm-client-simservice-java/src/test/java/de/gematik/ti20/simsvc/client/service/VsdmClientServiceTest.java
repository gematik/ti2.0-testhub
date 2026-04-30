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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
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
import org.springframework.http.ResponseEntity;
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
  private SimulatorAttachedCard mockEgkCard;

  private final String terminalId = "terminal1";
  private final int egkSlotId = 1;
  private final int smcBSlotId = 2;
  private final String cardId = "card1";
  private final String poppToken = "token123";

  @BeforeEach
  void setUp() throws Exception {
    vsdmClientConfig = new VsdmClientConfig();
    vsdmClientConfig.setResourceServerUrl("http://localhost:8080");
    vsdmClientConfig.setUseMockPoppToken(false);
    vsdmClientConfig.setPoppTokenGeneratorUrl("poppTokenGeneratorUrl");

    mockPoppClientAdapter = mock(PoppClientAdapter.class);
    mockCardTerminalService = mock(CardTerminalService.class);

    mockEgkCard = mock(SimulatorAttachedCard.class);
    when(mockEgkCard.isEgk()).thenReturn(true);
    when(mockEgkCard.getSlotId()).thenReturn(1);
    when(mockEgkCard.getId()).thenReturn("card1");
    mockEgkInfo = mock(EgkInfo.class);
    when(mockCardTerminalService.getEgkInfo(any())).thenReturn(mockEgkInfo);
    when(mockCardTerminalService.getAttachedCards()).thenReturn((List) Arrays.asList(mockEgkCard));

    mockPoppTokenService = mock(MockPoppTokenService.class);
    when(mockPoppTokenService.requestPoppToken(vsdmClientConfig, "iknr", "kvnr"))
        .thenReturn("mocked-token");

    mockFhirService = mock(FhirService.class);

    mockPoppTokenRepository = mock(PoppTokenRepository.class);
    when(mockPoppTokenRepository.get(anyString(), anyInt(), anyString())).thenReturn(null);

    mockVsdmDataRepository = mock(VsdmDataRepository.class);
    when(mockVsdmDataRepository.get(anyString(), anyInt(), anyString())).thenReturn(null);

    mockEgkCard = mock(SimulatorAttachedCard.class);
    when(mockEgkCard.isEgk()).thenReturn(true);
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
  class PoppToken {

    @Test
    void testRequestPoppToken_FromRepository() throws Exception {
      String expectedToken = "cached-token";
      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(expectedToken);

      String result =
          vsdmClientService.requestPoppToken(terminalId, egkSlotId, smcBSlotId, mockEgkCard);

      assertEquals(expectedToken, result);
      verify(mockPoppTokenRepository).get(terminalId, egkSlotId, "card1");
      verify(mockPoppClientAdapter, never()).getPoppToken(any());
    }

    @Test
    void testRequestPoppToken_FromService() throws Exception {
      String expectedToken = "service-token";

      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(null);

      when(mockPoppClientAdapter.getPoppToken(any())).thenReturn(expectedToken);

      String result =
          vsdmClientService.requestPoppToken(terminalId, egkSlotId, smcBSlotId, mockEgkCard);

      assertEquals(expectedToken, result);
      verify(mockPoppClientAdapter).getPoppToken(eq(mockEgkCard));
      verify(mockPoppTokenRepository).put(terminalId, egkSlotId, "card1", expectedToken);
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
            .thenReturn(
                new EgkInfo(
                    "kvnr",
                    "iknr",
                    "patient",
                    "actual-first",
                    "actual-last",
                    "2000",
                    "insurance",
                    "card",
                    "2012",
                    "true"));

        when(mockPoppTokenService.requestPoppToken(vsdmClientConfig, "iknr", "kvnr"))
            .thenReturn(expectedToken);

        String result =
            vsdmClientService.requestPoppToken(terminalId, egkSlotId, smcBSlotId, mockEgkCard);

        assertEquals(expectedToken, result);

        verify(mockPoppTokenRepository, never()).get(any(), any(), any());
        verify(mockPoppClientAdapter, never()).getPoppToken(any());
      }
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

        VsdmBundle mockBundle = mock(VsdmBundle.class);
        when(mockFhirService.parseString(anyString(), eq("json"), eq(VsdmBundle.class)))
            .thenReturn(mockBundle);
        when(mockFhirService.encodeResponse(mockBundle, EncodingType.JSON))
            .thenReturn("encoded response");

        ResponseEntity<String> response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, mockEgkCard, poppToken, null, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("encoded response", response.getBody());
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
            <Bundle xmlns="http://hl7.org/fhir"></Bundle>
            """);
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        VsdmBundle mockBundle = mock(VsdmBundle.class);
        when(mockFhirService.parseString(anyString(), eq("xml"), eq(VsdmBundle.class)))
            .thenReturn(mockBundle);
        when(mockFhirService.encodeResponse(mockBundle, EncodingType.XML))
            .thenReturn("encoded xml response");

        ResponseEntity<String> response =
            vsdmClientService.requestVsd(terminalId, egkSlotId, mockEgkCard, poppToken, null, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("encoded xml response", response.getBody());

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
                    + " interner Fehler des Fachdienstes VSDM. \"}],\"text\":\"Unerwarteter interner"
                    + " Fehler des Fachdienstes VSDM. \"}}]}}]}");

        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

        ResponseEntity<String> response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, mockEgkCard, poppToken, null, false);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.hasBody());
        assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));

        verify(mockVsdmDataRepository, never()).put(any(), any(), any(), any());
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

        VsdmBundle mockBundle = mock(VsdmBundle.class);
        when(mockFhirService.parseString(anyString(), eq("json"), eq(VsdmBundle.class)))
            .thenReturn(mockBundle);
        when(mockFhirService.encodeResponse(mockBundle, EncodingType.JSON))
            .thenReturn("encoded response");

        ResponseEntity<String> response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, mockEgkCard, "token123", "etag123", false);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("encoded response", response.getBody());
      }

      @Test
      @SneakyThrows
      void testRequestVsd_ServerUnreachable() {
        // kotlin is accessing deeply nested info when creating an instance, we avoid that pain by
        // mocking
        final ServerResponseException serverResponseException = mock(ServerResponseException.class);
        when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenThrow(serverResponseException);

        ResponseEntity<String> response =
            vsdmClientService.requestVsd(
                "terminalId", egkSlotId, mockEgkCard, "token123", "etag123", false);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
      }

      @Test
      void thatEmptyStringForNoneMatchHeaderChecksCache() {
        // GIVEN a repository with a cached value
        final VsdmCachedValue cachedValue = mock(VsdmCachedValue.class);
        when(cachedValue.vsdmData()).thenReturn("The Data");
        when(mockVsdmDataRepository.get(any(), any(), anyString())).thenReturn(cachedValue);

        // WHEN the client requests data from cache
        final ResponseEntity<String> response =
            vsdmClientService.requestVsd("terminal", 1, mockEgkCard, poppToken, "", false);

        // THEN the repository was accessed
        verify(mockVsdmDataRepository, times(1)).get("terminal", 1, mockEgkCard.getId());

        // AND the response matches
        assertThat(response.getBody()).isEqualTo("The Data");
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
        vsdmClientService.requestVsd("terminal", 1, mockEgkCard, poppToken, null, false);

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
                VsdmClientService.HEADER_ETAG, "etag",
                VsdmClientService.HEADER_VSDM_PZ, "ziffer-1");

        // AND the VSDM backend returns 304
        when(mockZetaSdkAdapter.httpGet(any(), any()))
            .thenReturn(
                new ZetaSdkClientAdapter.Response(HttpStatus.NOT_MODIFIED, responseHeaders, ""));

        // WHEN we request data
        final ResponseEntity<String> response =
            vsdmClientService.requestVsd(
                terminalId, egkSlotId, mockEgkCard, poppToken, "etag", false);

        // THEN we update the cache with expected values
        final VsdmCachedValue expectedCacheValue = new VsdmCachedValue("etag", "ziffer-1", "");
        verify(mockVsdmDataRepository, times(1))
            .put(terminalId, 1, mockEgkCard.getId(), expectedCacheValue);
        // AND return 304
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      }
    }

    @Test
    void testSetTerminalConnectionConfigs() {
      List<CardTerminalConnectionConfig> configs =
          List.of(new SimulatorConnectionConfig("Terminal1", "Url1"));

      vsdmClientService.setTerminalConnectionConfigs(configs);

      assertEquals(configs, vsdmClientService.getTerminalConnectionConfigs());
    }

    @Test
    void testGetTerminalConnectionConfigs() {
      List<CardTerminalConnectionConfig> configs = vsdmClientService.getTerminalConnectionConfigs();

      assertNotNull(configs);
      assertTrue(configs.isEmpty());
    }

    @Nested
    class getAttachedCard {

      @Test
      void thatGetAttachedCardWorks() {
        final AttachedCard attachedCard = vsdmClientService.getAttachedCard("id", 1);
        assertThat(attachedCard).isNotNull();
      }

      @Test
      void thatPoppClientExceptionsAreHandled() throws CardTerminalException {
        when(mockCardTerminalService.getAttachedCards()).thenThrow(new RuntimeException());
        assertThatExceptionOfType(ResponseStatusException.class)
            .isThrownBy(() -> vsdmClientService.getAttachedCard("any", 1));
      }
    }

    @Test
    void thatLoadTruncatedDataWorks() throws CardTerminalException {
      when(mockCardTerminalService.getEgkInfo(any()))
          .thenReturn(
              new EgkInfo(
                  "actual-kvnr",
                  "iknr",
                  "patient",
                  "actual-first",
                  "actual-last",
                  "2000",
                  "insurance",
                  "card",
                  "2012",
                  "true"));

      final AttachedCard mock = mock(AttachedCard.class);

      vsdmClientService.loadTruncatedDataFromCard(mock);
      verify(mockFhirService, times(1)).encodeResponse(any(), any());
    }
  }
}
