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
    expectRequestsWithNodeMatching("$..payload.sequenceCounter", "2")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..payload", VALID_APDU_SEQUENCE_2_FILE);
    log.info("Standard Cardreader APDUS succesfully validated");
  }

  public static void validateApdusforStdKTContactless() {
    log.info("Validate contactless standard card reader APDUs");

    expectRequestsWithNodeMatching("$..payload.sequenceCounter", "0")
        .nextResponse()
        .hasJsonAtPathEqualToFile("$..payload", VALID_APDU_SEQUENCE_0_FILE);

    validateContactlessSequence1();
  }

  private static void validateContactlessSequence1() {

    expectRequestsWithNodeMatching("$..payload.sequenceCounter", "1")
        .hasValueAtPathMatching("$..payload.version", "1\\.0\\.0")
        .hasValueAtPathMatching("$..payload.clientSessionId", "123456")
        .hasValueAtPathMatching("$..payload.timeSpan", "0")
        .hasValueAtPathMatching("$..payload.type", "StandardScenario")
        // Statische APDUs
        .hasValueAtPathMatching("$..payload.steps.0.commandApdu", "00B0870000")
        .hasValueAtPathMatching("$..payload.steps.1.commandApdu", "00B0860000")
        .hasValueAtPathMatching("$..payload.steps.2.commandApdu", "00A4040C0AA000000167455349474E")
        .hasValueAtPathMatching("$..payload.steps.3.commandApdu", "002241A406840109800100")
        .hasValueAtPathMatching("$..payload.steps.4.commandApdu", "00B08400000000")
        // Dynamischer Challenge-Anteil: 24 Byte bzw. 48 Hex-Zeichen
        .hasValueAtPathMatching("$..payload.steps.5.commandApdu", "0088000018[0-9A-F]{48}00");
  }
}
