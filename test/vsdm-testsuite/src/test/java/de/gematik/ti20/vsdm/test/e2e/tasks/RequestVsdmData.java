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

public class RequestVsdmData implements Task {

  public static RequestVsdmData fromCache() {
    return instrumented(RequestVsdmData.class);
  }

  @Override
  public <T extends Actor> void performAs(T actor) {

    var api = CallVsdmClient.as(actor);

    Integer egkSlot = actor.recall("egkSlot");
    String egkId = actor.recall("egkId");

    Response response =
        api.request()
            .queryParam("terminalId", "0")
            .queryParam("slotId", egkSlot)
            .queryParam("cardId", egkId)
            .get("/client/test/vsdmData");

    response.then().statusCode(200);
    actor.remember("lastResponse", response);
  }
}
