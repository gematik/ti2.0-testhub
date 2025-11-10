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
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenTest {

  @Mock private KeyStore keyStore;

  @Mock private PrivateKey privateKey;

  @Mock private PublicKey publicKey;

  @Mock private X509Certificate certificate;

  private AccessToken.TokenHeader tokenHeader;
  private AccessToken.TokenClaims tokenClaims;
  private AccessToken accessToken;

  @BeforeEach
  void setUp() {
    tokenHeader = new AccessToken.TokenHeader("test-kid");

    List<String> audience = Arrays.asList("audience1", "audience2");
    tokenClaims =
        new AccessToken.TokenClaims(
            "test-issuer",
            audience,
            "test-subject",
            "test-client-id",
            "test-scope",
            "test-jkt",
            "test-profession-oid");

    accessToken = new AccessToken(tokenHeader, tokenClaims);
  }

  @Test
  void testTokenHeaderConstructorWithAllParameters() {
    AccessToken.TokenHeader header =
        new AccessToken.TokenHeader("custom-typ", "RS256", "custom-kid");

    assertEquals("custom-typ", header.getTyp());
    assertEquals("RS256", header.getAlg());
    assertEquals("custom-kid", header.getKid());
  }

  @Test
  void testTokenHeaderConstructorWithKidOnly() {
    AccessToken.TokenHeader header = new AccessToken.TokenHeader("test-kid");

    assertEquals("vnd.telematik.access+jwt", header.getTyp());
    assertEquals("ES256", header.getAlg());
    assertEquals("test-kid", header.getKid());
  }

  @Test
  void testTokenClaimsConstructor() {
    List<String> audience = Arrays.asList("aud1", "aud2");
    AccessToken.TokenClaims claims =
        new AccessToken.TokenClaims(
            "issuer", audience, "subject", "client-id", "scope", "jkt-value", "profession-oid");

    assertEquals("issuer", claims.getIss());
    assertEquals(audience, claims.getAud());
    assertEquals("subject", claims.getSub());
    assertEquals("client-id", claims.getClientId());
    assertEquals("scope", claims.getScope());
    assertEquals("jkt-value", claims.getJkt());
    assertEquals("profession-oid", claims.getProfessionOid());
  }

  @Test
  void testAccessTokenConstructor() {
    assertNotNull(accessToken);
    assertEquals(tokenHeader, accessToken.getHeader());
    assertEquals(tokenClaims, accessToken.getClaims());
  }

  @Test
  void testGetHeader() {
    assertEquals(tokenHeader, accessToken.getHeader());
  }

  @Test
  void testGetClaims() {
    assertEquals(tokenClaims, accessToken.getClaims());
  }

  @Test
  void testToJwtWithValidKeyStore() throws Exception {
    // Arrange
    String alias = "test-alias";
    char[] keyPassword = "password".toCharArray();

    when(keyStore.getKey(alias, keyPassword)).thenReturn(privateKey);
    when(keyStore.getCertificate(alias)).thenReturn(certificate);

    // Create real KeyPair for testing
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(256);
    KeyPair keyPair = keyGen.generateKeyPair();

    when(keyStore.getKey(alias, keyPassword)).thenReturn(keyPair.getPrivate());

    // Act
    String jwt = accessToken.toJwt(keyStore, alias, keyPassword);

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
    assertThrows(Exception.class, () -> accessToken.toJwt(keyStore, alias, keyPassword));
  }

  @Test
  void testFromJwtWithValidJwtAndKeyStore() throws Exception {
    // This test would require a real JWT and KeyStore setup
    // For now, we test the exception case
    String invalidJwt = "invalid.jwt.token";

    assertThrows(Exception.class, () -> AccessToken.fromJwt(invalidJwt, keyStore));
  }

  @Test
  void testFromJwtWithNullJwt() {
    assertThrows(Exception.class, () -> AccessToken.fromJwt(null, keyStore));
  }

  @Test
  void testFromJwtWithEmptyJwt() {
    assertThrows(Exception.class, () -> AccessToken.fromJwt("", keyStore));
  }

  @Test
  void testTokenHeaderGettersReturnCorrectValues() {
    assertEquals("vnd.telematik.access+jwt", tokenHeader.getTyp());
    assertEquals("ES256", tokenHeader.getAlg());
    assertEquals("test-kid", tokenHeader.getKid());
  }

  @Test
  void testTokenClaimsGettersReturnCorrectValues() {
    assertEquals("test-issuer", tokenClaims.getIss());
    assertEquals(Arrays.asList("audience1", "audience2"), tokenClaims.getAud());
    assertEquals("test-subject", tokenClaims.getSub());
    assertEquals("test-client-id", tokenClaims.getClientId());
    assertEquals("test-scope", tokenClaims.getScope());
    assertEquals("test-jkt", tokenClaims.getJkt());
    assertEquals("test-profession-oid", tokenClaims.getProfessionOid());
  }

  @Test
  void testTokenClaimsWithNullValues() {
    AccessToken.TokenClaims claims =
        new AccessToken.TokenClaims(null, null, null, null, null, null, null);

    assertNull(claims.getIss());
    assertNull(claims.getAud());
    assertNull(claims.getSub());
    assertNull(claims.getClientId());
    assertNull(claims.getScope());
    assertNull(claims.getJkt());
    assertNull(claims.getProfessionOid());
  }

  @Test
  void testTokenClaimsWithEmptyAudience() {
    List<String> emptyAudience = Arrays.asList();
    AccessToken.TokenClaims claims =
        new AccessToken.TokenClaims(
            "issuer", emptyAudience, "subject", "client-id", "scope", "jkt", "profession-oid");

    assertEquals(emptyAudience, claims.getAud());
    assertTrue(claims.getAud().isEmpty());
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.model", AccessToken.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(AccessToken.class.getModifiers()));
  }

  @Test
  void testInnerClassesArePublicAndStatic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(AccessToken.TokenHeader.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(AccessToken.TokenHeader.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(AccessToken.TokenClaims.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(AccessToken.TokenClaims.class.getModifiers()));
  }

  @Test
  void testTokenHeaderFieldsArePrivate() throws NoSuchFieldException {
    var typField = AccessToken.TokenHeader.class.getDeclaredField("typ");
    var algField = AccessToken.TokenHeader.class.getDeclaredField("alg");
    var kidField = AccessToken.TokenHeader.class.getDeclaredField("kid");

    assertTrue(java.lang.reflect.Modifier.isPrivate(typField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(algField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(kidField.getModifiers()));
  }

  @Test
  void testTokenClaimsFieldsArePrivate() throws NoSuchFieldException {
    var issField = AccessToken.TokenClaims.class.getDeclaredField("iss");
    var audField = AccessToken.TokenClaims.class.getDeclaredField("aud");
    var subField = AccessToken.TokenClaims.class.getDeclaredField("sub");
    var clientIdField = AccessToken.TokenClaims.class.getDeclaredField("clientId");
    var scopeField = AccessToken.TokenClaims.class.getDeclaredField("scope");
    var jktField = AccessToken.TokenClaims.class.getDeclaredField("jkt");
    var professionOidField = AccessToken.TokenClaims.class.getDeclaredField("professionOid");

    assertTrue(java.lang.reflect.Modifier.isPrivate(issField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(audField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(subField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(clientIdField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(scopeField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(jktField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(professionOidField.getModifiers()));
  }

  @Test
  void testAccessTokenFieldsArePrivate() throws NoSuchFieldException {
    var headerField = AccessToken.class.getDeclaredField("header");
    var claimsField = AccessToken.class.getDeclaredField("claims");

    assertTrue(java.lang.reflect.Modifier.isPrivate(headerField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(claimsField.getModifiers()));
  }

  @Test
  void testFromJwtIsStaticMethod() throws NoSuchMethodException {
    var method = AccessToken.class.getDeclaredMethod("fromJwt", String.class, KeyStore.class);
    assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
  }

  @Test
  void testExtractTrustedCertificatesIsPrivateStatic() throws NoSuchMethodException {
    var method = AccessToken.class.getDeclaredMethod("extractTrustedCertificates", KeyStore.class);
    assertTrue(java.lang.reflect.Modifier.isPrivate(method.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
  }

  @Nested
  class FromJwt {
    private static final String TOKEN =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30";

    @Test
    void thatExceptionIsRaisedForMissingCertificates() throws KeyStoreException {
      when(keyStore.aliases()).thenReturn(Collections.emptyEnumeration());
      assertThrows(InvalidJwtException.class, () -> AccessToken.fromJwt(TOKEN, keyStore));
    }
  }
}
