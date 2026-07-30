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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.simsvc.client.config.PoppClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class PoppClientAdapterTest {

  @Mock private PoppClientConfig poppClientConfig;

  @Mock private WebClient webClient;

  @Mock private AttachedCard attachedCard;

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
  void testAdapterAccessesPoppClientConfig() {
    String testUrl = "http://localhost:8080/popp";
    when(poppClientConfig.getTokenType()).thenReturn(PoppClientConfig.TokenType.CONTACT_CONNECTOR);
    when(poppClientConfig.getUrlPoppServerHttp(attachedCard)).thenReturn(testUrl);

    try {
      poppClientAdapter.getPoppToken(attachedCard);
    } catch (Exception e) {
      // Expected - WebClient is not fully mocked
    }

    verify(poppClientConfig, atLeastOnce()).getTokenType();
    verify(poppClientConfig, atLeastOnce()).getUrlPoppServerHttp(attachedCard);
  }

  @Test
  void testAdapterPassesCardInstanceToConfig() {
    String testUrl = "http://localhost:8080/popp";
    when(poppClientConfig.getTokenType())
        .thenReturn(PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR);
    when(poppClientConfig.getUrlPoppServerHttp(attachedCard)).thenReturn(testUrl);

    try {
      poppClientAdapter.getPoppToken(attachedCard);
    } catch (Exception e) {
      // Expected
    }

    ArgumentCaptor<AttachedCard> cardCaptor = ArgumentCaptor.forClass(AttachedCard.class);
    verify(poppClientConfig, atLeastOnce()).getUrlPoppServerHttp(cardCaptor.capture());
    assertSame(attachedCard, cardCaptor.getValue());
  }

  @Test
  void testAdapterSupportsContactConnectorTokenType() {
    String testUrl = "http://localhost:8080/popp";
    when(poppClientConfig.getTokenType()).thenReturn(PoppClientConfig.TokenType.CONTACT_CONNECTOR);
    when(poppClientConfig.getUrlPoppServerHttp(attachedCard)).thenReturn(testUrl);

    try {
      poppClientAdapter.getPoppToken(attachedCard);
    } catch (Exception e) {
      // Expected
    }

    verify(poppClientConfig, atLeastOnce()).getTokenType();
  }

  @Test
  void testAdapterSupportsContactlessConnectorTokenType() {
    String testUrl = "http://localhost:8080/popp";
    when(poppClientConfig.getTokenType())
        .thenReturn(PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR);
    when(poppClientConfig.getUrlPoppServerHttp(attachedCard)).thenReturn(testUrl);

    try {
      poppClientAdapter.getPoppToken(attachedCard);
    } catch (Exception e) {
      // Expected
    }

    verify(poppClientConfig, atLeastOnce()).getTokenType();
  }

  @Test
  void testAdapterSupportsContactVirtualTokenType() {
    String testUrl = "http://localhost:8080/popp";
    when(poppClientConfig.getTokenType()).thenReturn(PoppClientConfig.TokenType.CONTACT_VIRTUAL);
    when(poppClientConfig.getUrlPoppServerHttp(attachedCard)).thenReturn(testUrl);

    try {
      poppClientAdapter.getPoppToken(attachedCard);
    } catch (Exception e) {
      // Expected
    }

    verify(poppClientConfig, atLeastOnce()).getTokenType();
  }
}
