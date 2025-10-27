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
class SecurityConfigTest {

  private SecurityConfig securityConfig;
  private SecurityConfig.StoreConfig storeConfig;
  private SecurityConfig.KeyConfig keyConfig;

  @BeforeEach
  void setUp() {
    securityConfig = new SecurityConfig();
    storeConfig = new SecurityConfig.StoreConfig();
    keyConfig = new SecurityConfig.KeyConfig();
  }

  @Test
  void testNoArgsConstructor() {
    assertNotNull(securityConfig);
    assertNull(securityConfig.getStore());
    assertNull(securityConfig.getKey());
  }

  @Test
  void testGetterAndSetterStore() {
    securityConfig.setStore(storeConfig);

    assertEquals(storeConfig, securityConfig.getStore());
  }

  @Test
  void testGetterAndSetterKey() {
    securityConfig.setKey(keyConfig);

    assertEquals(keyConfig, securityConfig.getKey());
  }

  @Test
  void testSetStoreToNull() {
    securityConfig.setStore(storeConfig);
    securityConfig.setStore(null);

    assertNull(securityConfig.getStore());
  }

  @Test
  void testSetKeyToNull() {
    securityConfig.setKey(keyConfig);
    securityConfig.setKey(null);

    assertNull(securityConfig.getKey());
  }

  @Test
  void testStoreConfigNoArgsConstructor() {
    assertNotNull(storeConfig);
    assertNull(storeConfig.getPath());
    assertNull(storeConfig.getPass());
  }

  @Test
  void testStoreConfigGetterAndSetterPath() {
    String testPath = "/path/to/keystore.p12";

    storeConfig.setPath(testPath);

    assertEquals(testPath, storeConfig.getPath());
  }

  @Test
  void testStoreConfigGetterAndSetterPass() {
    String testPass = "storePassword";

    storeConfig.setPass(testPass);

    assertEquals(testPass, storeConfig.getPass());
  }

  @Test
  void testStoreConfigSetPathToNull() {
    storeConfig.setPath("test-path");
    storeConfig.setPath(null);

    assertNull(storeConfig.getPath());
  }

  @Test
  void testStoreConfigSetPassToNull() {
    storeConfig.setPass("test-pass");
    storeConfig.setPass(null);

    assertNull(storeConfig.getPass());
  }

  @Test
  void testKeyConfigNoArgsConstructor() {
    assertNotNull(keyConfig);
    assertNull(keyConfig.getAlias());
    assertNull(keyConfig.getPass());
  }

  @Test
  void testKeyConfigGetterAndSetterAlias() {
    String testAlias = "test-key-alias";

    keyConfig.setAlias(testAlias);

    assertEquals(testAlias, keyConfig.getAlias());
  }

  @Test
  void testKeyConfigGetterAndSetterPass() {
    String testPass = "keyPassword";

    keyConfig.setPass(testPass);

    assertEquals(testPass, keyConfig.getPass());
  }

  @Test
  void testKeyConfigSetAliasToNull() {
    keyConfig.setAlias("test-alias");
    keyConfig.setAlias(null);

    assertNull(keyConfig.getAlias());
  }

  @Test
  void testKeyConfigSetPassToNull() {
    keyConfig.setPass("test-pass");
    keyConfig.setPass(null);

    assertNull(keyConfig.getPass());
  }

  @Test
  void testComponentAnnotation() {
    assertTrue(SecurityConfig.class.isAnnotationPresent(Component.class));
  }

  @Test
  void testConfigurationPropertiesAnnotation() {
    ConfigurationProperties annotation =
        SecurityConfig.class.getAnnotation(ConfigurationProperties.class);

    assertNotNull(annotation);
    assertEquals("proxy.sec", annotation.prefix());
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.config", SecurityConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(SecurityConfig.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            SecurityConfig.class.getDeclaredField("store").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            SecurityConfig.class.getDeclaredField("key").getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(
        SecurityConfig.StoreConfig.class, SecurityConfig.class.getDeclaredField("store").getType());
    assertEquals(
        SecurityConfig.KeyConfig.class, SecurityConfig.class.getDeclaredField("key").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(2, SecurityConfig.class.getDeclaredFields().length);
  }

  @Test
  void testStoreConfigInnerClassStructure() {
    assertTrue(SecurityConfig.StoreConfig.class.getEnclosingClass().equals(SecurityConfig.class));
    assertTrue(
        java.lang.reflect.Modifier.isStatic(SecurityConfig.StoreConfig.class.getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPublic(SecurityConfig.StoreConfig.class.getModifiers()));
  }

  @Test
  void testKeyConfigInnerClassStructure() {
    assertTrue(SecurityConfig.KeyConfig.class.getEnclosingClass().equals(SecurityConfig.class));
    assertTrue(java.lang.reflect.Modifier.isStatic(SecurityConfig.KeyConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(SecurityConfig.KeyConfig.class.getModifiers()));
  }

  @Test
  void testStoreConfigFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            SecurityConfig.StoreConfig.class.getDeclaredField("path").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            SecurityConfig.StoreConfig.class.getDeclaredField("pass").getModifiers()));
  }

  @Test
  void testKeyConfigFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            SecurityConfig.KeyConfig.class.getDeclaredField("alias").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            SecurityConfig.KeyConfig.class.getDeclaredField("pass").getModifiers()));
  }

  @Test
  void testStoreConfigFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, SecurityConfig.StoreConfig.class.getDeclaredField("path").getType());
    assertEquals(String.class, SecurityConfig.StoreConfig.class.getDeclaredField("pass").getType());
  }

  @Test
  void testKeyConfigFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, SecurityConfig.KeyConfig.class.getDeclaredField("alias").getType());
    assertEquals(String.class, SecurityConfig.KeyConfig.class.getDeclaredField("pass").getType());
  }

  @Test
  void testCompleteSecurityConfigUsage() {
    storeConfig.setPath("/path/to/keystore.p12");
    storeConfig.setPass("storePassword");

    keyConfig.setAlias("test-key-alias");
    keyConfig.setPass("keyPassword");

    securityConfig.setStore(storeConfig);
    securityConfig.setKey(keyConfig);

    assertEquals(storeConfig, securityConfig.getStore());
    assertEquals(keyConfig, securityConfig.getKey());
    assertEquals("/path/to/keystore.p12", securityConfig.getStore().getPath());
    assertEquals("storePassword", securityConfig.getStore().getPass());
    assertEquals("test-key-alias", securityConfig.getKey().getAlias());
    assertEquals("keyPassword", securityConfig.getKey().getPass());
  }

  @Test
  void testMultipleSettersOnSameField() {
    SecurityConfig.StoreConfig firstStore = new SecurityConfig.StoreConfig();
    SecurityConfig.StoreConfig secondStore = new SecurityConfig.StoreConfig();

    securityConfig.setStore(firstStore);
    securityConfig.setStore(secondStore);

    assertEquals(secondStore, securityConfig.getStore());
  }

  @Test
  void testSettersReturnVoid() throws NoSuchMethodException {
    var setStoreMethod =
        SecurityConfig.class.getMethod("setStore", SecurityConfig.StoreConfig.class);
    var setKeyMethod = SecurityConfig.class.getMethod("setKey", SecurityConfig.KeyConfig.class);

    assertEquals(void.class, setStoreMethod.getReturnType());
    assertEquals(void.class, setKeyMethod.getReturnType());
  }

  @Test
  void testGettersReturnCorrectType() throws NoSuchMethodException {
    var getStoreMethod = SecurityConfig.class.getMethod("getStore");
    var getKeyMethod = SecurityConfig.class.getMethod("getKey");

    assertEquals(SecurityConfig.StoreConfig.class, getStoreMethod.getReturnType());
    assertEquals(SecurityConfig.KeyConfig.class, getKeyMethod.getReturnType());
  }

  @Test
  void testGettersAndSettersArePublic() throws NoSuchMethodException {
    var getStoreMethod = SecurityConfig.class.getMethod("getStore");
    var setStoreMethod =
        SecurityConfig.class.getMethod("setStore", SecurityConfig.StoreConfig.class);
    var getKeyMethod = SecurityConfig.class.getMethod("getKey");
    var setKeyMethod = SecurityConfig.class.getMethod("setKey", SecurityConfig.KeyConfig.class);

    assertTrue(java.lang.reflect.Modifier.isPublic(getStoreMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setStoreMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(getKeyMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setKeyMethod.getModifiers()));
  }
}
