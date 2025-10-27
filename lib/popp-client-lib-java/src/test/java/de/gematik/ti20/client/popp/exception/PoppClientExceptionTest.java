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
package de.gematik.ti20.client.popp.exception;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import org.junit.jupiter.api.Test;

class PoppClientExceptionTest {

  @Test
  void testMessageConstructor() {
    PoppClientException ex = new PoppClientException("Fehler");
    assertEquals("Fehler", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testMessageAndCauseConstructor() {
    Throwable cause = new RuntimeException("Ursache");
    PoppClientException ex = new PoppClientException("Fehler", cause);
    assertEquals("Fehler", ex.getMessage());
    assertEquals(cause, ex.getCause());
  }

  @Test
  void testZetaHttpExceptionConstructor() {
    ZetaHttpException zetaEx = new ZetaHttpException("Zeta-Fehler", null);
    PoppClientException ex = new PoppClientException(zetaEx);
    assertEquals(zetaEx, ex.getCause());
    // Die Message ist die von zetaEx, da super(cause) auf Exception das übernimmt
    assertEquals(
        "de.gematik.ti20.client.zeta.exception.ZetaHttpException: Zeta-Fehler", ex.getMessage());
  }
}
