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
package de.gematik.ti20.vsdm.test.e2e.steps;

import static io.restassured.RestAssured.given;
import static java.time.Instant.now;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import de.gematik.ti20.vsdm.fhir.def.VsdmOperationOutcome;
import de.gematik.ti20.vsdm.test.e2e.enums.ProofMethod;
import de.gematik.ti20.vsdm.test.e2e.models.EgkCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.SmcbCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.TokenResults;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;

public class BaseSteps {
  protected static final String BASE_URL_CARD_CLIENT_SIM = "http://localhost:8000";
  protected static final String BASE_URL_VSDM_CLIENT_SIM = "http://localhost:8220";
  protected static final String BASE_URL_POPP_CLIENT_SIM = "http://localhost:9210";
  protected static final String TERMINAL_ID = "0";
  protected static final Boolean IS_FIRE_XML = false;
  protected static final Boolean FORCE_UPDATE = true;

  protected String generatePoppToken(SmcbCardInfo smcbCardInfo, EgkCardInfo egkCardInfo)
      throws Exception {
    Map<String, List<Map<String, String>>> tokenArgs =
        Map.of(
            "tokenParamsList",
            List.of(
                Map.of(
                    "proofMethod",
                    ProofMethod.EHC_PRACTITIONER_TRUSTEDCHANNEL.getValue(),
                    "patientProofTime",
                    String.valueOf(now().getEpochSecond()),
                    "iat",
                    String.valueOf(now().getEpochSecond() + 86_400),
                    "patientId",
                    egkCardInfo.getKvnr(),
                    "insurerId",
                    egkCardInfo.getIknr(),
                    "actorId",
                    smcbCardInfo.getTelematikId(),
                    "actorProfessionOid",
                    smcbCardInfo.getProfessionOid())));

    Response response =
        given()
            .baseUri(BASE_URL_POPP_CLIENT_SIM)
            .contentType(ContentType.JSON)
            .body(new ObjectMapper().writeValueAsString(tokenArgs))
            .post("/popp/test/api/v1/token-generator");

    response.then().assertThat().statusCode(200);

    String jsonBody = Objects.requireNonNull(response.body().asString());
    ObjectMapper mapper = new ObjectMapper();
    TokenResults tokenResults = mapper.readValue(jsonBody, TokenResults.class);

    String poppToken = tokenResults.getTokenResults().get(0);
    Assertions.assertNotNull(poppToken);
    return poppToken;
  }

  protected Response getVsd(String etag, String poppToken, Integer smcbSlot, Integer egkSlot) {
    RequestSpecification request =
        given()
            .baseUri(BASE_URL_VSDM_CLIENT_SIM)
            .queryParam("forceUpdate", FORCE_UPDATE)
            .queryParam("terminalId", TERMINAL_ID)
            .queryParam("isFhirXml", IS_FIRE_XML)
            .queryParam("smcBSlotId", smcbSlot)
            .queryParam("egkSlotId", egkSlot);

    if (etag != null) {
      request.header("If-None-Match", etag);
    }

    if (poppToken != null) {
      request.header("poppToken", poppToken);
    }

    return request.get("/client/vsdm/vsd");
  }

  protected VsdmBundle getVsdmBundle(Response response) {
    String body = Objects.requireNonNull(response.body().asString());
    try {
      final FhirCodec FHIR_CODEC = FhirCodec.forR4().andDummyValidator();
      return FHIR_CODEC.decode(VsdmBundle.class, body);
    } catch (Exception ignored) {
      return null;
    }
  }

  protected VsdmOperationOutcome getVsdmOperationOutcome(Response response) {
    String body = Objects.requireNonNull(response.body().asString());
    try {
      final FhirCodec FHIR_CODEC = FhirCodec.forR4().andDummyValidator();
      return FHIR_CODEC.decode(VsdmOperationOutcome.class, body);
    } catch (Exception ignored) {
      return null;
    }
  }
}
