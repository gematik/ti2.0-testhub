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
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import io.restassured.response.Response;
import java.util.Objects;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

public class LastOperationOutcome implements Question<VsdmOperationOutcome> {

  public static LastOperationOutcome value() {
    return new LastOperationOutcome();
  }

  @Override
  public VsdmOperationOutcome answeredBy(Actor actor) {
    Response response = actor.recall("lastResponse");
    String body = Objects.requireNonNull(response.getBody().asString());

    try {
      FhirCodec codec = FhirCodec.forR4().andDummyValidator();
      return codec.decode(VsdmOperationOutcome.class, body);
    } catch (Exception e) {
      return null;
    }
  }

  public static Question<String> text() {
    return actor -> {
      VsdmOperationOutcome outcome = actor.asksFor(LastOperationOutcome.value());
      return outcome.getIssue().getFirst().getDetails().getText();
    };
  }

  public static Question<String> code() {
    return actor -> {
      VsdmOperationOutcome outcome = actor.asksFor(LastOperationOutcome.value());
      return outcome.getIssue().getFirst().getDetails().getCoding().getFirst().getCode();
    };
  }
}
