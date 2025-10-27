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

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenTest {

  @Mock private KeyStore keyStore;

  @Mock private X509Certificate certificate;

  @Mock private PrivateKey privateKey;

  @Mock private PublicKey publicKey;

  private AccessToken.TokenHeader tokenHeader;
  private AccessToken.TokenClaims tokenClaims;
  private AccessToken accessToken;

  @BeforeEach
  void setUp() {
    tokenHeader = new AccessToken.TokenHeader("testKid");
    List<String> audience = Arrays.asList("aud1", "aud2");
    tokenClaims =
        new AccessToken.TokenClaims(
            "testIssuer",
            audience,
            "testSubject",
            "testClientId",
            "testScope",
            "testJkt",
            "testProfessionOid");
    accessToken = new AccessToken(tokenHeader, tokenClaims);
  }

  @Test
  void testTokenHeaderConstructorWithKidOnly() {
    String kid = "testKid";
    AccessToken.TokenHeader header = new AccessToken.TokenHeader(kid);

    assertEquals("vnd.telematik.access+jwt", header.getTyp());
    assertEquals("ES256", header.getAlg());
    assertEquals(kid, header.getKid());
  }

  @Test
  void testTokenHeaderConstructorWithAllParameters() {
    String typ = "customType";
    String alg = "customAlg";
    String kid = "customKid";

    AccessToken.TokenHeader header = new AccessToken.TokenHeader(typ, alg, kid);

    assertEquals(typ, header.getTyp());
    assertEquals(alg, header.getAlg());
    assertEquals(kid, header.getKid());
  }

  @Test
  void testTokenClaimsConstructor() {
    String iss = "testIssuer";
    List<String> aud = Arrays.asList("aud1", "aud2");
    String sub = "testSubject";
    String clientId = "testClientId";
    String scope = "testScope";
    String jkt = "testJkt";
    String professionOid = "testProfessionOid";

    AccessToken.TokenClaims claims =
        new AccessToken.TokenClaims(iss, aud, sub, clientId, scope, jkt, professionOid);

    assertEquals(iss, claims.getIss());
    assertEquals(aud, claims.getAud());
    assertEquals(sub, claims.getSub());
    assertEquals(clientId, claims.getClientId());
    assertEquals(scope, claims.getScope());
    assertEquals(jkt, claims.getJkt());
    assertEquals(professionOid, claims.getProfessionOid());
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
  void testToJwtThrowsExceptionWhenKeyStoreIsNull() {
    assertThrows(
        Exception.class,
        () -> {
          accessToken.toJwt(null, "alias", "password".toCharArray());
        });
  }

  @Test
  void testToJwtThrowsExceptionWhenAliasIsNull() {
    assertThrows(
        Exception.class,
        () -> {
          accessToken.toJwt(keyStore, null, "password".toCharArray());
        });
  }

  @Test
  void testToJwtThrowsExceptionWhenKeyPasswordIsNull() {
    assertThrows(
        Exception.class,
        () -> {
          accessToken.toJwt(keyStore, "alias", null);
        });
  }

  @Test
  void testFromJwtThrowsExceptionWhenJwtIsNull() {
    assertThrows(
        Exception.class,
        () -> {
          AccessToken.fromJwt(null, keyStore);
        });
  }

  @Test
  void testFromJwtThrowsExceptionWhenKeyStoreIsNull() {
    assertThrows(
        Exception.class,
        () -> {
          AccessToken.fromJwt("invalidJwt", null);
        });
  }

  @Test
  void testFromJwtThrowsExceptionWhenJwtIsInvalid() {
    assertThrows(
        Exception.class,
        () -> {
          AccessToken.fromJwt("invalidJwt", keyStore);
        });
  }

  @Test
  void testTokenHeaderInnerClassStructure() {
    assertTrue(AccessToken.TokenHeader.class.getEnclosingClass().equals(AccessToken.class));
    assertTrue(java.lang.reflect.Modifier.isStatic(AccessToken.TokenHeader.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(AccessToken.TokenHeader.class.getModifiers()));
  }

  @Test
  void testTokenClaimsInnerClassStructure() {
    assertTrue(AccessToken.TokenClaims.class.getEnclosingClass().equals(AccessToken.class));
    assertTrue(java.lang.reflect.Modifier.isStatic(AccessToken.TokenClaims.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(AccessToken.TokenClaims.class.getModifiers()));
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
  void testTokenHeaderGettersAreConsistent() {
    AccessToken.TokenHeader header = new AccessToken.TokenHeader("testKid");

    assertEquals("vnd.telematik.access+jwt", header.getTyp());
    assertEquals("ES256", header.getAlg());
    assertEquals("testKid", header.getKid());
  }

  @Test
  void testTokenClaimsGettersAreConsistent() {
    List<String> audience = Arrays.asList("aud1", "aud2");
    AccessToken.TokenClaims claims =
        new AccessToken.TokenClaims(
            "iss", audience, "sub", "clientId", "scope", "jkt", "professionOid");

    assertEquals("iss", claims.getIss());
    assertEquals(audience, claims.getAud());
    assertEquals("sub", claims.getSub());
    assertEquals("clientId", claims.getClientId());
    assertEquals("scope", claims.getScope());
    assertEquals("jkt", claims.getJkt());
    assertEquals("professionOid", claims.getProfessionOid());
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
  void testTokenHeaderWithNullKid() {
    AccessToken.TokenHeader header = new AccessToken.TokenHeader(null);

    assertEquals("vnd.telematik.access+jwt", header.getTyp());
    assertEquals("ES256", header.getAlg());
    assertNull(header.getKid());
  }

  @Test
  void testAccessTokenWithNullHeaderAndClaims() {
    AccessToken token = new AccessToken(null, null);

    assertNull(token.getHeader());
    assertNull(token.getClaims());
  }
}
