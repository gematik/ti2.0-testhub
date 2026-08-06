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

import de.gematik.bbriccs.cardterminal.CardTerminal;
import de.gematik.bbriccs.smartcards.Egk;
import de.gematik.bbriccs.smartcards.SmcB;
import net.serenitybdd.annotations.Step;

public class CatsClientSteps {

  @Step("resete Kartenlesegerät {0}")
  public void resetCardTerminal(CardTerminal ct) {
    ct.resetSlots();
  }

  @Step("eGK {1} in das Kartenlesegerät {0} einstecken")
  public void insertEgk(CardTerminal ct, Egk egk) {
    ct.insertCard(egk);
  }

  @Step("SMC-B {1} in das Kartenlesegerät {0} einstecken")
  public void insertSmcb(CardTerminal ct, SmcB smcb) {
    ct.insertCard(smcb);
  }
}
