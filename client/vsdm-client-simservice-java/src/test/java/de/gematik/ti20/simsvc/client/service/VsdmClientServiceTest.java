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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import de.gematik.ti20.client.card.terminal.simsvc.EgkInfo;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.message.TokenMessage;
import de.gematik.ti20.client.popp.service.PoppClientService;
import de.gematik.ti20.client.popp.service.PoppTokenSession;
import de.gematik.ti20.client.popp.service.PoppTokenSessionEventHandler;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.response.ZetaHttpResponse;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import de.gematik.ti20.simsvc.client.config.VsdmConfig;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import de.gematik.ti20.vsdm.fhir.builder.VsdmOperationOutcomeBuilder;
import de.gematik.ti20.vsdm.fhir.def.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
  private PoppClientService mockPoppClientService;
  private PoppClientConfig mockPoppClientConfig;
  private FhirService mockFhirService;
  private PoppTokenRepository mockPoppTokenRepository;
  private VsdmDataRepository mockVsdmDataRepository;

  private EgkInfo mockEgkInfo;
  private SimulatorAttachedCard mockEgkCard;

  @BeforeEach
  void setUp() throws Exception {
    vsdmConfig = new VsdmConfig();
    vsdmConfig.setUrl("http://localhost:8080");

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

    vsdmClientService =
        new VsdmClientService(
            vsdmConfig,
            mockPoppClientService,
            mockFhirService,
            mockPoppTokenRepository,
            mockVsdmDataRepository);
  }

  @Test
  void testRequestPoppToken_FromRepository() throws Exception {
    String terminalId = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String expectedToken = "cached-token";
    boolean forceUpdate = false;

    when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(expectedToken);

    String result =
        vsdmClientService.requestPoppToken(
            terminalId, egkSlotId, smcBSlotId, mockEgkCard, forceUpdate);

    assertEquals(expectedToken, result);
    verify(mockPoppTokenRepository).get(terminalId, egkSlotId, "card1");
    verify(mockPoppClientService, never()).startPoppTokenSession(any(), any(), any());
  }

  @Test
  void testRequestPoppToken_FromService() throws Exception {
    String terminalId = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String expectedToken = "service-token";
    boolean forceUpdate = false;

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
        vsdmClientService.requestPoppToken(
            terminalId, egkSlotId, smcBSlotId, mockEgkCard, forceUpdate);

    assertEquals(expectedToken, result);
    verify(mockPoppClientService).startPoppTokenSession(eq(mockEgkCard), any(), any());
    verify(mockPoppTokenRepository).put(terminalId, egkSlotId, "card1", expectedToken);
  }

  @Test
  void testRequestPoppToken_ForceUpdate_FromService() throws Exception {
    String terminalId = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String expectedToken = "cached-token";
    boolean forceUpdate = true;

    when(mockPoppTokenRepository.get(terminalId, egkSlotId, "card1")).thenReturn(expectedToken);

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
        vsdmClientService.requestPoppToken(
            terminalId, egkSlotId, smcBSlotId, mockEgkCard, forceUpdate);

    assertEquals(expectedToken, result);
    verify(mockPoppTokenRepository, never()).get(terminalId, egkSlotId, "card1");
    verify(mockPoppClientService).startPoppTokenSession(eq(mockEgkCard), any(), any());
  }

  @Test
  void testRequestPoppToken_PoppClientException() throws Exception {
    String terminalId = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    boolean forceUpdate = false;

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
                    terminalId, egkSlotId, smcBSlotId, mockEgkCard, forceUpdate));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertEquals("Test error", exception.getReason());
  }

  @Test
  void testRequestVsd_Success() {
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;

    ZetaHttpResponse mockResponse = mock(ZetaHttpResponse.class);
    when(mockResponse.getStatusCode()).thenReturn(200);
    when(mockResponse.getBody()).thenReturn(Optional.of("{\"resourceType\":\"Bundle\"}"));
    when(mockResponse.getHeaders()).thenReturn(java.util.Map.of());
    when(mockZetaClientService.sendToPepProxy(any(ZetaHttpRequest.class), eq(true)))
        .thenReturn(mockResponse);

    VsdmBundle mockBundle = mock(VsdmBundle.class);
    when(mockFhirService.parseString(anyString(), eq("json"))).thenReturn(mockBundle);
    when(mockFhirService.encodeResponse(eq(mockBundle), eq(EncodingType.JSON)))
        .thenReturn("encoded response");

    ResponseEntity<String> response =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token123", "etag123", false, false);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("encoded response", response.getBody());
  }

  @Test
  void testRequestVsd_ServerUnreachable() {
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;

    when(mockZetaClientService.sendToPepProxy(any(ZetaHttpRequest.class), eq(true)))
        .thenThrow(new ZetaHttpException("Service unreachable", null));

    ResponseEntity<String> response =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token123", "etag123", false, false);

    assertNotNull(response);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
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

  @Test
  void testRequestVsd_FromCache() {
    String terminal = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String cardId = "card1";
    String poppToken = "token123";
    String ifNoneMatch = "etag123";

    VsdmCachedValue cachedValue = new VsdmCachedValue("etag123", "pz123", "cached data");
    when(mockVsdmDataRepository.get(terminal, egkSlotId, cardId)).thenReturn(cachedValue);

    ResponseEntity<String> response =
        vsdmClientService.requestVsd(
            terminal, egkSlotId, smcBSlotId, mockEgkCard, poppToken, ifNoneMatch, false, false);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("cached data", response.getBody());
    assertEquals("pz123", response.getHeaders().getFirst(VsdmClientService.HEADER_VSDM_PZ));
    assertEquals("etag123", response.getHeaders().getFirst(VsdmClientService.HEADER_ETAG));
    verify(mockZetaClientService, never()).sendToPepProxy(any(), anyBoolean());
  }

  @Test
  void testRequestVsd_SuccessfulServerResponse() {
    String terminal = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String cardId = "card1";
    String poppToken = "token123";

    when(mockVsdmDataRepository.get(terminal, egkSlotId, cardId)).thenReturn(null);

    ZetaHttpResponse mockResponse = mock(ZetaHttpResponse.class);
    when(mockResponse.getStatusCode()).thenReturn(200);
    when(mockResponse.getBody()).thenReturn(Optional.of("{\"resourceType\":\"Bundle\"}"));
    when(mockResponse.getHeaders())
        .thenReturn(
            Map.of(
                "etag", List.of("new-etag"),
                "VSDM-Pz", List.of("new-pz"),
                "Content-Type", List.of("application/fhir+json")));
    when(mockZetaClientService.sendToPepProxy(any(ZetaHttpRequest.class), eq(true)))
        .thenReturn(mockResponse);

    VsdmBundle mockBundle = mock(VsdmBundle.class);
    when(mockFhirService.parseString(anyString(), eq("json"))).thenReturn(mockBundle);
    when(mockFhirService.encodeResponse(eq(mockBundle), eq(EncodingType.JSON)))
        .thenReturn("encoded response");

    ResponseEntity<String> response =
        vsdmClientService.requestVsd(
            terminal, egkSlotId, smcBSlotId, mockEgkCard, poppToken, null, false, false);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("encoded response", response.getBody());
    verify(mockVsdmDataRepository)
        .put(eq(terminal), eq(egkSlotId), eq(cardId), any(VsdmCachedValue.class));
  }

  @Test
  void testRequestVsd_WithXmlFormat() {
    String terminal = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String cardId = "card1";
    String poppToken = "token123";

    when(mockVsdmDataRepository.get(terminal, egkSlotId, cardId)).thenReturn(null);

    ZetaHttpResponse mockResponse = mock(ZetaHttpResponse.class);
    when(mockResponse.getStatusCode()).thenReturn(200);
    when(mockResponse.getBody())
        .thenReturn(Optional.of("<Bundle xmlns=\"http://hl7.org/fhir\"></Bundle>"));
    when(mockResponse.getHeaders()).thenReturn(Map.of());
    when(mockZetaClientService.sendToPepProxy(any(ZetaHttpRequest.class), eq(true)))
        .thenReturn(mockResponse);

    VsdmBundle mockBundle = mock(VsdmBundle.class);
    when(mockFhirService.parseString(anyString(), eq("xml"))).thenReturn(mockBundle);
    when(mockFhirService.encodeResponse(eq(mockBundle), eq(EncodingType.JSON)))
        .thenReturn("encoded xml response");

    ResponseEntity<String> response =
        vsdmClientService.requestVsd(
            terminal, egkSlotId, smcBSlotId, mockEgkCard, poppToken, null, true, false);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("encoded xml response", response.getBody());

    ArgumentCaptor<ZetaHttpRequest> requestCaptor = ArgumentCaptor.forClass(ZetaHttpRequest.class);
    verify(mockZetaClientService).sendToPepProxy(requestCaptor.capture(), eq(true));
    assertEquals("application/fhir+xml", requestCaptor.getValue().getHeader("Accept"));
  }

  @Test
  void testRequestVsd_ServerError() {
    String terminal = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String cardId = "card1";
    String poppToken = "token123";

    vsdmClientService =
        new VsdmClientService(
            vsdmConfig,
            mockPoppClientService,
            new FhirService(),
            mockPoppTokenRepository,
            mockVsdmDataRepository);

    when(mockVsdmDataRepository.get(terminal, egkSlotId, cardId)).thenReturn(null);

    ZetaHttpResponse mockResponse = mock(ZetaHttpResponse.class);
    when(mockResponse.getStatusCode()).thenReturn(500);

    VsdmOperationOutcome operationOutcome =
        VsdmOperationOutcomeBuilder.create()
            .withCode("79010")
            .withReference("VSDSERVICE_INTERNAL_SERVER_ERROR")
            .withText("text")
            .build();
    String operationOutcomeJson =
        new FhirService().encodeResponse(operationOutcome, EncodingType.JSON);
    when(mockResponse.getBody()).thenReturn(Optional.of(operationOutcomeJson));

    when(mockResponse.getHeaders()).thenReturn(Map.of());
    when(mockZetaClientService.sendToPepProxy(any(ZetaHttpRequest.class), eq(true)))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientService.requestVsd(
            terminal, egkSlotId, smcBSlotId, mockEgkCard, poppToken, null, false, false);

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertTrue(response.hasBody());
    assertTrue(response.getBody().contains("\"resourceType\":\"OperationOutcome\""));

    verify(mockVsdmDataRepository, never()).put(any(), any(), any(), any());
  }

  @Test
  void testRequestVsd_WithIfNoneMatchHeader() {
    String terminal = "terminal1";
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;
    String cardId = "card1";
    String poppToken = "token123";
    String ifNoneMatch = "existing-etag";

    when(mockVsdmDataRepository.get(terminal, egkSlotId, cardId)).thenReturn(null);

    ZetaHttpResponse mockResponse = mock(ZetaHttpResponse.class);
    when(mockResponse.getStatusCode()).thenReturn(304);
    when(mockResponse.getBody()).thenReturn(Optional.empty());
    when(mockResponse.getHeaders()).thenReturn(Map.of());
    when(mockZetaClientService.sendToPepProxy(any(ZetaHttpRequest.class), eq(true)))
        .thenReturn(mockResponse);

    ResponseEntity<String> response =
        vsdmClientService.requestVsd(
            terminal, egkSlotId, smcBSlotId, mockEgkCard, poppToken, ifNoneMatch, false, false);

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());

    ArgumentCaptor<ZetaHttpRequest> requestCaptor = ArgumentCaptor.forClass(ZetaHttpRequest.class);
    verify(mockZetaClientService).sendToPepProxy(requestCaptor.capture(), eq(true));
    assertEquals("existing-etag", requestCaptor.getValue().getHeader("If-None-Match"));
  }
}
