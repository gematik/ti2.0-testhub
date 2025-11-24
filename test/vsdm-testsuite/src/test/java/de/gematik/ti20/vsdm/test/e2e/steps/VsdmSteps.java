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

import static net.serenitybdd.screenplay.GivenWhenThen.*;
import static org.hamcrest.Matchers.*;

import de.gematik.test.tiger.lib.TigerDirector;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallCardClient;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallPoppService;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallVsdmClient;
import de.gematik.ti20.vsdm.test.e2e.questions.*;
import de.gematik.ti20.vsdm.test.e2e.tasks.*;
import io.cucumber.java.Before;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Assertions;

public class VsdmSteps {

  private static final List<Long> answerTimes = new ArrayList<>();

  private Actor hccs() {
    return OnStage.theActorInTheSpotlight();
  }

  @Before
  public void setTheStage() {
    OnStage.setTheStage(new OnlineCast());
    OnStage.theActorCalled("Primärsystem")
        .can(CallCardClient.at("http://localhost:8000"))
        .can(CallVsdmClient.at("http://localhost:8220"))
        .can(CallPoppService.at("http://localhost:9210"));
  }

  @Angenommen("das Primärsystem in der LEI verwendet ein korrekt konfiguriertes Terminal")
  public void givenHccsIsUsingTerminalCorrectly() {
    hccs().attemptsTo(ConfigureTerminal.withDefaultConfig());
  }

  @Angenommen("das Primärsystem in der LEI verwendet eine SMC-B {string} im Slot {int}")
  public void givenHccsIsUsingItsSmcb(String smcbCard, Integer slot) {
    hccs().attemptsTo(InsertSmcbCard.fromFileInSlot(smcbCard, slot));
  }

  @Angenommen("der Versicherte in der LEI verwendet eine eGK {string} im Slot {int}")
  public void givenPatientIsUsingItsEgk(String egkCard, Integer slot) {
    hccs().attemptsTo(InsertEgkCard.fromFileInSlot(egkCard, slot));
  }

  @Angenommen("das Primärsystem hat die VSD bereits einmal im Quartal abgefragt")
  public void givenVsdmClientHasAlreadyRequestVsdBefore() {
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndNoPoppToken("0"));
    String etag = LastEtag.value().answeredBy(hccs());
    hccs().remember("etag", etag);
  }

  @Angenommen("das Primärsystem hat den Versorgungskontext als PoPP-Token gespeichert")
  public void givenClientSystemHasStoredPoppToken() {
    hccs().attemptsTo(GeneratePoppToken.now());
  }

  @Angenommen("der Fachdienst VSDM 2.0 befindet sich unter {int}% Maximallast")
  public void givenTheVsdmServiceIsUnderLoad(int load) {
    TigerDirector.pauseExecution(
        String.format(
            "Bitte legen Sie jetzt eine Hintergrundlast von %d%% der Maximallast an.", load));
  }

  @Wenn("das Primärsystem die VSD mittels PoPP- und Access-Token vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithAccessAndPoppToken() {
    String etag = hccs().recall("etag");
    hccs()
        .attemptsTo(
            RequestVsdFromServer.withEtagAndNoPoppToken(Objects.requireNonNullElse(etag, "0")));
  }

  @Und("der VSDM Ressource Server beim E-Tag-Vergleich einen Unterschied feststellt")
  public void andRessourceServerIsFindingDifferentEtag() {
    String previousEtag = hccs().recall("etag");
    String lastEtag = LastEtag.value().answeredBy(hccs());
    Assertions.assertNotEquals(previousEtag, lastEtag);
  }

  @Und("der VSDM Ressource Server beim E-Tag-Vergleich keinen Unterschied feststellt")
  public void andRessourceServerIsFindingEqualEtag() {
    String previousEtag = hccs().recall("etag");
    String lastEtag = LastEtag.value().answeredBy(hccs());
    Assertions.assertEquals(previousEtag, lastEtag);
  }

  @Dann(
      "sendet der VSDM Ressource Server die aktualisierten VSD mit dem Statuscode {int} zum Primärsystem")
  public void thenVsdmRessourceServerIsSendingStatusCodeOkayWithVsd(int httpCode) {
    hccs().should(seeThat(LastStatusCode.value(), is(httpCode)));
    hccs().should(seeThat(LastVsdmBundle.value(), is(notNullValue())));

    VsdmBundle vsdmBundle = LastVsdmBundle.value().answeredBy(hccs());

    Patient patient = (Patient) vsdmBundle.getEntry().get(0).getResource();
    Assertions.assertNotNull(patient);

    Organization organization = (Organization) vsdmBundle.getEntry().get(1).getResource();
    Assertions.assertNotNull(organization);

    Coverage coverage = (Coverage) vsdmBundle.getEntry().get(2).getResource();
    Assertions.assertNotNull(coverage);
  }

  @Und("die Antwortzeit des Fachdienstes VSDM 2.0 überschreitet nicht den Maximalwert von {int} ms")
  public void andVsdmRessourceServerIsAnsweringInTime(int maxAnswerTime) {
    long answerTime = LastResponseTime.value().answeredBy(hccs());
    TigerDirector.pauseExecution(
        String.format("Der VSDM 2.0 Fachdienst antwortete in %d Millisekunden.", answerTime));
    Assertions.assertTrue(answerTime < maxAnswerTime);
  }

  @Dann("sendet der VSDM Ressource Server den Statuscode {int} ohne VSD zum Primärsystem")
  public void thenVsdmRessourceServerIsSendingStatusCodeNotModifiedWithoutVsd(int httpCode) {
    hccs().should(seeThat(LastStatusCode.value(), is(httpCode)));
    hccs().should(seeThat(LastVsdmBundle.value(), is(nullValue())));
  }

  @Und("das Primärsystem speichert die aktualisierten VSD in seiner lokalen Datenbank")
  public void andClientSystemIsStoringCurrentVsdLocally() {
    hccs().attemptsTo(RequestVsdmData.fromCache());
    String cachedVsd = CachedVsdmData.value().answeredBy(hccs());
    Assertions.assertFalse(cachedVsd.isEmpty());
  }

  @Und("das Primärsystem speichert den PoPP-Token in seiner lokalen Datenbank")
  public void andClientSystemIsStoringPoppTokenLocally() {
    hccs().attemptsTo(RequestPoppToken.fromCache());
    String cachedPoppToken = CachedPoppToken.value().answeredBy(hccs());
    Assertions.assertFalse(cachedPoppToken.isEmpty());
  }

  @Und("das Primärsystem speichert die Prüfziffer in seiner lokalen Datenbank")
  public void andClientSystemIsStoringPruefzifferLocally() {
    hccs().attemptsTo(RequestVsdmData.fromCache());
    String cachedCheckDigit = CachedPruefziffer.value().answeredBy(hccs());
    Assertions.assertFalse(cachedCheckDigit.isEmpty());
  }

  @Und("das Primärsystem speichert das E-Tag in seiner lokalen Datenbank")
  public void andClientSystemIsStoringEtagLocally() {
    hccs().attemptsTo(RequestVsdmData.fromCache());
    String cachedEtag = CachedEtag.value().answeredBy(hccs());
    Assertions.assertFalse(cachedEtag.isEmpty());
  }

  @Wenn(
      "das Primärsystem die VSD mit einem ungültigen PoPP-Token vom VSDM Ressource Server abfragt TODO")
  public void whenClientSystemIsRequestingVsdWithInvalidPoppToken() {
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndPoppToken("0", "INVALID_POPP_TOKEN"));
  }

  @Dann("antwortet der VSDM Ressource Server mit dem Fehlercode {int} und dem Text {string} TODO")
  public void thenVsdmAnswersWithCorrespondingBdeCodeAndText(Integer httpCode, String bdeText) {
    hccs().should(seeThat(LastStatusCode.value(), is(httpCode)));
    // TODO: Implement VsdmOutcome check as soon as ZETA MS2 is available.
  }

  @Wenn(
      "das Primärsystem die VSD direkt von einer gültigen eGK des Versicherten in der LEI abfragt")
  public void whenClientSystemRequestsVsdFromValidEgkCardDirectly() {
    hccs().attemptsTo(RequestVsdFromCard.readEgk());
  }

  @Dann("werden die VSD von der eGK gelesen und der Versicherte kann versorgt werden")
  public void thenClientSystemIsReceivingVsdFromEgkCardDirectly() {
    hccs().should(seeThat(LastVsdmBundle.value(), is(notNullValue())));

    VsdmBundle vsdmBundle = LastVsdmBundle.value().answeredBy(hccs());

    Patient patient = (Patient) vsdmBundle.getEntry().getFirst().getResource();
    Assertions.assertNotNull(patient);

    Assertions.assertEquals(1, vsdmBundle.getEntry().size()); // Reduced VSD w/o coverage.
  }

  @Wenn("das Primärsystem {int} Anfragen mit VSD Update an den Fachdienst VSDM 2.0 sendet")
  public void whenClientSystemIsRequestingVsdPeriodicallyWithUpdate(int nbrCalls)
      throws InterruptedException {
    answerTimes.clear();
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

  private void sendReadVsd(int nbrCalls, boolean withUpdateVsd) throws InterruptedException {
    for (int i = 0; i < nbrCalls; i++) {
      whenClientSystemIsRequestingVsdWithAccessAndPoppToken();
      if (withUpdateVsd) {
        andRessourceServerIsFindingDifferentEtag();
      } else {
        andRessourceServerIsFindingEqualEtag();
      }
      answerTimes.add(LastResponseTime.value().answeredBy(hccs()));
      Thread.sleep(ThreadLocalRandom.current().nextInt(10, 100));
    }
  }
}
