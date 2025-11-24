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
package de.gematik.ti20.client.zeta.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.zeta.base.model.WellKnown;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuthContextTest {

  @Test
  void testConstructorAndGetRequest() {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    AuthContext context = new AuthContext(request);
    assertEquals(request, context.getRequest());
  }

  @Test
  void testSetAndGetWellKnownFromPep() {
    AuthContext context = new AuthContext(mock(ZetaHttpRequest.class));
    WellKnown wellKnown = new WellKnown();
    context.setWellKnownFromPep(wellKnown);
    assertEquals(wellKnown, context.getWellKnownFromPep());
  }

  @Test
  void testSetAccessToken() {
    AuthContext context = new AuthContext(mock(ZetaHttpRequest.class));
    context.setAccessToken(Map.of("access_token", "abc123"));
    // Zugriff auf getRequestAuthorized() setzt den Header, gibt aber das gleiche Objekt zurück
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    AuthContext context2 = new AuthContext(request);
    context2.setAccessToken(Map.of("access_token", "token456"));
    context2.getRequestAuthorized();
    verify(request).setHeaderAuthorization(ZetaHttpRequest.AuthorizationType.DPOP, "token456");
  }

  @Test
  void testSetAndGetSmcbAccessToken() {
    AuthContext context = new AuthContext(mock(ZetaHttpRequest.class));
    context.setSmcbAccessToken("smcbToken");
    assertEquals("smcbToken", context.getSmcbAccessToken());
  }
}
