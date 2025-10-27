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
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.server.config.WellKnownConfig;
import de.gematik.ti20.simsvc.server.model.WellKnown;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WellKnownServiceTest {

  @Mock private WellKnownConfig wellKnownConfig;

  @Mock private ResourceLoader resourceLoader;

  @Mock private HttpServletRequest request;

  @Mock private Resource resource;

  @Mock private ObjectMapper objectMapper;

  private WellKnownService wellKnownService;

  @BeforeEach
  void setUp() {
    wellKnownService = new WellKnownService(wellKnownConfig, resourceLoader);
  }

  @Test
  void testGetWellKnownSuccess() throws IOException {
    // Arrange
    String jsonContent = "{\"issuer\":\"test-issuer\",\"authorization_endpoint\":\"test-auth\"}";
    InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

    WellKnown wellKnown = new WellKnown();
    wellKnown.setIssuer("original-issuer");
    wellKnown.setAuthorization_endpoint("original-auth");

    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(wellKnownConfig.getIssuer()).thenReturn("config-issuer");
    when(wellKnownConfig.getToken_ep()).thenReturn("config-token-ep");
    when(wellKnownConfig.getAuth_ep()).thenReturn("config-auth-ep");
    when(wellKnownConfig.getNonce_ep()).thenReturn("config-nonce-ep");

    // Act
    String result = wellKnownService.getWellKnown(request);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains("config-issuer"));
    assertTrue(result.contains("config-token-ep"));
    assertTrue(result.contains("config-auth-ep"));
    assertTrue(result.contains("config-nonce-ep"));
    verify(resourceLoader).getResource("classpath:well-known.json");
    verify(wellKnownConfig).getIssuer();
    verify(wellKnownConfig).getToken_ep();
    verify(wellKnownConfig).getAuth_ep();
    verify(wellKnownConfig).getNonce_ep();
  }

  @Test
  void testGetWellKnownWithIOExceptionOnResourceLoad() throws IOException {
    // Arrange
    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(new IOException("Resource not found"));
    when(resource.getURI()).thenReturn(URI.create("classpath:well-known.json"));

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> wellKnownService.getWellKnown(request));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertEquals("Failed to load well-known configuration", exception.getReason());
    verify(resourceLoader).getResource("classpath:well-known.json");
  }

  @Test
  void testGetWellKnownWithIOExceptionOnResourceLoadAndURIError() throws IOException {
    // Arrange
    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(new IOException("Resource not found"));
    when(resource.getURI()).thenThrow(new IOException("URI error"));

    // Act & Assert
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> wellKnownService.getWellKnown(request));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    assertEquals("Failed to load well-known configuration", exception.getReason());
    verify(resourceLoader).getResource("classpath:well-known.json");
  }

  @Test
  void testGetWellKnownWithJsonProcessingException() throws IOException {
    // Arrange
    String jsonContent = "{\"issuer\":\"test-issuer\"}";
    InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(wellKnownConfig.getIssuer()).thenReturn("config-issuer");
    when(wellKnownConfig.getToken_ep()).thenReturn("config-token-ep");
    when(wellKnownConfig.getAuth_ep()).thenReturn("config-auth-ep");
    when(wellKnownConfig.getNonce_ep()).thenReturn("config-nonce-ep");

    // Use reflection to mock ObjectMapper
    try {
      var field = WellKnownService.class.getDeclaredField("objectMapper");
      field.setAccessible(true);
      field.set(wellKnownService, objectMapper);

      when(objectMapper.readValue(any(InputStream.class), eq(WellKnown.class)))
          .thenReturn(new WellKnown());
      when(objectMapper.writeValueAsString(any(WellKnown.class)))
          .thenThrow(new JsonProcessingException("JSON error") {});

      // Act & Assert
      ResponseStatusException exception =
          assertThrows(ResponseStatusException.class, () -> wellKnownService.getWellKnown(request));

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
      verify(objectMapper).readValue(any(InputStream.class), eq(WellKnown.class));
      verify(objectMapper).writeValueAsString(any(WellKnown.class));
    } catch (Exception e) {
      fail("Reflection failed: " + e.getMessage());
    }
  }

  @Test
  void testGetWellKnownWithCompleteWellKnownObject() throws IOException {
    // Arrange
    String jsonContent =
        """
        {
          "issuer": "original-issuer",
          "authorization_endpoint": "original-auth",
          "token_endpoint": "original-token",
          "jwks_uri": "original-jwks",
          "nonce_endpoint": "original-nonce",
          "scopes_supported": ["openid", "profile"]
        }
        """;
    InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(wellKnownConfig.getIssuer()).thenReturn("config-issuer");
    when(wellKnownConfig.getToken_ep()).thenReturn("config-token-ep");
    when(wellKnownConfig.getAuth_ep()).thenReturn("config-auth-ep");
    when(wellKnownConfig.getNonce_ep()).thenReturn("config-nonce-ep");

    // Act
    String result = wellKnownService.getWellKnown(request);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains("config-issuer"));
    assertTrue(result.contains("config-token-ep"));
    assertTrue(result.contains("config-auth-ep"));
    assertTrue(result.contains("config-nonce-ep"));
    assertTrue(result.contains("original-jwks"));
    assertTrue(result.contains("scopes_supported"));
  }

  @Test
  void testGetWellKnownWithNullConfigValues() throws IOException {
    // Arrange
    String jsonContent = "{\"issuer\":\"test-issuer\"}";
    InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(wellKnownConfig.getIssuer()).thenReturn(null);
    when(wellKnownConfig.getToken_ep()).thenReturn(null);
    when(wellKnownConfig.getAuth_ep()).thenReturn(null);
    when(wellKnownConfig.getNonce_ep()).thenReturn(null);

    // Act
    String result = wellKnownService.getWellKnown(request);

    // Assert
    assertNotNull(result);
    verify(wellKnownConfig).getIssuer();
    verify(wellKnownConfig).getToken_ep();
    verify(wellKnownConfig).getAuth_ep();
    verify(wellKnownConfig).getNonce_ep();
  }

  @Test
  void testGetWellKnownWithEmptyJsonFile() throws IOException {
    // Arrange
    String jsonContent = "{}";
    InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(wellKnownConfig.getIssuer()).thenReturn("config-issuer");
    when(wellKnownConfig.getToken_ep()).thenReturn("config-token-ep");
    when(wellKnownConfig.getAuth_ep()).thenReturn("config-auth-ep");
    when(wellKnownConfig.getNonce_ep()).thenReturn("config-nonce-ep");

    // Act
    String result = wellKnownService.getWellKnown(request);

    // Assert
    assertNotNull(result);
    assertTrue(result.contains("config-issuer"));
    assertTrue(result.contains("config-token-ep"));
    assertTrue(result.contains("config-auth-ep"));
    assertTrue(result.contains("config-nonce-ep"));
  }

  @Test
  void testWellKnownFileConstant() throws NoSuchFieldException, IllegalAccessException {
    // Arrange
    var field = WellKnownService.class.getDeclaredField("WELL_KNOWN_FILE");
    field.setAccessible(true);

    // Act
    String value = (String) field.get(null);

    // Assert
    assertEquals("well-known.json", value);
  }

  @Test
  void testGetWellKnownCallsCorrectResource() throws IOException {
    // Arrange
    String jsonContent = "{}";
    InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes());

    when(resourceLoader.getResource("classpath:well-known.json")).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(wellKnownConfig.getIssuer()).thenReturn("issuer");
    when(wellKnownConfig.getToken_ep()).thenReturn("token");
    when(wellKnownConfig.getAuth_ep()).thenReturn("auth");
    when(wellKnownConfig.getNonce_ep()).thenReturn("nonce");

    // Act
    wellKnownService.getWellKnown(request);

    // Assert
    verify(resourceLoader).getResource("classpath:well-known.json");
    verify(resource).getInputStream();
  }
}
