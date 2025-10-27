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
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

class PoppConfigTest {

  private PoppConfig poppConfig;
  private PoppConfig.SecurityConfig securityConfig;
  private PoppConfig.StoreConfig storeConfig;
  private PoppConfig.KeyConfig keyConfig;

  @BeforeEach
  void setUp() {
    poppConfig = new PoppConfig();
    securityConfig = new PoppConfig.SecurityConfig();
    storeConfig = new PoppConfig.StoreConfig();
    keyConfig = new PoppConfig.KeyConfig();
  }

  @Test
  void testPoppConfigInitialization() {
    // Assert
    assertNotNull(poppConfig);
    assertNull(poppConfig.getSec());
  }

  @Test
  void testPoppConfigSetAndGetSec() {
    // Act
    poppConfig.setSec(securityConfig);

    // Assert
    assertEquals(securityConfig, poppConfig.getSec());
  }

  @Test
  void testSecurityConfigInitialization() {
    // Assert
    assertNotNull(securityConfig);
    assertNull(securityConfig.getStore());
    assertNull(securityConfig.getKey());
  }

  @Test
  void testSecurityConfigSetAndGetStore() {
    // Act
    securityConfig.setStore(storeConfig);

    // Assert
    assertEquals(storeConfig, securityConfig.getStore());
  }

  @Test
  void testSecurityConfigSetAndGetKey() {
    // Act
    securityConfig.setKey(keyConfig);

    // Assert
    assertEquals(keyConfig, securityConfig.getKey());
  }

  @Test
  void testStoreConfigInitialization() {
    // Assert
    assertNotNull(storeConfig);
    assertNull(storeConfig.getPath());
    assertNull(storeConfig.getPass());
  }

  @Test
  void testStoreConfigSetAndGetPath() {
    // Arrange
    String path = "/path/to/store";

    // Act
    storeConfig.setPath(path);

    // Assert
    assertEquals(path, storeConfig.getPath());
  }

  @Test
  void testStoreConfigSetAndGetPass() {
    // Arrange
    String pass = "store-password";

    // Act
    storeConfig.setPass(pass);

    // Assert
    assertEquals(pass, storeConfig.getPass());
  }

  @Test
  void testKeyConfigInitialization() {
    // Assert
    assertNotNull(keyConfig);
    assertNull(keyConfig.getAlias());
    assertNull(keyConfig.getPass());
  }

  @Test
  void testKeyConfigSetAndGetAlias() {
    // Arrange
    String alias = "key-alias";

    // Act
    keyConfig.setAlias(alias);

    // Assert
    assertEquals(alias, keyConfig.getAlias());
  }

  @Test
  void testKeyConfigSetAndGetPass() {
    // Arrange
    String pass = "key-password";

    // Act
    keyConfig.setPass(pass);

    // Assert
    assertEquals(pass, keyConfig.getPass());
  }

  @Test
  void testCompleteConfigurationChain() {
    // Arrange
    storeConfig.setPath("/keystore/path");
    storeConfig.setPass("store-pass");
    keyConfig.setAlias("test-alias");
    keyConfig.setPass("key-pass");
    securityConfig.setStore(storeConfig);
    securityConfig.setKey(keyConfig);
    poppConfig.setSec(securityConfig);

    // Assert
    assertEquals("/keystore/path", poppConfig.getSec().getStore().getPath());
    assertEquals("store-pass", poppConfig.getSec().getStore().getPass());
    assertEquals("test-alias", poppConfig.getSec().getKey().getAlias());
    assertEquals("key-pass", poppConfig.getSec().getKey().getPass());
  }

  @Test
  void testPoppConfigAnnotations() {
    // Assert
    assertTrue(PoppConfig.class.isAnnotationPresent(Component.class));
    assertTrue(PoppConfig.class.isAnnotationPresent(ConfigurationProperties.class));

    ConfigurationProperties configProps =
        PoppConfig.class.getAnnotation(ConfigurationProperties.class);
    assertEquals("popp", configProps.prefix());
  }

  @Test
  void testNestedClassesAreStatic() {
    // Assert
    assertTrue(java.lang.reflect.Modifier.isStatic(PoppConfig.SecurityConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(PoppConfig.StoreConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(PoppConfig.KeyConfig.class.getModifiers()));
  }

  @Test
  void testNestedClassesArePublic() {
    // Assert
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppConfig.SecurityConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppConfig.StoreConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppConfig.KeyConfig.class.getModifiers()));
  }

  @Test
  void testPackageStructure() {
    // Assert
    assertEquals("de.gematik.ti20.simsvc.server.config", PoppConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    // Assert
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppConfig.class.getModifiers()));
  }

  @Test
  void testFieldsCount() {
    // Assert
    assertEquals(1, PoppConfig.class.getDeclaredFields().length);
    assertEquals(2, PoppConfig.SecurityConfig.class.getDeclaredFields().length);
    assertEquals(2, PoppConfig.StoreConfig.class.getDeclaredFields().length);
    assertEquals(2, PoppConfig.KeyConfig.class.getDeclaredFields().length);
  }

  @Test
  void testNullSafetyInChain() {
    // Arrange
    poppConfig.setSec(null);

    // Assert
    assertNull(poppConfig.getSec());
  }

  @Test
  void testSettersWithNullValues() {
    // Act & Assert
    assertDoesNotThrow(
        () -> {
          poppConfig.setSec(null);
          securityConfig.setStore(null);
          securityConfig.setKey(null);
          storeConfig.setPath(null);
          storeConfig.setPass(null);
          keyConfig.setAlias(null);
          keyConfig.setPass(null);
        });
  }

  @Test
  void testSettersWithEmptyStrings() {
    // Act
    storeConfig.setPath("");
    storeConfig.setPass("");
    keyConfig.setAlias("");
    keyConfig.setPass("");

    // Assert
    assertEquals("", storeConfig.getPath());
    assertEquals("", storeConfig.getPass());
    assertEquals("", keyConfig.getAlias());
    assertEquals("", keyConfig.getPass());
  }

  @Test
  void testMultipleSettersOnSameObject() {
    // Act
    storeConfig.setPath("path1");
    storeConfig.setPath("path2");
    storeConfig.setPass("pass1");
    storeConfig.setPass("pass2");

    // Assert
    assertEquals("path2", storeConfig.getPath());
    assertEquals("pass2", storeConfig.getPass());
  }
}
