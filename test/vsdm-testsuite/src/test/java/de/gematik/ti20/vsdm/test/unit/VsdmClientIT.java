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
package de.gematik.ti20.vsdm.test.unit;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Request;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;

@Slf4j
public class VsdmClientIT {

  private static final String CARD_CLIENT_URL = "http://localhost:8000";
  private static final String VSDM_CLIENT_URL = "http://localhost:8220";

  private static final Integer TERMINAL_ID = 1;
  private static final Boolean IS_FIRE_XML = false;
  private static final Integer EGK_SLOT = 1;
  private static final Integer SMCB_SLOT = 2;

  private static final String VSDM_ENDPOINT =
      VSDM_CLIENT_URL
          + "/client/vsdm/vsd?terminalId="
          + TERMINAL_ID
          + "&egkSlotId="
          + EGK_SLOT
          + "&smcBSlotId="
          + SMCB_SLOT
          + "&isFhirXml="
          + IS_FIRE_XML;

  private static OkHttpClient httpClient;
  private static FhirCodec fhirCodec;

  // Preconditions:
  // 1. All VSDM components are running via docker-compose.

  @BeforeAll
  public static void setup() {
    final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.setLevel(Level.INFO);

    httpClient =
        new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // set to 30 seconds
            .build();
    fhirCodec = FhirCodec.forR4().andDummyValidator();
  }

  @BeforeEach
  public void beforeEach() throws Exception {
    removeCardFromSlot(EGK_SLOT);
    removeCardFromSlot(SMCB_SLOT);

    insertEgkCard();
    insertSmcbCard();

    configureTerminal();
  }

  @Test
  @Order(1)
  public void testReadVsdReturnsExpectedData() throws Exception {
    final Result result = readVsdOnce("0");
    assertEquals(200, result.response.code());
    assertNotNull(result.resource);

    final VsdmBundle vsdmBundle = (VsdmBundle) result.resource;
    assertNotNull(vsdmBundle);

    final Resource patientResource = vsdmBundle.getEntry().get(0).getResource();
    assertNotNull(patientResource);
    assertTrue(patientResource instanceof Patient);

    final Patient patient = (Patient) patientResource;
    final HumanName name = patient.getName().get(0);
    assertEquals("Amelie Abigail H. Freifrau Bruser", name.getFamily());
    assertEquals("Kriemhild", name.getGiven().get(0).getValue());

    assertEquals("X110639491", patient.getIdentifierFirstRep().getValue());

    final Resource payorOrganization = vsdmBundle.getEntry().get(1).getResource();
    assertNotNull(payorOrganization);
    assertTrue(payorOrganization instanceof Organization);
    assertEquals("Test GKV Krankenkasse", ((Organization) payorOrganization).getName());

    final Resource coverageResource = vsdmBundle.getEntry().get(2).getResource();
    assertNotNull(coverageResource);
    assertTrue(coverageResource instanceof Coverage);

    final Coverage coverage = (Coverage) coverageResource;
    assertTrue(
        coverage
            .getPayor()
            .get(0)
            .getDisplay()
            .startsWith("https://gematik.de/fhir/Organization/"));
  }

  @Test
  @Order(2)
  // Caution! The test fails with Timeout during the first run. Further analysis is required.
  public void testPruefzifferHasCorrectLength() throws Exception {
    final Result result = readVsdOnce("0");
    assertEquals(200, result.response.code());

    final String pruefzifferEncoded = result.response.header("VSDM-Pz");
    assertNotNull(pruefzifferEncoded);

    byte[] pruefziffer = Base64.getUrlDecoder().decode(pruefzifferEncoded);
    log.info(" VSDM-Pz: " + pruefzifferEncoded);
    log.info(" VSDM-Pz Länge: " + pruefziffer.length);
    assertEquals(64, pruefziffer.length);
  }

  @Test
  @Order(3)
  public void testEtagIsConsistent() throws Exception {
    final Result result1 = readVsdOnce("0");
    assertEquals(200, result1.response.code());
    assertNotNull(result1.resource);

    final String etag1 = result1.response.header("ETag");
    log.info("ETag1: " + etag1);
    assertNotNull(etag1);

    final Result result2 = readVsdOnce(etag1);
    assertEquals(304, result2.response.code());
    assertNull(result2.resource);

    final String etag2 = result2.response.header("ETag");
    log.info("ETag2: " + etag2);
    assertNotNull(etag2);
    assertEquals(etag1, etag2);
  }

  @Test
  @Order(4)
  public void testResponseContentType() throws Exception {
    final Result result1 = readVsdOnce("0");
    assertTrue(result1.response.isSuccessful());

    assertTrue(result1.response.header("Content-Type").contains("application/fhir+json"));
  }

  @Test
  @Order(5)
  public void testHttpVersion() throws Exception {
    final Result result1 = readVsdOnce("0");
    assertTrue(result1.response.isSuccessful());

    assertEquals("http/1.1", result1.response.protocol().toString());
  }

  @Test
  @Order(6)
  public void testSmcBMissing() throws Exception {
    removeCardFromSlot(SMCB_SLOT);
    final Result result1 = readVsdOnce("0");
    assertTrue(result1.response.code() == 403);
  }

  private static void removeCardFromSlot(final int slot) throws Exception {
    Request removeCard =
        new Request.Builder().url(CARD_CLIENT_URL + "/slots/" + slot).delete().build();
    Response removeCardResponse = httpClient.newCall(removeCard).execute();

    log.info("removeCard: " + removeCardResponse.code());
    log.info(removeCardResponse.body().string());

    assertTrue(
        removeCardResponse.isSuccessful() || removeCardResponse.code() == 404, "Remove Card");
  }

  @Test
  @Order(7)
  public void testPoppTokenIsCached() throws Exception {
    final Result result1 = readVsdOnce("0");
    assertTrue(result1.response.isSuccessful());

    final String CARD_HANDLE_URL = CARD_CLIENT_URL + "/slots/" + EGK_SLOT;
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

    final Request readPoppToken = new Request.Builder().url(VSDM_TEST_POPP_TOKEN_URL).get().build();
    final Response readPoppTokenResponse = httpClient.newCall(readPoppToken).execute();
    assert (readPoppTokenResponse.isSuccessful());

    final String readPoppTokenBody = readPoppTokenResponse.body().string();

    assertNotNull(readPoppTokenBody);
    assertTrue(!readPoppTokenBody.isEmpty());
  }

  @Test
  @Order(8)
  public void testVsdmDataIsCached() throws Exception {
    final Result result1 = readVsdOnce("0");
    assertTrue(result1.response.isSuccessful());

    final String CARD_HANDLE_URL = CARD_CLIENT_URL + "/slots/" + EGK_SLOT;
    final Request cardHandleRequest = new Request.Builder().url(CARD_HANDLE_URL).get().build();
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
        new Request.Builder().url(VSDM_TEST_CACHED_DATA_URL).get().build();

    final Response readCachedVsdmDataResponse = httpClient.newCall(readCachedVsdmData).execute();
    assertTrue(readCachedVsdmDataResponse.isSuccessful());

    final String readCachedVsdmDataBody = readCachedVsdmDataResponse.body().string();

    assertNotNull(readCachedVsdmDataBody);
    assertTrue(!readCachedVsdmDataBody.isEmpty());
  }

  @Test
  @Order(9)
  public void testLoadTruncatedData() throws Exception {
    final String VSDM_TEST_LOAD_TRUNCATED_DATA_URL =
        VSDM_CLIENT_URL
            + "/client/test/readEgk?terminalId="
            + TERMINAL_ID
            + "&egkSlotId="
            + EGK_SLOT;

    final Request readTruncatedData =
        new Request.Builder().url(VSDM_TEST_LOAD_TRUNCATED_DATA_URL).get().build();

    final Response readTruncatedDataResponse = httpClient.newCall(readTruncatedData).execute();
    assertTrue(readTruncatedDataResponse.isSuccessful());

    final String readTruncatedDataBody = readTruncatedDataResponse.body().string();

    assertNotNull(readTruncatedDataBody);
    System.out.println(readTruncatedDataBody);
    assertTrue(!readTruncatedDataBody.isEmpty());
  }

  private static String loadFile(final String fileName) throws Exception {
    final InputStream is = VsdmClientIT.class.getClassLoader().getResourceAsStream(fileName);
    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
  }

  private static void insertCard(final String filename, final int slot) throws Exception {
    final String cardImage = loadFile(filename);

    final Request insertCard =
        new Request.Builder()
            .url(CARD_CLIENT_URL + "/slots/" + slot)
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
    final String terminalConfig = loadFile("data/cards/terminal.json");

    final Request configureTerminal =
        new Request.Builder()
            .url(VSDM_CLIENT_URL + "/client/config/terminal")
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

    public Result(final Resource resource, final Response response) {
      this.resource = resource;
      this.response = response;
    }
  }

  private static Result readVsdOnce(final String ifNoneMatch) throws Exception {
    final Request readVsd =
        new Request.Builder().url(VSDM_ENDPOINT).header("If-None-Match", ifNoneMatch).get().build();

    final Response readVsdResponse = httpClient.newCall(readVsd).execute();
    final String readVsdBody = readVsdResponse.body().string();

    log.info("readVsd: " + readVsdResponse.code());
    log.info(readVsdBody);

    VsdmBundle vsdmBundle = null;
    try {
      vsdmBundle = fhirCodec.decode(VsdmBundle.class, readVsdBody);
    } catch (final Exception ignored) {
    }

    return new Result(vsdmBundle, readVsdResponse);
  }
}
