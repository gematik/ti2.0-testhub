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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ExtendWith(MockitoExtension.class)
class OpenApiControllerTest {

  private OpenApiController openApiController;

  @BeforeEach
  void setUp() {
    openApiController = new OpenApiController();
  }

  @Test
  void testConstructor() {
    assertNotNull(openApiController);
  }

  @Test
  void testGetOpenApiYaml() {
    // Act
    ResponseEntity<Resource> response = openApiController.getOpenApiYaml();

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody() instanceof ClassPathResource);

    ClassPathResource resource = (ClassPathResource) response.getBody();
    assertEquals("openapi.yaml", resource.getPath());
  }

  @Test
  void testGetOpenApiYamlReturnsClassPathResource() {
    // Act
    ResponseEntity<Resource> response = openApiController.getOpenApiYaml();

    // Assert
    Resource resource = response.getBody();
    assertNotNull(resource);
    assertTrue(resource instanceof ClassPathResource);
    assertEquals("openapi.yaml", ((ClassPathResource) resource).getPath());
  }

  @Test
  void testGetOpenApiYamlResponseStatus() {
    // Act
    ResponseEntity<Resource> response = openApiController.getOpenApiYaml();

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getHeaders());
  }

  @Test
  void testControllerAnnotation() {
    assertTrue(OpenApiController.class.isAnnotationPresent(Controller.class));
  }

  @Test
  void testGetMappingAnnotation() throws NoSuchMethodException {
    var method = OpenApiController.class.getDeclaredMethod("getOpenApiYaml");
    GetMapping annotation = method.getAnnotation(GetMapping.class);

    assertNotNull(annotation);
    assertEquals("/openapi.yaml", annotation.value()[0]);
    assertEquals("application/x-yaml", annotation.produces()[0]);
  }

  @Test
  void testResponseBodyAnnotation() throws NoSuchMethodException {
    var method = OpenApiController.class.getDeclaredMethod("getOpenApiYaml");

    assertTrue(method.isAnnotationPresent(ResponseBody.class));
  }

  @Test
  void testMethodIsPublic() throws NoSuchMethodException {
    var method = OpenApiController.class.getDeclaredMethod("getOpenApiYaml");

    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
  }

  @Test
  void testMethodReturnType() throws NoSuchMethodException {
    var method = OpenApiController.class.getDeclaredMethod("getOpenApiYaml");

    assertEquals(ResponseEntity.class, method.getReturnType());
  }

  @Test
  void testMethodParameterCount() throws NoSuchMethodException {
    var method = OpenApiController.class.getDeclaredMethod("getOpenApiYaml");

    assertEquals(0, method.getParameterCount());
  }

  @Test
  void testMultipleCallsReturnNewInstances() {
    // Act
    ResponseEntity<Resource> response1 = openApiController.getOpenApiYaml();
    ResponseEntity<Resource> response2 = openApiController.getOpenApiYaml();

    // Assert
    assertNotSame(response1, response2);
    assertNotSame(response1.getBody(), response2.getBody());

    // But both should point to the same resource path
    ClassPathResource resource1 = (ClassPathResource) response1.getBody();
    ClassPathResource resource2 = (ClassPathResource) response2.getBody();
    assertEquals(resource1.getPath(), resource2.getPath());
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.controller", OpenApiController.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(OpenApiController.class.getModifiers()));
  }

  @Test
  void testClassIsNotFinal() {
    assertFalse(java.lang.reflect.Modifier.isFinal(OpenApiController.class.getModifiers()));
  }

  @Test
  void testResourcePathIsCorrect() {
    // Act
    ResponseEntity<Resource> response = openApiController.getOpenApiYaml();

    // Assert
    ClassPathResource resource = (ClassPathResource) response.getBody();
    assertEquals("openapi.yaml", resource.getPath());
    assertEquals("openapi.yaml", resource.getFilename());
  }
}
