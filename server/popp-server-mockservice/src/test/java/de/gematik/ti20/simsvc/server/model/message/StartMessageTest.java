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

class StartMessageTest {

  @Test
  void testDefaultConstructorSetsTypeAndVersion() {
    StartMessage msg = new StartMessage();
    assertEquals("StartMessage", msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertNull(msg.getCardConnectionType());
    assertNull(msg.getClientSessionId());
  }

  @Test
  void testConstructorWithFields() {
    StartMessage msg = new StartMessage("CT", "session42");
    assertEquals("StartMessage", msg.getType());
    assertEquals("1.0.0", msg.getVersion());
    assertEquals("CT", msg.getCardConnectionType());
    assertEquals("session42", msg.getClientSessionId());
  }

  @Test
  void testSettersAndGetters() {
    StartMessage msg = new StartMessage();
    msg.setCardConnectionType("ICC");
    msg.setClientSessionId("abc123");
    msg.setVersion("2.0.0");
    assertEquals("ICC", msg.getCardConnectionType());
    assertEquals("abc123", msg.getClientSessionId());
    assertEquals("2.0.0", msg.getVersion());
  }
}
