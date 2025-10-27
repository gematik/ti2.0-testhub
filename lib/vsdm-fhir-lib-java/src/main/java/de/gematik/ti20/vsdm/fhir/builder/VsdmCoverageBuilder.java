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
import de.gematik.ti20.vsdm.fhir.def.VsdmCoverage;
import java.util.List;
import org.hl7.fhir.r4.model.*;

public class VsdmCoverageBuilder extends ResourceBuilder<VsdmCoverage, VsdmCoverageBuilder> {

  protected String status;
  protected String beneficiary;
  protected String payor;

  private VsdmCoverageBuilder() {}

  public static VsdmCoverageBuilder create() {
    return new VsdmCoverageBuilder();
  }

  public VsdmCoverageBuilder withStatus(String status) {
    this.status = status;
    return this;
  }

  public VsdmCoverageBuilder withPayor(String payor) {
    this.payor = payor;
    return this;
  }

  public VsdmCoverageBuilder withBeneficiary(String beneficiary) {
    this.beneficiary = beneficiary;
    return this;
  }

  @Override
  public VsdmCoverage build() {
    var type = new CanonicalType(VsdmCoverage.class.getAnnotation(ResourceDef.class).profile());
    var coverage = this.createResource(VsdmCoverage::new, type);

    coverage.setStatus(Coverage.CoverageStatus.fromCode(this.status));

    // meta
    Meta meta = new Meta();
    meta.setProfile(
        List.of(
            new CanonicalType(
                "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMCoverageGKV")));
    coverage.setMeta(meta);

    // identier
    Identifier identifier = new Identifier();
    identifier.setSystem("http://fhir.de/sid/gkv/kvid-10");
    identifier.setValue("A123454321");
    coverage.setIdentifier(List.of(identifier));

    // payor
    Reference payorReference = new Reference();
    payorReference.setDisplay(this.payor);

    Extension hauptkostentraeger = new Extension();
    hauptkostentraeger.setUrl(
        "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMKostentraegerRolle");
    Coding hauptkostentraegerCoding = new Coding();
    hauptkostentraegerCoding.setCode("H");
    hauptkostentraegerCoding.setSystem(
        "https://gematik.de/fhir/vsdm2/CodeSystem/VSDMKostentraegerRolleCS");
    hauptkostentraegerCoding.setDisplay("Haupt-Kostenträger");
    hauptkostentraeger.setValue(hauptkostentraegerCoding);

    payorReference.addExtension(hauptkostentraeger);
    payorReference.setReference(payor);
    coverage.setPayor(List.of(payorReference));

    // beneficiary
    Reference beneficiaryReference = new Reference();
    beneficiaryReference.setReference(this.beneficiary);
    coverage.setBeneficiary(beneficiaryReference);

    // type
    CodeableConcept cc = new CodeableConcept();
    Coding coding = new Coding();
    coding.setCode("GKV");
    coding.setSystem("http://fhir.de/CodeSystem/versicherungsart-de-basis");
    cc.addCoding(coding);
    coverage.setType(cc);

    // extensions
    Extension extensionWop = new Extension();
    extensionWop.setUrl("http://fhir.de/StructureDefinition/gkv/wop");
    Coding wop = new Coding();
    wop.setCode("98");
    wop.setSystem("https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ITA_WOP");
    wop.setDisplay("Sachsen");
    extensionWop.setValue(wop);
    coverage.addExtension(extensionWop);

    Extension extensionVersichertenart = new Extension();
    extensionVersichertenart.setUrl("http://fhir.de/StructureDefinition/gkv/versichertenart");
    Coding versichertenart = new Coding();
    versichertenart.setCode("1");
    versichertenart.setSystem("https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_VERSICHERTENSTATUS");
    versichertenart.setDisplay("Mitglieder");
    extensionVersichertenart.setValue(versichertenart);
    coverage.addExtension(extensionVersichertenart);

    return coverage;
  }
}
