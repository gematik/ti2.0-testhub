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
package de.gematik.ti20.simsvc.client.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CardCommandExceptionTest {

  @Test
  void testConstructorAndGetters() {
    CardCommandException ex = new CardCommandException(0x6A82, "File not found", "00A40400");
    assertEquals(0x6A82, ex.getStatusWord());
    assertEquals("File not found (Command: 00A40400)", ex.getMessage());
    assertEquals("00A40400", ex.getCommand());
  }

  @Test
  void testGetMessageIncludesCommand() {
    CardCommandException ex =
        new CardCommandException(0x6982, "Security status not satisfied", "80CA9F7F00");
    String msg = ex.getMessage();
    assertTrue(msg.contains("Security status not satisfied"));
    assertTrue(msg.contains("80CA9F7F00"));
  }
}
