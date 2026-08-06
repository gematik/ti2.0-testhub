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
package de.gematik.ti20.vsdm.test.e2e.steps;

import de.gematik.ti20.vsdm.test.e2e.enums.CommType;
import de.gematik.ti20.vsdm.test.e2e.helper.PoppTokenValidator;
import de.gematik.ti20.vsdm.test.e2e.models.EgkCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.SmcbCardInfo;
import de.gematik.ti20.vsdm.test.e2e.questions.LastPoppToken;
import de.gematik.ti20.vsdm.test.e2e.tasks.RequestPoppTokenFromServer;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;

public class PoppSteps extends BaseSteps {

  private CommType commType;

  @Angenommen("der Versicherte in der LEI verwendet eine eGK {string} am Lesegerät {string}")
  public void givenPatientIsUsingEgk(final String readerType, final String commType) {
    this.commType = CommType.from(readerType, commType);
    hccs().remember("commType", this.commType);
  }

  @Wenn("das Primärsystem den PoPP-Token mit der eGK vom PoPP-Service abgefragt")
  public void whenClientSystemIsRequestingPoppToken() {
    hccs().attemptsTo(RequestPoppTokenFromServer.now());
    hccs().remember("poppToken", LastPoppToken.value().answeredBy(hccs()));
  }

  @Dann("empfängt das Primärsystem den vollständigen und spezifikationskonformen PoPP-Token")
  public void thenClientSystemIsReceivingValidPoppToken() {
    PoppTokenValidator.validateClaimsInPoppToken();
    PoppTokenValidator.proofMethodInTokenMatchesCommTypeOrThrow(commType);
  }

  @Und("die Daten des Versicherten im PoPP-Token entsprechen denen auf der eGK")
  public void andPoppTokenContainsPatientData() {
    EgkCardInfo egkCardInfo = hccs().recall("egkCardInfo");
    PoppTokenValidator.assertThatPatientDataInTokenMatchesDataOnEgk(egkCardInfo);
  }

  @Und("die Daten des Leistungserbringers im PoPP-Token entsprechen denen auf der SMC-B")
  public void andPoppTokenContainsClientSystemData() {
    SmcbCardInfo smcbCardInfo = hccs().recall("smcbCardInfo");
    PoppTokenValidator.asserThatPractitionerDataInTokenMatchesDataOnSmcb(smcbCardInfo);
  }
}
