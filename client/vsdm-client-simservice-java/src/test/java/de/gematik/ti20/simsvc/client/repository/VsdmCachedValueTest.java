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

import org.junit.jupiter.api.Test;

class VsdmCachedValueTest {

  @Test
  void testConstructorAndGetters() {
    String etag = "etag123";
    String pruefziffer = "pz456";
    String vsdmData = "test data";

    VsdmCachedValue cachedValue = new VsdmCachedValue(etag, pruefziffer, vsdmData);

    assertEquals(etag, cachedValue.getEtag());
    assertEquals(pruefziffer, cachedValue.getPruefziffer());
    assertEquals(vsdmData, cachedValue.getVsdmData());
  }

  @Test
  void testConstructorWithNullValues() {
    VsdmCachedValue cachedValue = new VsdmCachedValue(null, null, null);

    assertNull(cachedValue.getEtag());
    assertNull(cachedValue.getPruefziffer());
    assertNull(cachedValue.getVsdmData());
  }

  @Test
  void testConstructorWithEmptyStrings() {
    String emptyString = "";

    VsdmCachedValue cachedValue = new VsdmCachedValue(emptyString, emptyString, emptyString);

    assertEquals(emptyString, cachedValue.getEtag());
    assertEquals(emptyString, cachedValue.getPruefziffer());
    assertEquals(emptyString, cachedValue.getVsdmData());
  }

  @Test
  void testImmutability() {
    String originalEtag = "original-etag";
    String originalPruefziffer = "original-pz";
    String originalVsdmData = "original-data";

    VsdmCachedValue cachedValue =
        new VsdmCachedValue(originalEtag, originalPruefziffer, originalVsdmData);

    assertEquals(originalEtag, cachedValue.getEtag());
    assertEquals(originalPruefziffer, cachedValue.getPruefziffer());
    assertEquals(originalVsdmData, cachedValue.getVsdmData());
  }
}
