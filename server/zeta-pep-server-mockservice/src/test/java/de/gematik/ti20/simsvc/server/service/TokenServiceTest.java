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
package de.gematik.ti20.simsvc.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.ti20.simsvc.server.config.PoppConfig;
import de.gematik.ti20.simsvc.server.config.SecurityConfig;
import de.gematik.ti20.simsvc.server.model.AccessToken;
import de.gematik.ti20.simsvc.server.model.PoppToken;
import de.gematik.ti20.simsvc.server.model.UserInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  private SecurityConfig secConfig;
  //
  //  @Mock private SecurityConfig.Store store;

  @Mock private PoppConfig poppConfig;

  @Mock private AccessToken accessToken;

  @Mock private AccessToken.TokenClaims accessTokenClaims;

  @Mock private PoppToken poppToken;

  @Mock private PoppToken.TokenClaims poppTokenClaims;

  @Mock private KeyStore keyStore;

  private TokenService tokenService;

  @BeforeEach
  void setUp() {
    SecurityConfig.StoreConfig storeConfig = new SecurityConfig.StoreConfig();
    storeConfig.setPass("");
    storeConfig.setPath("");

    SecurityConfig.KeyConfig keyConfig = new SecurityConfig.KeyConfig();
    keyConfig.setPass("");
    keyConfig.setAlias("");

    secConfig = new SecurityConfig();
    secConfig.setStore(storeConfig);
    secConfig.setKey(keyConfig);
  }

  @Test
  void testConstructorInitializesSuccessfully() {
    // Arrange
    byte[] keystoreBytes = new byte[100]; // Mock keystore data
    InputStream mockInputStream = new ByteArrayInputStream(keystoreBytes);

    try (MockedStatic<KeyStore> keyStoreMock = mockStatic(KeyStore.class)) {
      keyStoreMock.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(keyStore);

      // Act
      tokenService = new TokenService(secConfig, poppConfig);

      // Assert
      assertNotNull(tokenService);
      keyStoreMock.verify(() -> KeyStore.getInstance("PKCS12"));
    }
  }

  @Test
  void testConstructorThrowsExceptionOnKeystoreFailure() {
    // Arrange
    try (MockedStatic<KeyStore> keyStoreMock = mockStatic(KeyStore.class)) {
      keyStoreMock
          .when(() -> KeyStore.getInstance("PKCS12"))
          .thenThrow(new RuntimeException("Keystore error"));

      // Act & Assert
      RuntimeException exception =
          assertThrows(RuntimeException.class, () -> new TokenService(secConfig, poppConfig));

      assertEquals("Failed to initialize KeyStore", exception.getMessage());
    }
  }

  @Test
  void testValidateAccessTokenWithEmptyHeader() {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> tokenService.validateAccessToken(""));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    assertEquals("AccessToken is empty", exception.getReason());
  }

  @Test
  void testValidateAccessTokenWithNullHeader() {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> tokenService.validateAccessToken(null));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    assertEquals("AccessToken is empty", exception.getReason());
  }

  @Test
  void testValidateAccessTokenWithInvalidPrefix() {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();
    String authzHeader = "Bearer invalid-token";

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> tokenService.validateAccessToken(authzHeader));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    assertEquals("AccessToken is not a DPoP auth", exception.getReason());
  }

  @Test
  void testValidatePoppTokenWhenNotRequired() {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();
    when(poppConfig.isRequired()).thenReturn(false);

    // Act
    PoppToken result = tokenService.validatePoppToken("some-token", accessToken);

    // Assert
    assertNull(result);
  }

  @Test
  void testValidatePoppTokenWithEmptyToken() {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();
    when(poppConfig.isRequired()).thenReturn(true);

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> tokenService.validatePoppToken("", accessToken));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Missing PoPP auth", exception.getReason());
  }

  @Test
  void testConvertToBase64JsonWithUserInfo() throws Exception {
    tokenService = createTokenServiceWithMockedKeystore();

    UserInfo userInfo = new UserInfo("subject", "identifier", "professionOID");
    String base64 = tokenService.convertToBase64Json(userInfo);

    assertNotNull(base64);
    String json = new String(Base64.getUrlDecoder().decode(base64));
    assertTrue(json.contains("subject"));
    assertTrue(json.contains("identifier"));
    assertTrue(json.contains("professionOID"));
  }

  @Test
  void testConvertToBase64JsonWithNull() throws Exception {
    tokenService = createTokenServiceWithMockedKeystore();

    String result = tokenService.convertToBase64Json(null);
    assertNull(result);
  }

  @Test
  void testValidatePoppTokenWithNullToken() {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();
    when(poppConfig.isRequired()).thenReturn(true);

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> tokenService.validatePoppToken(null, accessToken));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Missing PoPP auth", exception.getReason());
  }

  @Test
  void testConvertToBase64JsonSuccess() throws JsonProcessingException {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();
    UserInfo userInfo = new UserInfo("test-subject", "test-identifier", "test-profession-oid");

    // Act
    String result = tokenService.convertToBase64Json(userInfo);

    // Assert
    assertNotNull(result);
    assertFalse(result.isEmpty());

    // Verify base64 decoding
    byte[] decodedBytes = Base64.getUrlDecoder().decode(result);
    String decodedJson = new String(decodedBytes);
    assertTrue(decodedJson.contains("test-subject"));
    assertTrue(decodedJson.contains("test-identifier"));
    assertTrue(decodedJson.contains("test-profession-oid"));
  }

  @Test
  void testConvertToBase64JsonWithNullObject() throws JsonProcessingException {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();

    // Act
    String result = tokenService.convertToBase64Json(null);

    // Assert
    assertNull(result);
  }

  @Test
  void testConvertToBase64JsonWithComplexObject() throws JsonProcessingException {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();
    Map<String, Object> complexObject = new HashMap<>();
    complexObject.put("key1", "value1");
    complexObject.put("key2", 123);
    complexObject.put("key3", true);

    // Act
    String result = tokenService.convertToBase64Json(complexObject);

    // Assert
    assertNotNull(result);
    assertFalse(result.isEmpty());

    // Verify base64 decoding
    byte[] decodedBytes = Base64.getUrlDecoder().decode(result);
    String decodedJson = new String(decodedBytes);
    assertTrue(decodedJson.contains("\"key1\":\"value1\""));
    assertTrue(decodedJson.contains("\"key2\":123"));
    assertTrue(decodedJson.contains("\"key3\":true"));
  }

  @Test
  void testConvertToBase64JsonWithEmptyObject() throws JsonProcessingException {
    // Arrange
    tokenService = createTokenServiceWithMockedKeystore();
    Map<String, Object> emptyObject = new HashMap<>();

    // Act
    String result = tokenService.convertToBase64Json(emptyObject);

    // Assert
    assertNotNull(result);
    assertFalse(result.isEmpty());

    // Verify base64 decoding
    byte[] decodedBytes = Base64.getUrlDecoder().decode(result);
    String decodedJson = new String(decodedBytes);
    assertEquals("{}", decodedJson);
  }

  @Test
  void testPackageStructure() {
    // Assert
    assertEquals(
        "de.gematik.ti20.simsvc.server.service", TokenService.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    // Assert
    assertTrue(java.lang.reflect.Modifier.isPublic(TokenService.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    // Assert
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            TokenService.class.getDeclaredField("secConfig").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            TokenService.class.getDeclaredField("poppConfig").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isStatic(
            TokenService.class.getDeclaredField("keyStore").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isStatic(
            TokenService.class.getDeclaredField("jsonMapper").getModifiers()));
  }

  @Test
  void testConstructorCount() {
    // Assert
    assertEquals(1, TokenService.class.getDeclaredConstructors().length);
  }

  private TokenService createTokenServiceWithMockedKeystore() {
    try (MockedStatic<KeyStore> keyStoreMock = mockStatic(KeyStore.class)) {
      keyStoreMock.when(() -> KeyStore.getInstance("PKCS12")).thenReturn(keyStore);
      return new TokenService(secConfig, poppConfig);
    }
  }
}
