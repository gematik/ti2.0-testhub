/*-
 * #%L
 * PoPP Testsuite
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
package de.gematik.ti20.popp;

import static de.gematik.ti20.popp.ERezeptService.POPP_TOKEN;
import static de.gematik.ti20.popp.data.TestConstants.POPP_ENTITY_STATEMENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.glue.HttpGlueCode;
import de.gematik.ti20.popp.testsupport.PoppTokenValidator;
import de.gematik.ti20.popp.validation.ApduValidator;
import de.gematik.ti20.popp.validation.EntityStatementValidator;
import de.gematik.ti20.popp.validation.JwksValidator;
import de.gematik.ti20.rbel.fluent.RbelFluentApi;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.restassured.http.Method;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Steps {

  HttpGlueCode httpGlueCode = new HttpGlueCode();
  private CommunicationType communicationType;
  private CompletableFuture<Void> pendingPoppTokenRequest;

  public Steps() {}

  @Wenn("das Primärsystem den PoPP-Token vom PoPP-Service abfragt")
  public void psRequestsPoppToken() {
    requestPoppTokenWithValidEgkImage();
  }

  @Wenn("Das Primärsystem den PoPP-Token mit Image {string} vom PoPP-Service abgefragt")
  public void psRequestsPoppTokenWithImage(final String image) {
    final EgkType egkType = EgkType.valueOf(image);
    requestsPoppTokenWithImage(egkType);
  }

  @Und("das PoPP-Token ist vollständig und spezifikationskonform")
  public void tokenIsCorrect() {
    PoppTokenValidator.validateClaimsInPoppToken();
    PoppTokenValidator.proofMethodInTokenMatchesCommTypeOrThrow(communicationType.getCommType());
  }

  @Dann("Die Daten des Versicherten im PoPP-Token entsprechen denen auf der Karte {string}")
  public void patientDataInTokenMatchesEgkData(final String image) {
    final EgkType egkType = EgkType.valueOf(image);
    PoppTokenValidator.assertPatientData(egkType.getKvnr(), egkType.getIkNumber());
  }

  @Dann(
      "Die Daten der Leisungserbringerorganisation im PoPP-Token entsprichen denen auf der Karte"
          + " {string}")
  public void practitionerDataInTokenMatchesSmcbData(final String smcb) {
    final SmcbType smcbType = SmcbType.valueOf(smcb);
    PoppTokenValidator.assertPractitionerData(
        smcbType.getTelematikId(), smcbType.getProfessionOid());
  }

  @Angenommen("der Versicherte in der LEI präsentiert seine eGK {string} am Lesegerät {string}")
  public void derVersichertePreasentiertEgk(final String readerType, final String commType) {
    communicationType = CommunicationType.from(readerType, commType);
    log.info("Nutze {} für die Kommunikation", communicationType.getValue());
  }

  private void requestPoppTokenWithValidEgkImage() {
    requestsPoppTokenWithImage(EgkType.EGK_80276883110000152715_VALID);
  }

  private void requestsPoppTokenWithImage(final EgkType image) {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode json = mapper.createObjectNode();
    json.put("communicationType", communicationType.getValue());
    json.put("clientSessionId", "123456");
    json.put("virtualCard", image.getFileName());

    final String jsonBody = json.toString();

    String url = TigerGlobalConfiguration.readString("popp.client.tokenUrl");
    httpGlueCode.sendRequestWithMultiLineBody(
        Method.POST, URI.create(url), "application/json", jsonBody);

    RbelFluentApi.expectRequests(".*/token")
        .nextResponse()
        .elementAtPath("$.body")
        .findElement("$.token")
        .map(RbelElement::getRawStringContent)
        .ifPresentOrElse(POPP_TOKEN::putValue, POPP_TOKEN::clearValue);
  }

  @Wenn("das Primärsystem die PoPP-Token-Abfrage asynchron mit contact-connector startet")
  public void startContactConnectorPoppTokenRequest() {
    if (pendingPoppTokenRequest != null && !pendingPoppTokenRequest.isDone()) {
      throw new IllegalStateException("Es läuft bereits eine PoPP-Token-Abfrage");
    }

    communicationType = CommunicationType.CONTACT_CONNECTOR;
    final String jsonBody = createContactConnectorRequestBody();

    final String url = TigerGlobalConfiguration.readString("popp.client.tokenUrl");

    log.info("Starte PoPP-Token-Abfrage mit communicationType contact-connector");

    pendingPoppTokenRequest =
        CompletableFuture.runAsync(
            () ->
                httpGlueCode.sendRequestWithMultiLineBody(
                    Method.POST, URI.create(url), "application/json", jsonBody));
  }

  private String createContactConnectorRequestBody() {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode json = mapper.createObjectNode();

    json.put("communicationType", communicationType.getValue());

    json.put("clientSessionId", "123456");

    return json.toString();
  }

  @Dann("wird die PoPP-Token-Abfrage abgeschlossen")
  public void finishContactConnectorPoppTokenRequest() {
    if (pendingPoppTokenRequest == null) {
      throw new IllegalStateException("Es wurde keine asynchrone PoPP-Token-Abfrage gestartet");
    }

    try {
      pendingPoppTokenRequest.get(60, TimeUnit.SECONDS);

      String token =
          RbelFluentApi.expectRequests(".*/token")
              .nextResponse()
              .elementAtPath("$.body.token")
              .getRawStringContent();

      if (token == null || token.isBlank()) {
        throw new IllegalStateException("Die Antwort des PoPP-Service enthält keinen Token");
      }

      POPP_TOKEN.putValue(token);

      log.info("PoPP-Token-Abfrage mit contact-connector " + "wurde erfolgreich abgeschlossen");

    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();

      throw new IllegalStateException(
          "Das Warten auf die PoPP-Token-Abfrage " + "wurde unterbrochen", exception);

    } catch (TimeoutException exception) {
      pendingPoppTokenRequest.cancel(true);

      throw new IllegalStateException(
          "Die PoPP-Token-Abfrage wurde nicht innerhalb " + "von 60 Sekunden abgeschlossen",
          exception);

    } catch (ExecutionException exception) {
      Throwable cause = exception.getCause() != null ? exception.getCause() : exception;

      throw new IllegalStateException(
          "Die PoPP-Token-Abfrage mit contact-connector " + "ist fehlgeschlagen", cause);

    } finally {
      pendingPoppTokenRequest = null;
    }
  }

  @Und("die empfangenen APDUs sind korrekt")
  public void dieEmpfangenenAPDUsSindKorrekt() {

    switch (communicationType) {
      case CONTACT_STANDARD, CONTACT_VIRTUAL:
        ApduValidator.validateApdusforStdKT();
        break;

      case CONTACTLESS_STANDARD, CONTACTLESS_VIRTUAL:
        ApduValidator.validateApdusforStdKTContactless();
        break;

      case CONTACT_CONNECTOR:
        ApduValidator.validateApdusforEHealthKT();
        break;

      case CONTACTLESS_CONNECTOR:
        throw new IllegalArgumentException(
            "CommunicationType 'contactless-connector' wird nicht unterstützt.");
    }
  }

  @Angenommen("hole das EntityStatement vom Endpunkt")
  public void getEntityStatement() {
    httpGlueCode.sendEmptyRequest(Method.GET, URI.create(POPP_ENTITY_STATEMENT));
  }

  @Und("die Anfrage liefert ein gültiges EntityStatement mit einem gültigen JWKS-Link")
  public void checkValidEntityStatement() {
    EntityStatementValidator.validateEntityStatement();
  }

  @Wenn("frage das JWKS über den JWKS Link {tigerResolvedUrl} aus dem EntityStatement ab")
  public void getJwks(final URI adress) {
    httpGlueCode.sendEmptyRequest(Method.GET, adress);
  }

  @Und("validiere das JWKS")
  public void validateJwks() {
    JwksValidator.validateSignedJwks();
  }

  @Dann(
      "erhält das Primärsystem den Status ERROR vom PoPP-Service mit Message {tigerResolvedString}")
  public void validatePoppError(final String errorMessage) {
    PoppTokenValidator.validatePoppTokenForBasicErrorResponse();
    PoppTokenValidator.validatePoppTokenForInvalidCaErrorResponse(errorMessage);
  }
}
