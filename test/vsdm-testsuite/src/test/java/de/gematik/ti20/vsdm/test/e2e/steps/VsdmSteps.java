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

import static io.restassured.RestAssured.given;

import de.gematik.test.tiger.lib.TigerDirector;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.restassured.response.Response;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.serenitybdd.core.Serenity;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Assertions;

public class VsdmSteps extends BaseSteps {
  private static final List<Long> answerTimes = new ArrayList<>();
  private final LinkedList<Response> responses = new LinkedList<>();
  private VsdmBundle vsdmBundle;
  private String poppToken;
  private String etag = "0";
  private Integer smcbSlot;
  private Integer egkSlot;
  private String egkId;

  @Angenommen("das Primärsystem hat die VSD bereits einmal im Quartal abgefragt")
  public void givenVsdmClientHasAlreadyRequestVsdBefore() {
    smcbSlot = Serenity.sessionVariableCalled("smcbSlot");
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    responses.add(getVsd(etag, poppToken, smcbSlot, egkSlot));
    etag = responses.getLast().header("etag");
  }

  @Angenommen("der Fachdienst VSDM 2.0 befindet sich unter {int}% Maximallast")
  public void givenTheVsdmServiceIsUnderLoad(int load) {
    TigerDirector.pauseExecution(
        String.format(
            "Bitte legen Sie jetzt eine Hintergrundlast von %d%% der Maximallast an.", load));
  }

  @Wenn("das Primärsystem die VSD mittels PoPP- und Access-Token vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithAccessAndPoppToken() {
    smcbSlot = Serenity.sessionVariableCalled("smcbSlot");
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    responses.add(getVsd(etag, poppToken, smcbSlot, egkSlot));
  }

  @Und("der VSDM Ressource Server beim E-Tag-Vergleich einen Unterschied feststellt")
  public void andRessourceServerIsFindingDifferentEtag() {
    Assertions.assertNotEquals(etag, responses.getLast().header("etag"));
  }

  @Dann(
      "sendet der VSDM Ressource Server die aktualisierten VSD mit dem Statuscode {int} zum Primärsystem")
  public void thenVsdmRessourceServerIsSendingStatusCodeOkayWithVsd(int httpCode) {
    vsdmBundle = getVsdmBundle(responses.getLast());
    Assertions.assertNotNull(vsdmBundle);
    responses.getLast().then().assertThat().statusCode(httpCode);

    Patient patient = (Patient) vsdmBundle.getEntry().get(0).getResource();
    Assertions.assertNotNull(patient);

    Organization organization = (Organization) vsdmBundle.getEntry().get(1).getResource();
    Assertions.assertNotNull(organization);

    Coverage coverage = (Coverage) vsdmBundle.getEntry().get(2).getResource();
    Assertions.assertNotNull(coverage);
  }

  @Und("das Primärsystem speichert die aktualisierten VSD in seiner lokalen Datenbank")
  public void andClientSystemIsStoringCurrentVsdLocally() {
    getCachedVsd();
    responses.getLast().then().assertThat().statusCode(200);
    Assertions.assertNotNull(responses.getLast().jsonPath().get("vsdmData"));
  }

  @Und("das Primärsystem speichert den PoPP-Token in seiner lokalen Datenbank")
  public void andClientSystemIsStoringPoppTokenLocally() {
    getCachedPoppToken();
    responses.getLast().then().assertThat().statusCode(200);
    poppToken = responses.getLast().body().asString();
    Assertions.assertNotNull(poppToken);
  }

  @Und("das Primärsystem speichert das E-Tag in seiner lokalen Datenbank")
  public void andClientSystemIsStoringEtagLocally() {
    getCachedVsd();
    responses.getLast().then().assertThat().statusCode(200);
    etag = responses.getLast().jsonPath().get("etag");
    Assertions.assertNotNull(etag);
  }

  @Und("das Primärsystem speichert die Prüfziffer in seiner lokalen Datenbank")
  public void andClientSystemIsStoringProofNumberLocally() {
    getCachedVsd();
    responses.getLast().then().assertThat().statusCode(200);
    Assertions.assertNotNull(responses.getLast().jsonPath().get("pruefziffer"));
  }

  @Und("die Antwortzeit des Fachdienstes VSDM 2.0 überschreitet nicht den Maximalwert von {int} ms")
  public void andVsdmRessourceServerIsAnsweringInTime(int maxAnswerTime) {
    long answerTime = responses.getLast().time();
    TigerDirector.pauseExecution(
        String.format("Der VSDM 2.0 Fachdienst antwortete in %d Millisekunden.", answerTime));
    Assertions.assertTrue(answerTime < maxAnswerTime);
  }

  @Und("der VSDM Ressource Server beim E-Tag-Vergleich keinen Unterschied feststellt")
  public void andRessourceServerIsFindingEqualEtag() {
    Assertions.assertEquals(etag, responses.getLast().header("etag"));
  }

  @Dann("sendet der VSDM Ressource Server den Statuscode {int} ohne VSD zum Primärsystem")
  public void thenVsdmRessourceServerIsSendingStatusCodeNotModifiedWithoutVsd(int statusCode) {
    vsdmBundle = getVsdmBundle(responses.getLast());
    Assertions.assertNull(vsdmBundle);
    responses.getLast().then().assertThat().statusCode(statusCode);
  }

  @Wenn(
      "das Primärsystem die VSD direkt von einer gültigen eGK des Versicherten in der LEI abfragt")
  public void whenClientSystemRequestsVsdFromValidEgkCardDirectly() {
    getEgk();
  }

  @Dann("werden die VSD von der eGK gelesen und der Versicherte kann versorgt werden")
  public void thenClientSystemIsReceivingVsdFromEgkCardDirectly() {
    responses.getLast().then().assertThat().statusCode(200);
    vsdmBundle = getVsdmBundle(responses.getLast());
    Assertions.assertNotNull(vsdmBundle);

    Patient patient = (Patient) vsdmBundle.getEntry().get(0).getResource();
    Assertions.assertNotNull(patient);
    Assertions.assertEquals(1, vsdmBundle.getEntry().size()); // Reduced VSD w/o coverage.
  }

  @Wenn(
      "das Primärsystem die VSD direkt von einer ungültigen eGK des Versicherten in der LEI abfragt")
  public void whenClientSystemRequestsVsdFromInvalidEgkCardDirectly() {
    getEgk();
  }

  @Dann("werden die VSD nicht von der eGK gelesen und der Versicherte kann nicht versorgt werden")
  public void thenClientSystemIsNotReceivingVsdFromEgkCardDirectly() {
    responses.getLast().then().assertThat().statusCode(401);
    vsdmBundle = getVsdmBundle(responses.getLast());
    Assertions.assertNull(vsdmBundle);
  }

  @Wenn("das Primärsystem {int} Anfragen mit VSD Update an den Fachdienst VSDM 2.0 sendet")
  public void whenClientSystemIsRequestingVsdPeriodicallyWithUpdate(int nbrCalls)
      throws InterruptedException {
    sendReadVsd(nbrCalls, true);
  }

  @Wenn("das Primärsystem {int} Anfragen ohne VSD Update an den Fachdienst VSDM 2.0 sendet")
  public void whenClientSystemIsRequestingVsdPeriodicallyWithoutUpdate(int nbrCalls)
      throws InterruptedException {
    sendReadVsd(nbrCalls, false);
  }

  @Dann("überschreiten die Antworten des Fachdienstes VSDM 2.0 nicht den Maximalwert von {int} ms")
  public void thenVsdmRessourceServerIsAnsweringInTime(int maxTimeToAnswer) {
    OptionalLong min = answerTimes.stream().mapToLong(Long::longValue).min();
    OptionalLong max = answerTimes.stream().mapToLong(Long::longValue).max();
    OptionalDouble avg = answerTimes.stream().mapToLong(Long::longValue).average();
    int answerTimesSize = answerTimes.size();

    TigerDirector.pauseExecution(
        String.format(
            """
                Die folgenden Antwortzeiten des Fachdienstes VSDM 2.0 wurden ermittelt:
                Minimum: %d ms,
                Maximum: %d ms,
                Durchschnitt: %.2f ms
                (Anfragen: %d)
                """,
            min.orElse(0L), max.orElse(0L), avg.orElse(0D), answerTimesSize));

    for (Long answerTime : answerTimes) {
      Assertions.assertTrue(answerTime < maxTimeToAnswer);
    }
  }

  private void getEgk() {
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    egkId = Serenity.sessionVariableCalled("egkId");

    responses.add(
        given()
            .baseUri(BASE_URL_VSDM_CLIENT_SIM)
            .queryParam("terminalId", TERMINAL_ID)
            .queryParam("egkSlotId", egkSlot)
            .get("client/test/readEgk"));
  }

  private void getCachedPoppToken() {
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    egkId = Serenity.sessionVariableCalled("egkId");

    responses.add(
        given()
            .baseUri(BASE_URL_VSDM_CLIENT_SIM)
            .queryParam("terminalId", TERMINAL_ID)
            .queryParam("slotId", egkSlot)
            .queryParam("cardId", egkId)
            .get("client/test/poppToken"));
  }

  private void getCachedVsd() {
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    egkId = Serenity.sessionVariableCalled("egkId");

    responses.add(
        given()
            .baseUri(BASE_URL_VSDM_CLIENT_SIM)
            .queryParam("terminalId", TERMINAL_ID)
            .queryParam("slotId", egkSlot)
            .queryParam("cardId", egkId)
            .get("client/test/vsdmData"));
  }

  private void sendReadVsd(int nbrCalls, boolean withUpdateVsd) throws InterruptedException {
    for (int i = 0; i < nbrCalls; i++) {
      whenClientSystemIsRequestingVsdWithAccessAndPoppToken();
      if (withUpdateVsd) {
        andRessourceServerIsFindingDifferentEtag();
      } else {
        andRessourceServerIsFindingEqualEtag();
      }
      answerTimes.add(responses.getLast().time());
      Thread.sleep(ThreadLocalRandom.current().nextInt(10, 100));
    }
  }
}
