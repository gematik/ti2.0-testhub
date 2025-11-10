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
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PoppTokenTest {

  @Mock private KeyStore keyStore;

  @Mock private PrivateKey privateKey;

  @Mock private PublicKey publicKey;

  @Mock private X509Certificate certificate;

  private PoppToken.TokenHeader tokenHeader;
  private PoppToken.TokenClaims tokenClaims;
  private PoppToken poppToken;

  @BeforeEach
  void setUp() {
    List<String> x5c = Arrays.asList("cert1", "cert2");
    tokenHeader = new PoppToken.TokenHeader("vnd.telematik.popp+jwt", "ES256", "test-kid", x5c);

    tokenClaims =
        new PoppToken.TokenClaims(
            "1.0.0",
            "https://popp.example.com",
            System.currentTimeMillis() / 1000,
            "ehc-provider-user-x509",
            System.currentTimeMillis() / 1000,
            "patient-123",
            "insurer-456",
            "actor-789",
            "profession-oid-123");

    poppToken = new PoppToken(tokenHeader, tokenClaims);
  }

  @Test
  void testTokenHeaderConstructorWithAllParameters() {
    List<String> x5c = Arrays.asList("cert1", "cert2");
    PoppToken.TokenHeader header =
        new PoppToken.TokenHeader("custom-typ", "RS256", "custom-kid", x5c);

    assertEquals("custom-typ", header.getTyp());
    assertEquals("RS256", header.getAlg());
    assertEquals("custom-kid", header.getKid());
    assertEquals(x5c, header.getX5c());
  }

  @Test
  void testTokenHeaderConstructorWithKidOnly() {
    PoppToken.TokenHeader header = new PoppToken.TokenHeader("test-kid");

    assertEquals("vnd.telematik.popp+jwt", header.getTyp());
    assertEquals("ES256", header.getAlg());
    assertEquals("test-kid", header.getKid());
    assertNull(header.getX5c());
  }

  @Test
  void testTokenClaimsConstructorWithAllParameters() {
    PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims(
            "2.0.0",
            "https://custom.example.com",
            1234567890L,
            "custom-method",
            1234567890L,
            "patient-999",
            "insurer-888",
            "actor-777",
            "profession-oid-666");

    assertEquals("2.0.0", claims.getVersion());
    assertEquals("https://custom.example.com", claims.getIss());
    assertEquals(1234567890L, claims.getIat());
    assertEquals("custom-method", claims.getProofMethod());
    assertEquals(1234567890L, claims.getPatientProofTime());
    assertEquals("patient-999", claims.getPatientId());
    assertEquals("insurer-888", claims.getInsurerId());
    assertEquals("actor-777", claims.getActorId());
    assertEquals("profession-oid-666", claims.getActorProfessionOid());
  }

  @Test
  void testTokenClaimsConstructorWithMinimalParameters() {
    PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims("patient-123", "insurer-456", "actor-789", "profession-oid-123");

    assertEquals("1.0.0", claims.getVersion());
    assertEquals("https://popp.example.com", claims.getIss());
    assertEquals("ehc-provider-user-x509", claims.getProofMethod());
    assertEquals("patient-123", claims.getPatientId());
    assertEquals("insurer-456", claims.getInsurerId());
    assertEquals("actor-789", claims.getActorId());
    assertEquals("profession-oid-123", claims.getActorProfessionOid());
  }

  @Test
  void testPoppTokenConstructor() {
    assertNotNull(poppToken);
    assertEquals(tokenHeader, poppToken.getHeader());
    assertEquals(tokenClaims, poppToken.getClaims());
  }

  @Test
  void testGetHeader() {
    assertEquals(tokenHeader, poppToken.getHeader());
  }

  @Test
  void testGetClaims() {
    assertEquals(tokenClaims, poppToken.getClaims());
  }

  @Test
  void testToJwtWithValidKeyStore() throws Exception {
    // Arrange
    String alias = "test-alias";
    char[] keyPassword = "password".toCharArray();
    byte[] certBytes = "dummy-cert".getBytes();

    when(keyStore.getKey(alias, keyPassword)).thenReturn(privateKey);
    when(keyStore.getCertificate(alias)).thenReturn(certificate);
    when(certificate.getEncoded()).thenReturn(certBytes);

    // Create real KeyPair for testing
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(256);
    KeyPair keyPair = keyGen.generateKeyPair();

    when(keyStore.getKey(alias, keyPassword)).thenReturn(keyPair.getPrivate());

    // Act
    String jwt = poppToken.toJwt(keyStore, alias, keyPassword);

    // Assert
    assertNotNull(jwt);
    assertFalse(jwt.isEmpty());
    assertTrue(jwt.contains("."));
    assertEquals(2, jwt.split("\\.").length - 1); // JWT should have 2 dots

    verify(keyStore).getKey(alias, keyPassword);
    verify(keyStore).getCertificate(alias);
  }

  @Test
  void testToJwtThrowsExceptionWhenKeyNotFound() throws Exception {
    // Arrange
    String alias = "non-existent-alias";
    char[] keyPassword = "password".toCharArray();

    when(keyStore.getKey(alias, keyPassword)).thenReturn(null);

    // Act & Assert
    assertThrows(Exception.class, () -> poppToken.toJwt(keyStore, alias, keyPassword));
  }

  @Test
  void testFromJwtWithNullJwt() {
    assertThrows(Exception.class, () -> PoppToken.fromJwt(null, keyStore));
  }

  @Test
  void testFromJwtWithEmptyJwt() {
    assertThrows(Exception.class, () -> PoppToken.fromJwt("", keyStore));
  }

  @Test
  void testFromJwtWithInvalidJwt() {
    String invalidJwt = "invalid.jwt.token";
    assertThrows(Exception.class, () -> PoppToken.fromJwt(invalidJwt, keyStore));
  }

  @Nested
  class FromJwt {

    private static final String TOKEN_WITH_KNOWN_CERTIFICATE =
        "eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsucG9wcCtqd3QiLCJraWQiOiJhbGlhcyIsIng1YyI6WyJNSUlEQmpDQ0FxeWdBd0lCQWdJSEFtQ25LeHIvUnpBS0JnZ3Foa2pPUFFRREFqQ0JoREVMTUFrR0ExVUVCaE1DUkVVeEh6QWRCZ05WQkFvTUZtZGxiV0YwYVdzZ1IyMWlTQ0JPVDFRdFZrRk1TVVF4TWpBd0JnTlZCQXNNS1V0dmJYQnZibVZ1ZEdWdUxVTkJJR1JsY2lCVVpXeGxiV0YwYVd0cGJtWnlZWE4wY25WcmRIVnlNU0F3SGdZRFZRUUREQmRIUlUwdVMwOU5VQzFEUVRZeElGUkZVMVF0VDA1TVdUQWVGdzB5TlRBM01qZ3lNakF3TURCYUZ3MHpNREEzTWpneU1UVTVOVGxhTUdzeEN6QUpCZ05WQkFZVEFrUkZNU1l3SkFZRFZRUUtEQjFuWlcxaGRHbHJJRlJGVTFRdFQwNU1XU0F0SUU1UFZDMVdRVXhKUkRFME1ESUdBMVVFQXd3cmNHOXdjQzEwYjJ0bGJpNW5aVzFoZEdsckxuUmxiR1Z0WVhScGF5MTBaWE4wSUhOcGJYVnNZWFJ2Y2pCWk1CTUdCeXFHU000OUFnRUdDQ3FHU000OUF3RUhBMElBQkhBdkkzclBpTTRVNUhCekhUSXllc24vWmNaUkh0djlWeHVqbGJUYVJQdHYyc3FPNXZ5dmEyNEVRbjhBZ1M2bU9ObmVSaElJOHFoUkxxL1ZJMDZEeE9HamdnRWZNSUlCR3pBN0JnZ3JCZ0VGQlFjQkFRUXZNQzB3S3dZSUt3WUJCUVVITUFHR0gyaDBkSEE2THk5bGFHTmhMbWRsYldGMGFXc3VaR1V2WldOakxXOWpjM0F3SFFZRFZSME9CQllFRkRobnRHbnFueEVRTnpRdkRTTUpLSFllRUROYU1DRUdBMVVkSUFRYU1CZ3dDZ1lJS29JVUFFd0VnaDh3Q2dZSUtvSVVBRXdFZ1NNd0h3WURWUjBqQkJnd0ZvQVVuelhnTUtsL3l2aG1uNUFLUXMyN2dXV2ZTZjR3RGdZRFZSMFBBUUgvQkFRREFnWkFNRnNHQlNza0NBTURCRkl3VURCT01Fd3dTakJJTURvTU9GUnZhMlZ1TFZOcFoyNWhkSFZ5TFVsa1pXNTBhWFREcEhRZ1pzTzhjaUJRY205dlppQnZaaUJRWVhScFpXNTBJRkJ5WlhObGJtTmxNQW9HQ0NxQ0ZBQk1CSUpBTUF3R0ExVWRFd0VCL3dRQ01BQXdDZ1lJS29aSXpqMEVBd0lEU0FBd1JRSWhBS3VDdi9ZczBMd3lQRTh2eVBZWVF5UFpBTGltendwRjJHallVWUlwZ3QyNUFpQUhucVpubEV0dEhzL0Y0SEExckYyZWNmMjd0L2NMMlM4b0VvSFZaMmpra2c9PSJdfQ.eyJpYXQiOjE3NTM4NzcwNjAsInZlcnNpb24iOiIxLjAuMCIsImlzcyI6Imh0dHBzOi8vcG9wcC5leGFtcGxlLmNvbSIsInByb29mTWV0aG9kIjoiZWhjLXByYWN0aXRpb25lci11c2VyLXg1MDkiLCJwYXRpZW50UHJvb2ZUaW1lIjoxNzUzODc3MDUwLCJwYXRpZW50SWQiOiJGMTEwNjM5NDkxIiwiaW5zdXJlcklkIjoiMjA5NTAwOTY5IiwiYWN0b3JJZCI6Ijg4MzExMDAwMDE2ODY1MCIsImFjdG9yUHJvZmVzc2lvbk9pZCI6IjEuMi4yNzYuMC43Ni40LjMyIn0.To6Gtn7yV3zxJYJiQT0bTqU92uUFfk55BsdSzH3MflPLhpLq_8GyOY609O46qy9FvNWsoPOUjKaiCu12PF31hA";

    @Test
    void thatFromJwtParsesTokenWithKnownCertificate() throws Exception {
      when(keyStore.aliases()).thenReturn(Collections.emptyEnumeration());

      assertDoesNotThrow(() -> PoppToken.fromJwt(TOKEN_WITH_KNOWN_CERTIFICATE, keyStore));
    }

    @Test
    void thatExceptionIsRaisedForMissingCertificatesInToken() {
      assertThrows(
          CertificateException.class,
          () ->
              PoppToken.fromJwt(
                  "eyJhbGciOiJub25lIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTczNjI5MjEyNH0.",
                  keyStore));
    }
  }

  @Test
  void testTokenHeaderGettersReturnCorrectValues() {
    List<String> x5c = Arrays.asList("cert1", "cert2");
    PoppToken.TokenHeader header =
        new PoppToken.TokenHeader("custom-typ", "RS256", "custom-kid", x5c);

    assertEquals("custom-typ", header.getTyp());
    assertEquals("RS256", header.getAlg());
    assertEquals("custom-kid", header.getKid());
    assertEquals(x5c, header.getX5c());
  }

  @Test
  void testTokenClaimsGettersReturnCorrectValues() {
    assertEquals("1.0.0", tokenClaims.getVersion());
    assertEquals("https://popp.example.com", tokenClaims.getIss());
    assertEquals("ehc-provider-user-x509", tokenClaims.getProofMethod());
    assertEquals("patient-123", tokenClaims.getPatientId());
    assertEquals("insurer-456", tokenClaims.getInsurerId());
    assertEquals("actor-789", tokenClaims.getActorId());
    assertEquals("profession-oid-123", tokenClaims.getActorProfessionOid());
  }

  @Test
  void testTokenClaimsWithNullValues() {
    PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims(null, null, 0, null, 0, null, null, null, null);

    assertNull(claims.getVersion());
    assertNull(claims.getIss());
    assertEquals(0, claims.getIat());
    assertNull(claims.getProofMethod());
    assertEquals(0, claims.getPatientProofTime());
    assertNull(claims.getPatientId());
    assertNull(claims.getInsurerId());
    assertNull(claims.getActorId());
    assertNull(claims.getActorProfessionOid());
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.model", PoppToken.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppToken.class.getModifiers()));
  }

  @Test
  void testInnerClassesArePublicAndStatic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppToken.TokenHeader.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(PoppToken.TokenHeader.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppToken.TokenClaims.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(PoppToken.TokenClaims.class.getModifiers()));
  }

  @Test
  void testTokenHeaderFieldsArePrivate() throws NoSuchFieldException {
    var typField = PoppToken.TokenHeader.class.getDeclaredField("typ");
    var algField = PoppToken.TokenHeader.class.getDeclaredField("alg");
    var kidField = PoppToken.TokenHeader.class.getDeclaredField("kid");
    var x5cField = PoppToken.TokenHeader.class.getDeclaredField("x5c");

    assertTrue(java.lang.reflect.Modifier.isPrivate(typField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(algField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(kidField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(x5cField.getModifiers()));
  }

  @Test
  void testTokenClaimsFieldsArePrivate() throws NoSuchFieldException {
    var versionField = PoppToken.TokenClaims.class.getDeclaredField("version");
    var issField = PoppToken.TokenClaims.class.getDeclaredField("iss");
    var iatField = PoppToken.TokenClaims.class.getDeclaredField("iat");
    var proofMethodField = PoppToken.TokenClaims.class.getDeclaredField("proofMethod");
    var patientProofTimeField = PoppToken.TokenClaims.class.getDeclaredField("patientProofTime");
    var patientIdField = PoppToken.TokenClaims.class.getDeclaredField("patientId");
    var insurerIdField = PoppToken.TokenClaims.class.getDeclaredField("insurerId");
    var actorIdField = PoppToken.TokenClaims.class.getDeclaredField("actorId");
    var actorProfessionOidField =
        PoppToken.TokenClaims.class.getDeclaredField("actorProfessionOid");

    assertTrue(java.lang.reflect.Modifier.isPrivate(versionField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(issField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(iatField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(proofMethodField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(patientProofTimeField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(patientIdField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(insurerIdField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(actorIdField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(actorProfessionOidField.getModifiers()));
  }

  @Test
  void testPoppTokenFieldsArePrivate() throws NoSuchFieldException {
    var headerField = PoppToken.class.getDeclaredField("header");
    var claimsField = PoppToken.class.getDeclaredField("claims");

    assertTrue(java.lang.reflect.Modifier.isPrivate(headerField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(claimsField.getModifiers()));
  }

  @Test
  void testFromJwtIsStaticMethod() throws NoSuchMethodException {
    var method = PoppToken.class.getDeclaredMethod("fromJwt", String.class, KeyStore.class);
    assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
  }

  @Test
  void testValidateCertificateChainIsPrivateStatic() throws NoSuchMethodException {
    var method =
        PoppToken.class.getDeclaredMethod("validateCertificateChain", List.class, KeyStore.class);
    assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
  }

  @Test
  void testTokenHeaderDefaultValues() {
    PoppToken.TokenHeader header = new PoppToken.TokenHeader("test-kid");

    assertEquals("vnd.telematik.popp+jwt", header.getTyp());
    assertEquals("ES256", header.getAlg());
    assertEquals("test-kid", header.getKid());
    assertNull(header.getX5c());
  }

  @Test
  void testTokenClaimsDefaultValues() {
    PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims("patient-123", "insurer-456", "actor-789", "profession-oid-123");

    assertEquals("1.0.0", claims.getVersion());
    assertEquals("https://popp.example.com", claims.getIss());
    assertEquals("ehc-provider-user-x509", claims.getProofMethod());
    assertTrue(claims.getIat() > 0);
    assertTrue(claims.getPatientProofTime() > 0);
  }

  @Test
  void testTokenClaimsTimestampGeneration() {
    long beforeCreation = System.currentTimeMillis() / 1000;
    PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims("patient-123", "insurer-456", "actor-789", "profession-oid-123");
    long afterCreation = System.currentTimeMillis() / 1000;

    assertTrue(claims.getIat() >= beforeCreation);
    assertTrue(claims.getIat() <= afterCreation);
    assertTrue(claims.getPatientProofTime() >= beforeCreation);
    assertTrue(claims.getPatientProofTime() <= afterCreation);
  }

  @Test
  void testConstructorCount() {
    var constructors = PoppToken.class.getDeclaredConstructors();
    assertEquals(2, constructors.length);
  }

  @Test
  void testTokenHeaderConstructorCount() {
    var constructors = PoppToken.TokenHeader.class.getDeclaredConstructors();
    assertEquals(2, constructors.length);
  }

  @Test
  void testTokenClaimsConstructorCount() {
    var constructors = PoppToken.TokenClaims.class.getDeclaredConstructors();
    assertEquals(2, constructors.length);
  }
}
