/*-
 * #%L
 * PoPP Testsuite
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
package de.gematik.ti20.popp.validation;

import static de.gematik.ti20.popp.data.TestConstants.*;
import static de.gematik.ti20.rbel.fluent.RbelFluentApi.expectRequestsWithNodeMatching;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ApduValidator {
  private ApduValidator() {}

  public static void validateApdusforEHealthKT() {
    log.info("Valdidate eH-KT APDUS");
    expectRequestsWithNodeMatching("$..body.message.sequenceCounter", "0")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..body.message", VALID_APDU_SEQUENCE_0_FILE);
    expectRequestsWithNodeMatching("$..body.message.sequenceCounter", "1")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..body.message", VALID_APDU_SEQUENCE_1_FILE);
    expectRequestsWithNodeMatching("$..body.message.sequenceCounter", "3")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..body.message", VALID_APDU_SEQUENCE_3_FILE);
    log.info("eH-KT APDUS succesfully validated");
  }

  public static void validateApdusforStdKT() {
    log.info("Valdidate Standard Cardreader APDUS");
    expectRequestsWithNodeMatching("$..payload.sequenceCounter", "0")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..payload", VALID_APDU_SEQUENCE_0_FILE);
    expectRequestsWithNodeMatching("$..payload.sequenceCounter", "1")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..payload", VALID_APDU_SEQUENCE_1_FILE);
    expectRequestsWithNodeMatching("$..payload.sequenceCounter", "3")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..payload", VALID_APDU_SEQUENCE_3_FILE);
    log.info("Standard Cardreader APDUS succesfully validated");
  }
}
