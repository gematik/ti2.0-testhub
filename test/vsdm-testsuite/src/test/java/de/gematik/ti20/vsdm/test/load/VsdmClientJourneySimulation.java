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
public class VsdmClientJourneySimulation extends BaseSimulation {

  private static final HttpProtocolBuilder httpProtocol = http.acceptHeader("application/json");

  private static final FeederBuilder.FileBased<String> SMCB_FEEDER =
      csv("feeder/smcb_slots.csv").circular();
  private static final FeederBuilder.FileBased<String> EGK_FEEDER =
      csv("feeder/egk_slots.csv").circular();

  private static final ScenarioBuilder readVsdScenario =
      scenario("Complete Read VSD Journey")
          .feed(SMCB_FEEDER)
          .feed(EGK_FEEDER)
          .exec(
              http("Removing SMC-B card")
                  .delete(CARD_CLIENT_URL + "/slots/#{smcb_slot}")
                  .check(status().in(200, 204, 404)))
          .exec(
              http("Inserting SMC-B card")
                  .put(CARD_CLIENT_URL + "/slots/#{smcb_slot}")
                  .body(ElFileBody("data/cards/smcbCardImage.xml"))
                  .asXml()
                  .check(status().is(201)))
          .exec(
              http("Removing eGK card")
                  .delete(CARD_CLIENT_URL + "/slots/#{egk_slot}")
                  .check(status().in(200, 204, 404)))
          .exec(
              http("Inserting eGK card")
                  .put(CARD_CLIENT_URL + "/slots/#{egk_slot}")
                  .body(ElFileBody("data/cards/egkCardImage.xml"))
                  .asXml()
                  .check(status().is(201)))
          .exec(
              http("Configuring Terminals")
                  .put(VSDM_CLIENT_URL + "/client/config/terminal")
                  .body(ElFileBody("data/cards/terminal.json"))
                  .asJson()
                  .check(status().is(200)))
          .exec(
              http("Reading VSD")
                  .get(VSDM_CLIENT_URL + "/client/vsdm/vsd")
                  .header("If-None-Match", "0")
                  .queryParam("terminalId", "#{egk_slot}")
                  .queryParam("egkSlotId", "#{egk_slot}")
                  .queryParam("smcBSlotId", "#{smcb_slot}")
                  .queryParam("isFhirXml", false)
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
