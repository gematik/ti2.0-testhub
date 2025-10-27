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

class ApplicationTest {

  @Test
  void testApplicationGettersAndSetters() {
    Application app = new Application();
    app.setApplicationId("AID123");
    app.setDeactivated(true);

    Application.Containers containers = new Application.Containers();
    FileData file = new FileData();
    Key key = new Key();
    Pin pin = new Pin();

    containers.setFiles(List.of(file));
    containers.setKeys(List.of(key));
    containers.setPins(List.of(pin));

    app.setContainers(containers);

    assertEquals("AID123", app.getApplicationId());
    assertTrue(app.isDeactivated());
    assertNotNull(app.getContainers());
    assertEquals(1, app.getContainers().getFiles().size());
    assertEquals(1, app.getContainers().getKeys().size());
    assertEquals(1, app.getContainers().getPins().size());
  }

  @Test
  void testDefaultConstructor() {
    Application app = new Application();
    assertNull(app.getApplicationId());
    assertFalse(app.isDeactivated());
    assertNull(app.getContainers());
  }
}
