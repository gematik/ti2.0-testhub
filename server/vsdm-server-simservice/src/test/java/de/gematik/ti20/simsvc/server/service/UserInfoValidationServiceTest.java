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

import de.gematik.ti20.simsvc.server.exception.ZetaErrorException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

class UserInfoValidationServiceTest {

  private UserInfoValidationService userInfoValidationService;

  @BeforeEach
  void setUp() {
    userInfoValidationService = new UserInfoValidationService();
    userInfoValidationService.init();
  }

  private static String toBase64(final String json) {
    return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertZetaErrorException(final Throwable throwable) {
    assertThat(throwable).isInstanceOf(ZetaErrorException.class);
    assertThat(((ZetaErrorException) throwable).getErrorCase().getHttpCode())
        .isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validUserInfoJsons")
  void validateUserInfo_validJsons_doesNotThrow(final String testName, final String json) {
    assertThatCode(() -> userInfoValidationService.validateUserInfo(toBase64(json)))
        .doesNotThrowAnyException();
  }

  private static List<Arguments> validUserInfoJsons() {
    return List.of(
        Arguments.of(
            "validWithRequiredFields",
            """
            {"identifier": "12345", "professionOID": "1.2.276.0.76.4.49", "commonName": "cn"}
            """),
        Arguments.of(
            "validWithAllFields",
            """
            {
              "subject": "sub-001",
              "identifier": "12345",
              "professionOID": "1.2.276.0.76.4.49",
              "organizationName": "Musterkrankenhaus",
              "commonName": "Dr. Max Mustermann"
            }
            """),
        Arguments.of(
            "validWithAdditionalProperties",
            """
            {
              "identifier": "12345",
              "professionOID": "1.2.276.0.76.4.49",
              "commonName": "cn",
              "extraField": "extraValue"
            }
            """));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidUserInfoJsons")
  void validateUserInfo_invalidJsons_throwsZetaError(
      final String testName, final String userInfoBase64) {
    assertThatThrownBy(() -> userInfoValidationService.validateUserInfo(userInfoBase64))
        .satisfies(UserInfoValidationServiceTest::assertZetaErrorException);
  }

  private static List<Arguments> invalidUserInfoJsons() {
    return List.of(
        Arguments.of("invalidBase64", "!kein-base64!"),
        Arguments.of("validBase64ButInvalidJson", toBase64("das ist kein JSON {{{")),
        Arguments.of("emptyJson", toBase64("{}")),
        Arguments.of(
            "missingIdentifier",
            toBase64(
                """
                {"professionOID": "1.2.276.0.76.4.49"}
                """)),
        Arguments.of(
            "missingProfessionOID",
            toBase64(
                """
                {"identifier": "12345"}
                """)),
        Arguments.of(
            "missingBothRequiredFields",
            toBase64(
                """
                {"subject": "sub-001", "organizationName": "Musterkrankenhaus"}
                """)),
        Arguments.of(
            "identifierIsNotString",
            toBase64(
                """
                {"identifier": 12345, "professionOID": "1.2.276.0.76.4.49"}
                """)),
        Arguments.of(
            "professionOIDIsNotString",
            toBase64(
                """
                {"identifier": "12345", "professionOID": 123}
                """)));
  }
}
