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
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
public class VsdmBackgroundLoadSimulation extends BaseSimulation {

  private static final HttpProtocolBuilder httpProtocol =
      http.acceptHeader("application/fhir+json");

  private static final FeederBuilder.FileBased<String> POPP_TOKEN_FEEDER =
      csv("feeder/popp_tokens.csv").circular();

  private static final ScenarioBuilder readVsdScenario =
      scenario("ReadVSD using PoPP-Token-Feeder")
          .feed(POPP_TOKEN_FEEDER)
          .exec(
              http("ReadVSD")
                  .get(VSDM_SERVER_URL + "/vsdservice/v1/vsdmbundle")
                  .header("zeta-popp-token-content", "#{popp_token}")
                  .header("zeta-user-info", "MOCK_USER_INFO")
                  .header("if-none-match", "0")
                  .check(status().is(200)));

  {
    if (RANDOM_READ_VSD) {
      List<OpenInjectionStep> randomReadVsdSteps = getRandomReadVsdSteps();
      setUp(readVsdScenario.injectOpen(randomReadVsdSteps)).protocols(httpProtocol);
    } else {
      setUp(
              readVsdScenario.injectOpen(
                  rampUsersPerSec(USERS_PER_SEC).to(USERS_PER_SEC).during(USERS_DURATION_SECS)))
          .protocols(httpProtocol);
    }
  }
}
