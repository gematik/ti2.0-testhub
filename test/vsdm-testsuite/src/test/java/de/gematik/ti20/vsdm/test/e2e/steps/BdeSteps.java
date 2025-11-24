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

import de.gematik.ti20.vsdm.test.e2e.models.EgkCardInfo;
import de.gematik.ti20.vsdm.test.e2e.models.SmcbCardInfo;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import io.restassured.response.Response;
import java.util.LinkedList;
import net.serenitybdd.core.Serenity;
import org.junit.jupiter.api.Assertions;

public class BdeSteps extends BaseSteps {
  private final LinkedList<Response> responses = new LinkedList<>();
  private SmcbCardInfo smcbCardInfo;
  private Integer smcbSlot;
  private EgkCardInfo egkCardInfo;
  private Integer egkSlot;
  private String poppToken;

  @Wenn("das Primärsystem die VSD mit einer falschen IK-Nummer vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithWrongIkNumber() throws Exception {
    egkCardInfo = Serenity.sessionVariableCalled("egkCardInfo");
    Assertions.assertNotNull(egkCardInfo.getIknr());
    egkCardInfo.setIknr("WRONG_IKNR"); // Invalid IKNR.
    smcbCardInfo = Serenity.sessionVariableCalled("smcbCardInfo");
    poppToken = generatePoppToken(smcbCardInfo, egkCardInfo);
    smcbSlot = Serenity.sessionVariableCalled("smcbSlot");
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    responses.add(getVsd("0", poppToken, smcbSlot, egkSlot));
  }

  @Wenn("das Primärsystem die VSD mit einer falschen KVNR-Nummer vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithWrongKvnrNumber() throws Exception {
    egkCardInfo = Serenity.sessionVariableCalled("egkCardInfo");
    Assertions.assertNotNull(egkCardInfo.getKvnr());
    egkCardInfo.setKvnr("WRONG_KVNR"); // Invalid KVNR.
    smcbCardInfo = Serenity.sessionVariableCalled("smcbCardInfo");
    poppToken = generatePoppToken(smcbCardInfo, egkCardInfo);
    smcbSlot = Serenity.sessionVariableCalled("smcbSlot");
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    responses.add(getVsd("0", poppToken, smcbSlot, egkSlot));
  }

  @Wenn(
      "das Primärsystem die VSD vom VSDM Ressource Server abfragt und dieser die VSD nicht finden kann")
  public void whenClientSystemIsRequestingMissingVsd() throws Exception {
    egkCardInfo = Serenity.sessionVariableCalled("egkCardInfo");
    Assertions.assertNotNull(egkCardInfo.getKvnr());
    egkCardInfo.setKvnr("X4567890123"); // Unknown KVNR.
    smcbCardInfo = Serenity.sessionVariableCalled("smcbCardInfo");
    poppToken = generatePoppToken(smcbCardInfo, egkCardInfo);
    smcbSlot = Serenity.sessionVariableCalled("smcbSlot");
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    responses.add(getVsd("0", poppToken, smcbSlot, egkSlot));
  }

  @Wenn("das Primärsystem die VSD mit einem fehlenden E-Tag vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithWrongEtag() throws Exception {
    egkCardInfo = Serenity.sessionVariableCalled("egkCardInfo");
    smcbCardInfo = Serenity.sessionVariableCalled("smcbCardInfo");
    poppToken = generatePoppToken(smcbCardInfo, egkCardInfo);
    smcbSlot = Serenity.sessionVariableCalled("smcbSlot");
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    responses.add(getVsd(null, poppToken, smcbSlot, egkSlot));
  }

  @Wenn(
      "das Primärsystem die VSD mit einem ungültigen PoPP-Token vom VSDM Ressource Server abfragt")
  public void whenClientSystemIsRequestingVsdWithInvalidPoppToken() {
    smcbSlot = Serenity.sessionVariableCalled("smcbSlot");
    egkSlot = Serenity.sessionVariableCalled("egkSlot");
    responses.add(getVsd("0", "INVALID_POPP_TOKEN", smcbSlot, egkSlot));
  }

  @Dann("antwortet der VSDM Ressource Server mit dem Fehlercode {int} und dem Text {string}")
  public void thenVsdmAnswersWithCorrespondingBdeCodeAndText(Integer httpCode, String bdeText) {
    // TODO Zeta captures 500 responses and masks the content.
    //    assertThat(getVsdmOperationOutcome(responses.getLast()))
    //        .extracting(vsdmOperationOutcome -> vsdmOperationOutcome.getIssue().getFirst())
    //        .isInstanceOf(OperationOutcome.OperationOutcomeIssueComponent.class)
    //        .isNotNull()
    //        .extracting(OperationOutcome.OperationOutcomeIssueComponent::getDetails)
    //        .isInstanceOf(CodeableConcept.class)
    //        .isNotNull()
    //        .extracting(codeableConcept -> codeableConcept.getCoding().getFirst().getCode())
    //        .isNotNull()
    //        .isEqualTo(bdeText);

    responses.getLast().then().assertThat().statusCode(httpCode);
  }
}
