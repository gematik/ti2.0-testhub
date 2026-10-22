/*-
 * #%L
 * VSDM2 FHIR Library
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.vsdm.fhir.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.fhir.service.CodecServiceR4;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VsdmBundleBuilderTest {

  @Mock private Resource mockResource;

  @Mock private Patient mockPatient;

  @Test
  void testCreateBuilder() {
    VsdmBundleBuilder builder = VsdmBundleBuilder.create();

    assertNotNull(builder);
  }

  @Test
  void testInheritance() {
    VsdmBundleBuilder builder = VsdmBundleBuilder.create();

    assertTrue(builder instanceof ResourceBuilder);
  }

  @Test
  void testAddEntry() {
    VsdmBundleBuilder builder = VsdmBundleBuilder.create().addEntry(mockResource);

    assertNotNull(builder);
    assertEquals(1, builder.entries.size());
    assertEquals(mockResource, builder.entries.get(0));
  }

  @Test
  void testAddMultipleEntries() {
    VsdmBundleBuilder builder =
        VsdmBundleBuilder.create().addEntry(mockResource).addEntry(mockPatient);

    assertNotNull(builder);
    assertEquals(2, builder.entries.size());
    assertEquals(mockResource, builder.entries.get(0));
    assertEquals(mockPatient, builder.entries.get(1));
  }

  @Test
  void testBuildEmptyBundle() {
    VsdmBundle bundle = VsdmBundleBuilder.create().build();

    assertNotNull(bundle);
    assertTrue(bundle.getEntry().isEmpty());
    assertNotNull(bundle.getIdentifier());
    assertEquals("urn:ietf:rfc:3986", bundle.getIdentifier().getSystem());
    assertTrue(bundle.getIdentifier().getValue().startsWith("urn:uuid:"));
  }

  @Test
  void testBuildWithSingleEntry() {
    when(mockResource.getResourceType()).thenReturn(ResourceType.Patient);
    when(mockResource.getId()).thenReturn("patient123");

    VsdmBundle bundle = VsdmBundleBuilder.create().addEntry(mockResource).build();

    assertNotNull(bundle);
    assertEquals(1, bundle.getEntry().size());
    assertEquals(mockResource, bundle.getEntry().get(0).getResource());
    assertEquals(
        "https://gematik.de/fhir/Patient/patient123", bundle.getEntry().get(0).getFullUrl());
  }

  @Test
  void testBuildWithMultipleEntries() {
    when(mockResource.getResourceType()).thenReturn(ResourceType.Patient);
    when(mockResource.getId()).thenReturn("patient123");
    when(mockPatient.getResourceType()).thenReturn(ResourceType.Patient);
    when(mockPatient.getId()).thenReturn("patient456");

    VsdmBundle bundle =
        VsdmBundleBuilder.create().addEntry(mockResource).addEntry(mockPatient).build();

    assertNotNull(bundle);
    assertEquals(2, bundle.getEntry().size());
    assertEquals(mockResource, bundle.getEntry().get(0).getResource());
    assertEquals(mockPatient, bundle.getEntry().get(1).getResource());
  }

  @Test
  void testBundleIdentifierGeneration() {
    VsdmBundle bundle1 = VsdmBundleBuilder.create().build();
    VsdmBundle bundle2 = VsdmBundleBuilder.create().build();

    assertNotNull(bundle1.getIdentifier());
    assertNotNull(bundle2.getIdentifier());
    assertNotEquals(bundle1.getIdentifier().getValue(), bundle2.getIdentifier().getValue());
  }

  @Test
  void testChainedCalls() {
    when(mockResource.getResourceType()).thenReturn(ResourceType.Patient);
    when(mockResource.getId()).thenReturn("patient123");

    VsdmBundle bundle = VsdmBundleBuilder.create().addEntry(mockResource).build();

    assertNotNull(bundle);
    assertEquals(1, bundle.getEntry().size());
  }

  @Test
  void testFullUrlGeneration() {
    when(mockResource.getResourceType()).thenReturn(ResourceType.Coverage);
    when(mockResource.getId()).thenReturn("coverage789");

    VsdmBundle bundle = VsdmBundleBuilder.create().addEntry(mockResource).build();

    assertEquals(
        "https://gematik.de/fhir/Coverage/coverage789", bundle.getEntry().get(0).getFullUrl());
  }

  @Test
  void testBuildPreservesVersionedProfileAndLastUpdated() {
    VsdmBundle bundle = VsdmBundleBuilder.create().build();

    assertEquals(
        List.of("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle|1.1.0"),
        bundle.getMeta().getProfile().stream().map(CanonicalType::getValue).toList());
    assertTrue(bundle.getMeta().hasLastUpdated());
  }

  @ParameterizedTest
  @EnumSource(
      value = EncodingType.class,
      names = {"JSON", "XML"})
  void testSerializedBundlePreservesVersionedProfileAndLastUpdated(EncodingType encoding) {
    VsdmBundle bundle = VsdmBundleBuilder.create().build();
    var codec = new CodecServiceR4().getCodec();

    String encoded = codec.encode(bundle, encoding);
    Resource decoded = codec.decode(encoded);

    assertTrue(
        encoded.contains("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle|1.1.0"));
    assertEquals(
        List.of("https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle|1.1.0"),
        decoded.getMeta().getProfile().stream().map(CanonicalType::getValue).toList());
    assertEquals(bundle.getMeta().getLastUpdated(), decoded.getMeta().getLastUpdated());
  }
}
