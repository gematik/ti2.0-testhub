/*
 *
 * Copyright 2025-2026 gematik GmbH
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
/*-
 * #%L
 * ZETA Testsuite
 * %%
 * (C) achelos GmbH, 2025, licensed for gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Test-only factory to create a valid Authorization Bearer token for the ZETA-PEP mockservice.
 *
 * <p>The mockservice validates JWT signatures against its keystore (zetakeystore.p12). For a
 * realistic PEP/PoPP flow test we generate a token signed with the same key.
 */
public class ZetaPepJwtTestFactory {

  public static final String DEFAULT_KEYSTORE_CLASSPATH = "zetakeystore.p12";
  public static final String DEFAULT_KEYSTORE_PASSWORD = "testpassword";
  public static final String DEFAULT_KEY_ALIAS = "zetamock";
  public static final String DEFAULT_KEY_PASSWORD = "testpassword";

  private ZetaPepJwtTestFactory() {
    // util
  }

  public static String createBearerToken() {
    return "Bearer " + createJwt();
  }

  /**
   * Creates a signed JWT which is accepted by the zeta-pep-server-mockservice.
   *
   * <p>Important claims for ZETA-User-Info transformation:
   *
   * <ul>
   *   <li>sub -> UserInfo.subject
   *   <li>client_id -> UserInfo.identifier
   *   <li>profession_oid -> UserInfo.professionOID
   * </ul>
   */
  public static String createJwt() {
    try {
      var keyStore = loadKeyStoreFromResources();

      Key key = keyStore.getKey(DEFAULT_KEY_ALIAS, DEFAULT_KEY_PASSWORD.toCharArray());
      if (!(key instanceof PrivateKey privateKey)) {
        throw new IllegalStateException(
            "Keystore entry '" + DEFAULT_KEY_ALIAS + "' is not a PrivateKey");
      }
      if (!(privateKey instanceof ECPrivateKey ecPrivateKey)) {
        throw new IllegalStateException(
            "Keystore entry '" + DEFAULT_KEY_ALIAS + "' is not an EC private key");
      }

      var now = Instant.now();
      var claims =
          new JWTClaimsSet.Builder()
              .issuer("http://localhost:9110")
              // Audience validation is disabled in the mock, but we still set something plausible.
              .audience(List.of("popp"))
              .subject("subject-1")
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plusSeconds(300)))
              .jwtID("jwt-" + now.toEpochMilli())
              .claim("client_id", "client-1")
              .claim("scope", "popp")
              .claim("profession_oid", "1.2.276.0.76.4.49")
              // Mock reads cnf.jkt, but does not validate it further.
              .claim("cnf", java.util.Map.of("jkt", "dummy-jkt"))
              .build();

      var header =
          new JWSHeader.Builder(JWSAlgorithm.ES256)
              .type(new JOSEObjectType("vnd.telematik.access+jwt"))
              .keyID(DEFAULT_KEY_ALIAS)
              .build();

      var jwt = new SignedJWT(header, claims);
      jwt.sign(new ECDSASigner(ecPrivateKey));
      return jwt.serialize();

    } catch (Exception e) {
      throw new AssertionError("Failed to create JWT for ZETA-PEP flow test: " + e.getMessage(), e);
    }
  }

  private static KeyStore loadKeyStoreFromResources() throws Exception {
    var ks = KeyStore.getInstance("PKCS12");
    try (InputStream is =
        ZetaPepJwtTestFactory.class
            .getClassLoader()
            .getResourceAsStream(DEFAULT_KEYSTORE_CLASSPATH)) {
      if (is == null) {
        throw new IllegalStateException(
            "Keystore '" + DEFAULT_KEYSTORE_CLASSPATH + "' not found on classpath");
      }
      ks.load(is, DEFAULT_KEYSTORE_PASSWORD.toCharArray());
    }
    return ks;
  }
}
