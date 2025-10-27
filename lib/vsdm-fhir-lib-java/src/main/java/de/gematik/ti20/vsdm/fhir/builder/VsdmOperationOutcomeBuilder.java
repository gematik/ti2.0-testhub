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
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

public class VsdmOperationOutcomeBuilder
    extends ResourceBuilder<VsdmOperationOutcome, VsdmOperationOutcomeBuilder> {

  protected String code;
  protected String text;
  protected String reference;

  private VsdmOperationOutcomeBuilder() {}

  public static VsdmOperationOutcomeBuilder create() {
    return new VsdmOperationOutcomeBuilder();
  }

  public VsdmOperationOutcomeBuilder withCode(final String code) {
    this.code = code;
    return this;
  }

  public VsdmOperationOutcomeBuilder withText(final String text) {
    this.text = text;
    return this;
  }

  public VsdmOperationOutcomeBuilder withReference(final String reference) {
    this.reference = reference;
    return this;
  }

  @Override
  public VsdmOperationOutcome build() {
    var type =
        new CanonicalType(VsdmOperationOutcome.class.getAnnotation(ResourceDef.class).profile());
    var operationOutcome = this.createResource(VsdmOperationOutcome::new, type);

    var issue = operationOutcome.addIssue();
    issue.setSeverity(OperationOutcome.IssueSeverity.FATAL);
    issue.setCode(OperationOutcome.IssueType.INVALID);
    issue.setDiagnostics(text);
    var details = issue.getDetails();
    details.setText(text);
    var coding = details.addCoding();

    coding.setCode(String.valueOf(reference));
    coding.setDisplay(text);
    coding.setSystem("https://gematik.de/fhir/vsdm2/CodeSystem/VSDMErrorcodeCS");

    Narrative narrative = new Narrative();
    narrative.setStatus(Narrative.NarrativeStatus.GENERATED);
    XhtmlNode div = new XhtmlNode();
    div.setValue(code);
    narrative.setDiv(div);

    operationOutcome.setText(narrative);
    return operationOutcome;
  }
}
