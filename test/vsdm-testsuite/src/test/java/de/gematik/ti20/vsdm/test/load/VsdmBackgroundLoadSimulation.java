/*-
 * #%L
 * VSDM 2.0 Testsuite
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.vsdm.test.load;

import static de.gematik.ti20.vsdm.test.load.ZetaDsl.ZetaRequestBuilder;
import static de.gematik.ti20.vsdm.test.load.ZetaDsl.bodyContains;
import static de.gematik.ti20.vsdm.test.load.ZetaDsl.bodyEmpty;
import static de.gematik.ti20.vsdm.test.load.ZetaDsl.status;
import static de.gematik.ti20.vsdm.test.load.ZetaDsl.zeta;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
public class VsdmBackgroundLoadSimulation extends BaseSimulation {

  private static final HttpProtocolBuilder httpProtocol =
      http.acceptHeader("application/fhir+json");

  private static final ChainBuilder readVsdWithCachedEtagRequest;
  private static final ChainBuilder readVsdWithZeroEtagRequest;
  private static final ScenarioBuilder readVsdScenario;

  static {
    log.debug("URL_SERVER_VSDM {}", URL_SERVER_VSDM);
    log.debug("ZETA_POOL_CAPACITY {}", ZETA_POOL_CAPACITY);
    log.debug("ZERO_ETAG_REQUEST_PROBABILITY_PERCENT {}", ZERO_ETAG_REQUEST_PROBABILITY_PERCENT);

    readVsdWithCachedEtagRequest = exec(readVsdRequest("ReadVSD w/o Update", "\"#{etag}\"", 304));

    readVsdWithZeroEtagRequest = exec(readVsdRequest("ReadVSD w/ Update", "\"0\"", 200));

    readVsdScenario =
        scenario("GET VSD from VSDM Server")
            .feed(POPP_TOKEN_ETAG_FEEDER)
            .doIfOrElse(
                session ->
                    ThreadLocalRandom.current().nextDouble(100.0)
                        < ZERO_ETAG_REQUEST_PROBABILITY_PERCENT)
            .then(readVsdWithZeroEtagRequest)
            .orElse(readVsdWithCachedEtagRequest);
  }

  private static ZetaRequestBuilder readVsdRequest(
      String requestName, String ifNoneMatchHeaderValue, int expectedStatus) {
    ZetaRequestBuilder request =
        zeta(requestName, ZETA_POOL_CAPACITY)
            .get(URL_SERVER_VSDM + "/vsdservice/v1/vsdmbundle")
            .queryParam("profileVersion", FHIR_PROFILE_VERSION)
            .header("PoPP", "#{popp_token}")
            .header("if-none-match", ifNoneMatchHeaderValue)
            .header("Accept", "application/fhir+json")
            .check(status().is(expectedStatus));

    if (expectedStatus == 200) {
      return request.check(bodyContains("Bundle"));
    }

    return request.check(bodyEmpty());
  }

  @Override
  public void after() {
    ZetaDsl.ZetaClientFactory.shutdown();
  }

  {
    if (RANDOM_READ_VSD) {
      List<OpenInjectionStep> randomReadVsdSteps = getRandomReadVsdSteps();
      setUp(readVsdScenario.injectOpen(randomReadVsdSteps).protocols(httpProtocol));
    } else {
      setUp(
          readVsdScenario
              .injectOpen(
                  rampUsersPerSec(RAMP_USERS_STEADY_NUMBER)
                      .to(RAMP_USERS_STEADY_NUMBER)
                      .during(RAMP_USERS_STEADY_DURATION))
              .protocols(httpProtocol));
    }
  }
}
