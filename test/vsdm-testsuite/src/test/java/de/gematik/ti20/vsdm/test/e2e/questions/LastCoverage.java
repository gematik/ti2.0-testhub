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
package de.gematik.ti20.vsdm.test.e2e.questions;

import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import io.restassured.response.Response;
import java.util.Objects;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;
import org.hl7.fhir.r4.model.Coverage;

public class LastCoverage implements Question<Coverage> {

  public static LastCoverage value() {
    return new LastCoverage();
  }

  @Override
  public Coverage answeredBy(Actor actor) {
    Response response = actor.recall("lastResponse");
    String body = Objects.requireNonNull(response.getBody().asString());

    try {
      FhirCodec codec = FhirCodec.forR4().andDummyValidator();
      VsdmBundle vsdmBundle = codec.decode(VsdmBundle.class, body);
      actor.remember("lastCoverage", vsdmBundle.getEntry().get(2).getResource());
      return (Coverage) vsdmBundle.getEntry().get(2).getResource();
    } catch (Exception e) {
      return null;
    }
  }
}
