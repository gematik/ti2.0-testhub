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
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterControllerTest {

  private RegisterController registerController;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private HttpServletRequest httpServletRequest;

  @BeforeEach
  void setUp() {
    registerController = new RegisterController("");
  }

  @Test
  void testRegisterClientSuccess() throws Exception {
    stubDefaultRequestBaseUrl();
    RegisterController.DcrRequest request = createValidRequest();

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertInstanceOf(RegisterController.DcrResponse.class, response.getBody());

    RegisterController.DcrResponse dcrResponse =
        (RegisterController.DcrResponse) response.getBody();
    assertNotNull(dcrResponse.getClientId());
    assertFalse(dcrResponse.getClientId().isBlank());
    assertTrue(dcrResponse.getClientIdIssuedAt() > 0);
    assertEquals("sdk-client", dcrResponse.getClientName());
    assertEquals(
        List.of("urn:ietf:params:oauth:grant-type:token-exchange", "refresh_token"),
        dcrResponse.getGrantTypes());
    assertEquals("private_key_jwt", dcrResponse.getTokenEndpointAuthMethod());
    assertNotNull(dcrResponse.getJwks());
    assertNotNull(dcrResponse.getRegistrationClientUri());
    assertTrue(
        dcrResponse.getRegistrationClientUri().startsWith("http://localhost:9112/register/"));
    assertTrue(dcrResponse.getRegistrationClientUri().contains(dcrResponse.getClientId()));
    assertNotNull(dcrResponse.getRegistrationAccessToken());
  }

  @Test
  void testRegisterClientUsesConfiguredRegistrationBaseUrl() throws Exception {
    registerController = new RegisterController("https://public.example.com/pdp/");
    RegisterController.DcrRequest request = createValidRequest();

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrResponse dcrResponse =
        (RegisterController.DcrResponse) response.getBody();
    assertNotNull(dcrResponse);
    assertTrue(
        dcrResponse
            .getRegistrationClientUri()
            .startsWith("https://public.example.com/pdp/register/"));
    assertFalse(dcrResponse.getRegistrationClientUri().contains("/pdp//register/"));
  }

  @Test
  void testRegisterClientUsesForwardedHeadersForRegistrationUri() throws Exception {
    when(httpServletRequest.getHeader("X-Forwarded-Proto")).thenReturn("https");
    when(httpServletRequest.getHeader("X-Forwarded-Host")).thenReturn("external.example.com");
    when(httpServletRequest.getHeader("X-Forwarded-Port")).thenReturn("443");
    when(httpServletRequest.getContextPath()).thenReturn("/auth");

    RegisterController.DcrRequest request = createValidRequest();
    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrResponse dcrResponse =
        (RegisterController.DcrResponse) response.getBody();
    assertNotNull(dcrResponse);
    assertTrue(
        dcrResponse
            .getRegistrationClientUri()
            .startsWith("https://external.example.com/auth/register/"));
  }

  @Test
  void testRegisterClientIgnoresInvalidForwardedPortAndFallsBackToHostPort() throws Exception {
    when(httpServletRequest.getHeader("X-Forwarded-Proto")).thenReturn("https");
    when(httpServletRequest.getHeader("X-Forwarded-Host")).thenReturn("external.example.com:8443");
    when(httpServletRequest.getHeader("X-Forwarded-Port")).thenReturn("not-a-port");
    when(httpServletRequest.getContextPath()).thenReturn("");

    RegisterController.DcrRequest request = createValidRequest();
    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrResponse dcrResponse =
        (RegisterController.DcrResponse) response.getBody();
    assertNotNull(dcrResponse);
    assertTrue(
        dcrResponse
            .getRegistrationClientUri()
            .startsWith("https://external.example.com:8443/register/"));
  }

  @Test
  void testRegisterClientIgnoresInvalidForwardedAndHostPortAndFallsBackToRequestPort()
      throws Exception {
    when(httpServletRequest.getHeader("X-Forwarded-Proto")).thenReturn("https");
    when(httpServletRequest.getHeader("X-Forwarded-Host")).thenReturn("external.example.com:bad");
    when(httpServletRequest.getHeader("X-Forwarded-Port")).thenReturn("70000");
    when(httpServletRequest.getServerPort()).thenReturn(9443);
    when(httpServletRequest.getContextPath()).thenReturn("/auth");

    RegisterController.DcrRequest request = createValidRequest();
    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrResponse dcrResponse =
        (RegisterController.DcrResponse) response.getBody();
    assertNotNull(dcrResponse);
    assertTrue(
        dcrResponse
            .getRegistrationClientUri()
            .startsWith("https://external.example.com:9443/auth/register/"));
  }

  @Test
  void testRegisterClientWithRedirectUris() throws Exception {
    stubDefaultRequestBaseUrl();
    RegisterController.DcrRequest request = createValidRequest();
    request.setRedirectUris(List.of("http://localhost/callback", "http://localhost/other"));

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrResponse dcrResponse =
        (RegisterController.DcrResponse) response.getBody();
    assertNotNull(dcrResponse);
    assertEquals(
        List.of("http://localhost/callback", "http://localhost/other"),
        dcrResponse.getRedirectUris());
  }

  @Test
  void testRegisterClientMissingClientName() throws Exception {
    RegisterController.DcrRequest request = createValidRequest();
    request.setClientName(null);

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertInstanceOf(RegisterController.DcrErrorResponse.class, response.getBody());
    RegisterController.DcrErrorResponse error =
        (RegisterController.DcrErrorResponse) response.getBody();
    assertNotNull(error);
    assertEquals("invalid_client_metadata", error.getError());
    assertTrue(error.getErrorDescription().contains("client_name"));
  }

  @Test
  void testRegisterClientBlankClientName() throws Exception {
    RegisterController.DcrRequest request = createValidRequest();
    request.setClientName("   ");

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    assertInstanceOf(RegisterController.DcrErrorResponse.class, response.getBody());
  }

  @Test
  void testRegisterClientMissingGrantTypes() throws Exception {
    RegisterController.DcrRequest request = createValidRequest();
    request.setGrantTypes(null);

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrErrorResponse error =
        (RegisterController.DcrErrorResponse) response.getBody();
    assertNotNull(error);
    assertEquals("invalid_client_metadata", error.getError());
    assertTrue(error.getErrorDescription().contains("grant_types"));
  }

  @Test
  void testRegisterClientEmptyGrantTypes() throws Exception {
    RegisterController.DcrRequest request = createValidRequest();
    request.setGrantTypes(List.of());

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void testRegisterClientMissingTokenEndpointAuthMethod() throws Exception {
    RegisterController.DcrRequest request = createValidRequest();
    request.setTokenEndpointAuthMethod(null);

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrErrorResponse error =
        (RegisterController.DcrErrorResponse) response.getBody();
    assertNotNull(error);
    assertTrue(error.getErrorDescription().contains("token_endpoint_auth_method"));
  }

  @Test
  void testRegisterClientMissingJwks() throws Exception {
    RegisterController.DcrRequest request = createValidRequest();
    request.setJwks(null);

    ResponseEntity<?> response = registerController.registerClient(httpServletRequest, request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertNotNull(response.getBody());
    RegisterController.DcrErrorResponse error =
        (RegisterController.DcrErrorResponse) response.getBody();
    assertNotNull(error);
    assertTrue(error.getErrorDescription().contains("jwks"));
  }

  private RegisterController.DcrRequest createValidRequest() throws Exception {
    RegisterController.DcrRequest request = new RegisterController.DcrRequest();
    request.setClientName("sdk-client");
    request.setGrantTypes(
        List.of("urn:ietf:params:oauth:grant-type:token-exchange", "refresh_token"));
    request.setTokenEndpointAuthMethod("private_key_jwt");

    String jwksJson =
        """
        {"keys": [{"kty": "RSA", "kid": "test-key", "use": "sig", "alg": "RS256", "n": "abc", "e": "AQAB"}]}
        """;
    request.setJwks(objectMapper.readTree(jwksJson));
    return request;
  }

  private void stubDefaultRequestBaseUrl() {
    when(httpServletRequest.getScheme()).thenReturn("http");
    when(httpServletRequest.getServerName()).thenReturn("localhost");
    when(httpServletRequest.getServerPort()).thenReturn(9112);
    when(httpServletRequest.getContextPath()).thenReturn("");
  }
}
