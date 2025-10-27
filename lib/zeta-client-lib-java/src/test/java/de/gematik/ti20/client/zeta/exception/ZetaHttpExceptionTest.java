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
import org.junit.jupiter.api.Test;

class ZetaHttpExceptionTest {

  @Test
  void testConstructorWithMessageAndRequest() {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    ZetaHttpException ex = new ZetaHttpException("Fehler", request);

    assertEquals("Fehler", ex.getMessage());
    assertEquals(request, ex.getRequest());
    assertNull(ex.getCause());
  }

  @Test
  void testConstructorWithMessageCauseAndRequest() {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    Throwable cause = new RuntimeException("Ursache");
    ZetaHttpException ex = new ZetaHttpException("Fehler", cause, request);

    assertEquals("Fehler", ex.getMessage());
    assertEquals(request, ex.getRequest());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void testConstructorWithCauseAndRequest() {
    ZetaHttpRequest request = mock(ZetaHttpRequest.class);
    Throwable cause = new RuntimeException("Ursache");
    ZetaHttpException ex = new ZetaHttpException(cause, request);

    assertEquals("Ursache", ex.getMessage());
    assertEquals(request, ex.getRequest());
    assertEquals(cause, ex.getCause());
  }
}
