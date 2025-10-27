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

class EGKTest {

  @Test
  void testGettersAndSetters() {
    EGK egk = new EGK();
    egk.setCardGeneration("G2");
    egk.setCommonName("Max Mustermann");
    egk.setExpirationDate("2030-12-31");
    egk.setIccsn("1234567890");
    egk.setKeyDerivation("KD1");
    egk.setObjectSystemIds("SYS1");

    assertEquals("G2", egk.getCardGeneration());
    assertEquals("Max Mustermann", egk.getCommonName());
    assertEquals("2030-12-31", egk.getExpirationDate());
    assertEquals("1234567890", egk.getIccsn());
    assertEquals("KD1", egk.getKeyDerivation());
    assertEquals("SYS1", egk.getObjectSystemIds());
  }

  @Test
  void testApplicationsContainer() {
    EGK.Applications applications = new EGK.Applications();
    Application app1 = new Application();
    Application app2 = new Application();
    applications.setApplicationList(List.of(app1, app2));

    assertEquals(2, applications.getApplicationList().size());
    assertSame(app1, applications.getApplicationList().get(0));
    assertSame(app2, applications.getApplicationList().get(1));

    EGK egk = new EGK();
    egk.setApplications(applications);
    assertSame(applications, egk.getApplications());
  }

  @Test
  void testDefaultConstructor() {
    EGK egk = new EGK();
    assertNull(egk.getApplications());
    assertNull(egk.getCardGeneration());
    assertNull(egk.getCommonName());
    assertNull(egk.getExpirationDate());
    assertNull(egk.getIccsn());
    assertNull(egk.getKeyDerivation());
    assertNull(egk.getObjectSystemIds());
  }
}
