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
package de.gematik.ti20.client.zeta.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.exception.ZetaHttpResponseException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.response.ZetaHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZetaClientServiceTest {

  private ZetaClientConfig config;
  private PepProxyService pepProxyService;
  private PdpAuthzService pdpAuthzService;
  private CardTerminalService cardTerminalService;
  private ZetaClientService zetaClientService;

  @BeforeEach
  void setUp() {
    config = mock(ZetaClientConfig.class);
    cardTerminalService = mock(CardTerminalService.class);
    pepProxyService = mock(PepProxyService.class);
    pdpAuthzService = mock(PdpAuthzService.class);

    zetaClientService =
        new ZetaClientService(config) {
          @Override
          public PepProxyService getPepProxyService() {
            return pepProxyService;
          }

          @Override
          public PdpAuthzService getPdpAuthzService() {
            return pdpAuthzService;
          }

          @Override
          public CardTerminalService getCardTerminalService() {
            return cardTerminalService;
          }

          @Override
          public ZetaClientConfig getZetaClientConfig() {
            return config;
          }

          // Autorisierung überspringen
          @Override
          public ZetaHttpResponse authorizeAndResend(ZetaHttpRequest request) {
            return new ZetaHttpResponse(200, "authorized");
          }
        };
  }

  @Test
  void testSendToPepProxySuccess() throws Exception {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    ZetaHttpResponse response = new ZetaHttpResponse(200, "ok");
    when(pepProxyService.send(request)).thenReturn(response);

    ZetaHttpResponse result = zetaClientService.sendToPepProxy(request, false);

    assertEquals(200, result.getStatusCode());
    assertEquals("ok", result.getBody().orElse(null));
  }

  @Test
  void testSendToPepProxyUnauthorizedTriggersAuthorizeAndResend() throws Exception {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    ZetaHttpResponseException ex = mock(ZetaHttpResponseException.class);
    when(ex.isUnauthorized()).thenReturn(true);
    when(ex.getRequest()).thenReturn(request);

    when(pepProxyService.send(request)).thenThrow(ex);

    ZetaHttpResponse result = zetaClientService.sendToPepProxy(request, true);

    assertEquals(200, result.getStatusCode());
    assertEquals("authorized", result.getBody().orElse(null));
  }
}
