/*
 *
 * Copyright 2026 gematik GmbH
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

package de.gematik.ti20.popp;

import static de.gematik.ti20.popp.data.TestConstants.MAX_AGE_POPP_TOKEN_IN_SECONDS;
import static de.gematik.ti20.popp.data.TestConstants.VALID_POPP_TOKEN_BODY_CLAIMS;
import static de.gematik.ti20.popp.data.TestConstants.VALID_POPP_TOKEN_HEADER_CLAIMS;
import static de.gematik.ti20.popp.data.TestConstants.VALID_POPP_TOKEN_JSON_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.glue.HttpGlueCode;
import de.gematik.test.tiger.lib.rbel.ModeType;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.test.tiger.lib.rbel.RequestParameter;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.restassured.http.Method;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.InstanceOfAssertFactories;

@Slf4j
public class Steps {

  HttpGlueCode httpGlueCode = new HttpGlueCode();
  private String communicationType = "";
  private final RbelMessageRetriever rbelMessageRetriever;
  private final RbelValidator rbelValidator;

  public Steps(final RbelMessageRetriever rbelMessageRetriever) {
    this.rbelMessageRetriever = rbelMessageRetriever;
    this.rbelValidator = new RbelValidator();
  }

  public Steps() {
    this(RbelMessageRetriever.getInstance());
  }

  @Angenommen("das Primärsystem hat einen gültigen Access- und Refresh-Token vom ZETA Guard")
  public void primaersystem_hat_token() {
    // Access Token mit SMC-B
  }

  @Wenn("das Primärsystem den PoPP-Token vom PoPP-Service abfragt")
  public void psRequestsPoppToken() {
    requestPoppToken();
  }

  @Dann("erhält das Primärsystem APDU Szenarien für die Kommunikation mit der eGK")
  public void primaersystem_erhaelt_apdu() {
    // APDU Szenarien vom Tiger Proxy erhalten und auswerten
    // readerType == connector -> APDUs in in StandardScenarioMessage in ConnectorScenarioMessages
    // readerType == standard -> APDUs in StandardScenarioMessages
  }

  @Und("das PoPP-Token ist vollständig und spezifikationskonform")
  public void tokenIsCorrect() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/token").build().resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        "$.body",
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + VALID_POPP_TOKEN_JSON_RESPONSE + "')}"),
        "",
        this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        "$.body.token.content.body",
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + VALID_POPP_TOKEN_BODY_CLAIMS + "')}"),
        "",
        this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        "$.body.token.content.header",
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + VALID_POPP_TOKEN_HEADER_CLAIMS + "')}"),
        "",
        this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.body.token.content.signature.isValid", "true", true, this.rbelMessageRetriever);

    assertThat(
            this.rbelMessageRetriever.findElementInCurrentResponse("$.body.token.content.body.iat"))
        .as("iat element should exist")
        .isNotNull()
        .extracting(RbelElement::getRawStringContent)
        .as("raw iat should not be null")
        .isNotNull()
        .extracting(Long::parseLong)
        .extracting(
            epochSeconds ->
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC))
        .asInstanceOf(InstanceOfAssertFactories.ZONED_DATE_TIME)
        .as("iat must be recent enough")
        .isAfter(ZonedDateTime.now().minusSeconds(MAX_AGE_POPP_TOKEN_IN_SECONDS));

    assertThat(
            this.rbelMessageRetriever.findElementInCurrentResponse(
                "$.body.token.content.body.patientProofTime"))
        .as("patientProofTime element should exist")
        .isNotNull()
        .extracting(RbelElement::getRawStringContent)
        .as("raw patientProofTime should not be null")
        .isNotNull()
        .extracting(Long::parseLong)
        .extracting(
            epochSeconds ->
                ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC))
        .asInstanceOf(InstanceOfAssertFactories.ZONED_DATE_TIME)
        .as("patientProofTime must be recent enough")
        .isAfter(ZonedDateTime.now().minusSeconds(MAX_AGE_POPP_TOKEN_IN_SECONDS));
  }

  @Dann("erhält das Primärsystem den PoPP-Token vom PoPP-Service")
  public void erhaeltDasPrimaersystemDenPoPPTokenVomPoPPService() {
    // PoPP-Token empfangen
  }

  @Angenommen("der Versicherte in der LEI präsentiert seine eGK {string} am Lesegerät {string}")
  public void derVersichertePreasentiertEgk(final String readerType, final String commType) {
    communicationType = CommunicationType.from(readerType, commType).getValue();
    log.info("Nutze {} für die Kommunikation", communicationType);
  }

  @Dann("erhält das Primärsystem den Fehlercode {string} und den Text {string} vom PoPP-Service")
  public void validatePoppError(final String errorCode, final String errorMessage) {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/token").build().resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.body.status", errorMessage, true, this.rbelMessageRetriever);
  }

  private void requestPoppToken() {
    // StartMessage mit cardConnectionType commType-readerType
    // if (readerType == connector) => clientSessionId must be generated by connector (SOAP call
    // StartCardSession

    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode json = mapper.createObjectNode();
    json.put("communicationType", communicationType);
    json.put("clientSessionId", "123456");

    final String jsonBody = json.toString();

    httpGlueCode.sendRequestWithMultiLineBody(
        Method.POST, URI.create("http://localhost:8081/token"), "application/json", jsonBody);
  }
}
