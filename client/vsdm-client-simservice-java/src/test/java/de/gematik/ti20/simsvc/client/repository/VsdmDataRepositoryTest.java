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
package de.gematik.ti20.simsvc.client.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VsdmDataRepositoryTest {

  private VsdmDataRepository repository;

  @BeforeEach
  void setUp() {
    repository = new VsdmDataRepository();
  }

  @Test
  void testPutAndGet() {
    String terminalId = "terminal1";
    Integer slotId = 1;
    String cardId = "card1";
    VsdmCachedValue cachedValue = new VsdmCachedValue("etag123", "pz456", "test data");

    repository.put(terminalId, slotId, cardId, cachedValue);
    VsdmCachedValue result = repository.get(terminalId, slotId, cardId);

    assertEquals(cachedValue, result);
    assertEquals("etag123", result.getEtag());
    assertEquals("pz456", result.getPruefziffer());
    assertEquals("test data", result.getVsdmData());
  }

  @Test
  void testGetNonExistentValue() {
    VsdmCachedValue result = repository.get("terminal1", 1, "card1");

    assertNull(result);
  }

  @Test
  void testPutOverwritesExistingValue() {
    String terminalId = "terminal1";
    Integer slotId = 1;
    String cardId = "card1";
    VsdmCachedValue oldValue = new VsdmCachedValue("old-etag", "old-pz", "old data");
    VsdmCachedValue newValue = new VsdmCachedValue("new-etag", "new-pz", "new data");

    repository.put(terminalId, slotId, cardId, oldValue);
    repository.put(terminalId, slotId, cardId, newValue);
    VsdmCachedValue result = repository.get(terminalId, slotId, cardId);

    assertEquals(newValue, result);
    assertEquals("new-etag", result.getEtag());
  }

  @Test
  void testDifferentKeysStoreDifferentValues() {
    VsdmCachedValue value1 = new VsdmCachedValue("etag1", "pz1", "data1");
    VsdmCachedValue value2 = new VsdmCachedValue("etag2", "pz2", "data2");
    VsdmCachedValue value3 = new VsdmCachedValue("etag3", "pz3", "data3");
    VsdmCachedValue value4 = new VsdmCachedValue("etag4", "pz4", "data4");

    repository.put("terminal1", 1, "card1", value1);
    repository.put("terminal2", 1, "card1", value2);
    repository.put("terminal1", 2, "card1", value3);
    repository.put("terminal1", 1, "card2", value4);

    assertEquals(value1, repository.get("terminal1", 1, "card1"));
    assertEquals(value2, repository.get("terminal2", 1, "card1"));
    assertEquals(value3, repository.get("terminal1", 2, "card1"));
    assertEquals(value4, repository.get("terminal1", 1, "card2"));
  }

  @Test
  void testWithNullValues() {
    VsdmCachedValue cachedValue = new VsdmCachedValue("etag", "pz", "data");

    repository.put(null, null, null, cachedValue);
    VsdmCachedValue result = repository.get(null, null, null);

    assertEquals(cachedValue, result);
  }

  @Test
  void testPutNullCachedValue() {
    String terminalId = "terminal1";
    Integer slotId = 1;
    String cardId = "card1";

    repository.put(terminalId, slotId, cardId, null);
    VsdmCachedValue result = repository.get(terminalId, slotId, cardId);

    assertNull(result);
  }

  @Test
  void testHashCollisionHandling() {
    VsdmCachedValue value1 = new VsdmCachedValue("etag1", "pz1", "data1");
    VsdmCachedValue value2 = new VsdmCachedValue("etag2", "pz2", "data2");

    repository.put("a", 1, "b", value1);
    repository.put("b", 1, "a", value2);

    assertEquals(value1, repository.get("a", 1, "b"));
    assertEquals(value2, repository.get("b", 1, "a"));
  }

  @Test
  void testConcurrentAccess() {
    String terminalId = "terminal1";
    Integer slotId = 1;
    String cardId = "card1";
    VsdmCachedValue cachedValue = new VsdmCachedValue("etag", "pz", "data");

    repository.put(terminalId, slotId, cardId, cachedValue);

    assertEquals(cachedValue, repository.get(terminalId, slotId, cardId));
    assertEquals(cachedValue, repository.get(terminalId, slotId, cardId));
  }
}
