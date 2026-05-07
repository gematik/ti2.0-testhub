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

import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.internal.HttpCheckBuilders.status;

import io.gatling.javaapi.core.ChainBuilder;

final class VsdmClientTokenModule extends BaseSimulation {

  private VsdmClientTokenModule() {}

  static ChainBuilder getTokenChain() {
    return exec(http("GET Access Token")
            .get(URL_CLIENT_VSDM + "/client/test/accessToken")
            .check(status().is(200))
            .check(bodyString().saveAs("access_token")))
        .exec(
            http("GET DPoP Token")
                .get(URL_CLIENT_VSDM + "/client/test/dpopToken")
                .queryParam("htm", "GET")
                .queryParam("htu", "https://vsdm-zeta-ingress/vsdservice/v1/vsdmbundle")
                .check(status().is(200))
                .check(bodyString().saveAs("dpop_token")));
  }
}
