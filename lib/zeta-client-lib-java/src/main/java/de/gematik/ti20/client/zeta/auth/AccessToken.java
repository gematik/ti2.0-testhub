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
package de.gematik.ti20.client.zeta.auth;

import java.util.Arrays;
import java.util.UUID;
import org.jose4j.jwk.EcJwkGenerator;
import org.jose4j.jwk.EllipticCurveJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.lang.JoseException;

public class AccessToken {

  private static final EllipticCurveJsonWebKey KEY;

  static {
    try {
      KEY = EcJwkGenerator.generateJwk(EllipticCurves.P256);
    } catch (JoseException e) {
      throw new RuntimeException(e);
    }
  }

  public static String create() {
    // TODO: this must be implemented with the ZETA Authorization Server

    JwtClaims claims = new JwtClaims();
    claims.setJwtId(UUID.randomUUID().toString());
    claims.setIssuer("http://test.authz.server.local");
    claims.setExpirationTimeMinutesInTheFuture(60);
    claims.setIssuedAtToNow();
    claims.setAudience(
        Arrays.asList("http://zeta.guard.popp.local", "https://zeta.guard.vsdm.local"));
    claims.setStringClaim("scope", "read write todo");
    //    claims.setClaim("name", "TEST");
    //    claims.setClaim("idNummer", "KVNR/12345678");
    //    claims.setClaim("professionOID", "1.2.276.0.76.4.49");
    //    claims.setClaim("displayName", "Vorname Name");

    JsonWebSignature jws = new JsonWebSignature();
    jws.setPayload(claims.toJson());
    jws.setKey(KEY.getPrivateKey());
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

    try {
      return jws.getCompactSerialization();
    } catch (JoseException e) {
      throw new RuntimeException(e);
    }
  }
}
