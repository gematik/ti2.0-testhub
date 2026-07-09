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
import static de.gematik.ti20.popp.data.TestConstants.*;
import static de.gematik.ti20.rbel.fluent.RbelFluentApi.expectRequests;

import de.gematik.test.tiger.glue.TigerProxyGlue;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.cucumber.java.en.When;
import io.restassured.http.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;

@Slf4j
public class StepsHashdb {

  TigerProxyGlue tigerProxyGlue = new TigerProxyGlue();

  @Angenommen(
      "der TSP sendet den signierten eContent {tigerResolvedString} zum importieren an den PoPP Service")
  public void importDataToHashDb(final String econtentFileName) {
    sendSignedEContent(econtentFileName, true);
  }

  @Angenommen("der TSP sendet den signierten eContent {string} zum löschen an den PoPP Service")
  public void deleteDataFromHashDb(final String econtentFileName) {
    sendSignedEContent(econtentFileName, false);
  }

  @Wenn("der TSP fragt den Status seines Imports oder seiner Löschung ab")
  public void sendStatusRequest() {
    final String jobId = findeJobIdsInResponseOrEmpty().getFirst();
    executeCommandWithContingentWait(
        () ->
            givenDefaultSpec()
                .request(Method.GET, URL_HASH_DB_IMPORT_RU + "/" + jobId + "/status"));
  }

  @Wenn("der TSP fragt das Ergebnis des Jobs mit der jobID {tigerResolvedString} ab")
  public void sendStatusRequestForSuccessfulDelete(final String jobId) {
    sendResultRequest(jobId);
  }

  public void sendResultRequest(final String jobId) {
    executeCommandWithContingentWait(
        () ->
            givenDefaultSpec()
                .request(Method.GET, URL_HASH_DB_IMPORT_RU + "/" + jobId + "/result"));
  }

  @Wenn("der TSP erfragt seine jobID")
  public void sendGetJobIdRequest() {
    executeCommandWithContingentWait(
        () -> givenDefaultSpec().request(Method.GET, URL_HASH_DB_IMPORT_RU));
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
    expectRequests(".*/api/v1/hash-db/import")
        .nextResponse()
        .hasValueAtPathMatchingAnyOf("$.responseCode", "400", "401", "403");
  }

  @Dann("der TSP erhält eine positive Rückmeldung mit einer jobID")
  public void checkLastResponseForSuccess() {
    expectRequests(".*/api/v1/hash-db/import")
        .nextResponse()
        .hasValueAtPathEqualTo("$.responseCode", "201")
        .hasJsonAtPathEqualToFile("$.body", VALID_HASH_DB_IMPORT_RESPONSE_FILE);
  }

  @Dann(
      "der TSP erhält eine positive Rückmeldung mit einer jobID und diese entspricht {tigerResolvedString}")
  public void jobIdInLastResponseMatchesJobIdPreviouslyGiven(final String previousJobId) {
    expectRequests(".*/api/v1/hash-db/import")
        .nextResponse()
        .hasValueAtPathEqualTo("$.responseCode", "200")
        .hasValueAtPathEqualTo("$.body.jobIds.0", previousJobId);
  }

  @Dann("der TSP erhält eine positive Rückmeldung mit einer leeren jobID-Liste")
  public void lastResponseResponseContainsEmptyJobIdList() {
    expectRequests(".*/api/v1/hash-db/import")
        .nextResponse()
        .hasValueAtPathEqualTo("$.responseCode", "200")
        .hasValueAtPathEqualTo("$.body.jobIds", "[]");
  }

  @Dann("der TSP erhält Informationen über den Status seines Imports")
  public void checkLastResponseForStatus() {
    expectRequests(".*/api/v1/hash-db/import/.*/status")
        .nextResponse()
        .hasValueAtPathEqualTo("$.responseCode", "200")
        .hasJsonAtPathEqualToFile("$.body", VALID_HASH_DB_JOB_STATUS_RESPONSE_FILE);
  }

  @Dann("der TSP erhält Informationen über das Ergebnis seines Imports")
  public void checkLastResponseForResult() {
    expectRequests(".*/api/v1/hash-db/import/.*/result")
        .nextResponse()
        .hasValueAtPathEqualTo("$.responseCode", "200")
        .hasValueAtPathEqualToFile("$.body", HASH_DB_SUCCESSFUL_IMPORT_RESULT_RESPONSE_FILE);
  }

  @SneakyThrows
  @When("warte für {tigerResolvedString} Sekunden")
  public void waitForSeconds(final String seconds) {
    final int sec = Integer.parseInt(seconds);
    Awaitility.await()
        .atMost(sec + 1, TimeUnit.SECONDS)
        .pollDelay(sec, TimeUnit.SECONDS)
        .until(() -> true);
  }

  @Wenn("der TSP löscht den abgeschlossenen Auftrag mit der JobId {tigerResolvedString}")
  public void sendDeleteRequestWithJobId(final String jobId) {
    executeCommandWithContingentWait(
        () -> givenDefaultSpec().request(Method.DELETE, URL_HASH_DB_IMPORT_RU + "/" + jobId));
  }

  public void sendSignedEContent(final String econtentFileName, final boolean importData) {
    final String importOrDelete = importData ? "import/" : "delete/";
    try {
      final byte[] eContentPayload =
          Files.readAllBytes(
              Path.of(FOLDER_FOR_SIGNED_HASHDB_PAYLOADS + importOrDelete + econtentFileName));
      executeCommandWithContingentWait(
          () ->
              givenDefaultSpec().body(eContentPayload).request(Method.POST, URL_HASH_DB_IMPORT_RU));
      writeJobIdsToFile(findeJobIdsInResponseOrEmpty());

    } catch (final IOException e) {
      throw new RuntimeException(
          "Error loading signed eContent at "
              + FOLDER_FOR_SIGNED_HASHDB_PAYLOADS
              + econtentFileName,
          e);
    }
  }

  private List<String> findeJobIdsInResponseOrEmpty() {
    return expectRequests(".*/api/v1/hash-db/import")
        .nextResponse()
        .readValuesAtPath("$.body.jobId");
  }

  private void writeJobIdsToFile(final List<String> jobIds) {
    final Path path = Path.of("jobIds.txt");
    jobIds.forEach(
        jobId -> {
          try {
            Files.write(
                path,
                (jobId + System.lineSeparator()).getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
          } catch (final IOException e) {
            throw new RuntimeException(
                "Error while writing the following jobid to jobIds.txt: " + jobId, e);
          }
        });
  }
}
