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

import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;

public class ZetaSteps extends BaseSteps {

  @Wenn(
      "das Primärsystem sich mit seiner SMC-B beim ZETA Guard des VSDM 2.0 Fachdienstes authentifiziert")
  public void whenClientSystemIsAuthorizingAtZetaGuard() {
    // TODO: ZETA client endpoint needed to directly request ZETA guard.
  }

  @Dann("erhält das Primärsystem einen Access- und Refresh-Token vom ZETA Guard")
  public void thenClientSystemIsReceivingAccessAndRefreshToken() {
    // TODO: ZETA client endpoint needed to verify access and refresh token.
  }
}
