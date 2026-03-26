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
import de.gematik.ti20.vsdm.test.e2e.models.SmcbCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.TokenResults;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;
import org.junit.jupiter.api.Assertions;

public class GeneratePoppTokenList implements Task {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final int nbrPoppTokens;

  public GeneratePoppTokenList(int nbrPoppTokens) {
    this.nbrPoppTokens = nbrPoppTokens;
  }

  public static GeneratePoppTokenList now(int nbrPoppTokens) {
    return instrumented(GeneratePoppTokenList.class, nbrPoppTokens);
  }

  private List<String> loadLines() {
    InputStream is =
        GeneratePoppTokenList.class.getClassLoader().getResourceAsStream("feeder/iknr_kvnr.csv");
    Objects.requireNonNull(is, "Ressource nicht gefunden im Classpath: " + "feeder/iknr_kvnr.csv");

    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      List<String> lines = reader.lines().toList();
      return lines.subList(1, nbrPoppTokens + 1);
    } catch (final IOException e) {
      throw new UncheckedIOException(
          "Fehler beim Lesen der Ressource: " + "feeder/iknr_kvnr.csv", e);
    }
  }

  private Map<String, String> makeTokenArgument(
      final String iknrKvnrRow, final long now, final SmcbCardInfo smcbCardInfo) {
    String[] row = iknrKvnrRow.split(",");
    String iknr = row[0];
    String kvnr = row[1];

    return Map.of(
        "proofMethod",
        ProofMethod.EHC_PRACTITIONER_TRUSTEDCHANNEL.getValue(),
        "patientProofTime",
        String.valueOf(now),
        "iat",
        String.valueOf(now),
        "exp",
        String.valueOf(now + 86_400),
        "patientId",
        kvnr,
        "insurerId",
        iknr,
        "actorId",
        smcbCardInfo.getTelematikId(),
        "actorProfessionOid",
        smcbCardInfo.getProfessionOid());
  }

  @Override
  @SneakyThrows
  public <T extends Actor> void performAs(T actor) {

    final SmcbCardInfo smcbCardInfo = actor.recall("smcbCardInfo");
    final long now = Instant.now().getEpochSecond();

    final List<String> iknrKvnrRows = loadLines();
    final Map<String, List<Map<String, String>>> tokenArgs =
        Map.of(
            "tokenParamsList",
            iknrKvnrRows.stream().map(row -> makeTokenArgument(row, now, smcbCardInfo)).toList());

    var api = CallPoppService.as(actor);

    Response response =
        api.request()
            .contentType(ContentType.JSON)
            .body(MAPPER.writeValueAsString(tokenArgs))
            .post("/popp/test/api/v1/token-generator");

    response.then().statusCode(200);

    final TokenResults tokenResults =
        MAPPER.readValue(response.getBody().asString(), TokenResults.class);

    final List<String> poppTokens = tokenResults.getTokenResults();
    Assertions.assertNotNull(poppTokens);

    actor.remember("poppTokens", poppTokens);
  }
}
