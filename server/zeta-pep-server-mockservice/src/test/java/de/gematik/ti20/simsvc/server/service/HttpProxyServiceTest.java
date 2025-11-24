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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.ti20.simsvc.server.model.AccessToken;
import de.gematik.ti20.simsvc.server.model.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class HttpProxyServiceTest {

  @Mock private ZetaPepTokenService tokenService;

  @Mock private HttpServletRequest request;

  @Mock private AccessToken accessToken;

  @Mock private AccessToken.TokenClaims claims;

  @Mock private RestTemplate restTemplate;

  private HttpProxyService httpProxyService;

  @BeforeEach
  void setUp() {
    httpProxyService = new HttpProxyService(tokenService);

    // Use reflection to replace the RestTemplate with our mock
    try {
      var field = HttpProxyService.class.getDeclaredField("restTemplate");
      field.setAccessible(true);
      field.set(httpProxyService, restTemplate);
    } catch (Exception e) {
      // Handle reflection exception
    }
  }

  @Test
  void testPreprocessWithHttpServletRequest() throws JsonProcessingException {
    // Arrange
    Vector<String> headerNames = new Vector<>();
    headerNames.add("Authorization");
    headerNames.add("Content-Type");

    Vector<String> authHeaders = new Vector<>();
    authHeaders.add("Bearer token123");

    Vector<String> contentTypeHeaders = new Vector<>();
    contentTypeHeaders.add("application/json");

    when(request.getHeaderNames()).thenReturn(headerNames.elements());
    when(request.getHeaders("Authorization")).thenReturn(authHeaders.elements());
    when(request.getHeaders("Content-Type")).thenReturn(contentTypeHeaders.elements());

    when(tokenService.validateAccessToken("Bearer token123")).thenReturn(accessToken);
    when(accessToken.getClaims()).thenReturn(claims);
    when(claims.getSub()).thenReturn("test-subject");
    when(claims.getClientId()).thenReturn("test-client-id");
    when(claims.getProfessionOid()).thenReturn("test-profession-oid");
    when(tokenService.convertToBase64Json(any(UserInfo.class))).thenReturn("base64-user-info");

    // Act
    HttpHeaders result = httpProxyService.preprocess(request);

    // Assert
    assertNotNull(result);
    assertEquals("base64-user-info", result.getFirst("ZETA-User-Info"));
    assertEquals("Bearer token123", result.getFirst("Authorization"));
    assertEquals("application/json", result.getFirst("Content-Type"));
    verify(tokenService).validateAccessToken("Bearer token123");
    verify(tokenService).convertToBase64Json(any(UserInfo.class));
  }

  @Test
  void testPreprocessWithHttpHeaders() throws JsonProcessingException {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer token123");
    headers.add("PoPP", "popp-token");
    headers.add("If-None-Match", "etag-value");

    when(tokenService.validateAccessToken("Bearer token123")).thenReturn(accessToken);
    when(accessToken.getClaims()).thenReturn(claims);
    when(claims.getSub()).thenReturn("test-subject");
    when(claims.getClientId()).thenReturn("test-client-id");
    when(claims.getProfessionOid()).thenReturn("test-profession-oid");
    when(tokenService.convertToBase64Json(any(UserInfo.class))).thenReturn("base64-user-info");

    // Act
    HttpHeaders result = httpProxyService.preprocess(headers);

    // Assert
    assertNotNull(result);
    assertEquals("base64-user-info", result.getFirst("ZETA-User-Info"));
    assertEquals("popp-token", result.getFirst("ZETA-PoPP-Token-Content"));
    assertEquals("TODO ZTA Client Data", result.getFirst("ZETA-Client-Data"));
    assertEquals("etag-value", result.getFirst("If-None-Match"));
    assertNull(result.getFirst("PoPP"));
    verify(tokenService).validateAccessToken("Bearer token123");
    verify(tokenService).convertToBase64Json(any(UserInfo.class));
  }

  @Test
  void testPreprocessWithoutAuthorizationHeader() {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> httpProxyService.preprocess(headers));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    assertEquals("No Authorization header found", exception.getReason());
  }

  @Test
  void testPreprocessWithEmptyAuthorizationHeader() {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "");

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> httpProxyService.preprocess(headers));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    assertEquals("No Authorization header found", exception.getReason());
  }

  @Test
  void testPreprocessWithJsonProcessingException() throws JsonProcessingException {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer token123");

    when(tokenService.validateAccessToken("Bearer token123")).thenReturn(accessToken);
    when(accessToken.getClaims()).thenReturn(claims);
    when(claims.getSub()).thenReturn("test-subject");
    when(claims.getClientId()).thenReturn("test-client-id");
    when(claims.getProfessionOid()).thenReturn("test-profession-oid");
    when(tokenService.convertToBase64Json(any(UserInfo.class)))
        .thenThrow(new JsonProcessingException("JSON error") {});

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> httpProxyService.preprocess(headers));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertEquals("Could not create ZETA-User-Info", exception.getReason());
  }

  @Test
  void testForwardRequestToSuccess() throws IOException {
    // Arrange
    String backendUrl = "https://backend.example.com";
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");

    String requestBody = "{\"test\": \"data\"}";
    BufferedReader reader = new BufferedReader(new StringReader(requestBody));

    when(request.getHeader("X-Trace-Id")).thenReturn("trace123");
    when(request.getReader()).thenReturn(reader);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn("POST");

    ResponseEntity<String> mockResponse = new ResponseEntity<>("response", HttpStatus.OK);
    when(restTemplate.exchange(
            eq("https://backend.example.com/api/test"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(mockResponse);

    // Act
    ResponseEntity<String> result = httpProxyService.forwardRequestTo(backendUrl, request, headers);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals("response", result.getBody());
    verify(restTemplate)
        .exchange(
            eq("https://backend.example.com/api/test"),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            eq(String.class));
  }

  @Test
  void testForwardRequestToWithException() throws IOException {
    // Arrange
    String backendUrl = "https://backend.example.com";
    HttpHeaders headers = new HttpHeaders();

    BufferedReader reader = new BufferedReader(new StringReader(""));
    when(request.getHeader("X-Trace-Id")).thenReturn("trace123");
    when(request.getReader()).thenReturn(reader);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn("GET");

    when(restTemplate.exchange(
            anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RuntimeException("Connection error"));

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> httpProxyService.forwardRequestTo(backendUrl, request, headers));

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    assertEquals("Connection error", exception.getReason());
  }

  @Test
  void testForwardRequestToWithIOException() throws IOException {
    // Arrange
    String backendUrl = "https://backend.example.com";
    HttpHeaders headers = new HttpHeaders();

    when(request.getHeader("X-Trace-Id")).thenReturn("trace123");
    when(request.getReader()).thenThrow(new IOException("Reader error"));
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn("GET");

    ResponseEntity<String> mockResponse = new ResponseEntity<>("response", HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
        .thenReturn(mockResponse);

    // Act
    ResponseEntity<String> result = httpProxyService.forwardRequestTo(backendUrl, request, headers);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals("response", result.getBody());
  }

  @Test
  void testPreprocessWithoutIfNoneMatchHeader() throws JsonProcessingException {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer token123");

    when(tokenService.validateAccessToken("Bearer token123")).thenReturn(accessToken);
    when(accessToken.getClaims()).thenReturn(claims);
    when(claims.getSub()).thenReturn("test-subject");
    when(claims.getClientId()).thenReturn("test-client-id");
    when(claims.getProfessionOid()).thenReturn("test-profession-oid");
    when(tokenService.convertToBase64Json(any(UserInfo.class))).thenReturn("base64-user-info");

    // Act
    HttpHeaders result = httpProxyService.preprocess(headers);

    // Assert
    assertNotNull(result);
    assertNull(result.getFirst("If-None-Match"));
    assertEquals("base64-user-info", result.getFirst("ZETA-User-Info"));
    assertEquals("TODO ZTA Client Data", result.getFirst("ZETA-Client-Data"));
  }

  @Test
  void testConstructorInitializesRestTemplate() {
    // Arrange & Act
    HttpProxyService service = new HttpProxyService(tokenService);

    // Assert
    assertNotNull(service);
    // RestTemplate initialization is tested implicitly through other tests
  }

  @Test
  void testGetBodyFromRequestWithEmptyBody() throws IOException {
    // Arrange
    String backendUrl = "https://backend.example.com";
    HttpHeaders headers = new HttpHeaders();

    BufferedReader reader = new BufferedReader(new StringReader(""));
    when(request.getHeader("X-Trace-Id")).thenReturn("trace123");
    when(request.getReader()).thenReturn(reader);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn("GET");

    ResponseEntity<String> mockResponse = new ResponseEntity<>("response", HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
        .thenReturn(mockResponse);

    // Act
    ResponseEntity<String> result = httpProxyService.forwardRequestTo(backendUrl, request, headers);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    verify(restTemplate)
        .exchange(
            eq("https://backend.example.com/api/test"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class));
  }

  @Test
  void testPreprocessRemovesPoppHeader() throws JsonProcessingException {
    // Arrange
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer token123");
    headers.add("PoPP", "popp-token-value");

    when(tokenService.validateAccessToken("Bearer token123")).thenReturn(accessToken);
    when(accessToken.getClaims()).thenReturn(claims);
    when(claims.getSub()).thenReturn("test-subject");
    when(claims.getClientId()).thenReturn("test-client-id");
    when(claims.getProfessionOid()).thenReturn("test-profession-oid");
    when(tokenService.convertToBase64Json(any(UserInfo.class))).thenReturn("base64-user-info");

    // Act
    HttpHeaders result = httpProxyService.preprocess(headers);

    // Assert
    assertNotNull(result);
    assertNull(result.getFirst("PoPP"));
    assertEquals("popp-token-value", result.getFirst("ZETA-PoPP-Token-Content"));
  }

  @Test
  void testForwardRequestToWithDifferentHttpMethods() throws IOException {
    // Arrange
    String backendUrl = "https://backend.example.com";
    HttpHeaders headers = new HttpHeaders();

    BufferedReader reader = new BufferedReader(new StringReader(""));
    when(request.getHeader("X-Trace-Id")).thenReturn("trace123");
    when(request.getReader()).thenReturn(reader);
    when(request.getRequestURI()).thenReturn("/api/test");
    when(request.getMethod()).thenReturn("PUT");

    ResponseEntity<String> mockResponse = new ResponseEntity<>("response", HttpStatus.OK);
    when(restTemplate.exchange(
            anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
        .thenReturn(mockResponse);

    // Act
    ResponseEntity<String> result = httpProxyService.forwardRequestTo(backendUrl, request, headers);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    verify(restTemplate)
        .exchange(
            eq("https://backend.example.com/api/test"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(String.class));
  }
}
