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
package de.gematik.ti20.simsvc.server.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.data.core.RbelValueFacet;
import de.gematik.test.testdata.TestDataManager;
import de.gematik.test.testdata.exceptions.TestDataInitializationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class TestDataRepositoryTest {

  private TestDataRepository testDataRepository;

  @BeforeEach
  void setUp() {
    testDataRepository = new TestDataRepository();
  }

  @Test
  void testValidateAlias_ValidAlias() {
    assertDoesNotThrow(() -> testDataRepository.validateAlias("validAlias123"));
    assertDoesNotThrow(() -> testDataRepository.validateAlias("äüöÄÜÖß"));
    assertDoesNotThrow(() -> testDataRepository.validateAlias("test with spaces"));
  }

  @Test
  void testValidateAlias_InvalidAlias() {
    assertThrows(IllegalArgumentException.class, () -> testDataRepository.validateAlias(null));
    assertThrows(
        IllegalArgumentException.class, () -> testDataRepository.validateAlias("invalid@alias"));
    assertThrows(
        IllegalArgumentException.class, () -> testDataRepository.validateAlias("alias#test"));
    assertThrows(IllegalArgumentException.class, () -> testDataRepository.validateAlias("alias!"));
  }

  @Test
  void testFindElementByAlias_ElementFound() {
    RbelElement mockRoot = mock(RbelElement.class);
    RbelElement mockElement = mock(RbelElement.class);
    List<RbelElement> elements = Collections.singletonList(mockElement);

    try (MockedStatic<TestDataManager> mockedStatic = mockStatic(TestDataManager.class)) {
      mockedStatic.when(TestDataManager::getTestDataRoot).thenReturn(mockRoot);
      when(mockRoot.findRbelPathMembers("$..[?(@.alias.. == 'testAlias')]")).thenReturn(elements);

      Optional<RbelElement> result = testDataRepository.findElementByAlias("testAlias");

      assertTrue(result.isPresent());
      assertEquals(mockElement, result.get());
    }
  }

  @Test
  void testFindElementByAlias_NoElementFound() {
    RbelElement mockRoot = mock(RbelElement.class);

    try (MockedStatic<TestDataManager> mockedStatic = mockStatic(TestDataManager.class)) {
      mockedStatic.when(TestDataManager::getTestDataRoot).thenReturn(mockRoot);
      when(mockRoot.findRbelPathMembers("$..[?(@.alias.. == 'nonExistentAlias')]"))
          .thenReturn(Collections.emptyList());

      Optional<RbelElement> result = testDataRepository.findElementByAlias("nonExistentAlias");

      assertTrue(result.isEmpty());
    }
  }

  @Test
  void testFindElementByAlias_MultipleElementsFound() {
    RbelElement mockRoot = mock(RbelElement.class);
    RbelElement mockElement1 = mock(RbelElement.class);
    RbelElement mockElement2 = mock(RbelElement.class);
    List<RbelElement> elements = Arrays.asList(mockElement1, mockElement2);

    try (MockedStatic<TestDataManager> mockedStatic = mockStatic(TestDataManager.class)) {
      mockedStatic.when(TestDataManager::getTestDataRoot).thenReturn(mockRoot);
      when(mockRoot.findRbelPathMembers("$..[?(@.alias.. == 'duplicateAlias')]"))
          .thenReturn(elements);

      assertThrows(
          TestDataInitializationException.class,
          () -> testDataRepository.findElementByAlias("duplicateAlias"));
    }
  }

  @Test
  void testFindElementByKeyValue_ElementFound() {
    RbelElement mockRoot = mock(RbelElement.class);
    RbelElement mockElement = mock(RbelElement.class);
    List<RbelElement> elements = Collections.singletonList(mockElement);

    try (MockedStatic<TestDataManager> mockedStatic = mockStatic(TestDataManager.class)) {
      mockedStatic.when(TestDataManager::getTestDataRoot).thenReturn(mockRoot);
      when(mockRoot.findRbelPathMembers("$..[?(@.testKey.. == 'testValue')]")).thenReturn(elements);

      Optional<RbelElement> result =
          testDataRepository.findElementByKeyValue("testKey", "testValue");

      assertTrue(result.isPresent());
      assertEquals(mockElement, result.get());
    }
  }

  @Test
  void testGetStringFor_ValueFound() {
    RbelElement mockParent = mock(RbelElement.class);
    RbelElement mockElement = mock(RbelElement.class);
    RbelValueFacet mockFacet = mock(RbelValueFacet.class);

    when(mockParent.findElement("testPath")).thenReturn(Optional.of(mockElement));
    when(mockElement.getFacet(RbelValueFacet.class)).thenReturn(Optional.of(mockFacet));
    when(mockFacet.getValue()).thenReturn("testValue");

    Optional<String> result = testDataRepository.getStringFor(mockParent, "testPath");

    assertTrue(result.isPresent());
    assertEquals("testValue", result.get());
  }

  @Test
  void testGetStringFor_NoValueFound() {
    RbelElement mockParent = mock(RbelElement.class);
    when(mockParent.findElement("testPath")).thenReturn(Optional.empty());

    Optional<String> result = testDataRepository.getStringFor(mockParent, "testPath");

    assertTrue(result.isEmpty());
  }

  @Test
  void testGetDateFor_ValidDate() {
    RbelElement mockParent = mock(RbelElement.class);
    RbelElement mockElement = mock(RbelElement.class);
    RbelValueFacet mockFacet = mock(RbelValueFacet.class);

    String dateString = "Mon Jan 01 12:00:00 CET 2024";
    when(mockParent.findElement("datePath")).thenReturn(Optional.of(mockElement));
    when(mockElement.getFacet(RbelValueFacet.class)).thenReturn(Optional.of(mockFacet));
    when(mockFacet.getValue()).thenReturn(dateString);

    Optional<Date> result = testDataRepository.getDateFor(mockParent, "datePath");

    assertTrue(result.isPresent());
    assertNotNull(result.get());
  }

  @Test
  void testGetDateFor_InvalidDate() {
    RbelElement mockParent = mock(RbelElement.class);
    RbelElement mockElement = mock(RbelElement.class);
    RbelValueFacet mockFacet = mock(RbelValueFacet.class);

    when(mockParent.findElement("datePath")).thenReturn(Optional.of(mockElement));
    when(mockElement.getFacet(RbelValueFacet.class)).thenReturn(Optional.of(mockFacet));
    when(mockFacet.getValue()).thenReturn("invalid date format");

    Optional<Date> result = testDataRepository.getDateFor(mockParent, "datePath");

    assertTrue(result.isEmpty());
  }

  @Test
  void testGetDateFor_NoDateValue() {
    RbelElement mockParent = mock(RbelElement.class);
    when(mockParent.findElement("datePath")).thenReturn(Optional.empty());

    Optional<Date> result = testDataRepository.getDateFor(mockParent, "datePath");

    assertTrue(result.isEmpty());
  }
}
