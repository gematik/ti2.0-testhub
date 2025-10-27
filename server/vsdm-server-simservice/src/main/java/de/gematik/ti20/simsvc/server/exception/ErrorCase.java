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

public enum ErrorCase {
  VSDSERVICE_INVALID_IK(
      "79010",
      400,
      "VSDSERVICE_INVALID_IK",
      "Ungültige oder nicht bekannte Institutionskennung <ik>."),
  VSDSERVICE_INVALID_KVNR(
      "79011",
      400,
      "VSDSERVICE_INVALID_KVNR",
      "Ungültige oder nicht bekannte Krankenversichertennummer <kvnr>."),
  VSDSERVICE_PATIENT_RECORD_NOT_FOUND(
      "79020",
      404,
      "VSDSERVICE_PATIENT_RECORD_NOT_FOUND",
      "Die Versichertenstammdaten zur Versichertennummer <kvnr> konnten für die Institutionskennung <ik> nicht ermittelt werden."),
  VSDSERVICE_MISSING_OR_INVALID_HEADER(
      "79030",
      400,
      "VSDSERVICE_MISSING_OR_INVALID_HEADER",
      "Der erforderliche HTTP-Header <header> fehlt oder ist ungültig."),
  VSDSERVICE_UNSUPPORTED_MEDIATYPE(
      "79031",
      400,
      "VSDSERVICE_UNSUPPORTED_MEDIATYPE",
      "Der vom Clientsystem angefragte Medientyp <media type> wird nicht unterstützt."),
  VSDSERVICE_UNSUPPORTED_ENCODING(
      "79032",
      400,
      "VSDSERVICE_UNSUPPORTED_ENCODING",
      "Das vom Clientsystem angefragte Komprimierungsverfahren <encoding scheme> wird nicht unterstützt."),
  VSDSERVICE_INVALID_PATIENT_RECORD_VERSION(
      "79033",
      428,
      "VSDSERVICE_INVALID_PATIENT_RECORD_VERSION",
      "Der Änderungsindikator <etag_value> kann nicht verarbeitet werden."),
  VSDSERVICE_INVALID_HTTP_OPERATION(
      "79040",
      405,
      "VSDSERVICE_INVALID_HTTP_OPERATION",
      "Die HTTP-Operation <http-operation> wird nicht unterstützt."),
  VSDSERVICE_INVALID_ENDPOINT(
      "79041",
      403,
      "VSDSERVICE_INVALID_ENDPOINT",
      "Der angefragte Endpunkt <endpoint> wird nicht unterstützt."),
  VSDSERVICE_INTERNAL_SERVER_ERROR(
      "79100",
      500,
      "VSDSERVICE_INTERNAL_SERVER_ERROR",
      "Unerwarteter interner Fehler des Fachdienstes VSDM."),
  VSDSERVICE_VSDD_NOTREACHABLE(
      "79110",
      502,
      "VSDSERVICE_VSDD_NOTREACHABLE",
      "Fachdienst VSDM ist für den Kostenträger <ik> nicht erreichbar."),
  VSDSERVICE_VSDD_TIMEOUT(
      "79111",
      504,
      "VSDSERVICE_VSDD_TIMEOUT",
      "Fachdienst VSDM für den Kostenträger  <ik> hat das Zeitlimit für eine Antwort überschritten.");

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

  public String getBdeCode() {
    return bdeCode;
  }

  public String getBdeReference() {
    return bdeReference;
  }

  public String getBdeText() {
    return bdeText;
  }

  public Integer getHttpCode() {
    return httpCode;
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
