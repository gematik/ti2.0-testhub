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

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import de.gematik.bbriccs.fhir.builder.ResourceBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmPayorOrganization;
import java.util.List;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Identifier;

public class VsdmPayorOrganizationBuilder
    extends ResourceBuilder<VsdmPayorOrganization, VsdmPayorOrganizationBuilder> {

  private String iknr;
  private String name;

  private VsdmPayorOrganizationBuilder() {}

  public static VsdmPayorOrganizationBuilder create() {
    return new VsdmPayorOrganizationBuilder();
  }

  public VsdmPayorOrganizationBuilder iknr(String iknr) {
    this.iknr = iknr;
    return this;
  }

  public VsdmPayorOrganizationBuilder name(String name) {
    this.name = name;
    return this;
  }

  @Override
  public VsdmPayorOrganization build() {
    var type =
        new CanonicalType(VsdmPayorOrganization.class.getAnnotation(ResourceDef.class).profile());
    var payorOrganization = this.createResource(VsdmPayorOrganization::new, type);

    // identifier
    Identifier identifier = new Identifier();
    identifier.setSystem("http://fhir.de/sid/arge-ik/iknr");
    identifier.setValue(iknr);
    payorOrganization.setIdentifier(List.of(identifier));

    payorOrganization.setName(name);

    return payorOrganization;
  }
}
