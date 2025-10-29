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
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Slf4j
public class VsdmLoadSimulation extends Simulation {

  private static final boolean RANDOM_READ_VSD =
      Boolean.parseBoolean(System.getProperty("randomReadVsd", "true"));

  // Equal load: cardsPerSec * cardsDurationSecs should be 1000 to insert 1.000 cards
  private static final int USERS_PER_SEC = Integer.getInteger("usersPerSec", 25);
  private static final int USERS_DURATION_SECS = Integer.getInteger("usersDurationSecs", 40);

  // Non-equal load: Controlling min and max of sawtooth curve in readVSD load scenario.
  private static final int READ_VSD_PER_SEC_MIN = Integer.getInteger("readVsdPerSecMin", 5);
  private static final int READ_VSD_PER_SEC_MAX = Integer.getInteger("readVsdPerSecMax", 25);

  // Non-equal load: Controlling duration time of sawtooth curves in readVSD load scenario.
  private static final int READ_VSD_DURATION_SECS = Integer.getInteger("readVsdDurationSecs", 10);
  private static final int READ_VSD_NUMBER_CYCLES = Integer.getInteger("readVsdNumberCycles", 30);

  private static final FeederBuilder.FileBased<String> SMCB_FEEDER =
      csv("feeder/smcb_slots.csv").circular();
  private static final FeederBuilder.FileBased<String> EGK_FEEDER =
      csv("feeder/egk_slots.csv").circular();

  private static final String CARD_CLIENT_URL = "http://localhost:8000";
  private static final String VSDM_CLIENT_URL = "http://localhost:8220";

  private static final HttpProtocolBuilder httpProtocol = http.acceptHeader("application/json");

  private static final ScenarioBuilder readVsdScenario =
      scenario("Reading VSD Journey")
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

  @NotNull
  private static List<OpenInjectionStep> getRandomReadVsdSteps() {
    List<OpenInjectionStep> steps = new ArrayList<>();

    // Ramp up and ramp down cycles like a sawtooth curve.
    for (int i = 1; i <= READ_VSD_NUMBER_CYCLES; i++) {
      int readVsdPerSecMaxRandom =
          new Random().nextInt(READ_VSD_PER_SEC_MIN, READ_VSD_PER_SEC_MAX + 1);
      steps.add(
          rampUsersPerSec(READ_VSD_PER_SEC_MIN)
              .to(readVsdPerSecMaxRandom)
              .during(READ_VSD_DURATION_SECS / 2));
      steps.add(
          rampUsersPerSec(readVsdPerSecMaxRandom)
              .to(READ_VSD_PER_SEC_MIN)
              .during(READ_VSD_DURATION_SECS / 2));
    }
    return steps;
  }
}
