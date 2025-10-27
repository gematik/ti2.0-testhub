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

class CardExceptionTest {

  @Test
  void testConstructorWithStatusWordAndMessage() {
    CardException ex = new CardException(0x6A82, "File not found");
    assertEquals(0x6A82, ex.getStatusWord());
    assertEquals("File not found", ex.getMessage());
    assertEquals("Error: File not found", ex.getStatusMessage());
    assertEquals("6A82", ex.getStatusWordHex());
  }

  @Test
  void testConstructorWithStatusWordMessageAndStatusMessage() {
    CardException ex = new CardException(0x6982, "Security status not satisfied", "Custom status");
    assertEquals(0x6982, ex.getStatusWord());
    assertEquals("Security status not satisfied", ex.getMessage());
    assertEquals("Custom status", ex.getStatusMessage());
    assertEquals("6982", ex.getStatusWordHex());
  }

  @Test
  void testConstructorWithMessageOnly() {
    CardException ex = new CardException("Unknown error occurred");
    assertEquals(0, ex.getStatusWord());
    assertEquals("Unknown error occurred", ex.getMessage());
    assertEquals("Unknown error", ex.getStatusMessage());
    assertEquals("0000", ex.getStatusWordHex());
  }
}
