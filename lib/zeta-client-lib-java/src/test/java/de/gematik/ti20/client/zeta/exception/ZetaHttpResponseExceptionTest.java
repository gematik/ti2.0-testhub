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
package de.gematik.ti20.client.zeta.exception;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.Test;

class ZetaHttpResponseExceptionTest {

  @Test
  void testConstructorWithMessageCauseAndRequest() {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    Throwable cause = new RuntimeException("Fehler");
    ZetaHttpResponseException ex =
        new ZetaHttpResponseException(404, "Nicht gefunden", cause, request);

    assertEquals(404, ex.getCode());
    assertEquals("Nicht gefunden", ex.getMessage());
    assertEquals(request, ex.getRequest());
    assertEquals(cause, ex.getCause());
    assertFalse(ex.isUnauthorized());
  }

  @Test
  void testConstructorWithMessageAndRequest() {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    ZetaHttpResponseException ex = new ZetaHttpResponseException(401, "Unauthorized", request);

    assertEquals(401, ex.getCode());
    assertEquals("Unauthorized", ex.getMessage());
    assertEquals(request, ex.getRequest());
    assertNull(ex.getCause());
    assertTrue(ex.isUnauthorized());
  }

  @Test
  void testIsUnauthorized() {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    ZetaHttpResponseException ex401 =
        new ZetaHttpResponseException(HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized", request);
    ZetaHttpResponseException ex403 =
        new ZetaHttpResponseException(HttpURLConnection.HTTP_FORBIDDEN, "Forbidden", request);

    assertTrue(ex401.isUnauthorized());
    assertFalse(ex403.isUnauthorized());
  }
}
