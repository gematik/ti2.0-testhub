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
package de.gematik.ti20.simsvc.server.model;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

public class PoppToken {

  private final JwtClaims jwtClaims;

  public PoppToken(JwtClaims jwtClaims) {
    this.jwtClaims = jwtClaims;
  }

  public void validate() throws InvalidJwtException {
    // TODO
  }

  public String getClaimValue(final String claimName) throws MalformedClaimException {
    return jwtClaims.getStringClaimValue(claimName);
  }

  public static PoppToken parse(final String token) throws InvalidJwtException {
    // TODO: Schlüsselverifikation einbauen
    final JwtConsumer jwtConsumer =
        new JwtConsumerBuilder()
            .setRequireExpirationTime()
            .setAllowedClockSkewInSeconds(30)
            .setRequireSubject()
            .setSkipSignatureVerification()
            .setSkipAllValidators()
            // .setVerificationKey(new HmacKey(secret.getBytes()))
            .build();

    var pt = new PoppToken(jwtConsumer.processToClaims(token));
    pt.validate();

    return pt;
  }
}
