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
import static java.time.Instant.now;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.vsdm.test.e2e.enums.ProofMethod;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.Simulation;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Slf4j
public class BaseSimulation extends Simulation {

  protected static final SimulationConfigBean CFG = SimulationConfigProvider.getInstance();
  protected static final boolean RANDOM_READ_VSD = CFG.isRandomReadVsd();

  // Equal load: cardsPerSec * cardsDurationSecs should be 1000 to insert 1.000 cards
  protected static final int RAMP_USERS_STEADY_NUMBER =
      CFG.getRamp().getUsers().getSteady().getNumber();
  protected static final Duration RAMP_USERS_STEADY_DURATION =
      CFG.getRamp().getUsers().getSteady().getDuration();

  // Non-equal load: Controlling min and max of sawtooth curve in readVSD load scenario.
  protected static final int RAMP_USERS_RANDOM_MIN = CFG.getRamp().getUsers().getRandom().getMin();
  protected static final int RAMP_USERS_RANDOM_MAX = CFG.getRamp().getUsers().getRandom().getMax();

  // Non-equal load: Controlling duration time of sawtooth curves in readVSD load scenario.
  protected static final Duration RAMP_USERS_RANDOM_DURATION =
      CFG.getRamp().getUsers().getRandom().getDuration();
  protected static final int RAMP_USERS_RANDOM_CYCLES =
      CFG.getRamp().getUsers().getRandom().getCycles();

  protected static final String URL_CLIENT_CARD = CFG.getUrl().getClient().getCard();
  protected static final String URL_CLIENT_VSDM = CFG.getUrl().getClient().getVsdm();
  protected static final String URL_SERVER_POPP = CFG.getUrl().getServer().getPopp();
  protected static final String URL_SERVER_VSDM = CFG.getUrl().getServer().getVsdm();

  @NotNull
  protected static List<OpenInjectionStep> getRandomReadVsdSteps() {
    List<OpenInjectionStep> steps = new ArrayList<>();

    // Ramp up and ramp down cycles like a sawtooth curve.
    for (int i = 1; i <= RAMP_USERS_RANDOM_CYCLES; i++) {
      int readVsdPerSecMaxRandom =
          new Random().nextInt(RAMP_USERS_RANDOM_MIN, RAMP_USERS_RANDOM_MAX + 1);
      steps.add(
          rampUsersPerSec(RAMP_USERS_RANDOM_MIN)
              .to(readVsdPerSecMaxRandom)
              .during(RAMP_USERS_RANDOM_DURATION.getSeconds() / 2));
      steps.add(
          rampUsersPerSec(readVsdPerSecMaxRandom)
              .to(RAMP_USERS_RANDOM_MIN)
              .during(RAMP_USERS_RANDOM_DURATION.getSeconds() / 2));
    }
    return steps;
  }

  protected static String getPoppTokenJsonBody(String iknr, String kvnr) {
    Map<String, List<Map<String, String>>> tokenArgs =
        Map.of(
            "tokenParamsList",
            List.of(
                Map.of(
                    "proofMethod",
                    ProofMethod.EHC_PRACTITIONER_TRUSTEDCHANNEL.getValue(),
                    "patientProofTime",
                    String.valueOf(now().getEpochSecond()),
                    "iat",
                    String.valueOf(now().getEpochSecond() + 86_400),
                    "patientId",
                    kvnr,
                    "insurerId",
                    iknr,
                    "actorId",
                    "883110000168650",
                    "actorProfessionOid",
                    "1.2.276.0.76.4.32")));

    try {
      return new ObjectMapper().writeValueAsString(tokenArgs);
    } catch (JsonProcessingException e) {
      return null;
    }
  }
}
