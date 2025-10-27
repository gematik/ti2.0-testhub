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
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.config.HttpConfig;
import de.gematik.ti20.simsvc.server.service.HttpProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class HttpProxyControllerTest {

  @Mock private HttpConfig httpConfig;

  @Mock private HttpProxyService httpProxyService;

  @Mock private HttpServletRequest request;

  private HttpProxyController httpProxyController;

  @BeforeEach
  void setUp() {
    httpProxyController = new HttpProxyController(httpConfig, httpProxyService);
  }

  @Test
  void testConstructorWithValidParameters() {
    assertNotNull(httpProxyController);
  }

  @Test
  void testProxyRequest() {
    // Arrange
    String targetUrl = "https://example.com/api";
    String traceId = "trace-123";
    String method = "GET";
    String requestUri = "/test";
    String responseBody = "response content";

    when(httpConfig.getUrl()).thenReturn(targetUrl);
    when(request.getHeader("X-Trace-Id")).thenReturn(traceId);
    when(request.getMethod()).thenReturn(method);
    when(request.getRequestURI()).thenReturn(requestUri);
    when(request.getHeader("Upgrade")).thenReturn(null);

    HttpHeaders additionalHeaders = new HttpHeaders();
    when(httpProxyService.preprocess(request)).thenReturn(additionalHeaders);

    ResponseEntity<String> serviceResponse =
        ResponseEntity.ok().header("Content-Type", "application/json").body(responseBody);
    when(httpProxyService.forwardRequestTo(targetUrl, request, additionalHeaders))
        .thenReturn(serviceResponse);

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(responseBody, result.getBody());
    assertTrue(result.getHeaders().containsKey("Content-Type"));
    assertEquals("application/json", result.getHeaders().getFirst("Content-Type"));

    verify(httpProxyService).preprocess(request);
    verify(httpProxyService).forwardRequestTo(targetUrl, request, additionalHeaders);
  }

  @Test
  void testProxyRequestWithWebSocketUpgrade() {
    // Arrange
    when(request.getHeader("Upgrade")).thenReturn("websocket");

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNull(result);
    verifyNoInteractions(httpProxyService);
  }

  @Test
  void testProxyRequestWithWebSocketUpgradeCaseInsensitive() {
    // Arrange
    when(request.getHeader("Upgrade")).thenReturn("WEBSOCKET");

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNull(result);
    verifyNoInteractions(httpProxyService);
  }

  @Test
  void testProxyRequestWithWebSocketUpgradeMixedCase() {
    // Arrange
    when(request.getHeader("Upgrade")).thenReturn("WebSocket");

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNull(result);
    verifyNoInteractions(httpProxyService);
  }

  @Test
  void testProxyRequestWithErrorResponse() {
    // Arrange
    String targetUrl = "https://example.com/api";
    when(httpConfig.getUrl()).thenReturn(targetUrl);
    when(request.getHeader("Upgrade")).thenReturn(null);

    HttpHeaders additionalHeaders = new HttpHeaders();
    when(httpProxyService.preprocess(request)).thenReturn(additionalHeaders);

    ResponseEntity<String> serviceResponse =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Error-Code", "500")
            .body("Internal Server Error");
    when(httpProxyService.forwardRequestTo(targetUrl, request, additionalHeaders))
        .thenReturn(serviceResponse);

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    assertEquals("Internal Server Error", result.getBody());
    assertTrue(result.getHeaders().containsKey("Error-Code"));
    assertEquals("500", result.getHeaders().getFirst("Error-Code"));
  }

  @Test
  void testProxyRequestWithNullResponseBody() {
    // Arrange
    String targetUrl = "https://example.com/api";
    when(httpConfig.getUrl()).thenReturn(targetUrl);
    when(request.getHeader("Upgrade")).thenReturn(null);

    HttpHeaders additionalHeaders = new HttpHeaders();
    when(httpProxyService.preprocess(request)).thenReturn(additionalHeaders);

    ResponseEntity<String> serviceResponse = ResponseEntity.noContent().build();
    when(httpProxyService.forwardRequestTo(targetUrl, request, additionalHeaders))
        .thenReturn(serviceResponse);

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    assertNull(result.getBody());
  }

  @Test
  void testProxyRequestWithEmptyAdditionalHeaders() {
    // Arrange
    String targetUrl = "https://example.com/api";
    when(httpConfig.getUrl()).thenReturn(targetUrl);
    when(request.getHeader("Upgrade")).thenReturn(null);

    HttpHeaders emptyHeaders = new HttpHeaders();
    when(httpProxyService.preprocess(request)).thenReturn(emptyHeaders);

    ResponseEntity<String> serviceResponse = ResponseEntity.ok("success");
    when(httpProxyService.forwardRequestTo(targetUrl, request, emptyHeaders))
        .thenReturn(serviceResponse);

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals("success", result.getBody());

    verify(httpProxyService).preprocess(request);
    verify(httpProxyService).forwardRequestTo(targetUrl, request, emptyHeaders);
  }

  @Test
  void testRestControllerAnnotation() {
    assertTrue(HttpProxyController.class.isAnnotationPresent(RestController.class));
  }

  @Test
  void testOrderAnnotation() {
    Order annotation = HttpProxyController.class.getAnnotation(Order.class);

    assertNotNull(annotation);
    assertEquals(2, annotation.value());
  }

  @Test
  void testProxyRequestMethodMapping() throws NoSuchMethodException {
    var method =
        HttpProxyController.class.getDeclaredMethod("proxyRequest", HttpServletRequest.class);
    RequestMapping annotation = method.getAnnotation(RequestMapping.class);

    assertNotNull(annotation);
    assertEquals("/**", annotation.value()[0]);
    assertEquals("!" + HttpHeaders.UPGRADE, annotation.headers()[0]);
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.controller",
        HttpProxyController.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(HttpProxyController.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivateAndFinal() throws NoSuchFieldException {
    var httpConfigField = HttpProxyController.class.getDeclaredField("httpConfig");
    var httpProxyServiceField = HttpProxyController.class.getDeclaredField("httpProxyService");

    assertTrue(java.lang.reflect.Modifier.isPrivate(httpConfigField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isFinal(httpConfigField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPrivate(httpProxyServiceField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isFinal(httpProxyServiceField.getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(
        HttpConfig.class, HttpProxyController.class.getDeclaredField("httpConfig").getType());
    assertEquals(
        HttpProxyService.class,
        HttpProxyController.class.getDeclaredField("httpProxyService").getType());
  }

  @Test
  void testConstructorIsPublic() throws NoSuchMethodException {
    var constructor =
        HttpProxyController.class.getDeclaredConstructor(HttpConfig.class, HttpProxyService.class);
    assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
  }

  @Test
  void testProxyRequestMethodIsPublic() throws NoSuchMethodException {
    var method =
        HttpProxyController.class.getDeclaredMethod("proxyRequest", HttpServletRequest.class);
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
  }

  @Test
  void testProxyRequestReturnType() throws NoSuchMethodException {
    var method =
        HttpProxyController.class.getDeclaredMethod("proxyRequest", HttpServletRequest.class);
    assertEquals(ResponseEntity.class, method.getReturnType());
  }

  @Test
  void testProxyRequestWithMultipleHeaders() {
    // Arrange
    String targetUrl = "https://example.com/api";
    when(httpConfig.getUrl()).thenReturn(targetUrl);
    when(request.getHeader("Upgrade")).thenReturn(null);

    HttpHeaders additionalHeaders = new HttpHeaders();
    additionalHeaders.add("Custom-Header", "custom-value");
    when(httpProxyService.preprocess(request)).thenReturn(additionalHeaders);

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.add("Response-Header", "response-value");
    ResponseEntity<String> serviceResponse =
        ResponseEntity.ok().headers(responseHeaders).body("response");
    when(httpProxyService.forwardRequestTo(targetUrl, request, additionalHeaders))
        .thenReturn(serviceResponse);

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals("response", result.getBody());
    assertTrue(result.getHeaders().containsKey("Response-Header"));
    assertEquals("response-value", result.getHeaders().getFirst("Response-Header"));
  }

  @Test
  void testReturnBadGatewayOnException() {
    // Arrange
    String targetUrl = "https://example.com/api";
    when(httpConfig.getUrl()).thenReturn(targetUrl);
    when(request.getHeader("Upgrade")).thenReturn(null);

    HttpHeaders additionalHeaders = new HttpHeaders();
    when(httpProxyService.preprocess(request)).thenReturn(additionalHeaders);

    when(httpProxyService.forwardRequestTo(targetUrl, request, additionalHeaders))
        .thenThrow(new RuntimeException("Service error"));

    // Act
    ResponseEntity<?> result = httpProxyController.proxyRequest(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.BAD_GATEWAY, result.getStatusCode());
    assertTrue(result.getBody().toString().contains("Error processing request: Service error"));
  }
}
