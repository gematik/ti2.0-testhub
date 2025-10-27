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
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CardImageTest {

  @Test
  void testGettersAndSetters() {
    CardImage card = new CardImage();
    card.setCardTypeString("EGK");
    card.setId("card-1");
    card.setLabel("Testkarte");

    assertEquals("EGK", card.getCardTypeString());
    assertEquals("card-1", card.getId());
    assertEquals("Testkarte", card.getLabel());
  }

  @Test
  void testCardTypeDetectionByString() {
    CardImage card = new CardImage();
    card.setCardTypeString("HPC");
    assertEquals(CardType.HPC, card.getCardType());

    card.setCardTypeString("HPIC");
    assertEquals(CardType.HPIC, card.getCardType());

    card.setCardTypeString("UNKNOWN");
    assertEquals(CardType.EGK, card.getCardType());
  }

  @Test
  void testCardTypeDetectionByElement() {
    CardImage card = new CardImage();
    card.setEgk(mock(EGK.class));
    assertEquals(CardType.EGK, card.getCardType());

    card = new CardImage();
    card.setHpc(mock(HPC.class));
    assertEquals(CardType.HPC, card.getCardType());

    card = new CardImage();
    card.setHpic(mock(HPIC.class));
    assertEquals(CardType.HPIC, card.getCardType());
  }

  @Test
  void testGetAllKeys() {
    // EGK mit Keys
    Application app = new Application();
    Application.Containers containers = new Application.Containers();
    Key key = new Key();
    containers.setKeys(List.of(key));
    app.setContainers(containers);

    EGK.Applications applications = mock(EGK.Applications.class);
    when(applications.getApplicationList()).thenReturn(List.of(app));
    EGK egk = mock(EGK.class);
    when(egk.getApplications()).thenReturn(applications);

    CardImage card = new CardImage();
    card.setEgk(egk);

    List<Key> keys = card.getAllKeys();
    assertEquals(1, keys.size());
    assertSame(key, keys.get(0));
  }

  @Test
  void testGetAllFiles() {
    // HPC mit Files
    Application app = new Application();
    Application.Containers containers = new Application.Containers();
    FileData file = new FileData();
    containers.setFiles(List.of(file));
    app.setContainers(containers);

    HPC.Applications applications = mock(HPC.Applications.class);
    when(applications.getApplicationList()).thenReturn(List.of(app));
    HPC hpc = mock(HPC.class);
    when(hpc.getApplications()).thenReturn(applications);

    CardImage card = new CardImage();
    card.setHpc(hpc);

    List<FileData> files = card.getAllFiles();
    assertEquals(1, files.size());
    assertSame(file, files.get(0));
  }

  @Test
  void testGetAllKeysAndFilesFromHPIC() {
    HPIC hpic = mock(HPIC.class);
    Key key = new Key();
    FileData file = new FileData();
    when(hpic.getAllKeys()).thenReturn(List.of(key));
    when(hpic.getAllFiles()).thenReturn(List.of(file));

    CardImage card = new CardImage();
    card.setHpic(hpic);

    assertEquals(1, card.getAllKeys().size());
    assertEquals(1, card.getAllFiles().size());
  }
}
