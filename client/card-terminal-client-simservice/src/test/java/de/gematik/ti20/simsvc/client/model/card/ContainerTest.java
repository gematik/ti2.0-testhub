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

import java.util.List;
import org.junit.jupiter.api.Test;

class ContainerTest {

  @Test
  void testGettersAndSetters() {
    FileData file = new FileData();
    file.setFileId("F1");
    Key key = new Key();
    key.setKeyRef("K1");
    Pin pin = new Pin();
    pin.setPinRef("P1");

    Container container = new Container();
    container.setFiles(List.of(file));
    container.setKeys(List.of(key));
    container.setPins(List.of(pin));

    assertEquals(1, container.getFiles().size());
    assertEquals(1, container.getKeys().size());
    assertEquals(1, container.getPins().size());
  }

  @Test
  void testConstructorWithAllElements() {
    FileData file = new FileData();
    Key key = new Key();
    Pin pin = new Pin();
    Container container = new Container(List.of(file), List.of(key), List.of(pin));
    assertEquals(1, container.getFiles().size());
    assertEquals(1, container.getKeys().size());
    assertEquals(1, container.getPins().size());
  }

  @Test
  void testFindFileById() {
    FileData file1 = new FileData();
    file1.setFileId("F1");
    FileData file2 = new FileData();
    file2.setFileId("F2");
    Container container = new Container(List.of(file1, file2), null, null);

    assertSame(file1, container.findFileById("F1"));
    assertSame(file2, container.findFileById("f2"));
    assertNull(container.findFileById("F3"));
  }

  @Test
  void testFindKeyByRef() {
    Key key1 = new Key();
    key1.setKeyRef("K1");
    Key key2 = new Key();
    key2.setKeyRef("K2");
    Container container = new Container(null, List.of(key1, key2), null);

    assertSame(key1, container.findKeyByRef("K1"));
    assertSame(key2, container.findKeyByRef("k2"));
    assertNull(container.findKeyByRef("K3"));
  }

  @Test
  void testFindPinByRef() {
    Pin pin1 = new Pin();
    pin1.setPinRef("P1");
    Pin pin2 = new Pin();
    pin2.setPinRef("P2");
    Container container = new Container(null, null, List.of(pin1, pin2));

    assertSame(pin1, container.findPinByRef("P1"));
    assertSame(pin2, container.findPinByRef("p2"));
    assertNull(container.findPinByRef("P3"));
  }

  @Test
  void testNullLists() {
    Container container = new Container();
    assertNull(container.findFileById("X"));
    assertNull(container.findKeyByRef("Y"));
    assertNull(container.findPinByRef("Z"));
  }
}
