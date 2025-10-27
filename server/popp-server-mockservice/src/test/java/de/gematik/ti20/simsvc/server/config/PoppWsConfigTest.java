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
package de.gematik.ti20.simsvc.server.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.controller.SmartcardController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@ExtendWith(MockitoExtension.class)
class PoppWsConfigTest {

  @Mock private SmartcardController smartcardController;

  @Mock private WebSocketHandlerRegistry registry;

  private PoppWsConfig poppWsConfig;

  @BeforeEach
  void setUp() {
    poppWsConfig = new PoppWsConfig(smartcardController);
  }

  @Test
  void testConstructorInitialization() {
    // Arrange & Act
    PoppWsConfig config = new PoppWsConfig(smartcardController);

    // Assert
    assertNotNull(config);
  }

  @Test
  void testRegisterWebSocketHandlers() {
    // Act
    poppWsConfig.registerWebSocketHandlers(registry);

    // Assert
    verify(smartcardController).addHandlers(registry);
  }

  @Test
  void testImplementsWebSocketConfigurer() {
    // Assert
    assertTrue(poppWsConfig instanceof WebSocketConfigurer);
  }

  @Test
  void testClassAnnotations() {
    // Assert
    assertTrue(PoppWsConfig.class.isAnnotationPresent(Configuration.class));
    assertTrue(PoppWsConfig.class.isAnnotationPresent(EnableWebSocket.class));
  }

  @Test
  void testPackageStructure() {
    // Assert
    assertEquals("de.gematik.ti20.simsvc.server.config", PoppWsConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    // Assert
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppWsConfig.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    // Assert
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            PoppWsConfig.class.getDeclaredField("smartcardController").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isFinal(
            PoppWsConfig.class.getDeclaredField("smartcardController").getModifiers()));
  }

  @Test
  void testConstructorCount() {
    // Assert
    assertEquals(1, PoppWsConfig.class.getDeclaredConstructors().length);
  }

  @Test
  void testFieldCount() {
    // Assert
    assertEquals(1, PoppWsConfig.class.getDeclaredFields().length);
  }

  @Test
  void testRegisterWebSocketHandlersIsPublic() throws NoSuchMethodException {
    // Act
    var method =
        PoppWsConfig.class.getDeclaredMethod(
            "registerWebSocketHandlers", WebSocketHandlerRegistry.class);

    // Assert
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
  }

  @Test
  void testRegisterWebSocketHandlersWithNullRegistry() {
    // Act & Assert
    assertDoesNotThrow(() -> poppWsConfig.registerWebSocketHandlers(null));
    verify(smartcardController).addHandlers(null);
  }

  @Test
  void testMultipleCallsToRegisterWebSocketHandlers() {
    // Act
    poppWsConfig.registerWebSocketHandlers(registry);
    poppWsConfig.registerWebSocketHandlers(registry);

    // Assert
    verify(smartcardController, times(2)).addHandlers(registry);
  }

  @Test
  void testFieldType() throws NoSuchFieldException {
    // Act
    var field = PoppWsConfig.class.getDeclaredField("smartcardController");

    // Assert
    assertEquals(SmartcardController.class, field.getType());
  }

  @Test
  void testConstructorParameterType() {
    // Act
    var constructors = PoppWsConfig.class.getDeclaredConstructors();
    var constructor = constructors[0];
    var parameterTypes = constructor.getParameterTypes();

    // Assert
    assertEquals(1, parameterTypes.length);
    assertEquals(SmartcardController.class, parameterTypes[0]);
  }

  @Test
  void testWebSocketConfigurerInterface() {
    // Assert
    var interfaces = PoppWsConfig.class.getInterfaces();
    assertEquals(1, interfaces.length);
    assertEquals(WebSocketConfigurer.class, interfaces[0]);
  }
}
