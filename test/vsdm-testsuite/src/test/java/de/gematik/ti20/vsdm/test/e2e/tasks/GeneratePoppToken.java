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
package de.gematik.ti20.vsdm.test.e2e.tasks;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallPoppService;
import de.gematik.ti20.vsdm.test.e2e.enums.ProofMethod;
import de.gematik.ti20.vsdm.test.e2e.models.EgkCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.SmcbCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.TokenResults;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import org.junit.jupiter.api.Assertions;

public class GeneratePoppToken implements Task {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static GeneratePoppToken now() {
    return instrumented(GeneratePoppToken.class);
  }

  @Override
  @SneakyThrows
  public <T extends Actor> void performAs(T actor) {

    SmcbCardInfo smcb = actor.recall("smcbCardInfo");
    EgkCardInfo egk = actor.recall("egkCardInfo");

    long now = Instant.now().getEpochSecond();

    Map<String, List<Map<String, String>>> tokenArgs =
        Map.of(
            "tokenParamsList",
            List.of(
                Map.of(
                    "proofMethod",
                    ProofMethod.EHC_PRACTITIONER_TRUSTEDCHANNEL.getValue(),
                    "patientProofTime",
                    String.valueOf(now),
                    "iat",
                    String.valueOf(now),
                    "exp",
                    String.valueOf(now + 86_400),
                    "patientId",
                    egk.getKvnr(),
                    "insurerId",
                    egk.getIknr(),
                    "actorId",
                    smcb.getTelematikId(),
                    "actorProfessionOid",
                    smcb.getProfessionOid())));

    var api = CallPoppService.as(actor);

    Response response =
        api.request()
            .contentType(ContentType.JSON)
            .body(MAPPER.writeValueAsString(tokenArgs))
            .post("/popp/test/api/v1/token-generator");

    response.then().statusCode(200);

    TokenResults tokenResults = MAPPER.readValue(response.getBody().asString(), TokenResults.class);

    String poppToken = tokenResults.getTokenResults().getFirst();
    Assertions.assertNotNull(poppToken);

    actor.remember("poppToken", poppToken);
  }
}
