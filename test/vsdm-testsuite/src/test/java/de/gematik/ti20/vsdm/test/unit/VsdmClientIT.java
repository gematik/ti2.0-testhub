/*-
 * #%L
 * VSDM 2.0 Testsuite
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
package de.gematik.ti20.vsdm.test.unit;

import static de.gematik.test.tiger.common.config.TigerGlobalConfiguration.resolvePlaceholders;
import static de.gematik.ti20.vsdm.test.util.ClasspathUtils.loadClasspathRessourceWithTigerResolving;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.MediaType;
import okhttp3.Request;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.*;

@Slf4j
class VsdmClientIT {

  private static final String CARD_CLIENT_URL = "http://127.0.0.1:${ports.cardTerminalPort}";
  private static final String VSDM_CLIENT_URL = "http://127.0.0.1:${ports.vsdmClientPort}";

  private static final Integer TERMINAL_ID = 1;
  private static final Integer EGK_SLOT = 1;
  private static final Integer SMCB_SLOT = 2;

  private static final String VSDM_ENDPOINT =
      VSDM_CLIENT_URL
          + "/client/vsdm/vsd?terminalId="
          + TERMINAL_ID
          + "&egkSlotId="
          + EGK_SLOT
          + "&smcBSlotId="
          + SMCB_SLOT;

  private static OkHttpClient httpClient;
  private static FhirCodec fhirCodec;

  // Preconditions:
  // 1. All VSDM components are running via docker-compose.

  @BeforeAll
  static void setup() {
    httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    fhirCodec = FhirCodec.forR4().andDummyValidator();
  }

  @BeforeEach
  void beforeEach() throws Exception {
    removeCardFromSlot(EGK_SLOT);
    removeCardFromSlot(SMCB_SLOT);

    insertEgkCard();
    insertSmcbCard();

    configureTerminal();
  }

  @Test
  @Order(1)
  void testReadVsdReturnsExpectedDataJson() throws Exception {
    final Result result = readVsdOnce("0", false);
    assertEquals(200, result.response.code());
    assertNotNull(result.resource);

    assertTrue(result.responseBody.startsWith("{\"resourceType\":\"Bundle\","));

    final VsdmBundle vsdmBundle = (VsdmBundle) result.resource;
    assertNotNull(vsdmBundle);
    final List<Resource> resources =
        vsdmBundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    final Patient patient =
        resources.stream()
            .filter(r -> r.getResourceType() == ResourceType.Patient)
            .map(r -> (Patient) r)
            .findFirst()
            .orElse(null);
    assertNotNull(patient);
    final HumanName name = patient.getName().getFirst();
    assertEquals("Amelie Abigail H. Freifrau Bruser", name.getFamily());
    assertEquals("Kriemhild", name.getGiven().getFirst().getValue());

    assertEquals("X110639491", patient.getIdentifierFirstRep().getValue());

    final Organization payorOrganization =
        resources.stream()
            .filter(r -> r.getResourceType() == ResourceType.Organization)
            .map(r -> (Organization) r)
            .findFirst()
            .orElse(null);
    assertNotNull(payorOrganization);
    assertEquals("Test GKV Krankenkasse", ((Organization) payorOrganization).getName());

    final Coverage coverage =
        resources.stream()
            .filter(r -> r.getResourceType() == ResourceType.Coverage)
            .map(r -> (Coverage) r)
            .findFirst()
            .orElse(null);
    assertNotNull(coverage);
    assertTrue(
        coverage
            .getPayor()
            .getFirst()
            .getDisplay()
            .startsWith("https://gematik.de/fhir/Organization/"));
  }

  @Test
  @Order(1)
  void testReadVsdReturnsExpectedDataXML() throws Exception {
    final Result result = readVsdOnce("0", true);
    assertEquals(200, result.response.code());
    assertNotNull(result.resource);

    assertTrue(result.responseBody.startsWith("<Bundle xmlns=\"http://hl7.org/fhir\">"));

    final VsdmBundle vsdmBundle = (VsdmBundle) result.resource;
    assertNotNull(vsdmBundle);
    final List<Resource> resources =
        vsdmBundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource).toList();

    final Patient patient =
        resources.stream()
            .filter(r -> r.getResourceType() == ResourceType.Patient)
            .map(r -> (Patient) r)
            .findFirst()
            .orElse(null);
    assertNotNull(patient);
    final HumanName name = patient.getName().getFirst();
    assertEquals("Amelie Abigail H. Freifrau Bruser", name.getFamily());
    assertEquals("Kriemhild", name.getGiven().getFirst().getValue());

    assertEquals("X110639491", patient.getIdentifierFirstRep().getValue());

    final Organization payorOrganization =
        resources.stream()
            .filter(r -> r.getResourceType() == ResourceType.Organization)
            .map(r -> (Organization) r)
            .findFirst()
            .orElse(null);
    assertNotNull(payorOrganization);
    assertEquals("Test GKV Krankenkasse", ((Organization) payorOrganization).getName());

    final Coverage coverage =
        resources.stream()
            .filter(r -> r.getResourceType() == ResourceType.Coverage)
            .map(r -> (Coverage) r)
            .findFirst()
            .orElse(null);
    assertNotNull(coverage);
    assertTrue(
        coverage
            .getPayor()
            .getFirst()
            .getDisplay()
            .startsWith("https://gematik.de/fhir/Organization/"));
  }

  @Test
  @Order(2)
  // Caution! The test fails with Timeout during the first run. Further analysis is required.
  void testPruefzifferHasCorrectLength() throws Exception {
    final Result result = readVsdOnce("0", false);
    assertEquals(200, result.response.code());

    final String pruefzifferEncoded = result.response.header("VSDM-Pz");
    assertNotNull(pruefzifferEncoded);
    assertEquals(64, pruefzifferEncoded.length());

    log.info(" VSDM-Pz: " + pruefzifferEncoded);
    log.info(" VSDM-Pz Länge: " + pruefzifferEncoded.length());

    byte[] pruefziffer = Base64.getUrlDecoder().decode(pruefzifferEncoded);
    // ensure can be decoded
    assertNotNull(pruefziffer);
  }

  @Test
  @Order(3)
  void testEtagIsConsistent() throws Exception {
    final Result result1 = readVsdOnce("0", false);
    assertEquals(200, result1.response.code());
    assertNotNull(result1.resource);

    final String etag1 = result1.response.header("ETag");
    log.info("ETag1: " + etag1);
    assertNotNull(etag1);

    final Result result2 = readVsdOnce(etag1, false);
    assertEquals(304, result2.response.code());
    assertNull(result2.resource);

    final String etag2 = result2.response.header("ETag");
    log.info("ETag2: " + etag2);
    assertNotNull(etag2);
    assertEquals(etag1, etag2);
  }

  @Test
  @Order(4)
  void testResponseContentType() throws Exception {
    final Result result1 = readVsdOnce("0", false);
    assertTrue(result1.response.isSuccessful());

    assertTrue(
        Objects.requireNonNull(result1.response.header("Content-Type"))
            .contains("application/fhir+json"));
  }

  @Test
  @Order(5)
  void testHttpVersion() throws Exception {
    final Result result1 = readVsdOnce("0", false);
    assertTrue(result1.response.isSuccessful());

    assertEquals("http/1.1", result1.response.protocol().toString());
  }

  private static void removeCardFromSlot(final int slot) throws Exception {
    Request removeCard =
        new Request.Builder()
            .url(resolvePlaceholders(CARD_CLIENT_URL + "/slots/" + slot))
            .delete()
            .build();
    Response removeCardResponse = httpClient.newCall(removeCard).execute();

    log.info("removeCard: " + removeCardResponse.code());
    log.info(removeCardResponse.body().string());

    assertTrue(
        removeCardResponse.isSuccessful() || removeCardResponse.code() == 404, "Remove Card");
  }

  @Test
  @Order(6)
  void testPoppTokenIsCached() throws Exception {
    final Result result1 = readVsdOnce("0", false);
    assertTrue(result1.response.isSuccessful());

    final String CARD_HANDLE_URL = resolvePlaceholders(CARD_CLIENT_URL + "/slots/" + EGK_SLOT);
    final Request cardHandleRequest = new Request.Builder().url(CARD_HANDLE_URL).get().build();
    final Response cardHandleResponse = httpClient.newCall(cardHandleRequest).execute();

    assertTrue(cardHandleResponse.isSuccessful());
    final String cardHandleBody = cardHandleResponse.body().string();

    final String cardId =
        cardHandleBody.substring(
            cardHandleBody.indexOf("card-"), cardHandleBody.indexOf("card-") + 18);

    final String VSDM_TEST_POPP_TOKEN_URL =
        VSDM_CLIENT_URL
            + "/client/test/poppToken?terminalId="
            + TERMINAL_ID
            + "&slotId="
            + EGK_SLOT
            + "&cardId="
            + cardId;

    final Request readPoppToken =
        new Request.Builder().url(resolvePlaceholders(VSDM_TEST_POPP_TOKEN_URL)).get().build();
    final Response readPoppTokenResponse = httpClient.newCall(readPoppToken).execute();

    assertThat(readPoppTokenResponse.isSuccessful()).isTrue();
    assertThat(readPoppTokenResponse.body().string()).asString().isNotEmpty();
  }

  @Test
  @Order(7)
  void testVsdmDataIsCached() throws Exception {
    final Result result1 = readVsdOnce("0", false);
    assertTrue(result1.response.isSuccessful());

    final String CARD_HANDLE_URL = CARD_CLIENT_URL + "/slots/" + EGK_SLOT;
    final Request cardHandleRequest =
        new Request.Builder().url(resolvePlaceholders(CARD_HANDLE_URL)).get().build();
    final Response cardHandleResponse = httpClient.newCall(cardHandleRequest).execute();

    assertTrue(cardHandleResponse.isSuccessful());
    final String cardHandleBody = cardHandleResponse.body().string();

    final String cardId =
        cardHandleBody.substring(
            cardHandleBody.indexOf("card-"), cardHandleBody.indexOf("card-") + 18);

    final String VSDM_TEST_CACHED_DATA_URL =
        VSDM_CLIENT_URL
            + "/client/test/vsdmData?terminalId="
            + TERMINAL_ID
            + "&slotId="
            + EGK_SLOT
            + "&cardId="
            + cardId;

    final Request readCachedVsdmData =
        new Request.Builder().url(resolvePlaceholders(VSDM_TEST_CACHED_DATA_URL)).get().build();

    final Response readCachedVsdmDataResponse = httpClient.newCall(readCachedVsdmData).execute();
    assertTrue(readCachedVsdmDataResponse.isSuccessful());

    final String readCachedVsdmDataBody = readCachedVsdmDataResponse.body().string();

    assertNotNull(readCachedVsdmDataBody);
    assertFalse(readCachedVsdmDataBody.isEmpty());
  }

  @Test
  @Order(8)
  void testLoadTruncatedData() throws Exception {
    final String VSDM_TEST_LOAD_TRUNCATED_DATA_URL =
        VSDM_CLIENT_URL
            + "/client/test/readEgk?terminalId="
            + TERMINAL_ID
            + "&egkSlotId="
            + EGK_SLOT;

    final Request readTruncatedData =
        new Request.Builder()
            .url(resolvePlaceholders(VSDM_TEST_LOAD_TRUNCATED_DATA_URL))
            .get()
            .build();

    final Response readTruncatedDataResponse = httpClient.newCall(readTruncatedData).execute();
    assertTrue(readTruncatedDataResponse.isSuccessful());

    final String readTruncatedDataBody = readTruncatedDataResponse.body().string();

    assertNotNull(readTruncatedDataBody);
    System.out.println(readTruncatedDataBody);
    assertFalse(readTruncatedDataBody.isEmpty());
  }

  private static void insertCard(final String filename, final int slot) throws Exception {
    final String cardImage = loadClasspathRessourceWithTigerResolving(filename);

    final Request insertCard =
        new Request.Builder()
            .url(resolvePlaceholders(CARD_CLIENT_URL + "/slots/" + slot))
            .put(RequestBody.create(cardImage, MediaType.parse("application/xml")))
            .build();

    final Response insertCardResponse = httpClient.newCall(insertCard).execute();

    log.info("insertCard: " + insertCardResponse.code());
    log.info(insertCardResponse.body().string());

    assertTrue(insertCardResponse.isSuccessful(), "Insert Card");
  }

  private static void insertEgkCard() throws Exception {
    insertCard("data/cards/egkCardImage.xml", EGK_SLOT);
  }

  private static void insertSmcbCard() throws Exception {
    insertCard("data/cards/smcbCardImage.xml", SMCB_SLOT);
  }

  private static void configureTerminal() throws Exception {
    final String terminalConfig =
        loadClasspathRessourceWithTigerResolving("data/cards/terminal.json");

    final Request configureTerminal =
        new Request.Builder()
            .url(resolvePlaceholders(VSDM_CLIENT_URL + "/client/config/terminal"))
            .put(RequestBody.create(terminalConfig, MediaType.parse("application/json")))
            .build();

    final Response configureTerminalResponse = httpClient.newCall(configureTerminal).execute();

    log.info("configureTerminal: " + configureTerminalResponse.code());
    log.info(configureTerminalResponse.body().string());

    assertTrue(configureTerminalResponse.isSuccessful(), "Configure Terminal");
  }

  private static class Result {
    public Resource resource;
    public Response response;
    public String responseBody;

    public Result(final Resource resource, final Response response, final String responseBody) {
      this.resource = resource;
      this.response = response;
      this.responseBody = responseBody;
    }
  }

  private static Result readVsdOnce(final String ifNoneMatch, final Boolean isFhirXml)
      throws Exception {
    final Request readVsd =
        new Request.Builder()
            .url(resolvePlaceholders(VSDM_ENDPOINT) + "&isFhirXml=" + isFhirXml)
            .header("If-None-Match", ifNoneMatch)
            // For some reason, VSDM client produces the error
            // 'o.a.coyote.http11.Http11Processor - Error state [CLOSE_CONNECTION_NOW] reported
            // while processing request'
            // if connection is kept alive, so we close it via the following header.
            // Further analysis is needed.
            .header("Connection", "close")
            .get()
            .build();

    final Response readVsdResponse = httpClient.newCall(readVsd).execute();

    System.out.println("readVsd: " + readVsdResponse.code());
    System.out.println("readVsd: " + readVsdResponse.body());

    final String readVsdBody = readVsdResponse.body().string();

    log.info("readVsd: " + readVsdResponse.code());
    log.info(readVsdBody);

    VsdmBundle vsdmBundle = null;
    try {
      vsdmBundle = fhirCodec.decode(VsdmBundle.class, readVsdBody);
    } catch (final Exception e) {
      log.debug("Response body is not a valid VsdmBundle", e);
    }

    return new Result(vsdmBundle, readVsdResponse, readVsdBody);
  }
}
