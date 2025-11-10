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
package de.gematik.ti20.simsvc.server.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCaseTest {
  @Test
  void thatGettersWorkAsIntended() {
    final ErrorCase internalServerError = ErrorCase.VSDSERVICE_INTERNAL_SERVER_ERROR;
    assertThat(internalServerError.getBdeCode()).isEqualTo("79100");
    assertThat(internalServerError.getBdeText())
        .isEqualTo("Unerwarteter interner Fehler des Fachdienstes VSDM.");
    assertThat(internalServerError.getBdeReference()).isEqualTo("VSDSERVICE_INTERNAL_SERVER_ERROR");
    assertThat(internalServerError.getHttpCode()).isEqualTo(500);
  }

  @Test
  void thatGetByBdeReferenceWorks() {
    final ErrorCase expected = ErrorCase.VSDSERVICE_INTERNAL_SERVER_ERROR;
    final ErrorCase actual = ErrorCase.getByBdeReference("VSDSERVICE_INTERNAL_SERVER_ERROR");
    assertThat(actual).isEqualTo(expected);
  }
}
