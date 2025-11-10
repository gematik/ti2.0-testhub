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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
@Slf4j
public class BaseSimulation extends Simulation {

  protected static final boolean RANDOM_READ_VSD =
      Boolean.parseBoolean(System.getProperty("randomReadVsd", "true"));

  // Equal load: cardsPerSec * cardsDurationSecs should be 1000 to insert 1.000 cards
  protected static final int USERS_PER_SEC = Integer.getInteger("usersPerSec", 25);
  protected static final int USERS_DURATION_SECS = Integer.getInteger("usersDurationSecs", 40);

  // Non-equal load: Controlling min and max of sawtooth curve in readVSD load scenario.
  protected static final int READ_VSD_PER_SEC_MIN = Integer.getInteger("readVsdPerSecMin", 5);
  protected static final int READ_VSD_PER_SEC_MAX = Integer.getInteger("readVsdPerSecMax", 25);

  // Non-equal load: Controlling duration time of sawtooth curves in readVSD load scenario.
  protected static final int READ_VSD_DURATION_SECS = Integer.getInteger("readVsdDurationSecs", 10);
  protected static final int READ_VSD_NUMBER_CYCLES = Integer.getInteger("readVsdNumberCycles", 30);

  protected static final String CARD_CLIENT_URL = "http://localhost:8000";
  protected static final String VSDM_CLIENT_URL = "http://localhost:8220";
  protected static final String POPP_SERVER_URL = "http://localhost:9210";
  protected static final String VSDM_SERVER_URL = "http://localhost:9130";

  @NotNull
  protected static List<OpenInjectionStep> getRandomReadVsdSteps() {
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
