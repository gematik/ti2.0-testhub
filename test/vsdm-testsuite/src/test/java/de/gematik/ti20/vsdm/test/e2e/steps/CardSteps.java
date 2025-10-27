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
package de.gematik.ti20.vsdm.test.e2e.steps;

import static de.gematik.ti20.vsdm.test.e2e.steps.BaseSteps.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.vsdm.test.e2e.models.CardSlot;
import de.gematik.ti20.vsdm.test.e2e.models.EgkCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.SmcbCardInfo;
import io.cucumber.java.de.Angenommen;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.io.File;
import java.util.LinkedList;
import net.serenitybdd.core.Serenity;

public class CardSteps extends BaseSteps {
  private static final ObjectMapper mapper = new ObjectMapper();
  private final LinkedList<Response> responses = new LinkedList<>();

  @Angenommen("das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal")
  public void givenClientSystemIsUsingTerminalCorrectly() {
    responses.add(
        given()
            .baseUri(BASE_URL_VSDM_CLIENT_SIM)
            .contentType(ContentType.JSON)
            .body(new File("src/test/resources/data/cards/terminal.json"))
            .put("/client/config/terminal"));

    responses.getLast().then().assertThat().statusCode(200);
  }

  @Angenommen("das Primärsystem in der LEI verwendet eine SMC-B {string} im Slot {int}")
  public void givenHccsIsUsingItsSmcb(String smcbCardImage, Integer smcbSlot) throws Exception {
    removeCard(smcbSlot);
    Serenity.setSessionVariable("smcbSlot").to(smcbSlot);

    String smcbId = insertCard(smcbCardImage, smcbSlot).getCardId();
    Serenity.setSessionVariable("smcbId").to(smcbId);

    responses.add(
        given().baseUri(BASE_URL_CARD_CLIENT_SIM).get("/cards/" + smcbId + "/smc-b-info"));
    responses.getLast().then().assertThat().statusCode(200);

    SmcbCardInfo smcbCardInfo =
        mapper.readValue(responses.getLast().body().asString(), SmcbCardInfo.class);
    Serenity.setSessionVariable("smcbCardInfo").to(smcbCardInfo);
  }

  @Angenommen("der Versicherte in der LEI verwendet eine eGK {string} im Slot {int}")
  public void givenPatientIsUsingItsEgk(String egkCardImage, Integer egkSlot) throws Exception {
    removeCard(egkSlot);
    Serenity.setSessionVariable("egkSlot").to(egkSlot);

    String egkId = insertCard(egkCardImage, egkSlot).getCardId();
    Serenity.setSessionVariable("egkId").to(egkId);

    responses.add(given().baseUri(BASE_URL_CARD_CLIENT_SIM).get("/cards/" + egkId + "/egk-info"));
    responses.getLast().then().assertThat().statusCode(200);

    EgkCardInfo egkCardInfo =
        mapper.readValue(responses.getLast().body().asString(), EgkCardInfo.class);
    Serenity.setSessionVariable("egkCardInfo").to(egkCardInfo);
  }

  private CardSlot insertCard(String cardImage, Integer slot) throws JsonProcessingException {
    responses.add(
        given()
            .baseUri(BASE_URL_CARD_CLIENT_SIM)
            .contentType(ContentType.XML)
            .body(new File("src/test/resources/data/cards/" + cardImage))
            .put("/slots/" + slot));

    responses.getLast().then().assertThat().statusCode(201);
    return mapper.readValue(responses.getLast().body().asString(), CardSlot.class);
  }

  private void removeCard(Integer slot) {
    responses.add(given().baseUri(BASE_URL_CARD_CLIENT_SIM).delete("/slots/" + slot));
    responses.getLast().then().assertThat().statusCode(anyOf(is(204), is(404)));
  }
}
