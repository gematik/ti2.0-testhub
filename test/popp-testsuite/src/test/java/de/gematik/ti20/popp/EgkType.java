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
public enum EgkType {
  EGK_80276883110000179865_EXPIRED(
      "EGK_80276883110000179865_gema5_abgelaufen2025.xml", "X110691122", "109500969"),
  EGK_80276883110000152715_VALID("IMG_eGK_G21_TU_root6 1.xml", "X110540756", "109500969"),
  EGK_80276883110000163142_VALID("EGK_80276883110000163142_gema5.xml", "X110644390", "109500969"),
  EGK_80276883110000165691_VALID("realCard", "X110629641", "109500969"),
  EGK_80276883110000180787_REVOKED(
      "EGK_80276883110000180787_revoked_gema5.xml", "X110620907", "109500969"),
  EGK_80276883110000165689_REDUZIERT(
      "EGK_80276883110000163142_gema5.xml", "X110644390", "109500969"),
  EGK_80276001042001660952_TK("EGK_TK_1b.xml", "T028447785", "101575519");

  private final String fileName;
  private final String kvnr;
  private final String ikNumber;

  EgkType(final String fileName, final String kvnr, final String ikNumber) {
    this.fileName = fileName;
    this.kvnr = kvnr;
    this.ikNumber = ikNumber;
  }
}
