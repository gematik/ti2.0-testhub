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
package de.gematik.ti20.popp;

import lombok.Getter;

@Getter
public enum SmcbType {
  SMCB_PRIVATE("smcb_private.p12", "1-20000300144", "1.2.276.0.76.4.50"),
  SMCB_80276001011699902303_APOTHEKE_VALID(
      "80276001011699902303-C_SMCB_AUT_E256_X509.p12",
      "3-01.2.2023001.16.103",
      "1.2.276.0.76.4.54"),
  SMCB_80276883110000168757_ARZT_VALID(
      "80276001011699902303-C_SMCB_AUT_E256_X509.p12",
      "1-SMC-B-Testkarte--883110000168757",
      "1.2.276.0.76.4.50"),
  SMCB_80276883110000168758_ARZT_VALID(
      "80276001011699902303-C_SMCB_AUT_E256_X509.p12",
      "1-SMC-B-Testkarte--883110000168758",
      "1.2.276.0.76.4.50"),
  SMCB_80276XXX_ARZTPRAXID_NOT_ALLOWED("todo", "todo", "1.2.276.0.76.4.50"),
  SMCB_80276001011699902303_ARZTPRAXIS_VALID(
      "80276688311000300108-Zeta-C_SMCB_AUT_E256_X509.p12", "1-20000300108", "1.2.276.0.76.4.50");

  private final String fileName;
  private final String telematikId;
  private final String professionOid;

  SmcbType(final String fileName, final String telematikId, final String professionOid) {
    this.fileName = fileName;
    this.telematikId = telematikId;
    this.professionOid = professionOid;
  }
}
