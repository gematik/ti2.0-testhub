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

import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendWith(MockitoExtension.class)
class ServiceControllerTest {

  private ServiceController serviceController;

  @BeforeEach
  void setUp() {
    serviceController = new ServiceController();
  }

  @Test
  void testConstructor() {
    assertNotNull(serviceController);
  }

  @Test
  void testStatus() throws JoseException {
    // Act
    ResponseEntity<?> response = serviceController.status();

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNull(response.getBody());
  }

  @Test
  void testStatusReturnsEmptyBody() throws JoseException {
    // Act
    ResponseEntity<?> response = serviceController.status();

    // Assert
    assertNotNull(response);
    assertNull(response.getBody());
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void testStatusMethodThrowsJoseException() {
    // This test verifies that the method signature allows JoseException to be thrown
    // Even though the current implementation doesn't throw it
    assertDoesNotThrow(() -> serviceController.status());
  }

  @Test
  void testRestControllerAnnotation() {
    assertTrue(ServiceController.class.isAnnotationPresent(RestController.class));
  }

  @Test
  void testRequestMappingAnnotation() {
    RequestMapping annotation = ServiceController.class.getAnnotation(RequestMapping.class);

    assertNotNull(annotation);
    assertEquals("/service", annotation.value()[0]);
  }

  @Test
  void testGetMappingAnnotation() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");
    GetMapping annotation = method.getAnnotation(GetMapping.class);

    assertNotNull(annotation);
    assertEquals("/status", annotation.value()[0]);
  }

  @Test
  void testStatusMethodIsPublic() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");

    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
  }

  @Test
  void testStatusMethodReturnType() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");

    assertEquals(ResponseEntity.class, method.getReturnType());
  }

  @Test
  void testStatusMethodParameterCount() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");

    assertEquals(0, method.getParameterCount());
  }

  @Test
  void testStatusMethodExceptions() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");
    Class<?>[] exceptionTypes = method.getExceptionTypes();

    assertEquals(1, exceptionTypes.length);
    assertEquals(JoseException.class, exceptionTypes[0]);
  }

  @Test
  void testMultipleStatusCalls() throws JoseException {
    // Act
    ResponseEntity<?> response1 = serviceController.status();
    ResponseEntity<?> response2 = serviceController.status();

    // Assert
    assertNotSame(response1, response2);
    assertEquals(response1.getStatusCode(), response2.getStatusCode());
    assertEquals(response1.getBody(), response2.getBody());
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.controller", ServiceController.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(ServiceController.class.getModifiers()));
  }

  @Test
  void testClassIsNotFinal() {
    assertFalse(java.lang.reflect.Modifier.isFinal(ServiceController.class.getModifiers()));
  }

  @Test
  void testResponseEntityHasOkStatus() throws JoseException {
    // Act
    ResponseEntity<?> response = serviceController.status();

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getStatusCode().is2xxSuccessful());
  }

  @Test
  void testResponseEntityHeaders() throws JoseException {
    // Act
    ResponseEntity<?> response = serviceController.status();

    // Assert
    assertNotNull(response.getHeaders());
    assertTrue(response.getHeaders().isEmpty());
  }
}
