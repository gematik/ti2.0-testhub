/*-
 * #%L
 * VSDM 2.0 Testsuite
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
package de.gematik.ti20.vsdm.test.e2e.questions;

import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import io.restassured.response.Response;
import java.util.List;
import java.util.Objects;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public class LastPatient implements Question<Patient> {

  public static LastPatient value() {
    return new LastPatient();
  }

  @Override
  public Patient answeredBy(Actor actor) {
    Response response = actor.recall("lastResponse");
    String body = Objects.requireNonNull(response.getBody().asString());

    try {
      FhirCodec codec = FhirCodec.forR4().andDummyValidator();
      VsdmBundle vsdmBundle = codec.decode(VsdmBundle.class, body);
      List<Resource> resources =
          vsdmBundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();
      Patient patient =
          resources.stream()
              .filter(resource -> resource.getResourceType() == ResourceType.Patient)
              .map(Patient.class::cast)
              .findFirst()
              .orElse(null);

      actor.remember("lastPatient", patient);
      return patient;
    } catch (Exception e) {
      return null;
    }
  }
}
