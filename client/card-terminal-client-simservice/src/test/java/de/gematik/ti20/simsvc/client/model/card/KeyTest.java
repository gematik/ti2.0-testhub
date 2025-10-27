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
package de.gematik.ti20.simsvc.client.model.card;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class KeyTest {

  @Test
  void testDefaultConstructorAndSetters() {
    Key key = new Key();
    key.setKeyRef("REF123");
    key.setName("AUT_KEY");
    key.setKeyIdentifier("ID456");
    key.setPrivateKey("cHJpdmF0ZUtleURhdGE=");

    assertEquals("REF123", key.getKeyRef());
    assertEquals("AUT_KEY", key.getName());
    assertEquals("ID456", key.getKeyIdentifier());
    assertEquals("cHJpdmF0ZUtleURhdGE=", key.getPrivateKey());
  }

  @Test
  void testAllArgsConstructor() {
    Key key = new Key("REF789", "ENC_KEY", "ID999", "YmFzZTY0U3RyaW5n");

    assertEquals("REF789", key.getKeyRef());
    assertEquals("ENC_KEY", key.getName());
    assertEquals("ID999", key.getKeyIdentifier());
    assertEquals("YmFzZTY0U3RyaW5n", key.getPrivateKey());
  }
}
