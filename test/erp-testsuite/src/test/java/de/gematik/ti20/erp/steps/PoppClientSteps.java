/*-
 * #%L
 * erp-testsuite
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
package de.gematik.ti20.erp.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.gematik.ti20.erp.popp.PoppClientCommunicationType;
import de.gematik.ti20.erp.popp.PoppClientRequestBody;
import de.gematik.ti20.erp.popp.PoppClientResponse;
import kong.unirest.core.UnirestInstance;
import lombok.SneakyThrows;
import lombok.val;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.core.Serenity;

public class PoppClientSteps {

  private final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

  @Step("PoPP-Token abrufen")
  public PoppClientResponse obtainPoPPToken(UnirestInstance client) {
    val pcResponse =
        client
            .request("post", "/token")
            .body(new PoppClientRequestBody(PoppClientCommunicationType.CONTACT_CONNECTOR))
            .asObject(PoppClientResponse.class);

    val responseBody = pcResponse.getBody();
    reportPoppClientResponse(responseBody);

    assertTrue(
        pcResponse.isSuccess(),
        "PoPP-Token konnte nicht abgerufen werden: ("
            + pcResponse.getStatus()
            + ") "
            + responseBody.errorMessage());
    return responseBody;
  }

  @SneakyThrows
  private void reportPoppClientResponse(PoppClientResponse response) {
    val title = "Response from PoPP-Client";
    val content = objectWriter.writeValueAsString(response);
    Serenity.recordReportData().withTitle(title).andContents(content);
  }
}
