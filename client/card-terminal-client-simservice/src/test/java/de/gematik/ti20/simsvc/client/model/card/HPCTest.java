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

class HPCTest {

  @Test
  void testGettersAndSetters() {
    HPC hpc = new HPC();
    hpc.setCardGeneration("G3");
    hpc.setCommonName("Dr. Max Beispiel");
    hpc.setExpirationDate("2032-11-15");
    hpc.setIccsn("1122334455");
    hpc.setKeyDerivation("KD3");
    hpc.setObjectSystemIds("SYS3");

    assertEquals("G3", hpc.getCardGeneration());
    assertEquals("Dr. Max Beispiel", hpc.getCommonName());
    assertEquals("2032-11-15", hpc.getExpirationDate());
    assertEquals("1122334455", hpc.getIccsn());
    assertEquals("KD3", hpc.getKeyDerivation());
    assertEquals("SYS3", hpc.getObjectSystemIds());
  }

  @Test
  void testApplicationsContainer() {
    HPC.Applications applications = new HPC.Applications();
    Application app1 = new Application();
    Application app2 = new Application();
    applications.setApplicationList(List.of(app1, app2));

    assertEquals(2, applications.getApplicationList().size());
    assertSame(app1, applications.getApplicationList().get(0));
    assertSame(app2, applications.getApplicationList().get(1));

    HPC hpc = new HPC();
    hpc.setApplications(applications);
    assertSame(applications, hpc.getApplications());
  }

  @Test
  void testDefaultConstructor() {
    HPC hpc = new HPC();
    assertNull(hpc.getApplications());
    assertNull(hpc.getCardGeneration());
    assertNull(hpc.getCommonName());
    assertNull(hpc.getExpirationDate());
    assertNull(hpc.getIccsn());
    assertNull(hpc.getKeyDerivation());
    assertNull(hpc.getObjectSystemIds());
  }
}
