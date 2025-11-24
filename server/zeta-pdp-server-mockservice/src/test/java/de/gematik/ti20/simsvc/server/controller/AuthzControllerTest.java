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
package de.gematik.ti20.simsvc.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.model.TokenRequestBody;
import de.gematik.ti20.simsvc.server.service.AccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class AuthzControllerTest {

  @Mock private AccessTokenService accessTokenService;

  @Mock private HttpServletRequest httpServletRequest;

  private AuthzController authzController;

  @BeforeEach
  void setUp() {
    authzController = new AuthzController(accessTokenService);
  }

  @Test
  void testConstructor() {
    assertNotNull(authzController);
  }

  @Test
  void testGetAccessTokenSuccess() throws Exception {
    // Arrange
    String subjectToken = "subject_token_123";
    String expectedAccessToken = "access_token_456";
    TokenRequestBody tokenRequestBody = new TokenRequestBody();
    tokenRequestBody.setSubject_token(subjectToken);

    when(accessTokenService.createToken(httpServletRequest, subjectToken))
        .thenReturn(expectedAccessToken);

    // Act
    ResponseEntity<?> response =
        authzController.getAccessToken(httpServletRequest, tokenRequestBody);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() instanceof AuthzController.TokenData);

    AuthzController.TokenData tokenData = (AuthzController.TokenData) response.getBody();
    assertEquals(expectedAccessToken, tokenData.getAccessToken());
    assertEquals("TODO REFRESHTOKEN", tokenData.getRefreshToken());

    verify(accessTokenService).createToken(httpServletRequest, subjectToken);
  }

  @Test
  void testGetAccessTokenThrowsRuntimeException() throws Exception {
    // Arrange
    String subjectToken = "subject_token_123";
    TokenRequestBody tokenRequestBody = new TokenRequestBody();
    tokenRequestBody.setSubject_token(subjectToken);

    when(accessTokenService.createToken(httpServletRequest, subjectToken))
        .thenThrow(new Exception("Service error"));

    // Act & Assert
    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              authzController.getAccessToken(httpServletRequest, tokenRequestBody);
            });

    assertEquals("java.lang.Exception: Service error", exception.getMessage());
    verify(accessTokenService).createToken(httpServletRequest, subjectToken);
  }

  @Test
  void testGetAccessTokenWithNullSubjectToken() throws Exception {
    // Arrange
    TokenRequestBody tokenRequestBody = new TokenRequestBody();
    tokenRequestBody.setSubject_token(null);

    when(accessTokenService.createToken(httpServletRequest, null))
        .thenReturn("access_token_for_null");

    // Act
    ResponseEntity<?> response =
        authzController.getAccessToken(httpServletRequest, tokenRequestBody);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    AuthzController.TokenData tokenData = (AuthzController.TokenData) response.getBody();
    assertEquals("access_token_for_null", tokenData.getAccessToken());

    verify(accessTokenService).createToken(httpServletRequest, null);
  }

  @Test
  void testRestControllerAnnotation() {
    assertTrue(AuthzController.class.isAnnotationPresent(RestController.class));
  }

  @Test
  void testTokenDataClass() {
    String accessToken = "test_access_token";
    String refreshToken = "test_refresh_token";

    AuthzController.TokenData tokenData = new AuthzController.TokenData(accessToken, refreshToken);

    assertEquals(accessToken, tokenData.getAccessToken());
    assertEquals(refreshToken, tokenData.getRefreshToken());
  }

  @Test
  void testTokenDataSetters() {
    AuthzController.TokenData tokenData = new AuthzController.TokenData("", "");
    String newAccessToken = "new_access_token";
    String newRefreshToken = "new_refresh_token";

    tokenData.setAccessToken(newAccessToken);
    tokenData.setRefreshToken(newRefreshToken);

    assertEquals(newAccessToken, tokenData.getAccessToken());
    assertEquals(newRefreshToken, tokenData.getRefreshToken());
  }

  @Test
  void testTokenDataIsStaticClass() {
    assertTrue(java.lang.reflect.Modifier.isStatic(AuthzController.TokenData.class.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(AuthzController.TokenData.class.getModifiers()));
  }

  @Test
  void testAccessTokenServiceInjection() throws Exception {
    assertNotNull(authzController);

    // Test that controller works with the injected service
    verify(accessTokenService, never()).createToken(any(), any());
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.controller", AuthzController.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(AuthzController.class.getModifiers()));
  }
}
