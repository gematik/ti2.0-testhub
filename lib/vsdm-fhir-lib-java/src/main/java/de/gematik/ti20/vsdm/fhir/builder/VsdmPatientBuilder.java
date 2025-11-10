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
import de.gematik.ti20.vsdm.fhir.def.VsdmPatient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.*;

public final class VsdmPatientBuilder extends ResourceBuilder<VsdmPatient, VsdmPatientBuilder> {

  protected String nameFamily;
  protected List<String> namesGiven = new ArrayList<>();
  protected String kvnr;
  protected Date birthDate;
  protected List<Address> addresses = new ArrayList<>();

  private VsdmPatientBuilder() {}

  public static VsdmPatientBuilder create() {
    return new VsdmPatientBuilder();
  }

  public VsdmPatientBuilder withNames(final String family, final String given) {
    this.nameFamily = family;
    if (given != null) {
      namesGiven.addAll(Arrays.stream(given.split(" ")).toList());
    }
    return this;
  }

  public VsdmPatientBuilder withKvnr(final String kvnr) {
    this.kvnr = kvnr;
    return this;
  }

  public VsdmPatientBuilder withBirthDate(final Date birthDate) {
    this.birthDate = birthDate;
    return this;
  }

  public VsdmPatientBuilder addAddress(final Address address) {
    this.addresses.add(address);
    return this;
  }

  public VsdmPatient build() {
    var type = new CanonicalType(VsdmPatient.class.getAnnotation(ResourceDef.class).profile());
    var patient = this.createResource(VsdmPatient::new, type);

    var name = patient.addName();
    name.setFamily(nameFamily);
    namesGiven.forEach(name::addGiven);
    name.setUse(HumanName.NameUse.OFFICIAL);
    name.setText("text");

    var identifiers = new ArrayList<Identifier>();
    identifiers.add(new Identifier().setSystem("http://fhir.de/sid/gkv/kvid-10").setValue(kvnr));
    patient.setIdentifier(identifiers);

    patient.setBirthDate(birthDate == null ? new Date() : birthDate);
    patient.setGender(Enumerations.AdministrativeGender.MALE);
    addresses.forEach(patient::addAddress);

    return patient;
  }
}
