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
package de.gematik.ti20.simsvc.client.repository;

import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

@Slf4j
public class PoppTokenInspector {

  private final JwtConsumer jwtConsumer =
      new JwtConsumerBuilder().setSkipSignatureVerification().setSkipAllValidators().build();

  public Long getPatientProofTime(final String poppToken) {
    try {
      return jwtConsumer.processToClaims(poppToken).getClaimValue("patientProofTime", Long.class);
    } catch (final Exception e) {
      log.warn("Cannot extract patient proof time of PoPP token: {}", e.getMessage());
    }

    return null;
  }

  public String getIkNr(final String poppToken) {
    try {
      return jwtConsumer.processToClaims(poppToken).getStringClaimValue("insurerId");
    } catch (final Exception e) {
      log.warn("Cannot extract insurer ID of PoPP token: {}", e.getMessage());
    }

    return null;
  }
}
