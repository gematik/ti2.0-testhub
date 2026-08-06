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
package de.gematik.ti20.erp;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.idp.client.IdpClientRuntimeException;
import de.gematik.ti20.erp.config.ErpTestsuiteConfiguration;
import de.gematik.ti20.erp.steps.CatsClientSteps;
import de.gematik.ti20.erp.steps.ErpClientSteps;
import de.gematik.ti20.erp.steps.PoppClientSteps;
import lombok.val;
import net.serenitybdd.annotations.Steps;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(SerenityJUnit5Extension.class)
public class ErpPoppIT {

  @Autowired private ErpTestsuiteConfiguration config;
  @Steps PoppClientSteps poppClientSteps;
  @Steps ErpClientSteps erpClientSteps;
  @Steps CatsClientSteps catsClientSteps;

  @Test
  @DisplayName("Alle E-Rezepte mit PoPP-Token abrufen")
  @Tag("positive")
  void shouldFetchPrescriptionsWithPoPPToken() {
    // GIVEN a pharmacy ERP client is instantiated
    val erpClient = config.createPharmacyErpClient("Arminius");
    // AND an SMC-B is instantiated
    val smcb = config.getSmcbFor("Arminius");
    // AND a card terminal client is connected and reset
    val cardTerminal = config.getCardTerminal();
    catsClientSteps.resetCardTerminal(cardTerminal);
    // AND a PoPP client is instantiated
    val poppClient = config.createPoppClient();
    // AND an eGK is instantiated
    val egk = config.getPatientEgk("Alice");

    // WHEN SMC-B is insterted into card terminal
    catsClientSteps.insertSmcb(cardTerminal, smcb);
    // WHEN eGK is inserted into card terminal
    catsClientSteps.insertEgk(cardTerminal, egk);
    // AND a valid PoPP token is obtained
    val poppToken = poppClientSteps.obtainPoPPToken(poppClient).token();

    // THEN the ERP client can retrieve all prescriptions for the token's KVNR
    val taskBundle = erpClientSteps.obtainPrescriptionsWithPoppToken(erpClient, poppToken);
  }

  @Test // just demonstrates the issue with the ZETA SMC-B
  @DisplayName("Mit ZETA SMC-B kann kein AccessToken beim IdP geholt werden")
  @Tag("negative")
  void shouldNotObtainIdpTokenWithZETASMCB() {
    // GIVEN a pharmacy ERP client configured with a ZeTA SMC-B
    val erpClient = config.createPharmacyErpClient("Ann-Beatrixe");

    // WHEN attempting to obtain prescriptions via the ERP FHIR endpoint
    val poppToken = "irrelevant — IDP rejects the SMC-B certificate before PoPP token is evaluated";
    val idpError =
        assertThrows(
            IdpClientRuntimeException.class,
            () -> erpClientSteps.obtainPrescriptionsWithPoppToken(erpClient, poppToken));

    // THEN the IDP rejects the request because the AUT certificate is invalid
    assertTrue(idpError.getMessage().contains("Das AUT Zertifikat ist ungültig"));
  }
}
