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

import de.gematik.ti20.vsdm.test.e2e.abilities.CallVsdmClient;
import io.restassured.response.Response;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

public class RequestVsdFromServer implements Task {

  private final String etag;
  private final String poppToken;

  public RequestVsdFromServer(String etag, String poppToken) {
    this.etag = etag;
    this.poppToken = poppToken;
  }

  public static RequestVsdFromServer withEtagAndPoppToken(String etag, String poppToken) {
    return instrumented(RequestVsdFromServer.class, etag, poppToken);
  }

  // PoPP-Token generation will be controlled by VSDM client itself.
  public static RequestVsdFromServer withEtagAndNoPoppToken(String etag) {
    return withEtagAndPoppToken(etag, null);
  }

  // Sending request w/o Etag will force an error situation on VSDM server.
  public static RequestVsdFromServer withNoEtagAndPoppToken(String poppToken) {
    return withEtagAndPoppToken(null, poppToken);
  }

  @Override
  public <T extends Actor> void performAs(T actor) {

    var api = CallVsdmClient.as(actor);

    Integer smcbSlot = actor.recall("smcbSlot");
    Integer egkSlot = actor.recall("egkSlot");

    var request =
        api.request()
            .queryParam("forceUpdate", false)
            .queryParam("terminalId", "0")
            .queryParam("isFhirXml", false)
            .queryParam("smcBSlotId", smcbSlot)
            .queryParam("egkSlotId", egkSlot);

    if (etag != null) {
      request.header("If-None-Match", etag);
    }
    if (poppToken != null) {
      request.header("poppToken", poppToken);
    }

    Response response = request.get("/client/vsdm/vsd");
    actor.remember("lastResponse", response);
  }
}
