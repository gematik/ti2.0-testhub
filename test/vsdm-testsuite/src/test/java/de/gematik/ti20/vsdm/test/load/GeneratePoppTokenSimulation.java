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
package de.gematik.ti20.vsdm.test.load;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.internal.HttpCheckBuilders.status;

import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
public class GeneratePoppTokenSimulation extends BaseSimulation {

  private static final HttpProtocolBuilder httpProtocol = http.acceptHeader("application/json");

  private static final Path TOKEN_FILE =
      Paths.get("src", "test", "resources", "feeder", "popp_tokens.csv");

  private static final Object FILE_LOCK = new Object();

  private static final FeederBuilder.FileBased<String> IKNR_KVNR_FEEDER =
      csv("feeder/iknr_kvnr.csv").circular();

  static {
    try {
      Files.createDirectories(TOKEN_FILE.getParent());
      Files.deleteIfExists(TOKEN_FILE);
      Files.write(
          TOKEN_FILE,
          Collections.singletonList("popp_token"), // CSV-Header
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("Konnte Token-Datei nicht initialisieren: " + TOKEN_FILE, e);
    }
  }

  private static final ScenarioBuilder generatePoppTokenScenario =
      scenario("Generate and Export PoPP Token")
          .feed(IKNR_KVNR_FEEDER)
          .exec(
              http("GeneratePoppToken")
                  .post(POPP_SERVER_URL + "/popp/test/api/v1/token-generator")
                  .header("Content-Type", "application/json")
                  .header("Accept", "application/json")
                  .body(
                      StringBody(
                          session ->
                              getPoppTokenJsonBody(session.get("iknr"), session.get("kvnr"))))
                  .asJson()
                  .check(jsonPath("$.tokenResults[0]").saveAs("poppToken"))
                  .check(status().is(200)))
          .exec(
              session -> {
                String token = session.getString("poppToken");
                appendTokenCsvLine(token);
                return session;
              });

  private static void appendTokenCsvLine(String token) {
    String line = token + ",";
    synchronized (FILE_LOCK) {
      try {
        Files.write(
            TOKEN_FILE,
            Collections.singleton(line),
            StandardCharsets.UTF_8,
            StandardOpenOption.APPEND);
      } catch (IOException e) {
        throw new RuntimeException("Fehler beim Schreiben des Tokens in CSV-Datei", e);
      }
    }
  }

  {
    // Ein User generiert 1000 Tokens seriell
    setUp(generatePoppTokenScenario.injectOpen(atOnceUsers(1000))).protocols(httpProtocol);
  }
}
