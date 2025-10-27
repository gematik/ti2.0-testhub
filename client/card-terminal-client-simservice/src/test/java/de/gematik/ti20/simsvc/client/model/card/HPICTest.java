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

class HPICTest {

  @Test
  void testGettersAndSetters() {
    HPIC hpic = new HPIC();
    hpic.setCardGeneration("G4");
    hpic.setCommonName("Klinik Musterstadt");
    hpic.setExpirationDate("2035-01-01");
    hpic.setIccsn("5566778899");
    hpic.setKeyDerivation("KD4");
    hpic.setObjectSystemIds("SYS4");

    assertEquals("G4", hpic.getCardGeneration());
    assertEquals("Klinik Musterstadt", hpic.getCommonName());
    assertEquals("2035-01-01", hpic.getExpirationDate());
    assertEquals("5566778899", hpic.getIccsn());
    assertEquals("KD4", hpic.getKeyDerivation());
    assertEquals("SYS4", hpic.getObjectSystemIds());
  }

  @Test
  void testApplicationsContainer() {
    HPIC.Applications applications = new HPIC.Applications();
    Application app1 = new Application();
    Application app2 = new Application();
    applications.setApplicationList(List.of(app1, app2));

    assertEquals(2, applications.getApplicationList().size());
    assertSame(app1, applications.getApplicationList().get(0));
    assertSame(app2, applications.getApplicationList().get(1));

    HPIC hpic = new HPIC();
    hpic.setApplications(applications);
    assertSame(applications, hpic.getApplications());
  }

  @Test
  void testGetAllKeysAndFiles() {
    // Key-Objekte mit passenden Namen für die Typ-Suche
    Key autKey = new Key();
    autKey.setName("PRK_HCI_AUT_E256");
    Key encKey = new Key();
    encKey.setName("PRK_HCI_ENC_E256");
    Key qesKey = new Key();
    qesKey.setName("PRK_HCI_QES_E256");

    // FileData-Objekte
    FileData file1 = new FileData("01", "file1", "data1");
    FileData file2 = new FileData("02", "file2", "data2");

    // Container mit Keys und Files
    Application.Containers containers = new Application.Containers();
    containers.setKeys(List.of(autKey, encKey, qesKey));
    containers.setFiles(List.of(file1, file2));

    // Application mit Container
    Application app = new Application();
    app.setContainers(containers);

    HPIC.Applications applications = new HPIC.Applications();
    applications.setApplicationList(List.of(app));

    HPIC hpic = new HPIC();
    hpic.setApplications(applications);

    // Test getAllKeys
    List<Key> allKeys = hpic.getAllKeys();
    assertEquals(3, allKeys.size());
    assertTrue(allKeys.contains(autKey));
    assertTrue(allKeys.contains(encKey));
    assertTrue(allKeys.contains(qesKey));

    // Test getAllFiles
    List<FileData> allFiles = hpic.getAllFiles();
    assertEquals(2, allFiles.size());
    assertTrue(allFiles.contains(file1));
    assertTrue(allFiles.contains(file2));
  }

  @Test
  void testFindKeyByType() {
    Key autKey = new Key();
    autKey.setName("PRK_HCI_AUT_E256");
    Key encKey = new Key();
    encKey.setName("PRK_HCI_ENC_R2048");
    Key qesKey = new Key();
    qesKey.setName("PRK_HCI_QES_E256");
    Key genericKey = new Key();
    genericKey.setName("SOME_OTHER_KEY");

    Application.Containers containers = new Application.Containers();
    containers.setKeys(List.of(autKey, encKey, qesKey, genericKey));

    Application app = new Application();
    app.setContainers(containers);

    HPIC.Applications applications = new HPIC.Applications();
    applications.setApplicationList(List.of(app));

    HPIC hpic = new HPIC();
    hpic.setApplications(applications);

    assertSame(autKey, hpic.findKeyByType("AUT"));
    assertSame(encKey, hpic.findKeyByType("ENC"));
    assertSame(qesKey, hpic.findKeyByType("QES"));
    assertSame(genericKey, hpic.findKeyByType("SOME_OTHER_KEY"));
    assertNull(hpic.findKeyByType("NOT_EXISTING"));
  }

  @Test
  void testDefaultConstructor() {
    HPIC hpic = new HPIC();
    assertNull(hpic.getApplications());
    assertNull(hpic.getCardGeneration());
    assertNull(hpic.getCommonName());
    assertNull(hpic.getExpirationDate());
    assertNull(hpic.getIccsn());
    assertNull(hpic.getKeyDerivation());
    assertNull(hpic.getObjectSystemIds());
    assertTrue(hpic.getAllKeys().isEmpty());
    assertTrue(hpic.getAllFiles().isEmpty());
  }
}
