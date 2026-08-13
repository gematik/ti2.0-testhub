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

import de.gematik.bbriccs.fhir.codec.EmptyResource;
import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
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

  private static final String HOST = System.getProperty("ports.host", "127.0.0.1");
  private static final String CARD_CLIENT_URL = "http://" + HOST + ":${ports.cardTerminalPort}";
  private static final String VSDM_CLIENT_URL = "http://" + HOST + ":${ports.vsdmClientPort}";

  private static final Integer TERMINAL_ID = 1;
  private static final Integer EGK_SLOT = 1;
  private static final Integer SMCB_SLOT = 2;
  private static final String PROFILE_VERSION = "1.0";

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
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    fhirCodec = FhirCodec.forR4().andDummyValidator();
  }

  @BeforeEach
  void beforeEach() throws Exception {
    deletePoppTokenCache();

    removeCardFromSlot(EGK_SLOT);
    removeCardFromSlot(SMCB_SLOT);

    insertEgkCard();
    insertSmcbCard();

    configureTerminal();

    // delete cached popp tokens and vsdm data
    final Request deletePoppToken =
        new Request.Builder()
            .url(resolvePlaceholders(VSDM_CLIENT_URL) + "/client/test/poppToken")
            .delete()
            .build();
    final Response deletePoppTokenResponse = httpClient.newCall(deletePoppToken).execute();
    assertThat(deletePoppTokenResponse.isSuccessful()).isTrue();

    final Request deleteVsdmData =
        new Request.Builder()
            .url(resolvePlaceholders(VSDM_CLIENT_URL) + "/client/test/vsdmData")
            .delete()
            .build();
    final Response deleteVsdmDataResponse = httpClient.newCall(deleteVsdmData).execute();
    assertThat(deleteVsdmDataResponse.isSuccessful()).isTrue();
  }

  @Test
  @Order(1)
  void testReadVsdReturnsExpectedDataJson() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .virtualCard("/data/cards/egkCardImage.xml")
                .build());
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
    assertEquals("Test GKV Krankenkasse", payorOrganization.getName());

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
  void testReadVsdReturnsExpectedDataJsonForVirtualCard() throws Exception {
    removeCardFromSlot(EGK_SLOT);
    insertCard("data/cards/EGK_80276883110000168584_gema5.xml", EGK_SLOT);

    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .virtualCard("/data/cards/EGK_80276883110000168584_gema5.xml")
                .build());
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
    assertEquals("family-name-X110630769", name.getFamily());
    assertEquals("given-name-X110630769", name.getGiven().getFirst().getValue());

    assertEquals("X110630769", patient.getIdentifierFirstRep().getValue());
  }

  @Test
  @Order(3)
  void testReadVsdReturnsExpectedDataXML() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(true)
                .profileVersion(PROFILE_VERSION)
                .virtualCard("/data/cards/egkCardImage.xml")
                .build());
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
    assertEquals("Test GKV Krankenkasse", payorOrganization.getName());

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
  @Order(4)
  void testPruefzifferHasCorrectLength() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .build());
    assertEquals(200, result.response.code());

    final String pruefzifferEncoded = result.response.header("vsdm-pz");
    assertNotNull(pruefzifferEncoded);
    assertEquals(64, pruefzifferEncoded.length());

    log.info(" vsdm-pz: {}", pruefzifferEncoded);
    log.info(" vsdm-pz Länge: {}", pruefzifferEncoded.length());

    byte[] pruefziffer = Base64.getUrlDecoder().decode(pruefzifferEncoded);
    // ensure can be decoded
    assertNotNull(pruefziffer);
  }

  @Test
  @Order(5)
  void testEtagIsConsistent() throws Exception {
    final Result result1 =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .build());
    assertEquals(200, result1.response.code());
    assertNotNull(result1.resource);

    final String etag1 = result1.response.header("etag");
    log.info("ETag1: " + etag1);
    assertNotNull(etag1);

    final Result result2 =
        readVsdOnce(
            EmptyResource.class,
            new RequestBuilder()
                .ifNoneMatch(etag1)
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .build());
    assertEquals(304, result2.response.code());
    assertEquals("", result2.responseBody);

    final String etag2 = result2.response.header("etag");
    log.info("ETag2: " + etag2);
    assertNotNull(etag2);
    assertEquals(etag1, etag2);
  }

  @Test
  @Order(6)
  void testResponseContentType() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .build());
    assertTrue(result.response.isSuccessful());

    assertTrue(
        Objects.requireNonNull(result.response.header("Content-Type"))
            .contains("application/fhir+json"));
  }

  @Test
  @Order(7)
  void testHttpVersion() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .build());
    assertTrue(result.response.isSuccessful());

    assertEquals("http/1.1", result.response.protocol().toString());
  }

  @Test
  @Order(8)
  void testPoppTokenIsCached() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .build());
    assertTrue(result.response.isSuccessful());

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
  @Order(9)
  void testVsdmDataIsCached() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmBundle.class,
            new RequestBuilder()
                .ifNoneMatch("0")
                .isFhirXml(false)
                .profileVersion(PROFILE_VERSION)
                .build());
    assertTrue(result.response.isSuccessful());

    final String CARD_HANDLE_URL = CARD_CLIENT_URL + "/slots/" + EGK_SLOT;
    final Request cardHandleRequest =
        new Request.Builder().url(resolvePlaceholders(CARD_HANDLE_URL)).get().build();
    final Response cardHandleResponse = httpClient.newCall(cardHandleRequest).execute();

    assertTrue(cardHandleResponse.isSuccessful());
    final String cardHandleBody = cardHandleResponse.body().string();

    System.out.println(cardHandleBody);
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
  @Order(10)
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

  @Test
  @Order(10)
  void testMissingProfileVersion() throws Exception {
    final Result result =
        readVsdOnce(
            VsdmOperationOutcome.class,
            new RequestBuilder().ifNoneMatch("0").isFhirXml(false).profileVersion(null).build());
    assertEquals(400, result.response.code());
    assertTrue(result.responseBody.contains("VSDSERVICE_MISSING_PROFILE_VERSION"));
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

  private static void insertCard(final String filename, final int slot) throws Exception {
    final String cardImage = loadClasspathRessourceWithTigerResolving(filename);

    final MediaType mediaType =
        filename.endsWith("xml")
            ? MediaType.parse("application/xml")
            : MediaType.parse("application/json");
    final Request insertCard =
        new Request.Builder()
            .url(resolvePlaceholders(CARD_CLIENT_URL + "/slots/" + slot))
            .put(RequestBody.create(cardImage, mediaType))
            .build();

    final Response insertCardResponse = httpClient.newCall(insertCard).execute();

    log.info("insertCard: " + insertCardResponse.code());
    log.info(insertCardResponse.body().string());

    assertTrue(insertCardResponse.isSuccessful(), "Insert Card");
  }

  private static void insertEgkCard() throws Exception {
    insertCard("data/cards/egkCardData.json", EGK_SLOT);
  }

  private static void insertSmcbCard() throws Exception {
    insertCard("private/cards/smcbCardImage.xml", SMCB_SLOT);
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

  private static void deletePoppTokenCache() throws Exception {
    final Request deletePoppTokenCache =
        new Request.Builder()
            .url(resolvePlaceholders(VSDM_CLIENT_URL + "/client/test/poppToken"))
            .delete()
            .build();

    final Response deletePoppTokenCacheResponse =
        httpClient.newCall(deletePoppTokenCache).execute();

    log.info("deletePoppTokenCache: " + deletePoppTokenCacheResponse.code());
    assertTrue(deletePoppTokenCacheResponse.isSuccessful(), "Delete Popp Token cache");
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

  private static class RequestBuilder {
    private String ifNoneMatch;
    private Boolean isFhirXml;
    private String profileVersion;
    private String virtualCard;

    public RequestBuilder ifNoneMatch(String ifNoneMatch) {
      this.ifNoneMatch = ifNoneMatch;
      return this;
    }

    public RequestBuilder isFhirXml(Boolean isFhirXml) {
      this.isFhirXml = isFhirXml;
      return this;
    }

    public RequestBuilder profileVersion(String profileVersion) {
      this.profileVersion = profileVersion;
      return this;
    }

    public RequestBuilder virtualCard(String virtualCard) {
      this.virtualCard = virtualCard;
      return this;
    }

    public Request build() {
      String url = resolvePlaceholders(VSDM_ENDPOINT);
      url += "&isFhirXml=" + isFhirXml;
      if (profileVersion != null) {
        url += "&profileVersion=" + profileVersion;
      }
      if (virtualCard != null) {
        url += "&virtualCard=" + virtualCard;
      }
      return new Request.Builder()
          .url(url)
          .header("If-None-Match", ifNoneMatch)
          // For some reason, VSDM client produces the error
          // 'o.a.coyote.http11.Http11Processor - Error state [CLOSE_CONNECTION_NOW] reported
          // while processing request'
          // if connection is kept alive, so we close it via the following header.
          // Further analysis is needed.
          .header("Connection", "close")
          .get()
          .build();
    }
  }

  private static Result readVsdOnce(Class<? extends Resource> expectedClazz, Request readVsd)
      throws Exception {
    final Response readVsdResponse = httpClient.newCall(readVsd).execute();

    final String readVsdBody = readVsdResponse.body().string();

    log.info("readVsd: " + readVsdResponse.code());
    log.info(readVsdBody);

    final Resource resource = fhirCodec.decode(expectedClazz, readVsdBody);

    return new Result(resource, readVsdResponse, readVsdBody);
  }
}
