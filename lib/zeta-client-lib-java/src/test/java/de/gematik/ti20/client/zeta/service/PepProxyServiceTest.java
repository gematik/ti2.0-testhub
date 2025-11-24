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
import org.junit.jupiter.api.Test;

class PepProxyServiceTest {

  @Test
  void testSendDelegatesToSuper() {
    ZetaClientService zetaClientService = mock(ZetaClientService.class);
    PepProxyService service =
        new PepProxyService(zetaClientService) {
          @Override
          public ZetaHttpResponse send(ZetaHttpRequest request) {
            return new ZetaHttpResponse(200, "ok");
          }
        };
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    ZetaHttpResponse response = service.send(request);
    assertEquals(200, response.getStatusCode());
    assertEquals("ok", response.getBody().orElse(null));
  }

  @Test
  void testRequestWellKnownSetsWellKnownFromPep() {
    ZetaClientService zetaClientService = mock(ZetaClientService.class);
    ZetaClientConfig.UserAgentConfig mockUserAgent =
        new ZetaClientConfig.UserAgentConfig("App", "1.0");

    var config = mock(de.gematik.ti20.client.zeta.config.ZetaClientConfig.class);
    when(config.getUserAgent()).thenReturn(mockUserAgent);
    when(zetaClientService.getZetaClientConfig()).thenReturn(config);

    AuthContext ac = mock(AuthContext.class);
    ZetaHttpRequest origRequest = mock(ZetaHttpRequest.class);
    when(ac.getRequest()).thenReturn(origRequest);
    when(origRequest.getUrl()).thenReturn("https://foo.bar/resource");
    when(origRequest.getTraceId()).thenReturn("trace-1");
    //    when(origRequest instanceof ZetaWsRequest).thenReturn(false);

    WellKnown wellKnown = new WellKnown();
    ZetaHttpResponse fakeResponse = new ZetaHttpResponse(200, wellKnown);

    PepProxyService service =
        new PepProxyService(zetaClientService) {
          @Override
          public ZetaHttpResponse send(ZetaHttpRequest request) {
            return fakeResponse;
          }
        };

    doNothing().when(ac).setWellKnownFromPep(any());

    service.requestWellKnown(ac);

    verify(ac).setWellKnownFromPep(any(WellKnown.class));
  }

  @Test
  void testRequestWellKnownThrowsOnMalformedUrl() {
    ZetaClientService zetaClientService = mock(ZetaClientService.class);
    AuthContext ac = mock(AuthContext.class);
    ZetaHttpRequest origRequest = mock(ZetaHttpRequest.class);
    when(ac.getRequest()).thenReturn(origRequest);
    when(origRequest.getUrl()).thenReturn("ht!tp://invalid-url");

    PepProxyService service = new PepProxyService(zetaClientService);

    assertThrows(ZetaHttpException.class, () -> service.requestWellKnown(ac));
  }
}
