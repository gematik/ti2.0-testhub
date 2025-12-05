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
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@Slf4j
public class VsdmServerIT {

  private static final String VSDM_SERVER_URL = "http://localhost:9130";
  private static final String VSDM_ENDPOINT = VSDM_SERVER_URL + "/vsdservice/v1/vsdmbundle";

  private static final String MOCK_POPP_TOKEN =
      "eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsucG9wcCtqd3QiLCJraWQiOiJhbGlhcyIsIng1YyI6WyJNSUlEQmpDQ0FxeWdBd0lCQWdJSEFtQ25LeHIvUnpBS0JnZ3Foa2pPUFFRREFqQ0JoREVMTUFrR0ExVUVCaE1DUkVVeEh6QWRCZ05WQkFvTUZtZGxiV0YwYVdzZ1IyMWlTQ0JPVDFRdFZrRk1TVVF4TWpBd0JnTlZCQXNNS1V0dmJYQnZibVZ1ZEdWdUxVTkJJR1JsY2lCVVpXeGxiV0YwYVd0cGJtWnlZWE4wY25WcmRIVnlNU0F3SGdZRFZRUUREQmRIUlUwdVMwOU5VQzFEUVRZeElGUkZVMVF0VDA1TVdUQWVGdzB5TlRBM01qZ3lNakF3TURCYUZ3MHpNREEzTWpneU1UVTVOVGxhTUdzeEN6QUpCZ05WQkFZVEFrUkZNU1l3SkFZRFZRUUtEQjFuWlcxaGRHbHJJRlJGVTFRdFQwNU1XU0F0SUU1UFZDMVdRVXhKUkRFME1ESUdBMVVFQXd3cmNHOXdjQzEwYjJ0bGJpNW5aVzFoZEdsckxuUmxiR1Z0WVhScGF5MTBaWE4wSUhOcGJYVnNZWFJ2Y2pCWk1CTUdCeXFHU000OUFnRUdDQ3FHU000OUF3RUhBMElBQkhBdkkzclBpTTRVNUhCekhUSXllc24vWmNaUkh0djlWeHVqbGJUYVJQdHYyc3FPNXZ5dmEyNEVRbjhBZ1M2bU9ObmVSaElJOHFoUkxxL1ZJMDZEeE9HamdnRWZNSUlCR3pBN0JnZ3JCZ0VGQlFjQkFRUXZNQzB3S3dZSUt3WUJCUVVITUFHR0gyaDBkSEE2THk5bGFHTmhMbWRsYldGMGFXc3VaR1V2WldOakxXOWpjM0F3SFFZRFZSME9CQllFRkRobnRHbnFueEVRTnpRdkRTTUpLSFllRUROYU1DRUdBMVVkSUFRYU1CZ3dDZ1lJS29JVUFFd0VnaDh3Q2dZSUtvSVVBRXdFZ1NNd0h3WURWUjBqQkJnd0ZvQVVuelhnTUtsL3l2aG1uNUFLUXMyN2dXV2ZTZjR3RGdZRFZSMFBBUUgvQkFRREFnWkFNRnNHQlNza0NBTURCRkl3VURCT01Fd3dTakJJTURvTU9GUnZhMlZ1TFZOcFoyNWhkSFZ5TFVsa1pXNTBhWFREcEhRZ1pzTzhjaUJRY205dlppQnZaaUJRWVhScFpXNTBJRkJ5WlhObGJtTmxNQW9HQ0NxQ0ZBQk1CSUpBTUF3R0ExVWRFd0VCL3dRQ01BQXdDZ1lJS29aSXpqMEVBd0lEU0FBd1JRSWhBS3VDdi9ZczBMd3lQRTh2eVBZWVF5UFpBTGltendwRjJHallVWUlwZ3QyNUFpQUhucVpubEV0dEhzL0Y0SEExckYyZWNmMjd0L2NMMlM4b0VvSFZaMmpra2c9PSJdfQ.eyJpYXQiOjE3NjE2NTkzNzAsInZlcnNpb24iOiIxLjAuMCIsImlzcyI6Imh0dHBzOi8vcG9wcC5leGFtcGxlLmNvbSIsInByb29mTWV0aG9kIjoiZWhjLXByb3ZpZGVyLXVzZXIteDUwOSIsInBhdGllbnRQcm9vZlRpbWUiOjE3NTIyMjcwODYsInBhdGllbnRJZCI6IlgxMjM0NTY3ODkwIiwiaW5zdXJlcklkIjoiMTA5NTAwOTY5IiwiYWN0b3JJZCI6Ijg4MzExMDAwMDE2ODY1MCIsImFjdG9yUHJvZmVzc2lvbk9pZCI6IjEuMi4yNzYuMC43Ni40LjMyIn0.bd61wFXpt1vcaGLPhaIfAMrJGyiDajmpVD978OSN0fp_fp1JrYm_9p5V5_QdqwgiktsegkOqJRP4mVQsZi5Ypg";
  private static final String MOCK_POPP_TOKEN_UNKNOWN_KVNR =
      "eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsucG9wcCtqd3QiLCJraWQiOiJhbGlhcyIsIng1YyI6WyJNSUlEQmpDQ0FxeWdBd0lCQWdJSEFtQ25LeHIvUnpBS0JnZ3Foa2pPUFFRREFqQ0JoREVMTUFrR0ExVUVCaE1DUkVVeEh6QWRCZ05WQkFvTUZtZGxiV0YwYVdzZ1IyMWlTQ0JPVDFRdFZrRk1TVVF4TWpBd0JnTlZCQXNNS1V0dmJYQnZibVZ1ZEdWdUxVTkJJR1JsY2lCVVpXeGxiV0YwYVd0cGJtWnlZWE4wY25WcmRIVnlNU0F3SGdZRFZRUUREQmRIUlUwdVMwOU5VQzFEUVRZeElGUkZVMVF0VDA1TVdUQWVGdzB5TlRBM01qZ3lNakF3TURCYUZ3MHpNREEzTWpneU1UVTVOVGxhTUdzeEN6QUpCZ05WQkFZVEFrUkZNU1l3SkFZRFZRUUtEQjFuWlcxaGRHbHJJRlJGVTFRdFQwNU1XU0F0SUU1UFZDMVdRVXhKUkRFME1ESUdBMVVFQXd3cmNHOXdjQzEwYjJ0bGJpNW5aVzFoZEdsckxuUmxiR1Z0WVhScGF5MTBaWE4wSUhOcGJYVnNZWFJ2Y2pCWk1CTUdCeXFHU000OUFnRUdDQ3FHU000OUF3RUhBMElBQkhBdkkzclBpTTRVNUhCekhUSXllc24vWmNaUkh0djlWeHVqbGJUYVJQdHYyc3FPNXZ5dmEyNEVRbjhBZ1M2bU9ObmVSaElJOHFoUkxxL1ZJMDZEeE9HamdnRWZNSUlCR3pBN0JnZ3JCZ0VGQlFjQkFRUXZNQzB3S3dZSUt3WUJCUVVITUFHR0gyaDBkSEE2THk5bGFHTmhMbWRsYldGMGFXc3VaR1V2WldOakxXOWpjM0F3SFFZRFZSME9CQllFRkRobnRHbnFueEVRTnpRdkRTTUpLSFllRUROYU1DRUdBMVVkSUFRYU1CZ3dDZ1lJS29JVUFFd0VnaDh3Q2dZSUtvSVVBRXdFZ1NNd0h3WURWUjBqQkJnd0ZvQVVuelhnTUtsL3l2aG1uNUFLUXMyN2dXV2ZTZjR3RGdZRFZSMFBBUUgvQkFRREFnWkFNRnNHQlNza0NBTURCRkl3VURCT01Fd3dTakJJTURvTU9GUnZhMlZ1TFZOcFoyNWhkSFZ5TFVsa1pXNTBhWFREcEhRZ1pzTzhjaUJRY205dlppQnZaaUJRWVhScFpXNTBJRkJ5WlhObGJtTmxNQW9HQ0NxQ0ZBQk1CSUpBTUF3R0ExVWRFd0VCL3dRQ01BQXdDZ1lJS29aSXpqMEVBd0lEU0FBd1JRSWhBS3VDdi9ZczBMd3lQRTh2eVBZWVF5UFpBTGltendwRjJHallVWUlwZ3QyNUFpQUhucVpubEV0dEhzL0Y0SEExckYyZWNmMjd0L2NMMlM4b0VvSFZaMmpra2c9PSJdfQ.eyJpYXQiOjE3NjE2NjE5NDYsInZlcnNpb24iOiIxLjAuMCIsImlzcyI6Imh0dHBzOi8vcG9wcC5leGFtcGxlLmNvbSIsInByb29mTWV0aG9kIjoiZWhjLXByb3ZpZGVyLXVzZXIteDUwOSIsInBhdGllbnRQcm9vZlRpbWUiOjE3NTI1ODA4NDUsInBhdGllbnRJZCI6Ilg5MTEwNjM5NDkyIiwiaW5zdXJlcklkIjoiMTA5NTAwOTY5IiwiYWN0b3JJZCI6Ijg4MzExMDAwMDE2ODY1MCIsImFjdG9yUHJvZmVzc2lvbk9pZCI6IjEuMi4yNzYuMC43Ni40LjMyIn0.3daAOddNDLA0MgoI3gEXsRSwGAvDaaGDhB2EzoyIEs4aAN00Q0kNNQ4eedMgSpOyokcFdoYGgfZK4lVT35B_Rg";
  private static final String MOCK_POPP_TOKEN_UNKNOWN_IKNR =
      "eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsucG9wcCtqd3QiLCJraWQiOiJwb3BwbW9jayIsIng1YyI6WyJNSUlCM1RDQ0FZR2dBd0lCQWdJRUJ3R0pSekFNQmdncWhrak9QUVFEQWdVQU1HTXhDekFKQmdOVkJBWVRBa1JGTVE0d0RBWURWUVFJRXdWVGRHRjBaVEVOTUFzR0ExVUVCeE1FUTJsMGVURVFNQTRHQTFVRUNoTUhSWGhoYlhCc1pURVVNQklHQTFVRUN4TUxSR1YyWld4dmNHMWxiblF4RFRBTEJnTlZCQU1UQkZSbGMzUXdIaGNOTWpVd05USXpNVEl6TWpVd1doY05Nall3TlRJek1USXpNalV3V2pCak1Rc3dDUVlEVlFRR0V3SkVSVEVPTUF3R0ExVUVDQk1GVTNSaGRHVXhEVEFMQmdOVkJBY1RCRU5wZEhreEVEQU9CZ05WQkFvVEIwVjRZVzF3YkdVeEZEQVNCZ05WQkFzVEMwUmxkbVZzYjNCdFpXNTBNUTB3Q3dZRFZRUURFd1JVWlhOME1Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMERBUWNEUWdBRVhwR00wL3ZjUnNjbWl4eEl0bjdLNjI0Y3dOdVFBUGc3djJCNWJrSmh2RUJWOVUvOVlyQXI3NjJDWnFPRTdSM2NqLzRDVjVwamdHNW45RTFRT2RScU1LTWhNQjh3SFFZRFZSME9CQllFRk5xSSt0NDZDMFo1SXJhbThKWnhXV3N2SGlKbE1Bd0dDQ3FHU000OUJBTUNCUUFEU0FBd1JRSWhBSXJUa2pjck1ZMDBMOU1VWDdNajc4OGhzL1c0aFNnWnNua2Y1M2hwSUZyQkFpQjlEWnQzNzlGOXRKbHArajRCN3Bsb3BybU5sT1hvRnh2ZnlObWNsVlVVVUE9PSJdfQ.eyJ2ZXJzaW9uIjoiMS4wLjAiLCJpc3MiOiJodHRwczovL3BvcHAuZXhhbXBsZS5jb20iLCJpYXQiOjE3NTI1ODQ4OTAsInByb29mTWV0aG9kIjoiZWhjLXByb3ZpZGVyLXVzZXIteDUwOSIsInBhdGllbnRQcm9vZlRpbWUiOjE3NTI1ODQ4OTAsInBhdGllbnRJZCI6IlgxMTA2Mzk0OTEiLCJpbnN1cmVySWQiOiIxMDk1MDA5NzEiLCJhY3RvcklkIjoiODgzMTEwMDAwMTY4NjUwIiwiYWN0b3JQcm9mZXNzaW9uT2lkIjoiMS4yLjI3Ni4wLjc2LjQuMzIifQ.vhXZuinnt93YhxYFYjzvyhsQZQQb7TK5YNhL_ICw7E6insMlAp7B3h-ONW396lDGYgNBHJwz8TCyH_wUqw3aNw";

  private static final String MOCK_USER_INFO = "MOCK_USER_INFO";

  private static OkHttpClient httpClient;
  private static FhirCodec fhirCodec;

  @BeforeAll
  public static void setup() {
    final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    logger.setLevel(Level.INFO);

    httpClient = new OkHttpClient.Builder().build();
    fhirCodec = FhirCodec.forR4().andDummyValidator();
  }

  @Test
  @Order(1)
  public void testCallSuccessful() throws Exception {
    final Result result = callOnce(MOCK_POPP_TOKEN, MOCK_USER_INFO, "0");

    assertEquals(200, result.response.code());
    assertNotNull(result.response.body());
    assertNotEquals("", result.response.body());

    final VsdmBundle vsdmBundle = (VsdmBundle) result.resource;
    assertNotNull(vsdmBundle);

    final Patient patient = (Patient) vsdmBundle.getEntry().get(0).getResource();
    assertNotNull(patient);
    assertEquals("given-name-X1234567890", patient.getName().get(0).getGiven().get(0).getValue());

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
    assertEquals("Ungültige oder nicht bekannte Krankenversichertennummer <kvnr>.", cc.getText());
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
    assertEquals("VSDSERVICE_INVALID_IK", cc.getCoding().get(0).getCode());
    assertEquals("Ungültige oder nicht bekannte Institutionskennung <ik>.", cc.getText());
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
    assertEquals("Der erforderliche HTTP-Header <header> fehlt oder ist ungültig.", cc.getText());
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
    assertEquals("Der erforderliche HTTP-Header <header> fehlt oder ist ungültig.", cc.getText());
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
