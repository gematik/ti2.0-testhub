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

import java.util.Arrays;
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
  void testNoArgsConstructor() {
    assertNotNull(serviceController);
  }

  @Test
  void testStatusEndpoint() throws JoseException {
    ResponseEntity<?> result = serviceController.status();

    assertNotNull(result);
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNull(result.getBody());
  }

  @Test
  void testStatusEndpointMultipleCalls() throws JoseException {
    ResponseEntity<?> result1 = serviceController.status();
    ResponseEntity<?> result2 = serviceController.status();

    assertNotNull(result1);
    assertNotNull(result2);
    assertEquals(HttpStatus.OK, result1.getStatusCode());
    assertEquals(HttpStatus.OK, result2.getStatusCode());
    assertNull(result1.getBody());
    assertNull(result2.getBody());
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
  void testStatusMethodGetMapping() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");
    GetMapping annotation = method.getAnnotation(GetMapping.class);

    assertNotNull(annotation);
    assertEquals("/status", annotation.value()[0]);
  }

  @Test
  void testStatusMethodThrowsJoseException() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");
    var exceptionTypes = method.getExceptionTypes();

    assertEquals(1, exceptionTypes.length);
    assertEquals(JoseException.class, exceptionTypes[0]);
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
  void testStatusMethodHasNoParameters() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");
    assertEquals(0, method.getParameterCount());
  }

  @Test
  void testClassHasNoFields() {
    assertEquals(0, ServiceController.class.getDeclaredFields().length);
  }

  @Test
  void testConstructorIsPublic() throws NoSuchMethodException {
    var constructor = ServiceController.class.getDeclaredConstructor();
    assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
  }

  @Test
  void testStatusResponseHasNoHeaders() throws JoseException {
    ResponseEntity<?> result = serviceController.status();

    assertTrue(result.getHeaders().isEmpty());
  }

  @Test
  void testStatusResponseHasNoContentType() throws JoseException {
    ResponseEntity<?> result = serviceController.status();

    assertNull(result.getHeaders().getContentType());
  }

  @Test
  void testStatusResponseHasNoContentLength() throws JoseException {
    ResponseEntity<?> result = serviceController.status();

    assertEquals(-1, result.getHeaders().getContentLength());
  }

  @Test
  void testMethodCount() {
    var methods =
        Arrays.stream(ServiceController.class.getDeclaredMethods())
            .filter(m -> !m.getName().startsWith("$")) // To filter out Jacoco code.
            .toArray(java.lang.reflect.Method[]::new);

    // Only the status method should be present
    assertEquals(1, methods.length);
    assertEquals("status", methods[0].getName());
  }

  @Test
  void testStatusMethodSignature() throws NoSuchMethodException {
    var method = ServiceController.class.getDeclaredMethod("status");

    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    assertEquals(ResponseEntity.class, method.getReturnType());
    assertEquals(0, method.getParameterCount());
    assertEquals(1, method.getExceptionTypes().length);
    assertEquals(JoseException.class, method.getExceptionTypes()[0]);
  }

  @Test
  void testStatusEndpointReturnsOkWithEmptyBody() throws JoseException {
    ResponseEntity<?> result = serviceController.status();

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertNull(result.getBody());
    assertTrue(result.getHeaders().isEmpty());
  }
}
