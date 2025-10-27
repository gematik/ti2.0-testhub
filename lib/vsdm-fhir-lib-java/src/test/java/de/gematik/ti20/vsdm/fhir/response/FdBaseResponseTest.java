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
package de.gematik.ti20.vsdm.fhir.response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ca.uhn.fhir.validation.ValidationResult;
import de.gematik.bbriccs.rest.fd.FhirBResponse;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FdBaseResponseTest {

  @Mock private FhirBResponse<Bundle> mockFdResponse;

  @Mock private ValidationResult mockValidationResult;

  @Mock private Bundle mockBundle;

  @Mock private Resource mockResource;

  @Mock private OperationOutcome mockOperationOutcome;

  private static class TestFdBaseResponse extends FdBaseResponse<Bundle> {
    public TestFdBaseResponse(FhirBResponse<Bundle> response) {
      super(response);
    }
  }

  @Test
  void testConstructor() {
    TestFdBaseResponse response = new TestFdBaseResponse(mockFdResponse);

    assertNotNull(response);
  }

  @Test
  void testGetStatusCode() {
    int expectedStatusCode = 200;
    when(mockFdResponse.getStatusCode()).thenReturn(expectedStatusCode);

    TestFdBaseResponse response = new TestFdBaseResponse(mockFdResponse);

    assertEquals(expectedStatusCode, response.getStatusCode());
    verify(mockFdResponse).getStatusCode();
  }

  @Test
  void testIsValid_ReturnsTrue() {
    when(mockFdResponse.getValidationResult()).thenReturn(mockValidationResult);
    when(mockValidationResult.isSuccessful()).thenReturn(true);

    TestFdBaseResponse response = new TestFdBaseResponse(mockFdResponse);

    assertTrue(response.isValid());
    verify(mockFdResponse).getValidationResult();
    verify(mockValidationResult).isSuccessful();
  }

  @Test
  void testIsValid_ReturnsFalse() {
    when(mockFdResponse.getValidationResult()).thenReturn(mockValidationResult);
    when(mockValidationResult.isSuccessful()).thenReturn(false);

    TestFdBaseResponse response = new TestFdBaseResponse(mockFdResponse);

    assertFalse(response.isValid());
    verify(mockFdResponse).getValidationResult();
    verify(mockValidationResult).isSuccessful();
  }

  @Test
  void testGetExpectedResource() {
    when(mockFdResponse.getExpectedResource()).thenReturn(mockBundle);

    TestFdBaseResponse response = new TestFdBaseResponse(mockFdResponse);

    assertEquals(mockBundle, response.getExpectedResource());
    verify(mockFdResponse).getExpectedResource();
  }

  @Test
  void testGetAsResource() {
    when(mockFdResponse.getAsBaseResource()).thenReturn(mockResource);

    TestFdBaseResponse response = new TestFdBaseResponse(mockFdResponse);

    assertEquals(mockResource, response.getAsResource());
    verify(mockFdResponse).getAsBaseResource();
  }

  @Test
  void testGetAsOperationOutcome() {
    when(mockFdResponse.getAsOperationOutcome()).thenReturn(mockOperationOutcome);

    TestFdBaseResponse response = new TestFdBaseResponse(mockFdResponse);

    assertEquals(mockOperationOutcome, response.getAsOperationOutcome());
    verify(mockFdResponse).getAsOperationOutcome();
  }

  @Test
  void testIsAbstractClass() {
    assertTrue(java.lang.reflect.Modifier.isAbstract(FdBaseResponse.class.getModifiers()));
  }
}
