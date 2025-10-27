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
package de.gematik.ti20.client.card.exception;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.client.card.message.CardMessage;
import org.junit.jupiter.api.Test;

class CardExceptionTest {

  @Test
  void testConstructorWithMessageAndRequest() {
    CardMessage msg = new CardMessage();
    CardException ex = new CardException("Fehlertext", msg);
    assertEquals("Fehlertext", ex.getMessage());
    assertEquals(msg, ex.getRequest());
  }

  @Test
  void testConstructorWithMessageCauseAndRequest() {
    Throwable cause = new RuntimeException("Ursache");
    CardMessage msg = new CardMessage();
    CardException ex = new CardException("Fehlertext", cause, msg);
    assertEquals("Fehlertext", ex.getMessage());
    assertEquals(cause, ex.getCause());
    assertEquals(msg, ex.getRequest());
  }

  @Test
  void testConstructorWithCauseAndRequest() {
    Throwable cause = new RuntimeException("Ursache");
    CardMessage msg = new CardMessage();
    CardException ex = new CardException(cause, msg);
    assertEquals("Ursache", ex.getMessage());
    assertEquals(cause, ex.getCause());
    assertEquals(msg, ex.getRequest());
  }

  @Test
  void testConstructorWithCauseOnly() {
    Throwable cause = new RuntimeException("Nur Ursache");
    CardException ex = new CardException(cause);
    assertEquals("Nur Ursache", ex.getMessage());
    assertEquals(cause, ex.getCause());
    assertNull(ex.getRequest());
  }
}
