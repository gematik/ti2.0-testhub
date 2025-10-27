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

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PoppTokenTest {

  private static KeyStore keyStore;
  private static final String KEYSTORE_PATH = "src/test/resources/keystore.p12";
  private static final String KEYSTORE_PASSWORD = "testpassword";
  private static final String ALIAS = "poppmock";
  private static final char[] KEY_PASSWORD = "testpassword".toCharArray();

  @BeforeAll
  static void setUp() throws Exception {
    // Load the KeyStore for testing
    keyStore = KeyStore.getInstance("PKCS12");
    try (FileInputStream fis = new FileInputStream(KEYSTORE_PATH)) {
      keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
    }
  }

  @Test
  void testToJwt() throws Exception {
    // Create TokenHeader
    PoppToken.TokenHeader header = new PoppToken.TokenHeader("key-id");

    // Create TokenClaims
    PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims(
            "ehc-practitioner-user-x509",
            1753877030L,
            1753877020L,
            "X123456789",
            "123456789",
            "1-2012345678",
            "1.2.276.0.76.4.50");

    // Create PoppToken
    PoppToken token = new PoppToken(header, claims);

    // Generate JWT
    String jwt = token.toJwt(keyStore, ALIAS, KEY_PASSWORD);

    // Assert that the JWT is not null or empty
    assertNotNull(jwt, "JWT should not be null");
    assertFalse(jwt.isEmpty(), "JWT should not be empty");
  }

  @Test
  void testFromJwt() throws Exception {
    // Create TokenHeader
    PoppToken.TokenHeader header = new PoppToken.TokenHeader("key-id");

    // Create TokenClaims
    PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims(
            "ehc-practitioner-user-x509",
            1753877030L,
            1753877020L,
            "X123456789",
            "123456789",
            "1-2012345678",
            "1.2.276.0.76.4.50");

    // Create PoppToken
    PoppToken token = new PoppToken(header, claims);

    // Generate JWT
    String jwt = token.toJwt(keyStore, ALIAS, KEY_PASSWORD);

    // Parse the JWT back to PoppToken
    PoppToken parsedToken = PoppToken.fromJwt(jwt, keyStore);

    // Assert that the parsed token matches the original claims
    assertNotNull(parsedToken, "Parsed token should not be null");
    assertEquals(
        claims.getVersion(), parsedToken.getClaims().getVersion(), "Version claim should match");
    assertEquals(claims.getIss(), parsedToken.getClaims().getIss(), "Issuer claim should match");
    assertEquals(
        claims.getProofMethod(),
        parsedToken.getClaims().getProofMethod(),
        "Proof method claim should match");
    assertEquals(
        claims.getPatientId(),
        parsedToken.getClaims().getPatientId(),
        "Patient ID claim should match");
    assertEquals(
        claims.getInsurerId(),
        parsedToken.getClaims().getInsurerId(),
        "Insurer ID claim should match");
  }

  @Test
  void testInvalidJwt() {
    // Invalid JWT string
    String invalidJwt = "invalid.jwt.token";

    // Attempt to parse the invalid JWT
    Exception exception =
        assertThrows(Exception.class, () -> PoppToken.fromJwt(invalidJwt, keyStore));

    // Assert that an exception is thrown
    assertNotNull(exception, "Exception should be thrown for invalid JWT");
  }

  @Test
  void testMissingClaims() throws Exception {
    // Create a JWT with missing claims
    JwtClaims jwtClaims = new JwtClaims();
    jwtClaims.setIssuer("https://popp.example.com");

    JsonWebSignature jws = new JsonWebSignature();
    jws.setPayload(jwtClaims.toJson());
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);

    PrivateKey privateKey = (PrivateKey) keyStore.getKey(ALIAS, KEY_PASSWORD);
    jws.setKey(privateKey);

    String jwt = jws.getCompactSerialization();

    // Attempt to parse the JWT
    Exception exception =
        assertThrows(CertificateException.class, () -> PoppToken.fromJwt(jwt, keyStore));
  }
}
