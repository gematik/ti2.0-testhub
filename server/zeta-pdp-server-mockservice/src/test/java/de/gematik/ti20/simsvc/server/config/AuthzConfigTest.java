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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ExtendWith(MockitoExtension.class)
class AuthzConfigTest {

  @Test
  void testConstructor() {
    AuthzConfig config = new AuthzConfig();

    assertNotNull(config);
  }

  @Test
  void testSecurityConfigProperty() {
    AuthzConfig config = new AuthzConfig();
    AuthzConfig.SecurityConfig securityConfig = new AuthzConfig.SecurityConfig();

    config.setSec(securityConfig);

    assertSame(securityConfig, config.getSec());
  }

  @Test
  void testComponentAnnotation() {
    assertTrue(AuthzConfig.class.isAnnotationPresent(Component.class));
  }

  @Test
  void testConfigurationPropertiesAnnotation() {
    ConfigurationProperties annotation =
        AuthzConfig.class.getAnnotation(ConfigurationProperties.class);

    assertNotNull(annotation);
    assertEquals("authz", annotation.prefix());
  }

  @Test
  void testSecurityConfigClass() {
    AuthzConfig.SecurityConfig securityConfig = new AuthzConfig.SecurityConfig();

    assertNotNull(securityConfig);
  }

  @Test
  void testSecurityConfigStoreProperty() {
    AuthzConfig.SecurityConfig securityConfig = new AuthzConfig.SecurityConfig();
    AuthzConfig.StoreConfig storeConfig = new AuthzConfig.StoreConfig();

    securityConfig.setStore(storeConfig);

    assertSame(storeConfig, securityConfig.getStore());
  }

  @Test
  void testSecurityConfigKeyProperty() {
    AuthzConfig.SecurityConfig securityConfig = new AuthzConfig.SecurityConfig();
    AuthzConfig.KeyConfig keyConfig = new AuthzConfig.KeyConfig();

    securityConfig.setKey(keyConfig);

    assertSame(keyConfig, securityConfig.getKey());
  }

  @Test
  void testStoreConfigClass() {
    AuthzConfig.StoreConfig storeConfig = new AuthzConfig.StoreConfig();

    assertNotNull(storeConfig);
  }

  @Test
  void testStoreConfigPathProperty() {
    AuthzConfig.StoreConfig storeConfig = new AuthzConfig.StoreConfig();
    String testPath = "/path/to/store";

    storeConfig.setPath(testPath);

    assertEquals(testPath, storeConfig.getPath());
  }

  @Test
  void testStoreConfigPassProperty() {
    AuthzConfig.StoreConfig storeConfig = new AuthzConfig.StoreConfig();
    String testPass = "storePassword";

    storeConfig.setPass(testPass);

    assertEquals(testPass, storeConfig.getPass());
  }

  @Test
  void testKeyConfigClass() {
    AuthzConfig.KeyConfig keyConfig = new AuthzConfig.KeyConfig();

    assertNotNull(keyConfig);
  }

  @Test
  void testKeyConfigAliasProperty() {
    AuthzConfig.KeyConfig keyConfig = new AuthzConfig.KeyConfig();
    String testAlias = "keyAlias";

    keyConfig.setAlias(testAlias);

    assertEquals(testAlias, keyConfig.getAlias());
  }

  @Test
  void testKeyConfigPassProperty() {
    AuthzConfig.KeyConfig keyConfig = new AuthzConfig.KeyConfig();
    String testPass = "keyPassword";

    keyConfig.setPass(testPass);

    assertEquals(testPass, keyConfig.getPass());
  }

  @Test
  void testInnerClassesAreStatic() {
    assertTrue(
        java.lang.reflect.Modifier.isStatic(AuthzConfig.SecurityConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(AuthzConfig.StoreConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isStatic(AuthzConfig.KeyConfig.class.getModifiers()));
  }

  @Test
  void testInnerClassesArePublic() {
    assertTrue(
        java.lang.reflect.Modifier.isPublic(AuthzConfig.SecurityConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(AuthzConfig.StoreConfig.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(AuthzConfig.KeyConfig.class.getModifiers()));
  }

  @Test
  void testCompleteConfigurationStructure() {
    AuthzConfig config = new AuthzConfig();
    AuthzConfig.SecurityConfig securityConfig = new AuthzConfig.SecurityConfig();
    AuthzConfig.StoreConfig storeConfig = new AuthzConfig.StoreConfig();
    AuthzConfig.KeyConfig keyConfig = new AuthzConfig.KeyConfig();

    storeConfig.setPath("/keystore/path");
    storeConfig.setPass("storePassword");
    keyConfig.setAlias("myKey");
    keyConfig.setPass("keyPassword");

    securityConfig.setStore(storeConfig);
    securityConfig.setKey(keyConfig);
    config.setSec(securityConfig);

    assertNotNull(config.getSec());
    assertNotNull(config.getSec().getStore());
    assertNotNull(config.getSec().getKey());
    assertEquals("/keystore/path", config.getSec().getStore().getPath());
    assertEquals("storePassword", config.getSec().getStore().getPass());
    assertEquals("myKey", config.getSec().getKey().getAlias());
    assertEquals("keyPassword", config.getSec().getKey().getPass());
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.config", AuthzConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(AuthzConfig.class.getModifiers()));
  }
}
