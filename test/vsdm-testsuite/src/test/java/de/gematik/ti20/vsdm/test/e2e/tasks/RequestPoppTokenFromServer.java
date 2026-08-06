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
package de.gematik.ti20.vsdm.test.e2e.tasks;

import static net.serenitybdd.screenplay.Tasks.instrumented;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallPoppClient;
import de.gematik.ti20.vsdm.test.e2e.enums.CommType;
import de.gematik.ti20.vsdm.test.e2e.helper.TigerConfigBean;
import de.gematik.ti20.vsdm.test.e2e.helper.TigerConfigProvider;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

public class RequestPoppTokenFromServer implements Task {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TigerConfigBean CFG = TigerConfigProvider.getInstance();
  private static final String EGK_CARD_IMAGE_FILE = CFG.getTestData().getEgkCardImageFile();

  public static RequestPoppTokenFromServer now() {
    return instrumented(RequestPoppTokenFromServer.class);
  }

  @Override
  @SneakyThrows
  public <T extends Actor> void performAs(T actor) {

    CommType commType = actor.recall("commType");

    final ObjectNode json = MAPPER.createObjectNode();
    json.put("communicationType", commType.getValue());
    json.put("clientSessionId", "123456");
    json.put("virtualCard", EGK_CARD_IMAGE_FILE);

    var api = CallPoppClient.as(actor);

    Response response =
        api.request()
            .contentType(ContentType.JSON)
            .body(MAPPER.writeValueAsString(json))
            .post("/token");

    response.then().statusCode(200);
    actor.remember("lastResponse", response);
  }
}
