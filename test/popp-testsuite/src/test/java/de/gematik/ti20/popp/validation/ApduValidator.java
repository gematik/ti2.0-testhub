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

import static de.gematik.ti20.popp.data.TestConstants.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApduValidator extends BaseValidator {

  public ApduValidator() {
    super();
  }

  public void validateApdusforEHealthKT() {
    log.info("Valdidate eH-KT APDUS");
    // Sequenz 0
    findNextRequestToPathContainingNode("$..body.message.sequenceCounter", "0");
    currentRequestAtMatchesAsJsonTheFile("$..body.message", VALID_APDU_SEQUENCE_0_FILE);

    // Sequenz 1
    findNextRequestToPathContainingNode("$..body.message.sequenceCounter", "1");
    currentRequestAtMatchesAsJsonTheFile("$..body.message", VALID_APDU_SEQUENCE_1_FILE);

    // Sequenz 2
    findNextRequestToPathContainingNode("$..body.message.sequenceCounter", "2");
    currentRequestAtMatchesAsJsonTheFile("$..body.message", VALID_APDU_SEQUENCE_2_FILE);

    // Sequenz 3
    findNextRequestToPathContainingNode("$..body.message.sequenceCounter", "3");
    currentRequestAtMatchesAsJsonTheFile("$..body.message", VALID_APDU_SEQUENCE_3_FILE);

    log.info("eH-KT APDUS succesfully validated");
  }

  public void validateApdusforStdKT() {
    log.info("Valdidate Standard Cardreader APDUS");
    // Sequenz 0
    findNextRequestToPathContainingNode("$..payload.sequenceCounter", "0");
    currentRequestAtMatchesAsJsonTheFile("$..payload", VALID_APDU_SEQUENCE_0_FILE);

    // Sequenz 1
    findNextRequestToPathContainingNode("$..payload.sequenceCounter", "1");
    currentRequestAtMatchesAsJsonTheFile("$..payload", VALID_APDU_SEQUENCE_1_FILE);

    // Sequenz 2
    findNextRequestToPathContainingNode("$..payload.sequenceCounter", "2");
    currentRequestAtMatchesAsJsonTheFile("$..payload", VALID_APDU_SEQUENCE_2_FILE);

    // Sequenz 3
    findNextRequestToPathContainingNode("$..payload.sequenceCounter", "3");
    currentRequestAtMatchesAsJsonTheFile("$..payload", VALID_APDU_SEQUENCE_3_FILE);

    log.info("Standard Cardreader APDUS succesfully validated");
  }
}
