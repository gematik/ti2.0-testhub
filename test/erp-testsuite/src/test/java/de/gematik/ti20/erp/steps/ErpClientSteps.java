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

import static java.text.MessageFormat.format;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.base.Strings;
import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.usecases.ErpBaseCommand;
import de.gematik.test.erezept.client.usecases.TaskGetByPoppTokenCommand;
import de.gematik.test.erezept.fhir.r4.erp.ErxTaskBundle;
import lombok.val;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.core.Serenity;
import org.hl7.fhir.r4.model.Resource;

public class ErpClientSteps {

  @Step("Alle E-Rezepte mit PoPP-Token abrufen")
  public ErxTaskBundle obtainPrescriptionsWithPoppToken(ErpClient client, String poppToken) {
    assertFalse(Strings.isNullOrEmpty(poppToken), "PoPP-Token must not be null or empty");
    val requestCmd = new TaskGetByPoppTokenCommand(poppToken);
    val response = client.request(requestCmd);

    reportResponseResource(client.getFhir(), requestCmd, response);
    // if negative response this will throw an UnexpectedResponseResourceError, otherwise return the
    // expected resource
    return response.getExpectedResource();
  }

  /**
   * Report the response to Serenity
   *
   * @param fhir to encode the Resource
   * @param request which initiated the response
   * @param response received from the SUT
   * @param <R> is the expected Resource in a successful response
   */
  private <R extends Resource> void reportResponseResource(
      FhirCodec fhir, ErpBaseCommand<R> request, FhirBResponse<R> response) {
    val title =
        format(
            "Response {0} for {1}", response.getStatusCode(), request.getClass().getSimpleName());
    val resource = response.getAsBaseResource();
    val content = fhir.encode(resource, EncodingType.XML, true);
    Serenity.recordReportData().withTitle(title).andContents(content);
  }
}
