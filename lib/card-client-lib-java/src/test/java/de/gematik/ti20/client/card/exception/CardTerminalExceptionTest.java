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

import de.gematik.ti20.client.card.terminal.CardTerminalV1;
import org.junit.jupiter.api.Test;

class CardTerminalExceptionTest {

  @Test
  void testConstructorAndGetter() {
    CardTerminalV1 terminal = null;
    CardTerminalException ex = new CardTerminalException("Fehler", terminal);
    assertEquals("Fehler", ex.getMessage());
    assertEquals(terminal, ex.getCardTerminal());
    assertNull(ex.getCause());
  }

  @Test
  void testConstructorWithCause() {
    CardTerminalV1 terminal = null;
    Throwable cause = new RuntimeException("Ursache");
    CardTerminalException ex = new CardTerminalException("Fehler2", terminal, cause);
    assertEquals("Fehler2", ex.getMessage());
    assertEquals(terminal, ex.getCardTerminal());
    assertEquals(cause, ex.getCause());
  }
}
