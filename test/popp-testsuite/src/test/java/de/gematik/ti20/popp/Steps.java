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

import static de.gematik.ti20.popp.data.TestConstants.POPP_ENTITY_STATEMENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.glue.HttpGlueCode;
import de.gematik.ti20.popp.validation.ApduValidator;
import de.gematik.ti20.popp.validation.EntityStatementValidator;
import de.gematik.ti20.popp.validation.JwksValidator;
import de.gematik.ti20.popp.validation.PoppTokenValidator;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.restassured.http.Method;
import java.net.URI;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Steps {

  HttpGlueCode httpGlueCode = new HttpGlueCode();
  private CommunicationType communicationType;

  public Steps() {}

  @Angenommen("das Primärsystem hat einen gültigen Access- und Refresh-Token vom ZETA Guard")
  public void primaersystem_hat_token() {
    // Access Token mit SMC-B
  }

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
    PoppTokenValidator.proofMethodInTokenMatchesCommTypeOrThrow(communicationType);
  }

  @Dann("Die Daten des Versicherten im PoPP-Token entsprechen denen auf der Karte {string}")
  public void patientDataInTokenMatchesEgkData(final String image) {
    final EgkType egkType = EgkType.valueOf(image);
    PoppTokenValidator.assertThatPatientDataInTokenMatchesDataOnEgk(egkType);
  }

  @Dann(
      "Die Daten der Leisungserbringerorganisation im PoPP-Token entsprichen denen auf der Karte {string}")
  public void practitionerDataInTokenMatchesSmcbData(final String smcb) {
    final SmcbType smcbType = SmcbType.valueOf(smcb);
    PoppTokenValidator.asserThatPractitionerDataInTokenMatchesDataOnSmcb(smcbType);
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

    httpGlueCode.sendRequestWithMultiLineBody(
        Method.POST,
        URI.create(TigerGlobalConfiguration.readString("tiger.popp.client.url")),
        "application/json",
        jsonBody);
  }

  @Und("die empfangenen APDUs sind korrekt {string}")
  public void dieEmpfangenenAPDUsSindKorrekt(final String readerType) {
    if (Objects.equals(readerType, "Standardleser") || Objects.equals(readerType, "virtuell")) {
      ApduValidator.validateApdusforStdKT();
    } else if (Objects.equals(readerType, "eH-KT")) {
      ApduValidator.validateApdusforEHealthKT();
    } else {
      throw new IllegalArgumentException(
          "Unsupported reader type: "
              + readerType
              + ". Expected 'Standardleser', 'virtuell', or 'eH-KT'.");
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

  @Dann("erhält das Primärsystem den Status ERROR vom PoPP-Service mit Message {string}")
  public void validatePoppError(final String errorMessage) {
    PoppTokenValidator.validatePoppTokenforBasicErrorResponse();
    PoppTokenValidator.validatePoppTokenforInvalidCaErrorResponse(errorMessage);
  }
}
