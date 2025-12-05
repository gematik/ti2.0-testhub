/*
 *
 * Copyright 2025 gematik GmbH
 *
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
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
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.message.TokenMessage;
import de.gematik.ti20.client.popp.service.PoppClientService;
import de.gematik.ti20.client.popp.service.PoppTokenSession;
import de.gematik.ti20.client.popp.service.PoppTokenSessionEventHandler;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import de.gematik.ti20.simsvc.client.config.VsdmConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import io.ktor.client.plugins.ServerResponseException;
import io.ktor.http.Headers;
import io.ktor.http.HeadersKt;
import java.util.Arrays;
import java.util.List;
import kotlin.Unit;
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

  private VsdmConfig vsdmConfig;
  private ZetaClientService mockZetaClientService;
  private ZetaClientConfig mockZetaClientConfig;
  private ZetaSdkClientAdapter mockZetaSdkAdapter;
  private PoppClientService mockPoppClientService;
  private PoppClientConfig mockPoppClientConfig;
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
    vsdmConfig = new VsdmConfig();
    vsdmConfig.setResourceServerUrl("http://localhost:8080");

    mockZetaClientConfig = mock(ZetaClientConfig.class);
    mockZetaClientService = mock(ZetaClientService.class);
    when(mockZetaClientService.getZetaClientConfig()).thenReturn(mockZetaClientConfig);

    mockPoppClientConfig = mock(PoppClientConfig.class);
    mockPoppClientService = mock(PoppClientService.class);
    when(mockPoppClientService.getPoppClientConfig()).thenReturn(mockPoppClientConfig);
    mockEgkInfo = mock(EgkInfo.class);
    when(mockPoppClientService.getEgkInfo(any())).thenReturn(mockEgkInfo);

    mockFhirService = mock(FhirService.class);

    mockPoppTokenRepository = mock(PoppTokenRepository.class);
    when(mockPoppTokenRepository.get(anyString(), anyInt(), anyString())).thenReturn(null);

    mockVsdmDataRepository = mock(VsdmDataRepository.class);
    when(mockVsdmDataRepository.get(anyString(), anyInt(), anyString())).thenReturn(null);

    mockEgkCard = mock(SimulatorAttachedCard.class);
    when(mockEgkCard.isEgk()).thenReturn(true);
    when(mockEgkCard.getSlotId()).thenReturn(1);
    when(mockEgkCard.getId()).thenReturn("card1");

    when(mockPoppClientService.getZetaClientService()).thenReturn(mockZetaClientService);
    when(mockPoppClientService.getAttachedCards()).thenReturn((List) Arrays.asList(mockEgkCard));

    mockZetaSdkAdapter = mock(ZetaSdkClientAdapter.class);

    vsdmClientService =
        new VsdmClientService(
            mockPoppClientService,
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
      verify(mockPoppClientService, never()).startPoppTokenSession(any(), any(), any());
    }

    @Test
    void testRequestPoppToken_FromService() throws Exception {
      String expectedToken = "service-token";

      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(null);

      TokenMessage mockTokenMessage = mock(TokenMessage.class);
      when(mockTokenMessage.getToken()).thenReturn(expectedToken);

      // Simuliere erfolgreiche Token-Antwort
      doAnswer(
              invocation -> {
                PoppTokenSessionEventHandler handler = invocation.getArgument(2);
                PoppTokenSession mockSession = mock(PoppTokenSession.class);
                when(mockSession.getAttachedCard()).thenReturn(mockEgkCard);
                handler.onReceivedPoppToken(mockSession, mockTokenMessage);
                return null;
              })
          .when(mockPoppClientService)
          .startPoppTokenSession(eq(mockEgkCard), any(), any());

      String result =
          vsdmClientService.requestPoppToken(terminalId, egkSlotId, smcBSlotId, mockEgkCard);

      assertEquals(expectedToken, result);
      verify(mockPoppClientService).startPoppTokenSession(eq(mockEgkCard), any(), any());
      verify(mockPoppTokenRepository).put(terminalId, egkSlotId, "card1", expectedToken);
    }

    @Test
    void testRequestPoppToken_PoppClientException() throws Exception {
      when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(null);

      PoppClientException expectedException = new PoppClientException("Test error");
      doThrow(expectedException)
          .when(mockPoppClientService)
          .startPoppTokenSession(eq(mockEgkCard), any(), any());

      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () ->
                  vsdmClientService.requestPoppToken(
                      terminalId, egkSlotId, smcBSlotId, mockEgkCard));

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
      assertEquals("Test error", exception.getReason());
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
              HeadersKt.headers(
                  headersBuilder -> {
                    headersBuilder.set("etag", "new-etag");
                    headersBuilder.set("VSDM-Pz", "new-pz");
                    headersBuilder.set("Content-Type", "application/fhir+json");
                    return Unit.INSTANCE;
                  }),
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
              terminalId, egkSlotId, smcBSlotId, mockEgkCard, poppToken, null, false);

      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals("encoded response", response.getBody());
      verify(mockVsdmDataRepository)
          .put(eq(terminalId), eq(egkSlotId), eq(cardId), any(VsdmCachedValue.class));
    }

    @Test
    @SneakyThrows
    void testRequestVsd_WithXmlFormat() {
      when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

      final ZetaSdkClientAdapter.Response mockResponse =
          new ZetaSdkClientAdapter.Response(
              HttpStatus.OK,
              HeadersKt.headersOf(),
              """
                      <Bundle xmlns="http://hl7.org/fhir"></Bundle>
                      """);
      when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

      VsdmBundle mockBundle = mock(VsdmBundle.class);
      when(mockFhirService.parseString(anyString(), eq("xml"), eq(VsdmBundle.class)))
          .thenReturn(mockBundle);
      when(mockFhirService.encodeResponse(mockBundle, EncodingType.JSON))
          .thenReturn("encoded xml response");

      ResponseEntity<String> response =
          vsdmClientService.requestVsd(
              terminalId, egkSlotId, smcBSlotId, mockEgkCard, poppToken, null, true);

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
              mockPoppClientService,
              new FhirService(),
              mockPoppTokenRepository,
              mockVsdmDataRepository,
              mockZetaSdkAdapter);

      when(mockVsdmDataRepository.get(terminalId, egkSlotId, cardId)).thenReturn(null);

      final ZetaSdkClientAdapter.Response mockResponse =
          new ZetaSdkClientAdapter.Response(
              HttpStatus.INTERNAL_SERVER_ERROR,
              HeadersKt.headersOf(),
              "{\"resourceType\":\"Bundle\",\"id\":\"9f8a388d-c6ba-47d3-a644-34750542d1a0\",\"meta\":{\"profile\":[\"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle\"]},\"identifier\":{\"system\":\"urn:ietf:rfc:3986\",\"value\":\"urn:uuid:9f8a388d-c6ba-47d3-a644-34750542d1a0\"},\"type\":\"document\",\"timestamp\":\"2025-08-21T14:15:33.402+02:00\",\"entry\":[{\"fullUrl\":\"https://gematik.de/fhir/OperationOutcome/70237e55-ec26-4ee9-8b8d-1e5cc7f0af26\",\"resource\":{\"resourceType\":\"OperationOutcome\",\"id\":\"70237e55-ec26-4ee9-8b8d-1e5cc7f0af26\",\"meta\":{\"profile\":[\"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMOperationOutcome\"]},\"issue\":[{\"severity\":\"fatal\",\"code\":\"invalid\",\"details\":{\"coding\":[{\"code\":\"VSDSERVICE_INTERNAL_SERVER_ERROR\",\"display\":\"Unerwarteter"
                  + " interner Fehler des Fachdienstes VSDM. \"}],\"text\":\"Unerwarteter interner"
                  + " Fehler des Fachdienstes VSDM. \"}}]}}]}");

      when(mockZetaSdkAdapter.httpGet(anyString(), any())).thenReturn(mockResponse);

      ResponseEntity<String> response =
          vsdmClientService.requestVsd(
              terminalId, egkSlotId, smcBSlotId, mockEgkCard, poppToken, null, false);

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
              HeadersKt.headersOf(),
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
              terminalId, egkSlotId, smcBSlotId, mockEgkCard, "token123", "etag123", false);

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
              "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token123", "etag123", false);

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
          vsdmClientService.requestVsd("terminal", 1, 1, mockEgkCard, poppToken, "", false);

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
              HeadersKt.headersOf(),
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
      vsdmClientService.requestVsd("terminal", 1, 1, mockEgkCard, poppToken, null, false);

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

      final Headers responseHeaders =
          HeadersKt.headers(
              header -> {
                header.append(VsdmClientService.HEADER_ETAG, "etag");
                header.append(VsdmClientService.HEADER_VSDM_PZ, "ziffer-1");
                return Unit.INSTANCE;
              });

      // AND the VSDM backend returns 304
      when(mockZetaSdkAdapter.httpGet(any(), any()))
          .thenReturn(
              new ZetaSdkClientAdapter.Response(HttpStatus.NOT_MODIFIED, responseHeaders, ""));

      // WHEN we request data
      final ResponseEntity<String> response =
          vsdmClientService.requestVsd(
              terminalId, egkSlotId, smcBSlotId, mockEgkCard, poppToken, "etag", false);

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

  @Test
  void testOnReceivedPoppToken() {
    PoppTokenSession mockSession = mock(PoppTokenSession.class);
    when(mockSession.getAttachedCard()).thenReturn(mockEgkCard);
    TokenMessage mockToken = mock(TokenMessage.class);

    assertDoesNotThrow(() -> vsdmClientService.onReceivedPoppToken(mockSession, mockToken));
  }

  @Test
  void testOnError() {
    PoppTokenSession mockSession = mock(PoppTokenSession.class);
    when(mockSession.getAttachedCard()).thenReturn(mockEgkCard);
    PoppClientException exception = new PoppClientException("Test error");

    assertDoesNotThrow(() -> vsdmClientService.onError(mockSession, exception));
  }

  @Test
  void testEventHandlerMethods() {
    PoppTokenSession mockSession = mock(PoppTokenSession.class);
    when(mockSession.getAttachedCard()).thenReturn(mockEgkCard);

    assertDoesNotThrow(() -> vsdmClientService.onConnectedToTerminalSlot(mockSession));
    assertDoesNotThrow(() -> vsdmClientService.onDisconnectedFromTerminalSlot(mockSession));
    assertDoesNotThrow(() -> vsdmClientService.onCardInserted(mockSession));
    assertDoesNotThrow(() -> vsdmClientService.onCardRemoved(mockSession));
    assertDoesNotThrow(() -> vsdmClientService.onCardPairedToServer(mockSession));
    assertDoesNotThrow(() -> vsdmClientService.onConnectedToServer(mockSession));
    assertDoesNotThrow(() -> vsdmClientService.onDisconnectedFromServer(mockSession));
    assertDoesNotThrow(() -> vsdmClientService.onFinished(mockSession));
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
      when(mockPoppClientService.getAttachedCards()).thenThrow(new RuntimeException());
      assertThatExceptionOfType(ResponseStatusException.class)
          .isThrownBy(() -> vsdmClientService.getAttachedCard("any", 1));
    }
  }

  @Test
  void thatLoadTruncatedDataWorks() throws CardTerminalException {
    when(mockPoppClientService.getEgkInfo(any()))
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
