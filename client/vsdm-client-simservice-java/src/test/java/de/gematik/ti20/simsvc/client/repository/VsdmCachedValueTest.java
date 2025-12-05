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

    assertEquals(etag, cachedValue.etag());
    assertEquals(pruefziffer, cachedValue.pruefziffer());
    assertEquals(vsdmData, cachedValue.vsdmData());
  }

  @Test
  void testConstructorWithNullValues() {
    VsdmCachedValue cachedValue = new VsdmCachedValue(null, null, null);

    assertNull(cachedValue.etag());
    assertNull(cachedValue.pruefziffer());
    assertNull(cachedValue.vsdmData());
  }

  @Test
  void testConstructorWithEmptyStrings() {
    String emptyString = "";

    VsdmCachedValue cachedValue = new VsdmCachedValue(emptyString, emptyString, emptyString);

    assertEquals(emptyString, cachedValue.etag());
    assertEquals(emptyString, cachedValue.pruefziffer());
    assertEquals(emptyString, cachedValue.vsdmData());
  }

  @Test
  void testImmutability() {
    String originalEtag = "original-etag";
    String originalPruefziffer = "original-pz";
    String originalVsdmData = "original-data";

    VsdmCachedValue cachedValue =
        new VsdmCachedValue(originalEtag, originalPruefziffer, originalVsdmData);

    assertEquals(originalEtag, cachedValue.etag());
    assertEquals(originalPruefziffer, cachedValue.pruefziffer());
    assertEquals(originalVsdmData, cachedValue.vsdmData());
  }
}
