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
package de.gematik.ti20.vsdm.fhir.def;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmBundleTest {

  @Test
  void testConstructor() {
    VsdmBundle bundle = new VsdmBundle();

    assertNotNull(bundle);
  }

  @Test
  void testInheritance() {
    VsdmBundle bundle = new VsdmBundle();

    assertTrue(bundle instanceof Bundle);
  }

  @Test
  void testGetResourceType() {
    VsdmBundle bundle = new VsdmBundle();

    assertEquals("Bundle", bundle.getResourceType().name());
  }

  @Test
  void testCanAddEntries() {
    VsdmBundle bundle = new VsdmBundle();
    Resource mockResource = mock(Resource.class);

    Bundle.BundleEntryComponent entry = bundle.addEntry();
    entry.setResource(mockResource);

    assertEquals(1, bundle.getEntry().size());
    assertEquals(mockResource, bundle.getEntry().get(0).getResource());
  }

  @Test
  void testBundleType() {
    VsdmBundle bundle = new VsdmBundle();

    // Test setting bundle type
    bundle.setType(Bundle.BundleType.COLLECTION);
    assertEquals(Bundle.BundleType.COLLECTION, bundle.getType());
  }

  @Test
  void testEmptyBundle() {
    VsdmBundle bundle = new VsdmBundle();

    assertTrue(bundle.getEntry().isEmpty());
    assertEquals(0, bundle.getEntry().size());
  }

  @Test
  void testHasDefaultConstructor() throws NoSuchMethodException {
    var constructor = VsdmBundle.class.getDeclaredConstructor();

    assertNotNull(constructor);
    assertEquals(0, constructor.getParameterCount());
  }
}
