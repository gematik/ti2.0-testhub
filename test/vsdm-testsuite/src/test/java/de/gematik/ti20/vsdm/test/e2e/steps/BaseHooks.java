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

import static de.gematik.test.tiger.common.config.TigerGlobalConfiguration.resolvePlaceholders;

import de.gematik.ti20.vsdm.test.e2e.abilities.CallCardClient;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallPoppClient;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallPoppTokenGenerator;
import de.gematik.ti20.vsdm.test.e2e.abilities.CallVsdmClient;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import net.serenitybdd.screenplay.actors.OnStage;
import net.serenitybdd.screenplay.actors.OnlineCast;

public class BaseHooks {

  @Before(order = 0)
  public void setTheStage() {
    OnStage.setTheStage(new OnlineCast());

    // Die Basis-URLs kommen aus dem aktiven Block in tiger/vsdm-environments.yaml,
    // ausgewaehlt ueber die Stufe -Denv=<name> bzw. -Dvsdm.env=<name>, wenn nur
    // VSDM abweichen soll. Unbekannte Werte fallen dort automatisch auf
    // `vsdm.common` zurueck. Einzelne Ports lassen sich weiterhin per
    // -Dports.<name>=... uebersteuern.
    OnStage.theActorCalled("Primärsystem")
        .can(CallPoppTokenGenerator.at(resolvePlaceholders("${vsdm.server.poppTokenGeneratorUrl}")))
        .can(CallCardClient.at(resolvePlaceholders("${vsdm.server.cardTerminalUrl}")))
        .can(CallPoppClient.at(resolvePlaceholders("${vsdm.server.poppClientUrl}")))
        .can(CallVsdmClient.at(resolvePlaceholders("${vsdm.server.clientUrl}")));
  }

  @After(order = 100)
  public void cleanUpStage() {
    OnStage.drawTheCurtain();
  }
}
