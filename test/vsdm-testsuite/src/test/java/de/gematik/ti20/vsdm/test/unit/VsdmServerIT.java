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

import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Slf4j
class VsdmServerIT {

  private static final String VSDM_SERVER_URL = "http://localhost:9130";
  private static final String VSDM_ENDPOINT = VSDM_SERVER_URL + "/vsdservice/v1/vsdmbundle";

  // KVNR from egk image: X110639491
  private static final String MOCK_POPP_TOKEN =
      makePoppTokenContentCoded("X110639491", "109500969");
  // unknown KVNR, but should return synthetic data: X123450264
  private static final String MOCK_POPP_TOKEN_SYNTHETIC_DATA =
      makePoppTokenContentCoded("X123450264", "109500969");

  private static final String MOCK_POPP_TOKEN_UNKNOWN_KVNR =
      makePoppTokenContentCoded("X9110639492", "109500969");
  private static final String MOCK_POPP_TOKEN_UNKNOWN_IKNR =
      makePoppTokenContentCoded("X110639491", "109500971");
  private static final String MOCK_POPP_TOKEN_INVALID_IKNR =
      makePoppTokenContentCoded("X110639491", "10950097123");

  private static final String MOCK_USER_INFO = "MOCK_USER_INFO";

  private static OkHttpClient httpClient;
  private static FhirCodec fhirCodec;

  @BeforeAll
  static void setup() {
    httpClient = new OkHttpClient.Builder().build();
    fhirCodec = FhirCodec.forR4().andDummyValidator();
  }

  private static String makePoppTokenContentCoded(final String kvnr, final String iknr) {
    String poppTokenContent =
        String.format(
            """
                        {
                            "actorId": "883110000168650",
                            "actorProfessionOid": "1.2.276.0.76.4.32",
                            "at": 1773397230,
                            "insurerId": "%1$s",
                            "iss": "https://popp.example.com",
                            "patientId": "%2$s",
                            "patientProofTime": 1773397230,
                            "proofMethod": "ehc-practitioner-trustedchannel",
                            "version": "1.0.0"
                      }
                  """,
            iknr, kvnr);
    String poppTokenContentCoded = Base64.getEncoder().encodeToString(poppTokenContent.getBytes());
    return poppTokenContentCoded;
  }

  @Test
  @Order(1)
  void testCallSuccessful() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, "0");

    assertEquals(200, result.response.code());
    assertNotNull(result.response.body());
    assertNotEquals("", result.response.body());

    final VsdmBundle vsdmBundle = (VsdmBundle) result.resource;
    assertNotNull(vsdmBundle);

    final Patient patient = (Patient) vsdmBundle.getEntry().get(0).getResource();
    assertNotNull(patient);
    assertEquals("Kriemhild", patient.getName().get(0).getGiven().get(0).getValue());

    final Organization organization = (Organization) vsdmBundle.getEntry().get(1).getResource();
    assertNotNull(organization);
    assertEquals("Test GKV Krankenkasse", organization.getName());

    final Coverage coverage = (Coverage) vsdmBundle.getEntry().get(2).getResource();
    assertNotNull(coverage);
    System.out.println("Coverage Payor Display: " + coverage.getPayor().get(0).getDisplay());
    assertTrue(
        coverage
            .getPayor()
            .get(0)
            .getReference()
            .startsWith("https://gematik.de/fhir/Organization/"));
  }

  @Test
  @Order(1)
  void testCallSuccessfulWithSyntheticData() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN_SYNTHETIC_DATA, MOCK_USER_INFO, "0");

    assertEquals(200, result.response.code());
    assertNotNull(result.response.body());
    assertNotEquals("", result.response.body());

    final VsdmBundle vsdmBundle = (VsdmBundle) result.resource;
    assertNotNull(vsdmBundle);

    final Patient patient = (Patient) vsdmBundle.getEntry().get(0).getResource();
    assertNotNull(patient);
    assertEquals("given-name-X123450264", patient.getName().get(0).getGiven().get(0).getValue());

    final Organization organization = (Organization) vsdmBundle.getEntry().get(1).getResource();
    assertNotNull(organization);
    assertEquals("Test GKV Krankenkasse", organization.getName());

    final Coverage coverage = (Coverage) vsdmBundle.getEntry().get(2).getResource();
    assertNotNull(coverage);
    System.out.println("Coverage Payor Display: " + coverage.getPayor().get(0).getDisplay());
    assertTrue(
        coverage
            .getPayor()
            .get(0)
            .getReference()
            .startsWith("https://gematik.de/fhir/Organization/"));
  }

  @Test
  @Order(1)
  public void testCallNotModified() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, "1");
    assertEquals(200, result.response.code());

    final String etag = result.response.header("etag");
    assertNotNull(etag);

    final Result result2 = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, etag);
    assertEquals(304, result2.response.code());

    assertNotNull(result2.response.header("etag"));
    assertEquals(etag, result2.response.header("etag"));

    assertNotNull(result2.response.header("VSDM-Pz"));
  }

  @Test
  @Order(2)
  public void testUnknownKVNR() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN_UNKNOWN_KVNR, MOCK_USER_INFO, "0");

    assertEquals(400, result.response.code());
    assertNotNull(result.response.body());
    assertNotEquals("", result.response.body());

    final VsdmOperationOutcome vsdmOperationOutcome = (VsdmOperationOutcome) result.resource;
    assertNotNull(vsdmOperationOutcome);

    final CodeableConcept cc = vsdmOperationOutcome.getIssue().get(0).getDetails();
    assertNotNull(cc);

    assertEquals("VSDSERVICE_INVALID_KVNR", cc.getCoding().get(0).getCode());
    assertEquals("[kvnr] aus dem PoPP-Token weist Formatfehler auf.", cc.getText());
  }

  @Test
  @Order(3)
  public void testUnknownIKNR() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN_UNKNOWN_IKNR, MOCK_USER_INFO, "0");

    assertEquals(400, result.response.code());
    assertNotNull(result.response.body());
    assertNotEquals("", result.response.body());

    final VsdmOperationOutcome vsdmOperationOutcome = (VsdmOperationOutcome) result.resource;
    assertNotNull(vsdmOperationOutcome);

    final CodeableConcept cc = vsdmOperationOutcome.getIssue().get(0).getDetails();
    assertNotNull(cc);
    assertEquals("VSDSERVICE_UNKNOWN_IK", cc.getCoding().get(0).getCode());
    assertEquals("[ik] aus dem PoPP-Token ist dem Fachdienst nicht bekannt.", cc.getText());
  }

  @Test
  @Order(3)
  public void testInvalidIKNR() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN_INVALID_IKNR, MOCK_USER_INFO, "0");

    assertEquals(400, result.response.code());
    assertNotNull(result.response.body());
    assertNotEquals("", result.response.body());

    final VsdmOperationOutcome vsdmOperationOutcome = (VsdmOperationOutcome) result.resource;
    assertNotNull(vsdmOperationOutcome);

    final CodeableConcept cc = vsdmOperationOutcome.getIssue().get(0).getDetails();
    assertNotNull(cc);
    assertEquals("VSDSERVICE_INVALID_IK", cc.getCoding().get(0).getCode());
    assertEquals("[ik] aus dem PoPP-Token weist Formatfehler auf.", cc.getText());
  }

  @Test
  @Order(4)
  public void testMissingPoppToken() throws Exception {
    final Result result = callOnce(null, MOCK_USER_INFO, "0");

    assertEquals(400, result.response.code());
    assertNotNull(result.response.body());

    assertNotNull(result.resource);

    final VsdmOperationOutcome operationOutcome = (VsdmOperationOutcome) result.resource;
    assertNotNull(operationOutcome);

    final CodeableConcept cc = operationOutcome.getIssue().get(0).getDetails();
    assertNotNull(cc);
    assertEquals("Der erforderliche HTTP-Header [header] ist ungültig.", cc.getText());
  }

  @Test
  @Order(5)
  public void testMissingUserInfo() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, null, "0");

    assertEquals(400, result.response.code());
    assertNotNull(result.response.body());

    assertNotNull(result.resource);

    final VsdmOperationOutcome vsdmOperationOutcome = (VsdmOperationOutcome) result.resource;
    assertNotNull(vsdmOperationOutcome);

    final CodeableConcept cc = vsdmOperationOutcome.getIssue().get(0).getDetails();
    assertNotNull(cc);
    assertEquals("Der erforderliche HTTP-Header [header] ist ungültig.", cc.getText());
  }

  @Test
  @Order(6)
  public void testMissingIfNoneMatch() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, null);

    assertEquals(428, result.response.code());
  }

  @Test
  @Order(7)
  public void testResponseContainsEtag() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, "0");

    assertNotNull(result.response.header("etag"));
  }

  @Test
  @Order(7)
  public void testResponseContainsPz() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, "0");

    assertNotNull(result.response.header("VSDM-Pz"));
  }

  @Test
  @Order(8)
  public void testProtocolHttp1_1() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, "0");

    assertEquals("http/1.1", result.response.protocol().toString());
  }

  private static class Result {
    public Resource resource;
    public Response response;

    public Result(final Resource resource, final Response response) {
      this.resource = resource;
      this.response = response;
    }
  }

  private Result callOnce(final String poppToken, final String userInfo, final String ifNoneMatch)
      throws Exception {
    final Request.Builder readVsdBuilder = new Request.Builder().url(VSDM_ENDPOINT).get();

    if (poppToken != null) {
      readVsdBuilder.header("zeta-popp-token-content", poppToken);
    }
    if (userInfo != null) {
      readVsdBuilder.header("zeta-user-info", userInfo);
    }
    if (ifNoneMatch != null) {
      readVsdBuilder.header("If-None-Match", ifNoneMatch);
    }

    final Request readVsd = readVsdBuilder.build();
    final Response readVsdResponse = httpClient.newCall(readVsd).execute();

    final String readVsdBody = readVsdResponse.body().string();

    log.debug("readVsd: " + readVsdResponse.code());
    log.debug(readVsdBody);

    Resource resource = null;
    try {
      VsdmBundle vsdmBundle = fhirCodec.decode(VsdmBundle.class, readVsdBody);
      resource = vsdmBundle;
    } catch (final Exception ignored) {
      log.error("readVsd: " + readVsdBody, ignored);
    }

    if (resource == null) {
      try {
        VsdmOperationOutcome vsdmOperationOutcome =
            fhirCodec.decode(VsdmOperationOutcome.class, readVsdBody);
        resource = vsdmOperationOutcome;
      } catch (final Exception ignored) {
        log.error("readVsd: " + readVsdBody, ignored);
      }
    }

    return new Result(resource, readVsdResponse);
  }
}
