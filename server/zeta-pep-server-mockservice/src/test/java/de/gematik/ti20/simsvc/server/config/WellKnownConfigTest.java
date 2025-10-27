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
package de.gematik.ti20.simsvc.server.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ExtendWith(MockitoExtension.class)
class WellKnownConfigTest {

  private WellKnownConfig wellKnownConfig;

  @BeforeEach
  void setUp() {
    wellKnownConfig = new WellKnownConfig();
  }

  @Test
  void testNoArgsConstructor() {
    assertNotNull(wellKnownConfig);
    assertNull(wellKnownConfig.getIssuer());
    assertNull(wellKnownConfig.getAuth_ep());
    assertNull(wellKnownConfig.getToken_ep());
    assertNull(wellKnownConfig.getNonce_ep());
  }

  @Test
  void testGetterAndSetterIssuer() {
    String testIssuer = "https://example.com/issuer";

    wellKnownConfig.setIssuer(testIssuer);

    assertEquals(testIssuer, wellKnownConfig.getIssuer());
  }

  @Test
  void testGetterAndSetterAuthEp() {
    String testAuthEp = "https://example.com/auth";

    wellKnownConfig.setAuth_ep(testAuthEp);

    assertEquals(testAuthEp, wellKnownConfig.getAuth_ep());
  }

  @Test
  void testGetterAndSetterTokenEp() {
    String testTokenEp = "https://example.com/token";

    wellKnownConfig.setToken_ep(testTokenEp);

    assertEquals(testTokenEp, wellKnownConfig.getToken_ep());
  }

  @Test
  void testGetterAndSetterNonceEp() {
    String testNonceEp = "https://example.com/nonce";

    wellKnownConfig.setNonce_ep(testNonceEp);

    assertEquals(testNonceEp, wellKnownConfig.getNonce_ep());
  }

  @Test
  void testSetIssuerToNull() {
    wellKnownConfig.setIssuer("https://example.com");
    wellKnownConfig.setIssuer(null);

    assertNull(wellKnownConfig.getIssuer());
  }

  @Test
  void testSetAuthEpToNull() {
    wellKnownConfig.setAuth_ep("https://example.com/auth");
    wellKnownConfig.setAuth_ep(null);

    assertNull(wellKnownConfig.getAuth_ep());
  }

  @Test
  void testSetTokenEpToNull() {
    wellKnownConfig.setToken_ep("https://example.com/token");
    wellKnownConfig.setToken_ep(null);

    assertNull(wellKnownConfig.getToken_ep());
  }

  @Test
  void testSetNonceEpToNull() {
    wellKnownConfig.setNonce_ep("https://example.com/nonce");
    wellKnownConfig.setNonce_ep(null);

    assertNull(wellKnownConfig.getNonce_ep());
  }

  @Test
  void testSetAllFieldsToEmptyString() {
    wellKnownConfig.setIssuer("");
    wellKnownConfig.setAuth_ep("");
    wellKnownConfig.setToken_ep("");
    wellKnownConfig.setNonce_ep("");

    assertEquals("", wellKnownConfig.getIssuer());
    assertEquals("", wellKnownConfig.getAuth_ep());
    assertEquals("", wellKnownConfig.getToken_ep());
    assertEquals("", wellKnownConfig.getNonce_ep());
  }

  @Test
  void testComponentAnnotation() {
    assertTrue(WellKnownConfig.class.isAnnotationPresent(Component.class));
  }

  @Test
  void testConfigurationPropertiesAnnotation() {
    ConfigurationProperties annotation =
        WellKnownConfig.class.getAnnotation(ConfigurationProperties.class);

    assertNotNull(annotation);
    assertEquals("well-known", annotation.prefix());
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.config", WellKnownConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(WellKnownConfig.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnownConfig.class.getDeclaredField("issuer").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnownConfig.class.getDeclaredField("auth_ep").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnownConfig.class.getDeclaredField("token_ep").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnownConfig.class.getDeclaredField("nonce_ep").getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, WellKnownConfig.class.getDeclaredField("issuer").getType());
    assertEquals(String.class, WellKnownConfig.class.getDeclaredField("auth_ep").getType());
    assertEquals(String.class, WellKnownConfig.class.getDeclaredField("token_ep").getType());
    assertEquals(String.class, WellKnownConfig.class.getDeclaredField("nonce_ep").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(4, WellKnownConfig.class.getDeclaredFields().length);
  }

  @Test
  void testCompleteWellKnownConfigUsage() {
    String issuer = "https://example.com/issuer";
    String authEp = "https://example.com/auth";
    String tokenEp = "https://example.com/token";
    String nonceEp = "https://example.com/nonce";

    wellKnownConfig.setIssuer(issuer);
    wellKnownConfig.setAuth_ep(authEp);
    wellKnownConfig.setToken_ep(tokenEp);
    wellKnownConfig.setNonce_ep(nonceEp);

    assertEquals(issuer, wellKnownConfig.getIssuer());
    assertEquals(authEp, wellKnownConfig.getAuth_ep());
    assertEquals(tokenEp, wellKnownConfig.getToken_ep());
    assertEquals(nonceEp, wellKnownConfig.getNonce_ep());
  }

  @Test
  void testMultipleSettersOnSameField() {
    wellKnownConfig.setIssuer("https://first.com");
    wellKnownConfig.setIssuer("https://second.com");

    assertEquals("https://second.com", wellKnownConfig.getIssuer());
  }

  @Test
  void testSettersReturnVoid() throws NoSuchMethodException {
    var setIssuerMethod = WellKnownConfig.class.getMethod("setIssuer", String.class);
    var setAuthEpMethod = WellKnownConfig.class.getMethod("setAuth_ep", String.class);
    var setTokenEpMethod = WellKnownConfig.class.getMethod("setToken_ep", String.class);
    var setNonceEpMethod = WellKnownConfig.class.getMethod("setNonce_ep", String.class);

    assertEquals(void.class, setIssuerMethod.getReturnType());
    assertEquals(void.class, setAuthEpMethod.getReturnType());
    assertEquals(void.class, setTokenEpMethod.getReturnType());
    assertEquals(void.class, setNonceEpMethod.getReturnType());
  }

  @Test
  void testGettersReturnCorrectType() throws NoSuchMethodException {
    var getIssuerMethod = WellKnownConfig.class.getMethod("getIssuer");
    var getAuthEpMethod = WellKnownConfig.class.getMethod("getAuth_ep");
    var getTokenEpMethod = WellKnownConfig.class.getMethod("getToken_ep");
    var getNonceEpMethod = WellKnownConfig.class.getMethod("getNonce_ep");

    assertEquals(String.class, getIssuerMethod.getReturnType());
    assertEquals(String.class, getAuthEpMethod.getReturnType());
    assertEquals(String.class, getTokenEpMethod.getReturnType());
    assertEquals(String.class, getNonceEpMethod.getReturnType());
  }

  @Test
  void testGettersAndSettersArePublic() throws NoSuchMethodException {
    var getIssuerMethod = WellKnownConfig.class.getMethod("getIssuer");
    var setIssuerMethod = WellKnownConfig.class.getMethod("setIssuer", String.class);
    var getAuthEpMethod = WellKnownConfig.class.getMethod("getAuth_ep");
    var setAuthEpMethod = WellKnownConfig.class.getMethod("setAuth_ep", String.class);
    var getTokenEpMethod = WellKnownConfig.class.getMethod("getToken_ep");
    var setTokenEpMethod = WellKnownConfig.class.getMethod("setToken_ep", String.class);
    var getNonceEpMethod = WellKnownConfig.class.getMethod("getNonce_ep");
    var setNonceEpMethod = WellKnownConfig.class.getMethod("setNonce_ep", String.class);

    assertTrue(java.lang.reflect.Modifier.isPublic(getIssuerMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setIssuerMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(getAuthEpMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setAuthEpMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(getTokenEpMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setTokenEpMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(getNonceEpMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setNonceEpMethod.getModifiers()));
  }
}
