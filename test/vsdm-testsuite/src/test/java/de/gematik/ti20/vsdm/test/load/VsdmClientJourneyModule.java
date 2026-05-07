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

import static de.gematik.ti20.vsdm.test.util.ClasspathUtils.loadClasspathRessourceWithTigerResolving;
import static io.gatling.javaapi.core.CoreDsl.ElFileBody;
import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.http.internal.HttpCheckBuilders;

final class VsdmClientJourneyModule extends BaseSimulation {

  private VsdmClientJourneyModule() {}

  static ScenarioBuilder readVsdScenario() {
    return scenario("GET VSD via VSDM Client")
        .feed(POPP_TOKEN_FEEDER)
        .feed(SMCB_FEEDER)
        .feed(EGK_FEEDER)
        .exec(
            http("DELETE SMC-B card")
                .delete(URL_CLIENT_CARD + "/slots/#{smcb_slot}")
                .check(HttpCheckBuilders.status().in(200, 204, 404)))
        .exec(
            http("PUT SMC-B card")
                .put(URL_CLIENT_CARD + "/slots/#{smcb_slot}")
                .body(ElFileBody("data/cards/smcbCardImage.xml"))
                .asXml()
                .check(HttpCheckBuilders.status().is(201)))
        .exec(
            http("DELETE eGK card")
                .delete(URL_CLIENT_CARD + "/slots/#{egk_slot}")
                .check(HttpCheckBuilders.status().in(200, 204, 404)))
        .exec(
            http("PUT eGK card")
                .put(URL_CLIENT_CARD + "/slots/#{egk_slot}")
                .body(ElFileBody("data/cards/egkCardData.json"))
                .asXml()
                .check(HttpCheckBuilders.status().is(201)))
        .exec(
            http("PUT Terminal Config")
                .put(URL_CLIENT_VSDM + "/client/config/terminal")
                .body(
                    StringBody(
                        loadClasspathRessourceWithTigerResolving("data/cards/terminal.json")))
                .asJson()
                .check(HttpCheckBuilders.status().is(200)))
        .exec(
            http("GET VSD via VSDM Client")
                .get(URL_CLIENT_VSDM + "/client/vsdm/vsd")
                .header("poppToken", "#{popp_token}")
                .header("If-None-Match", "0")
                .queryParam("terminalId", "#{egk_slot}")
                .queryParam("egkSlotId", "#{egk_slot}")
                .queryParam("smcBSlotId", "#{smcb_slot}")
                .queryParam("isFhirXml", false)
                .check(HttpCheckBuilders.status().is(200)));
  }
}
