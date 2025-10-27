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
class HttpConfigTest {

  private HttpConfig httpConfig;

  @BeforeEach
  void setUp() {
    httpConfig = new HttpConfig();
  }

  @Test
  void testNoArgsConstructor() {
    assertNotNull(httpConfig);
    assertNull(httpConfig.getUrl());
    assertEquals("Authorization", httpConfig.getHeaderAccessToken());
  }

  @Test
  void testGetterAndSetterUrl() {
    String testUrl = "https://example.com/api";

    httpConfig.setUrl(testUrl);

    assertEquals(testUrl, httpConfig.getUrl());
  }

  @Test
  void testGetterAndSetterHeaderAccessToken() {
    String testHeader = "X-Auth-Token";

    httpConfig.setHeaderAccessToken(testHeader);

    assertEquals(testHeader, httpConfig.getHeaderAccessToken());
  }

  @Test
  void testUrlDefaultValue() {
    assertNull(httpConfig.getUrl());
  }

  @Test
  void testHeaderAccessTokenDefaultValue() {
    assertEquals("Authorization", httpConfig.getHeaderAccessToken());
  }

  @Test
  void testSetUrlToNull() {
    httpConfig.setUrl("https://example.com");
    httpConfig.setUrl(null);

    assertNull(httpConfig.getUrl());
  }

  @Test
  void testSetHeaderAccessTokenToNull() {
    httpConfig.setHeaderAccessToken(null);

    assertNull(httpConfig.getHeaderAccessToken());
  }

  @Test
  void testSetUrlToEmptyString() {
    httpConfig.setUrl("");

    assertEquals("", httpConfig.getUrl());
  }

  @Test
  void testSetHeaderAccessTokenToEmptyString() {
    httpConfig.setHeaderAccessToken("");

    assertEquals("", httpConfig.getHeaderAccessToken());
  }

  @Test
  void testComponentAnnotation() {
    assertTrue(HttpConfig.class.isAnnotationPresent(Component.class));
  }

  @Test
  void testConfigurationPropertiesAnnotation() {
    ConfigurationProperties annotation =
        HttpConfig.class.getAnnotation(ConfigurationProperties.class);

    assertNotNull(annotation);
    assertEquals("proxy.http", annotation.prefix());
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.config", HttpConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(HttpConfig.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            HttpConfig.class.getDeclaredField("url").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            HttpConfig.class.getDeclaredField("headerAccessToken").getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, HttpConfig.class.getDeclaredField("url").getType());
    assertEquals(String.class, HttpConfig.class.getDeclaredField("headerAccessToken").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(2, HttpConfig.class.getDeclaredFields().length);
  }

  @Test
  void testCompleteHttpConfigUsage() {
    String testUrl = "https://api.example.com/v1/endpoint";
    String testHeader = "Bearer-Token";

    httpConfig.setUrl(testUrl);
    httpConfig.setHeaderAccessToken(testHeader);

    assertEquals(testUrl, httpConfig.getUrl());
    assertEquals(testHeader, httpConfig.getHeaderAccessToken());
  }

  @Test
  void testMultipleSettersOnSameField() {
    httpConfig.setUrl("https://first.com");
    httpConfig.setUrl("https://second.com");

    assertEquals("https://second.com", httpConfig.getUrl());
  }

  @Test
  void testSettersReturnVoid() throws NoSuchMethodException {
    var setUrlMethod = HttpConfig.class.getMethod("setUrl", String.class);
    var setHeaderAccessTokenMethod =
        HttpConfig.class.getMethod("setHeaderAccessToken", String.class);

    assertEquals(void.class, setUrlMethod.getReturnType());
    assertEquals(void.class, setHeaderAccessTokenMethod.getReturnType());
  }

  @Test
  void testGettersReturnCorrectType() throws NoSuchMethodException {
    var getUrlMethod = HttpConfig.class.getMethod("getUrl");
    var getHeaderAccessTokenMethod = HttpConfig.class.getMethod("getHeaderAccessToken");

    assertEquals(String.class, getUrlMethod.getReturnType());
    assertEquals(String.class, getHeaderAccessTokenMethod.getReturnType());
  }

  @Test
  void testGettersAndSettersArePublic() throws NoSuchMethodException {
    var getUrlMethod = HttpConfig.class.getMethod("getUrl");
    var setUrlMethod = HttpConfig.class.getMethod("setUrl", String.class);
    var getHeaderAccessTokenMethod = HttpConfig.class.getMethod("getHeaderAccessToken");
    var setHeaderAccessTokenMethod =
        HttpConfig.class.getMethod("setHeaderAccessToken", String.class);

    assertTrue(java.lang.reflect.Modifier.isPublic(getUrlMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setUrlMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(getHeaderAccessTokenMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setHeaderAccessTokenMethod.getModifiers()));
  }
}
