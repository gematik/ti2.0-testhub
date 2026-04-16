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

import static de.gematik.test.tiger.lib.TigerHttpClient.executeCommandWithContingentWait;
import static de.gematik.test.tiger.lib.TigerHttpClient.givenDefaultSpec;
import static de.gematik.ti20.popp.data.TestConstants.FOLDER_FOR_SIGNED_HASHDB_PAYLOADS;
import static de.gematik.ti20.popp.data.TestConstants.HASH_DB_SUCCESS_RESULT_RESPONSE_FILE;
import static de.gematik.ti20.popp.data.TestConstants.JOBID_FOR_FINISHED_SUCCESSFULL_IMPORT;
import static de.gematik.ti20.popp.data.TestConstants.URL_HASH_DB_IMPORT_RU;
import static de.gematik.ti20.popp.data.TestConstants.VALID_HASH_DB_IMPORT_RESPONSE_FILE;
import static de.gematik.ti20.popp.data.TestConstants.VALID_HASH_DB_JOB_STATUS_RESPONSE_FILE;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.glue.TigerProxyGlue;
import de.gematik.test.tiger.lib.rbel.ModeType;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.test.tiger.lib.rbel.RequestParameter;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.restassured.http.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import lombok.extern.slf4j.Slf4j;
import org.htmlunit.http.HttpStatus;

@Slf4j
public class StepsHashdb {

  private static final String ALIAS_FOR_TRUSTSTORE = "alias";
  static KeyStore tspEntrySigner;
  TigerProxyGlue tigerProxyGlue = new TigerProxyGlue();
  private final RbelMessageRetriever rbelMessageRetriever;
  private final RbelValidator rbelValidator;

  public StepsHashdb(final RbelMessageRetriever rbelMessageRetriever) {
    this.rbelMessageRetriever = rbelMessageRetriever;
    this.rbelValidator = new RbelValidator();
  }

  public StepsHashdb() {
    this(RbelMessageRetriever.getInstance());
  }

  @Angenommen("der TSP sendet den signierten eContent {string} an den PoPP Service")
  public void sendSignedEContent(final String econtentFileName) {
    try {
      final byte[] eContentPayload =
          Files.readAllBytes(Path.of(FOLDER_FOR_SIGNED_HASHDB_PAYLOADS + econtentFileName));
      executeCommandWithContingentWait(
          () ->
              givenDefaultSpec().body(eContentPayload).request(Method.POST, URL_HASH_DB_IMPORT_RU));
    } catch (final IOException e) {
      throw new RuntimeException(
          "Error loading signed eContetn at "
              + FOLDER_FOR_SIGNED_HASHDB_PAYLOADS
              + econtentFileName,
          e);
    }
  }

  @Wenn("der TSP fragt den Status seines Imports ab")
  public void sendStatusRequest() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/api/v1/hash-db/import").build().resolvePlaceholders());
    final String jobId =
        this.rbelMessageRetriever
            .findElementInCurrentResponse("$.body.jobId")
            .getRawStringContent();

    executeCommandWithContingentWait(
        () ->
            givenDefaultSpec()
                .request(Method.GET, URL_HASH_DB_IMPORT_RU + "/" + jobId + "/status"));
  }

  @Wenn("der TSP fragt das Ergebnis seines erfolgreichen Imports ab")
  public void sendResultRequest() {
    executeCommandWithContingentWait(
        () ->
            givenDefaultSpec()
                .request(
                    Method.GET,
                    URL_HASH_DB_IMPORT_RU
                        + "/"
                        + JOBID_FOR_FINISHED_SUCCESSFULL_IMPORT
                        + "/result"));
  }

  @Und("der TSP verwendet die Client Identität {string} für die mTLS-Verbindung zum PoPP-Service")
  public void configureTlsClientIdentity(final String identityFileName) {
    final String prefixWithFileLocation = "../../no-publish/test-data/p12/popp-testsuite/";
    final String suffixWithPassword = ".p12;00";
    tigerProxyGlue.setLocalTigerProxyForwardMutualTlsIdentity(
        prefixWithFileLocation + identityFileName + suffixWithPassword);
  }

  @Dann("wird die Verbindung vom PoPP-Service abgelehnt")
  public void wirdDieVerbindungVomPoPPServiceAbgelehnt() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/api/v1/hash-db/import").build().resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.responseCode",
        String.valueOf(HttpStatus.UNAUTHORIZED_401),
        true,
        this.rbelMessageRetriever);
  }

  @Dann("der TSP erhält eine positive Rückmeldung mit einer jobID")
  public void checkLastResponseForSuccess() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(".*/api/v1/hash-db/import").build().resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.responseCode", String.valueOf(HttpStatus.CREATED_201), true, this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        "$.body",
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + VALID_HASH_DB_IMPORT_RESPONSE_FILE + "')}"),
        "",
        this.rbelMessageRetriever);
  }

  @Dann("der TSP erhält Informationen über den Status seines Imports")
  public void checkLastResponseForStatus() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder()
            .path(".*/api/v1/hash-db/import/.*/status")
            .build()
            .resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.responseCode", String.valueOf(HttpStatus.OK_200), true, this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        "$.body",
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + VALID_HASH_DB_JOB_STATUS_RESPONSE_FILE + "')}"),
        "",
        this.rbelMessageRetriever);
  }

  @Dann("der TSP erhält Informationen über das Ergebnis seines Imports")
  public void checkLastResponseForResult() {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder()
            .path(".*/api/v1/hash-db/import/.*/result")
            .build()
            .resolvePlaceholders());
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.responseCode", String.valueOf(HttpStatus.OK_200), true, this.rbelMessageRetriever);
    this.rbelValidator.assertAttributeOfCurrentResponseMatches(
        "$.body",
        TigerGlobalConfiguration.resolvePlaceholders(
            "!{file('" + HASH_DB_SUCCESS_RESULT_RESPONSE_FILE + "')}"),
        true,
        this.rbelMessageRetriever);
  }
}
