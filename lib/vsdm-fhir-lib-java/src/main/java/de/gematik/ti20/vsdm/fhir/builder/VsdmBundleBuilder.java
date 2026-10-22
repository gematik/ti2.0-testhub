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

import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.bbriccs.fhir.coding.version.GenericProfileVersion;
import de.gematik.ti20.vsdm.fhir.def.StructureDefinitions;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Resource;

public final class VsdmBundleBuilder extends ResourceBuilder<VsdmBundle, VsdmBundleBuilder> {

  private static final GenericProfileVersion PROFILE_VERSION =
      new GenericProfileVersion(VsdmBundle.PROFILE_VERSION, false);

  List<Resource> entries = new ArrayList<>();

  private VsdmBundleBuilder() {}

  public static VsdmBundleBuilder create() {
    return new VsdmBundleBuilder();
  }

  public VsdmBundleBuilder addEntry(Resource entry) {
    this.entries.add(entry);
    return this;
  }

  public VsdmBundle build() {
    var bundle =
        this.createResource(VsdmBundle::new, StructureDefinitions.VSDM_BUNDLE, PROFILE_VERSION);

    bundle.setType(Bundle.BundleType.COLLECTION);
    bundle.setIdentifier(
        new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid:" + bundle.getId()));

    bundle
        .getMeta()
        .setLastUpdatedElement(
            new InstantType(Instant.now().truncatedTo(ChronoUnit.MILLIS).toString()));

    for (Resource entry : this.entries) {
      bundle
          .addEntry()
          .setResource(entry)
          .setFullUrl(
              "https://gematik.de/fhir/" + entry.getResourceType().name() + "/" + entry.getId());
    }

    return bundle;
  }
}
