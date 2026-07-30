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

import static de.gematik.ti20.popp.data.TestConstants.ENTITY_STATEMENT_PATH;
import static de.gematik.ti20.popp.data.TestConstants.POPP_SERVICE_BASE_URL;
import static de.gematik.ti20.rbel.fluent.RbelFluentApi.expectRequests;

import java.time.Duration;

public final class EntityStatementValidator {
  private EntityStatementValidator() {}

  public static void validateEntityStatement() {
    expectRequests(ENTITY_STATEMENT_PATH)
        .nextResponse()
        .hasValueAtPathEqualTo("$.body.body.iss", POPP_SERVICE_BASE_URL)
        .hasValueAtPathEqualTo("$.body.body.sub", POPP_SERVICE_BASE_URL)
        .hasValueAtPathMatching("$.body.body.metadata.oauth_resource.signed_jwks_uri", "https.*")
        .hasEpochSecondsAtPathAfterNowMinusSeconds(
            "$.body.body.iat", Duration.ofSeconds(30).toSeconds())
        .hasEpochSecondsAtPathBeforeNowPlusSeconds(
            "$.body.body.exp", Duration.ofHours(24).toSeconds())
        .extractChildWithPath("$.body.signature.isValid")
        .hasValueEqualTo(Boolean.TRUE);
  }
}
