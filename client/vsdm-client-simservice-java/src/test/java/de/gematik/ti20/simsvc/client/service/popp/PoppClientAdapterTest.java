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
package de.gematik.ti20.simsvc.client.service.popp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.client.config.PoppClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class PoppClientAdapterTest {

  @Mock private PoppClientConfig poppClientConfig;
  @Mock private WebClient webClient;
  @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock private WebClient.RequestBodySpec requestBodySpec;
  @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
  @Mock private WebClient.ResponseSpec responseSpec;

  private PoppClientAdapter poppClientAdapter;

  @BeforeEach
  void setUp() {
    poppClientAdapter = new PoppClientAdapter(poppClientConfig, webClient);
  }

  @Test
  void testConstructorInitializes() {
    assertNotNull(poppClientAdapter);
  }

  @Test
  void shouldRequestPoppTokenWithoutVirtualCard() {
    when(poppClientConfig.getTokenType()).thenReturn(PoppClientConfig.TokenType.CONTACT_CONNECTOR);
    when(poppClientConfig.getUrlPoppServerHttp()).thenReturn("http://localhost:8080/popp");
    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri("http://localhost:8080/popp")).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    when(requestBodySpec.bodyValue(any(PoppClientRequest.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(PoppClientResponse.class))
        .thenReturn(Mono.just(PoppClientResponse.ok("token-123")));

    String token = poppClientAdapter.getPoppToken();

    ArgumentCaptor<PoppClientRequest> payloadCaptor =
        ArgumentCaptor.forClass(PoppClientRequest.class);
    verify(requestBodySpec).bodyValue(payloadCaptor.capture());
    assertEquals("token-123", token);
    assertEquals("contact-connector", payloadCaptor.getValue().communicationType());
    assertEquals(null, payloadCaptor.getValue().virtualCard());
  }

  @Test
  void shouldRequestPoppTokenWithVirtualCard() {
    when(poppClientConfig.getTokenType()).thenReturn(PoppClientConfig.TokenType.CONTACT_VIRTUAL);
    when(poppClientConfig.getUrlPoppServerHttp()).thenReturn("http://localhost:8080/popp");
    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri("http://localhost:8080/popp")).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    when(requestBodySpec.bodyValue(any(PoppClientRequest.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(PoppClientResponse.class))
        .thenReturn(Mono.just(PoppClientResponse.ok("virtual-token")));

    String token = poppClientAdapter.getPoppToken("virtual-card-1");

    ArgumentCaptor<PoppClientRequest> payloadCaptor =
        ArgumentCaptor.forClass(PoppClientRequest.class);
    verify(requestBodySpec).bodyValue(payloadCaptor.capture());
    assertEquals("virtual-token", token);
    assertEquals("contact-virtual", payloadCaptor.getValue().communicationType());
    assertEquals("virtual-card-1", payloadCaptor.getValue().virtualCard());
  }

  @Test
  void shouldSupportContactlessConnectorTokenType() {
    when(poppClientConfig.getTokenType())
        .thenReturn(PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR);
    when(poppClientConfig.getUrlPoppServerHttp()).thenReturn("http://localhost:8080/popp");
    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.uri("http://localhost:8080/popp")).thenReturn(requestBodySpec);
    when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
    when(requestBodySpec.bodyValue(any(PoppClientRequest.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(PoppClientResponse.class))
        .thenReturn(Mono.just(PoppClientResponse.ok("token-456")));

    String token = poppClientAdapter.getPoppToken();

    ArgumentCaptor<PoppClientRequest> payloadCaptor =
        ArgumentCaptor.forClass(PoppClientRequest.class);
    verify(requestBodySpec).bodyValue(payloadCaptor.capture());
    assertEquals("token-456", token);
    assertEquals("contactless-connector", payloadCaptor.getValue().communicationType());
  }
}
