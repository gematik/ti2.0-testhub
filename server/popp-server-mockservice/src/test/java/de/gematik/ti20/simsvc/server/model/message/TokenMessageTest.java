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
package de.gematik.ti20.simsvc.server.model.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenMessageTest {

  @Test
  void testDefaultConstructorSetsType() {
    TokenMessage msg = new TokenMessage();
    assertEquals("Token", msg.getType());
    assertNull(msg.getToken());
    assertNull(msg.getPn());
  }

  @Test
  void testConstructorWithFields() {
    TokenMessage msg = new TokenMessage("myToken", "myPn");
    assertEquals("Token", msg.getType());
    assertEquals("myToken", msg.getToken());
    assertEquals("myPn", msg.getPn());
  }

  @Test
  void testSettersAndGetters() {
    TokenMessage msg = new TokenMessage();
    msg.setToken("abc123");
    msg.setPn("pnValue");
    assertEquals("abc123", msg.getToken());
    assertEquals("pnValue", msg.getPn());
  }
}
