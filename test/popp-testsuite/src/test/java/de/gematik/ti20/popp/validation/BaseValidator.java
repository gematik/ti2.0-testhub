/*
 *
 * Copyright 2026 gematik GmbH
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

package de.gematik.ti20.popp.validation;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.lib.rbel.ModeType;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.test.tiger.lib.rbel.RequestParameter;

public class BaseValidator {
  private final RbelMessageRetriever rbelMessageRetriever;
  private final RbelValidator rbelValidator;

  public BaseValidator(final RbelMessageRetriever rbelMessageRetriever) {
    this.rbelValidator = new RbelValidator();
    this.rbelMessageRetriever = rbelMessageRetriever;
  }

  public BaseValidator() {
    this(RbelMessageRetriever.getInstance());
  }

  void findNextRequestToPathContainingNode(String s, String value) {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder()
            .rbelPath(s)
            .value(value)
            .requireRequestMessage(false)
            .build()
            .resolvePlaceholders());
  }

  void currentRequestAtMatchesAsJsonTheFile(String rbelPath, String validationFile) {
    this.rbelValidator.assertAttributeOfCurrentResponseMatchesAs(
        rbelPath,
        ModeType.JSON,
        TigerGlobalConfiguration.resolvePlaceholders("!{file('" + validationFile + "')}"),
        "",
        this.rbelMessageRetriever);
  }

  void findRequestWithEndpoint(String path) {
    this.rbelMessageRetriever.filterRequestsAndStoreInContext(
        RequestParameter.builder().path(path).build().resolvePlaceholders());
  }
}
