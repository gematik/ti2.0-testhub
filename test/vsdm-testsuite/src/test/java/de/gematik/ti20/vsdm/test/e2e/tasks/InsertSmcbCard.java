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
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallCardClient;
import de.gematik.ti20.vsdm.test.e2e.models.CardSlot;
import de.gematik.ti20.vsdm.test.e2e.models.SmcbCardInfo;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.File;
import lombok.SneakyThrows;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Task;

public class InsertSmcbCard implements Task {

  private final String cardImage;
  private final int slot;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public InsertSmcbCard(String cardImage, int slot) {
    this.cardImage = cardImage;
    this.slot = slot;
  }

  public static InsertSmcbCard fromFileInSlot(String cardImage, int slot) {
    return instrumented(InsertSmcbCard.class, cardImage, slot);
  }

  @Override
  @SneakyThrows
  public <T extends Actor> void performAs(T actor) {
    var api = CallCardClient.as(actor);

    Response removeResponse = api.request().delete("/slots/" + slot);
    removeResponse.then().statusCode(anyOf(is(204), is(404)));

    Response insertResponse =
        api.request()
            .contentType(ContentType.XML)
            .body(new File("src/test/resources/data/cards/" + cardImage))
            .put("/slots/" + slot);
    insertResponse.then().statusCode(201);

    CardSlot slotInfo = MAPPER.readValue(insertResponse.getBody().asString(), CardSlot.class);

    actor.remember("smcbSlot", slot);
    actor.remember("smcbId", slotInfo.getCardId());

    Response infoResponse = api.request().get("/cards/" + slotInfo.getCardId() + "/smc-b-info");
    infoResponse.then().statusCode(200);

    SmcbCardInfo smcbCardInfo =
        MAPPER.readValue(infoResponse.getBody().asString(), SmcbCardInfo.class);
    actor.remember("smcbCardInfo", smcbCardInfo);
  }
}
