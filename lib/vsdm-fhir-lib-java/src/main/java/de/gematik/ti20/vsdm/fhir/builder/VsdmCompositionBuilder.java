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

import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.ti20.vsdm.fhir.def.StructureDefinitions;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;

public final class VsdmCompositionBuilder
    extends ResourceBuilder<VsdmBundle, VsdmCompositionBuilder> {

  List<Resource> entries = new ArrayList<>();

  private VsdmCompositionBuilder() {}

  public static VsdmCompositionBuilder create() {
    return new VsdmCompositionBuilder();
  }

  public VsdmCompositionBuilder addEntry(Resource entry) {
    this.entries.add(entry);
    return this;
  }

  public VsdmBundle build() {
    var bundle =
        this.createResource(VsdmBundle::new, StructureDefinitions.VSDM_BUNDLE.asCanonicalType());

    bundle.setType(BundleType.DOCUMENT);
    bundle.setIdentifier(
        new Identifier().setSystem("urn:ietf:rfc:3986").setValue("urn:uuid" + bundle.getId()));

    for (Resource entry : this.entries) {
      bundle.addEntry().setResource(entry);
    }

    return bundle;
  }
}
