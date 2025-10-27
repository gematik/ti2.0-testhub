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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ExtendWith(MockitoExtension.class)
class PoppConfigTest {

  private PoppConfig poppConfig;

  @BeforeEach
  void setUp() {
    poppConfig = new PoppConfig();
  }

  @Test
  void testNoArgsConstructor() {
    assertNotNull(poppConfig);
    assertFalse(poppConfig.isRequired());
  }

  @Test
  void testGetterAndSetterRequired() {
    poppConfig.setRequired(true);

    assertTrue(poppConfig.isRequired());
  }

  @Test
  void testRequiredDefaultValue() {
    assertFalse(poppConfig.isRequired());
  }

  @Test
  void testSetRequiredToTrue() {
    poppConfig.setRequired(true);

    assertTrue(poppConfig.isRequired());
  }

  @Test
  void testSetRequiredToFalse() {
    poppConfig.setRequired(false);

    assertFalse(poppConfig.isRequired());
  }

  @Test
  void testMultipleSettersOnSameField() {
    poppConfig.setRequired(true);
    poppConfig.setRequired(false);

    assertFalse(poppConfig.isRequired());
  }

  @Test
  void testComponentAnnotation() {
    assertTrue(PoppConfig.class.isAnnotationPresent(Component.class));
  }

  @Test
  void testConfigurationPropertiesAnnotation() {
    ConfigurationProperties annotation =
        PoppConfig.class.getAnnotation(ConfigurationProperties.class);

    assertNotNull(annotation);
    assertEquals("proxy.popp", annotation.prefix());
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.config", PoppConfig.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(PoppConfig.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            PoppConfig.class.getDeclaredField("required").getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(boolean.class, PoppConfig.class.getDeclaredField("required").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(1, PoppConfig.class.getDeclaredFields().length);
  }

  @Test
  void testCompletePoppConfigUsage() {
    poppConfig.setRequired(true);

    assertTrue(poppConfig.isRequired());
  }

  @Test
  void testSetterReturnVoid() throws NoSuchMethodException {
    var setRequiredMethod = PoppConfig.class.getMethod("setRequired", boolean.class);

    assertEquals(void.class, setRequiredMethod.getReturnType());
  }

  @Test
  void testGetterReturnCorrectType() throws NoSuchMethodException {
    var isRequiredMethod = PoppConfig.class.getMethod("isRequired");

    assertEquals(boolean.class, isRequiredMethod.getReturnType());
  }

  @Test
  void testGetterAndSetterArePublic() throws NoSuchMethodException {
    var isRequiredMethod = PoppConfig.class.getMethod("isRequired");
    var setRequiredMethod = PoppConfig.class.getMethod("setRequired", boolean.class);

    assertTrue(java.lang.reflect.Modifier.isPublic(isRequiredMethod.getModifiers()));
    assertTrue(java.lang.reflect.Modifier.isPublic(setRequiredMethod.getModifiers()));
  }

  @Test
  void testBooleanGetterHasCorrectPrefix() throws NoSuchMethodException {
    // Boolean getter should use 'is' prefix instead of 'get'
    var isRequiredMethod = PoppConfig.class.getMethod("isRequired");

    assertNotNull(isRequiredMethod);
    assertEquals("isRequired", isRequiredMethod.getName());
  }

  @Test
  void testToggleBooleanValue() {
    // Test toggling between true and false
    poppConfig.setRequired(true);
    assertTrue(poppConfig.isRequired());

    poppConfig.setRequired(false);
    assertFalse(poppConfig.isRequired());

    poppConfig.setRequired(true);
    assertTrue(poppConfig.isRequired());
  }
}
