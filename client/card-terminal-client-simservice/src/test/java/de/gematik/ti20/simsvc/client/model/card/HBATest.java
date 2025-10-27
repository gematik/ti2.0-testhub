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

class HBATest {

  @Test
  void testGettersAndSetters() {
    HBA hba = new HBA();
    hba.setCardGeneration("G2");
    hba.setCommonName("Dr. Erika Musterfrau");
    hba.setExpirationDate("2031-05-20");
    hba.setIccsn("9876543210");
    hba.setKeyDerivation("KD2");
    hba.setObjectSystemIds("SYS2");

    assertEquals("G2", hba.getCardGeneration());
    assertEquals("Dr. Erika Musterfrau", hba.getCommonName());
    assertEquals("2031-05-20", hba.getExpirationDate());
    assertEquals("9876543210", hba.getIccsn());
    assertEquals("KD2", hba.getKeyDerivation());
    assertEquals("SYS2", hba.getObjectSystemIds());
  }

  @Test
  void testApplicationsContainer() {
    HBA.Applications applications = new HBA.Applications();
    Application app1 = new Application();
    Application app2 = new Application();
    applications.setApplicationList(List.of(app1, app2));

    assertEquals(2, applications.getApplicationList().size());
    assertSame(app1, applications.getApplicationList().get(0));
    assertSame(app2, applications.getApplicationList().get(1));

    HBA hba = new HBA();
    hba.setApplications(applications);
    assertSame(applications, hba.getApplications());
  }

  @Test
  void testDefaultConstructor() {
    HBA hba = new HBA();
    assertNull(hba.getApplications());
    assertNull(hba.getCardGeneration());
    assertNull(hba.getCommonName());
    assertNull(hba.getExpirationDate());
    assertNull(hba.getIccsn());
    assertNull(hba.getKeyDerivation());
    assertNull(hba.getObjectSystemIds());
  }
}
