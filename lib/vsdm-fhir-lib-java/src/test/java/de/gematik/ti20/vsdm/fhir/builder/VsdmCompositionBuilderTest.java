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
package de.gematik.ti20.vsdm.fhir.builder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmCompositionBuilderTest {

  @Mock private Resource mockResource;

  @Mock private Patient mockPatient;

  @Test
  void testCreateBuilder() {
    VsdmCompositionBuilder builder = VsdmCompositionBuilder.create();

    assertNotNull(builder);
  }

  @Test
  void testInheritance() {
    VsdmCompositionBuilder builder = VsdmCompositionBuilder.create();

    assertTrue(builder instanceof ResourceBuilder);
  }

  @Test
  void testAddEntry() {
    VsdmCompositionBuilder builder = VsdmCompositionBuilder.create().addEntry(mockResource);

    assertNotNull(builder);
    assertEquals(1, builder.entries.size());
    assertEquals(mockResource, builder.entries.get(0));
  }

  @Test
  void testAddMultipleEntries() {
    VsdmCompositionBuilder builder =
        VsdmCompositionBuilder.create().addEntry(mockResource).addEntry(mockPatient);

    assertNotNull(builder);
    assertEquals(2, builder.entries.size());
    assertEquals(mockResource, builder.entries.get(0));
    assertEquals(mockPatient, builder.entries.get(1));
  }

  @Test
  void testBuildEmptyBundle() {
    VsdmBundle bundle = VsdmCompositionBuilder.create().build();

    assertNotNull(bundle);
    assertTrue(bundle.getEntry().isEmpty());
    assertEquals(Bundle.BundleType.DOCUMENT, bundle.getType());
    assertNotNull(bundle.getIdentifier());
    assertEquals("urn:ietf:rfc:3986", bundle.getIdentifier().getSystem());
    assertTrue(bundle.getIdentifier().getValue().startsWith("urn:uuid"));
  }

  @Test
  void testBuildWithSingleEntry() {
    VsdmBundle bundle = VsdmCompositionBuilder.create().addEntry(mockResource).build();

    assertNotNull(bundle);
    assertEquals(1, bundle.getEntry().size());
    assertEquals(mockResource, bundle.getEntry().get(0).getResource());
    assertEquals(Bundle.BundleType.DOCUMENT, bundle.getType());
  }

  @Test
  void testBuildWithMultipleEntries() {
    VsdmBundle bundle =
        VsdmCompositionBuilder.create().addEntry(mockResource).addEntry(mockPatient).build();

    assertNotNull(bundle);
    assertEquals(2, bundle.getEntry().size());
    assertEquals(mockResource, bundle.getEntry().get(0).getResource());
    assertEquals(mockPatient, bundle.getEntry().get(1).getResource());
  }

  @Test
  void testBundleTypeIsDocument() {
    VsdmBundle bundle = VsdmCompositionBuilder.create().build();

    assertEquals(Bundle.BundleType.DOCUMENT, bundle.getType());
  }

  @Test
  void testBundleIdentifierGeneration() {
    VsdmBundle bundle1 = VsdmCompositionBuilder.create().build();
    VsdmBundle bundle2 = VsdmCompositionBuilder.create().build();

    assertNotNull(bundle1.getIdentifier());
    assertNotNull(bundle2.getIdentifier());
    assertNotEquals(bundle1.getIdentifier().getValue(), bundle2.getIdentifier().getValue());
  }

  @Test
  void testChainedCalls() {
    VsdmBundle bundle =
        VsdmCompositionBuilder.create().addEntry(mockResource).addEntry(mockPatient).build();

    assertNotNull(bundle);
    assertEquals(2, bundle.getEntry().size());
  }

  @Test
  void testIdentifierSystemAndFormat() {
    VsdmBundle bundle = VsdmCompositionBuilder.create().build();

    assertEquals("urn:ietf:rfc:3986", bundle.getIdentifier().getSystem());
    assertTrue(bundle.getIdentifier().getValue().startsWith("urn:uuid"));
    assertTrue(bundle.getIdentifier().getValue().contains(bundle.getId()));
  }

  @Test
  void testEntriesListInitialization() {
    VsdmCompositionBuilder builder = VsdmCompositionBuilder.create();

    assertNotNull(builder.entries);
    assertTrue(builder.entries.isEmpty());
  }
}
