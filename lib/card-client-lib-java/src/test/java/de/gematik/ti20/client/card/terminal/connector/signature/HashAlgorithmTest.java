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
package de.gematik.ti20.client.card.terminal.connector.signature;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class HashAlgorithmTest {

  @ParameterizedTest
  @MethodSource("provideHashAlgorithms")
  void thatHashAlgorithmHasExpectedValues(
      final HashAlgorithm algorithm,
      final String expectedUri,
      final String expectedSignatureAlgorithm) {
    assertThat(algorithm.getUri()).isEqualTo(expectedUri);
    assertThat(algorithm.getSignatureAlgorithm()).isEqualTo(expectedSignatureAlgorithm);
  }

  private static Stream<Arguments> provideHashAlgorithms() {
    return Stream.of(
        Arguments.of(
            HashAlgorithm.SHA256, "http://www.w3.org/2001/04/xmlenc#sha256", "SHA256withRSA"),
        Arguments.of(
            HashAlgorithm.SHA384, "http://www.w3.org/2001/04/xmldsig-more#sha384", "SHA384withRSA"),
        Arguments.of(
            HashAlgorithm.SHA512, "http://www.w3.org/2001/04/xmlenc#sha512", "SHA512withRSA"));
  }
}
