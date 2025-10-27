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

import de.gematik.ti20.simsvc.server.service.WellKnownService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ExtendWith(MockitoExtension.class)
class WellKnownControllerTest {

  @Mock private WellKnownService wellKnownService;

  @Mock private HttpServletRequest request;

  private WellKnownController wellKnownController;

  @BeforeEach
  void setUp() {
    wellKnownController = new WellKnownController(wellKnownService);
  }

  @Test
  void testConstructorWithValidParameters() {
    assertNotNull(wellKnownController);
  }

  @Test
  void testGetWellKnown() throws IOException {
    // Arrange
    String expectedResponse =
        "{\"issuer\":\"https://example.com\",\"auth_endpoint\":\"https://example.com/auth\"}";
    when(wellKnownService.getWellKnown(request)).thenReturn(expectedResponse);

    // Act
    ResponseEntity<String> result = wellKnownController.getWellKnown(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(expectedResponse, result.getBody());
    verify(wellKnownService).getWellKnown(request);
  }

  @Test
  void testGetWellKnownWithEmptyResponse() throws IOException {
    // Arrange
    String expectedResponse = "";
    when(wellKnownService.getWellKnown(request)).thenReturn(expectedResponse);

    // Act
    ResponseEntity<String> result = wellKnownController.getWellKnown(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(expectedResponse, result.getBody());
    verify(wellKnownService).getWellKnown(request);
  }

  @Test
  void testGetWellKnownWithNullResponse() throws IOException {
    // Arrange
    when(wellKnownService.getWellKnown(request)).thenReturn(null);

    // Act
    ResponseEntity<String> result = wellKnownController.getWellKnown(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNull(result.getBody());
    verify(wellKnownService).getWellKnown(request);
  }

  @Test
  void testGetWellKnownWithJsonResponse() throws IOException {
    // Arrange
    String jsonResponse =
        "{\"issuer\":\"https://test.com\",\"authorization_endpoint\":\"https://test.com/oauth/authorize\",\"token_endpoint\":\"https://test.com/oauth/token\"}";
    when(wellKnownService.getWellKnown(request)).thenReturn(jsonResponse);

    // Act
    ResponseEntity<String> result = wellKnownController.getWellKnown(request);

    // Assert
    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(jsonResponse, result.getBody());
    verify(wellKnownService).getWellKnown(request);
  }

  @Test
  void testGetWellKnownMultipleCalls() throws IOException {
    // Arrange
    String response1 = "{\"issuer\":\"https://first.com\"}";
    String response2 = "{\"issuer\":\"https://second.com\"}";
    when(wellKnownService.getWellKnown(request)).thenReturn(response1).thenReturn(response2);

    // Act
    ResponseEntity<String> result1 = wellKnownController.getWellKnown(request);
    ResponseEntity<String> result2 = wellKnownController.getWellKnown(request);

    // Assert
    assertNotNull(result1);
    assertNotNull(result2);
    assertEquals(HttpStatus.OK, result1.getStatusCode());
    assertEquals(HttpStatus.OK, result2.getStatusCode());
    assertEquals(response1, result1.getBody());
    assertEquals(response2, result2.getBody());
    verify(wellKnownService, times(2)).getWellKnown(request);
  }

  @Test
  void testControllerAnnotation() {
    assertTrue(WellKnownController.class.isAnnotationPresent(Controller.class));
  }

  @Test
  void testGetWellKnownMethodMapping() throws NoSuchMethodException {
    var method =
        WellKnownController.class.getDeclaredMethod("getWellKnown", HttpServletRequest.class);
    GetMapping annotation = method.getAnnotation(GetMapping.class);

    assertNotNull(annotation);
    assertEquals("/.well-known/oauth-protected-resource", annotation.value()[0]);
    assertEquals("application/json", annotation.produces()[0]);
  }

  @Test
  void testGetWellKnownMethodResponseBody() throws NoSuchMethodException {
    var method =
        WellKnownController.class.getDeclaredMethod("getWellKnown", HttpServletRequest.class);
    assertTrue(method.isAnnotationPresent(ResponseBody.class));
  }

  @Test
  void testGetWellKnownMethodThrowsIOException() throws NoSuchMethodException {
    var method =
        WellKnownController.class.getDeclaredMethod("getWellKnown", HttpServletRequest.class);
    var exceptionTypes = method.getExceptionTypes();

    assertEquals(1, exceptionTypes.length);
    assertEquals(IOException.class, exceptionTypes[0]);
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.controller",
        WellKnownController.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(WellKnownController.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivateAndFinal() throws NoSuchFieldException {
    var wellKnownServiceField = WellKnownController.class.getDeclaredField("wellKnownService");

    assertTrue(java.lang.reflect.Modifier.isPrivate(wellKnownServiceField.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isFinal(wellKnownServiceField.getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(
        WellKnownService.class,
        WellKnownController.class.getDeclaredField("wellKnownService").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(1, WellKnownController.class.getDeclaredFields().length);
  }

  @Test
  void testConstructorIsPublic() throws NoSuchMethodException {
    var constructor = WellKnownController.class.getDeclaredConstructor(WellKnownService.class);
    assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
  }

  @Test
  void testConstructorParameterHasAutowiredAnnotation() throws NoSuchMethodException {
    var constructor = WellKnownController.class.getDeclaredConstructor(WellKnownService.class);
    var parameterAnnotations = constructor.getParameterAnnotations();

    assertEquals(1, parameterAnnotations.length);
    assertEquals(1, parameterAnnotations[0].length);
    assertTrue(parameterAnnotations[0][0] instanceof Autowired);
  }

  @Test
  void testGetWellKnownMethodIsPublic() throws NoSuchMethodException {
    var method =
        WellKnownController.class.getDeclaredMethod("getWellKnown", HttpServletRequest.class);
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
  }

  @Test
  void testGetWellKnownReturnType() throws NoSuchMethodException {
    var method =
        WellKnownController.class.getDeclaredMethod("getWellKnown", HttpServletRequest.class);
    assertEquals(ResponseEntity.class, method.getReturnType());
  }

  @Test
  void testGetWellKnownMethodHasOneParameter() throws NoSuchMethodException {
    var method =
        WellKnownController.class.getDeclaredMethod("getWellKnown", HttpServletRequest.class);
    assertEquals(1, method.getParameterCount());
    assertEquals(HttpServletRequest.class, method.getParameterTypes()[0]);
  }

  @Test
  void testMethodCount() {
    var methods =
        Arrays.stream(WellKnownController.class.getDeclaredMethods())
            .filter(m -> !m.getName().startsWith("$")) // To filter out Jacoco code.
            .toArray(java.lang.reflect.Method[]::new);

    // Only the getWellKnown method should be present
    assertEquals(1, methods.length);
    assertEquals("getWellKnown", methods[0].getName());
  }

  @Test
  void testGetWellKnownMethodSignature() throws NoSuchMethodException {
    var method =
        WellKnownController.class.getDeclaredMethod("getWellKnown", HttpServletRequest.class);

    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    assertEquals(ResponseEntity.class, method.getReturnType());
    assertEquals(1, method.getParameterCount());
    assertEquals(1, method.getExceptionTypes().length);
    assertEquals(IOException.class, method.getExceptionTypes()[0]);
  }
}
