/*-
 * #%L
 * VSDM Server Simservice
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
package de.gematik.ti20.simsvc.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class UserInfoValidationServiceTest {

  private UserInfoValidationService userInfoValidationService;

  @BeforeEach
  void setUp() throws IOException {
    userInfoValidationService = new UserInfoValidationService();
    userInfoValidationService.init();
  }

  private static String toBase64(final String json) {
    return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertBadRequest(final Throwable throwable) {
    assertThat(throwable).isInstanceOf(ResponseStatusException.class);
    assertThat(((ResponseStatusException) throwable).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void validateUserInfo_validWithRequiredFields_doesNotThrow() {
    final String json =
        """
        {"identifier": "12345", "professionOID": "1.2.276.0.76.4.49", "commonName": "cn"}
        """;

    assertThatCode(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .doesNotThrowAnyException();
  }

  @Test
  void validateUserInfo_validWithAllFields_doesNotThrow() {
    final String json =
        """
        {
          "subject": "sub-001",
          "identifier": "12345",
          "professionOID": "1.2.276.0.76.4.49",
          "organizationName": "Musterkrankenhaus",
          "commonName": "Dr. Max Mustermann"
        }
        """;

    assertThatCode(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .doesNotThrowAnyException();
  }

  @Test
  void validateUserInfo_validWithAdditionalProperties_doesNotThrow() {
    final String json =
        """
        {
          "identifier": "12345",
          "professionOID": "1.2.276.0.76.4.49",
          "commonName": "cn",
          "extraField": "extraValue"
        }
        """;

    assertThatCode(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .doesNotThrowAnyException();
  }

  @Test
  void validateUserInfo_invalidBase64_throwsBadRequest() {
    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo("!kein-base64!"))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }

  @Test
  void validateUserInfo_validBase64ButInvalidJson_throwsBadRequest() {
    final String notJson = toBase64("das ist kein JSON {{{");

    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(notJson))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }

  @Test
  void validateUserInfo_emptyJson_throwsBadRequest() {
    final String emptyJson = toBase64("{}");

    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(emptyJson))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }

  @Test
  void validateUserInfo_missingIdentifier_throwsBadRequest() {
    final String json =
        """
        {"professionOID": "1.2.276.0.76.4.49"}
        """;

    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }

  @Test
  void validateUserInfo_missingProfessionOID_throwsBadRequest() {
    final String json =
        """
        {"identifier": "12345"}
        """;

    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }

  @Test
  void validateUserInfo_missingBothRequiredFields_throwsBadRequest() {
    final String json =
        """
        {"subject": "sub-001", "organizationName": "Musterkrankenhaus"}
        """;

    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }

  @Test
  void validateUserInfo_identifierIsNotString_throwsBadRequest() {
    final String json =
        """
        {"identifier": 12345, "professionOID": "1.2.276.0.76.4.49"}
        """;

    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }

  @Test
  void validateUserInfo_professionOIDIsNotString_throwsBadRequest() {
    final String json =
        """
        {"identifier": "12345", "professionOID": 123}
        """;

    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .satisfies(UserInfoValidationServiceTest::assertBadRequest);
  }
}
