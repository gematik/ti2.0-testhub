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
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.File;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

public class ConfigureTerminal implements Task {

  public static ConfigureTerminal withDefaultConfig() {
    return instrumented(ConfigureTerminal.class);
  }

  @Override
  public <T extends Actor> void performAs(T actor) {
    var api = CallVsdmClient.as(actor);

    Response response =
        api.request()
            .contentType(ContentType.JSON)
            .body(new File("src/test/resources/data/cards/terminal.json"))
            .put("/client/config/terminal");

    response.then().statusCode(200);

    actor.remember("terminalConfigured", true);
  }
}
