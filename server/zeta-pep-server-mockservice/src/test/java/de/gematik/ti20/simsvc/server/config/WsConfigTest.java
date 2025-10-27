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

import de.gematik.ti20.simsvc.server.controller.WsProxyController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@ExtendWith(MockitoExtension.class)
class WsConfigTest {

  @Mock private HttpConfig httpConfig;

  @Mock private WsProxyController wsProxyController;

  @Mock private WebSocketHandlerRegistry registry;

  private WsConfig wsConfig;

  @BeforeEach
  void setUp() {
    when(httpConfig.getUrl()).thenReturn("https://example.com/ws");
    wsConfig = new WsConfig(httpConfig, wsProxyController);
  }

  @Test
  void testConstructorWithValidParameters() {
    assertNotNull(wsConfig);
    verify(httpConfig).getUrl();
  }

  @Test
  void testRegisterWebSocketHandlers() {
    wsConfig.registerWebSocketHandlers(registry);

    verify(wsProxyController).addHandlers(registry);
  }

  @Test
  void testRegisterWebSocketHandlersWithNullRegistry() {
    wsConfig.registerWebSocketHandlers(null);

    verify(wsProxyController).addHandlers(null);
  }

  @Test
  void testConfigurationAnnotation() {
    assertTrue(WsConfig.class.isAnnotationPresent(Configuration.class));
  }

  @Test
  void testEnableWebSocketAnnotation() {
    assertTrue(WsConfig.class.isAnnotationPresent(EnableWebSocket.class));
  }

  @Test
  void testOrderAnnotation() {
    Order annotation = WsConfig.class.getAnnotation(Order.class);

    assertNotNull(annotation);
    assertEquals(1, annotation.value());
  }

  @Test
  void testImplementsWebSocketConfigurer() {
    assertTrue(WebSocketConfigurer.class.isAssignableFrom(WsConfig.class));
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.config", WsConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(WsConfig.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WsConfig.class.getDeclaredField("targetUrl").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WsConfig.class.getDeclaredField("wsProxyController").getModifiers()));
  }

  @Test
  void testFieldsAreFinal() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isFinal(
            WsConfig.class.getDeclaredField("targetUrl").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isFinal(
            WsConfig.class.getDeclaredField("wsProxyController").getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, WsConfig.class.getDeclaredField("targetUrl").getType());
    assertEquals(
        WsProxyController.class, WsConfig.class.getDeclaredField("wsProxyController").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(2, WsConfig.class.getDeclaredFields().length);
  }

  @Test
  void testConstructorIsPublic() throws NoSuchMethodException {
    var constructor =
        WsConfig.class.getDeclaredConstructor(HttpConfig.class, WsProxyController.class);
    assertTrue(java.lang.reflect.Modifier.isPublic(constructor.getModifiers()));
  }

  @Test
  void testRegisterWebSocketHandlersMethodSignature() throws NoSuchMethodException {
    var method =
        WsConfig.class.getDeclaredMethod(
            "registerWebSocketHandlers", WebSocketHandlerRegistry.class);
    assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
    assertEquals(void.class, method.getReturnType());
  }

  @Test
  void testMultipleCallsToRegisterWebSocketHandlers() {
    wsConfig.registerWebSocketHandlers(registry);
    wsConfig.registerWebSocketHandlers(registry);

    verify(wsProxyController, times(2)).addHandlers(registry);
  }

  @Test
  void testTargetUrlFieldInitialization() throws NoSuchFieldException, IllegalAccessException {
    var targetUrlField = WsConfig.class.getDeclaredField("targetUrl");
    targetUrlField.setAccessible(true);

    String targetUrl = (String) targetUrlField.get(wsConfig);

    assertEquals("https://example.com/ws", targetUrl);
  }

  @Test
  void testWsProxyControllerFieldInitialization()
      throws NoSuchFieldException, IllegalAccessException {
    var wsProxyControllerField = WsConfig.class.getDeclaredField("wsProxyController");
    wsProxyControllerField.setAccessible(true);

    WsProxyController controller = (WsProxyController) wsProxyControllerField.get(wsConfig);

    assertEquals(wsProxyController, controller);
  }
}
