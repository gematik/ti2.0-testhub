/*-
 * #%L
 * Card Terminal Simulator
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
package de.gematik.ti20.simsvc.client.model;

import java.util.Map;
import java.util.UUID;

public class VirtualCardImageData implements CardImageData {

  private final String cardId = "card-" + UUID.randomUUID();

  private final Map<String, String> certificates;

  public VirtualCardImageData(final Map<String, String> certificates) {
    this.certificates = certificates;
  }

  public String getCertificate(final String certificateName) {
    return certificates.get(certificateName);
  }

  @Override
  public String cardType() {
    return certificates.get("EF.PD") != null ? "EGK" : "SMCB";
  }

  @Override
  public String cardId() {
    return cardId;
  }
}
