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
package de.gematik.ti20.simsvc.server.exception;

import lombok.Getter;

@Getter
public enum ErrorCase {
  VSDSERVICE_INVALID_IK(
      "79010", 400, "VSDSERVICE_INVALID_IK", "[ik] aus dem PoPP-Token weist Formatfehler auf."),
  VSDSERVICE_INVALID_KVNR(
      "79011", 400, "VSDSERVICE_INVALID_KVNR", "[kvnr] aus dem PoPP-Token weist Formatfehler auf."),
  VSDSERVICE_UNKNOWN_IK(
      "79012",
      400,
      "VSDSERVICE_UNKNOWN_IK",
      "[ik] aus dem PoPP-Token ist dem Fachdienst nicht bekannt."),
  VSDSERVICE_UNKNOWN_KVNR(
      "79013",
      404,
      "VSDSERVICE_UNKNOWN_KVNR",
      "[kvnr] aus dem PoPP-Token ist dem Fachdienst nicht bekannt."),
  VSDSERVICE_MISSING_PATIENT_RECORD_VERSION(
      "79014",
      428,
      "VSDSERVICE_MISSING_PATIENT_RECORD_VERSION",
      "Der erforderliche Änderungsindikator [etag_value] fehlt in der Anfrage."),
  SERVICE_MISSING_OR_INVALID_HEADER(
      "79030",
      400,
      "SERVICE_MISSING_OR_INVALID_HEADER",
      "Der erforderliche HTTP-Header [header] ist ungültig."),
  SERVICE_UNSUPPORTED_MEDIATYPE(
      "79031",
      406,
      "SERVICE_UNSUPPORTED_MEDIATYPE",
      "Der vom Clientsystem angefragte Medientyp [media type] wird nicht unterstützt."),
  SERVICE_INVALID_HTTP_OPERATION(
      "79040", 405, "SERVICE_INVALID_HTTP_OPERATION", "Die HTTP-Operation wird nicht unterstützt."),
  SERVICE_INTERNAL_SERVER_ERROR(
      "79100",
      500,
      "SERVICE_INTERNAL_SERVER_ERROR",
      "Ein unerwarteter interner Fehler ist aufgetreten.");

  private final String bdeCode;
  private final Integer httpCode;
  private final String bdeReference;
  private final String bdeText;

  private ErrorCase(
      final String bdeCode,
      final Integer httpCode,
      final String bdeReference,
      final String bdeText) {
    this.bdeCode = bdeCode;
    this.httpCode = httpCode;
    this.bdeReference = bdeReference;
    this.bdeText = bdeText;
  }

  public static ErrorCase getByBdeReference(final String bdeReference) {
    for (ErrorCase errorCase : ErrorCase.values()) {
      if (errorCase.getBdeReference().equals(bdeReference)) {
        return errorCase;
      }
    }
    return null;
  }
}
