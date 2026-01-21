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

import static de.gematik.ti20.vsdm.test.e2e.helper.DateNormalizer.normalizeToLocalDate;
import static net.serenitybdd.screenplay.GivenWhenThen.*;
import static org.hamcrest.Matchers.*;

import de.gematik.test.tiger.lib.TigerDirector;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallCardClient;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallPoppService;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallVsdmClient;
import de.gematik.ti20.vsdm.test.e2e.enums.Error;
import de.gematik.ti20.vsdm.test.e2e.models.EgkCardInfo;
import de.gematik.ti20.vsdm.test.e2e.questions.*;
import de.gematik.ti20.vsdm.test.e2e.tasks.*;
import io.cucumber.java.Before;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;
import net.serenitybdd.screenplay.ensure.Ensure;
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
        .can(CallCardClient.at("http://127.0.0.1:8000"))
        .can(CallVsdmClient.at("http://127.0.0.1:8220"))
        .can(CallPoppService.at("http://127.0.0.1:9210"));
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
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndPoppToken("0", null));
    hccs().remember("etag", LastEtag.value().answeredBy(hccs()));
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
    String etag = Optional.ofNullable(hccs().recall("etag")).orElse("0").toString();
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndPoppToken(etag, null));
  }

  @Und("der VSDM Ressource Server beim E-Tag-Vergleich einen Unterschied feststellt")
  public void andRessourceServerIsFindingDifferentEtag() {
    String previousEtag = hccs().recall("etag");
    hccs().should(seeThat(LastEtag.value(), not(equalTo(previousEtag))));
  }

  @Und("der VSDM Ressource Server beim E-Tag-Vergleich keinen Unterschied feststellt")
  public void andRessourceServerIsFindingEqualEtag() {
    String previousEtag = hccs().recall("etag");
    hccs().should(seeThat(LastEtag.value(), equalTo(previousEtag)));
  }

  @Dann(
      "sendet der VSDM Ressource Server die aktualisierten VSD mit dem Statuscode {int} zum Primärsystem")
  public void thenVsdmRessourceServerIsSendingStatusCodeOkayWithVsd(int httpCode) {
    hccs().should(seeThat(LastStatusCode.value(), is(httpCode)));
    hccs().should(seeThat(LastVsdmBundle.value(), is(notNullValue())));
    hccs().should(seeThat(LastPatient.value(), is(notNullValue())));
    hccs().should(seeThat(LastOrganization.value(), is(notNullValue())));
    hccs().should(seeThat(LastCoverage.value(), is(notNullValue())));
  }

  @Und("die aktualisierten VSD enthalten das VsdmBundle mit den korrekten Patientendaten")
  public void andRessourceServerIsSendingCorrectPatientData() {
    EgkCardInfo egkCardInfo = hccs().recall("egkCardInfo");
    Patient patient = hccs().recall("lastPatient");

    Assertions.assertEquals(egkCardInfo.getLastName(), patient.getName().getFirst().getFamily());
    Assertions.assertEquals(
        egkCardInfo.getFirstName(), patient.getName().getFirst().getGiven().getFirst().toString());
    Assertions.assertEquals(egkCardInfo.getKvnr(), patient.getIdentifier().getFirst().getValue());

    var egkBirthDateNormalized = normalizeToLocalDate(egkCardInfo.getDateOfBirth());
    var patientBirthDateNormalized = normalizeToLocalDate(String.valueOf(patient.getBirthDate()));
    Assertions.assertEquals(egkBirthDateNormalized, patientBirthDateNormalized);
  }

  @Und("die aktualisierten VSD enthalten das VsdmBundle mit den korrekten Versicherungsdaten")
  public void andRessourceServerIsSendingCorrectCoverageData() {
    EgkCardInfo egkCardInfo = hccs().recall("egkCardInfo");

    Coverage coverage = hccs().recall("lastCoverage");
    Assertions.assertEquals(egkCardInfo.getKvnr(), coverage.getIdentifier().getFirst().getValue());

    Organization organization = hccs().recall("lastOrganization");
    Assertions.assertEquals(
        egkCardInfo.getIknr(), organization.getIdentifier().getFirst().getValue());
  }

  @Und(
      "die Antwortzeit des Fachdienstes VSDM 2.0 überschreitet nicht den Maximalwert von {long} ms")
  public void andVsdmRessourceServerIsAnsweringInTime(long maxAnswerTime) {
    long answerTime = LastResponseTime.value().answeredBy(hccs());
    TigerDirector.pauseExecution(
        String.format("Der VSDM 2.0 Fachdienst antwortete in %d Millisekunden.", answerTime));
    hccs().should(seeThat(LastResponseTime.value(), is(lessThan(maxAnswerTime))));

    Serenity.recordReportData()
        .withTitle("Gemessene Antwortzeit des Fachdienstes VSDM 2.0")
        .andContents(answerTime + " Millisekunden");
  }

  @Dann("sendet der VSDM Ressource Server den Statuscode {int} ohne VSD zum Primärsystem")
  public void thenVsdmRessourceServerIsSendingStatusCodeNotModifiedWithoutVsd(int httpCode) {
    hccs().should(seeThat(LastStatusCode.value(), is(httpCode)));
    hccs().should(seeThat(LastVsdmBundle.value(), is(nullValue())));
  }

  @Und("das Primärsystem speichert die aktualisierten VSD in seiner lokalen Datenbank")
  public void andClientSystemIsStoringCurrentVsdLocally() {
    hccs().attemptsTo(RequestVsdmDataFromCache.readCache());
    hccs().should(seeThat(CachedVsdmData.value(), not(emptyOrNullString())));
  }

  @Und("das Primärsystem speichert den PoPP-Token in seiner lokalen Datenbank")
  public void andClientSystemIsStoringPoppTokenLocally() {
    hccs().attemptsTo(RequestPoppToken.fromCache());
    hccs().should(seeThat(CachedPoppToken.value(), not(emptyOrNullString())));
  }

  @Und("das Primärsystem speichert die Prüfziffer in seiner lokalen Datenbank")
  public void andClientSystemIsStoringPruefzifferLocally() {
    hccs().attemptsTo(RequestVsdmDataFromCache.readCache());
    hccs().should(seeThat(CachedPruefziffer.value(), not(emptyOrNullString())));
  }

  @Und("das Primärsystem speichert das E-Tag in seiner lokalen Datenbank")
  public void andClientSystemIsStoringEtagLocally() {
    hccs().attemptsTo(RequestVsdmDataFromCache.readCache());
    hccs().should(seeThat(CachedEtag.value(), not(emptyOrNullString())));
  }

  @Wenn(
      "das Primärsystem die VSD direkt von einer gültigen eGK des Versicherten in der LEI abfragt")
  public void whenClientSystemRequestsVsdFromValidEgkCardDirectly() {
    hccs().attemptsTo(RequestVsdFromCard.readEgk());
  }

  @Dann("werden die VSD von der eGK gelesen und der Versicherte kann versorgt werden")
  public void thenClientSystemIsReceivingVsdFromEgkCardDirectly() {
    hccs().should(seeThat(LastVsdmBundle.value(), is(notNullValue())));
    hccs().should(seeThat(LastPatient.value(), is(notNullValue())));
    hccs().should(seeThat(LastOrganization.value(), is(nullValue())));
    hccs().should(seeThat(LastCoverage.value(), is(nullValue())));
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

  @Dann("überschreiten die Antworten des Fachdienstes VSDM 2.0 nicht den Maximalwert von {long} ms")
  public void thenVsdmRessourceServerIsAnsweringInTime(long maxTimeToAnswer) {
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

    Serenity.recordReportData()
        .withTitle("Anzahl Anfragen an den Fachdienstes VSDM 2.0")
        .andContents(String.valueOf(answerTimesSize));
    Serenity.recordReportData()
        .withTitle("Minimale Antwortzeit des Fachdienstes VSDM 2.0")
        .andContents(min.orElse(0L) + " Millisekunden");
    Serenity.recordReportData()
        .withTitle("Maximale Antwortzeit des Fachdienstes VSDM 2.0")
        .andContents(max.orElse(0L) + " Millisekunden");
    Serenity.recordReportData()
        .withTitle("Durchschnittliche Antwortzeit des Fachdienstes VSDM 2.0")
        .andContents(avg.orElse(0D) + " Millisekunden");

    for (Long answerTime : answerTimes) {
      Ensure.that(answerTime).isLessThan(maxTimeToAnswer);
    }
  }

  @Wenn("das Primärsystem die VSD mit einer falschen IK-Nummer vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithWrongIkNumber() {
    EgkCardInfo egk = hccs().recall("egkCardInfo");
    egk.setIknr("WRONG_IKNR");
    hccs().attemptsTo(GeneratePoppToken.now());
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndPoppToken("0", hccs().recall("poppToken")));
  }

  @Wenn("das Primärsystem die VSD mit einer falschen KVNR-Nummer vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithWrongKvnrNumber() {
    EgkCardInfo egk = hccs().recall("egkCardInfo");
    egk.setKvnr("WRONG_KVNR");
    hccs().attemptsTo(GeneratePoppToken.now());
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndPoppToken("0", hccs().recall("poppToken")));
  }

  @Wenn("das Primärsystem die VSD mit einem fehlenden E-Tag vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithWrongEtag() {
    hccs().attemptsTo(DeleteVsdmDataFromCache.deleteCache());
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndPoppToken(null, null));
  }

  @Wenn(
      "das Primärsystem die VSD mit einem ungültigen PoPP-Token vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithInvalidPoppToken() {
    hccs().attemptsTo(RequestVsdFromServer.withEtagAndPoppToken("0", "INVALID_POPP_TOKEN"));
  }

  @Dann("antwortet der VSDM Ressource Server mit dem Fehlercode {int} und dem Text {string}")
  public void thenVsdmAnswersWithCorrespondingBdeCodeAndText(Integer httpCode, String bdeText) {
    hccs().should(seeThat(LastStatusCode.value(), is(httpCode)));
    hccs().should(seeThat(LastOperationOutcome.value(), is(notNullValue())));
    hccs().should(seeThat(LastOperationOutcome.code(), is(bdeText)));
    hccs().should(seeThat(LastOperationOutcome.text(), is(Error.valueOf(bdeText).getValue())));
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
