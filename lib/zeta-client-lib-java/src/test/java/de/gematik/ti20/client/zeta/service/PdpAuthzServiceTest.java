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

import de.gematik.ti20.client.zeta.auth.AuthContext;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.response.ZetaHttpResponse;
import de.gematik.ti20.zeta.base.model.WellKnown;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PdpAuthzServiceTest {

  @Test
  void testRequestAccessTokenSetsAccessToken() throws Exception {
    // Mocks für Abhängigkeiten
    ZetaClientService zetaClientService = mock(ZetaClientService.class);
    ZetaClientConfig config = mock(ZetaClientConfig.class);
    ZetaClientConfig.UserAgentConfig mockUserAgent =
        new ZetaClientConfig.UserAgentConfig("App", "1.0");

    when(config.getUserAgent()).thenReturn(mockUserAgent);
    when(zetaClientService.getZetaClientConfig()).thenReturn(config);
    when(config.getPathTokenAS()).thenReturn("/token");

    AuthContext ac = mock(AuthContext.class);
    ZetaHttpRequest origRequest = mock(ZetaHttpRequest.class);
    when(ac.getRequest()).thenReturn(origRequest);
    when(origRequest.getTraceId()).thenReturn("trace-1");

    var wellKnown = mock(WellKnown.class);
    when(ac.getWellKnownFromPep()).thenReturn(wellKnown);
    when(wellKnown.getAuthorization_endpoint()).thenReturn("https://auth.example.org/auth");

    when(ac.getSmcbAccessToken()).thenReturn("jwt-token");

    // Service unter Test, send wird überschrieben
    PdpAuthzService service =
        new PdpAuthzService(zetaClientService) {
          @Override
          public ZetaHttpResponse send(ZetaHttpRequest request) {
            // Simuliere erfolgreiche Antwort mit Access Token
            return new ZetaHttpResponse(200, Map.of("access_token", "abc123"));
          }
        };

    doNothing().when(ac).setAccessToken(any());

    // Testaufruf
    service.requestAccessToken(ac);

    // Überprüfe, dass setAccessToken mit dem erwarteten Wert aufgerufen wurde
    verify(ac)
        .setAccessToken(argThat(token -> "abc123".equals(((Map<?, ?>) token).get("access_token"))));
  }

  @Test
  void testRequestAccessTokenThrowsOnMalformedUrl() {
    ZetaClientService zetaClientService = mock(ZetaClientService.class);
    AuthContext ac = mock(AuthContext.class);
    var wellKnown = mock(WellKnown.class);
    when(ac.getWellKnownFromPep()).thenReturn(wellKnown);
    when(wellKnown.getAuthorization_endpoint()).thenReturn("ht!tp://invalid-url");

    when(ac.getRequest()).thenReturn(mock(ZetaHttpRequest.class));

    PdpAuthzService service = new PdpAuthzService(zetaClientService);

    assertThrows(ZetaHttpException.class, () -> service.requestAccessToken(ac));
  }
}
