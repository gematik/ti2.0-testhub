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
package de.gematik.ti20.simsvc.client.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FhirServiceTest {

  private FhirService fhirService;

  @BeforeEach
  void setUp() {
    fhirService = new FhirService();
  }

  @Test
  void testParsePostRequest_Success() throws IOException {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getReader())
        .thenReturn(new BufferedReader(new StringReader("{\"resourceType\":\"Patient\"}")));
    when(mockRequest.getHeader("content-type")).thenReturn("application/fhir+json");

    Resource resource = fhirService.parsePostRequest(mockRequest);

    assertNotNull(resource);
    assertEquals("Patient", resource.getResourceType().name());
  }

  @Test
  void testParsePostRequest_InvalidBody() throws IOException {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader("Invalid Body")));
    when(mockRequest.getHeader("content-type")).thenReturn("application/fhir+json");

    assertThrows(ResponseStatusException.class, () -> fhirService.parsePostRequest(mockRequest));
  }

  @Test
  void testGetBodyString_Success() throws IOException {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getReader()).thenReturn(new BufferedReader(new StringReader("Test Body")));

    String body = fhirService.getBodyString(mockRequest);

    assertEquals("Test Body", body);
  }

  @Test
  void testGetBodyString_Error() throws IOException {
    HttpServletRequest mockRequest = mock(HttpServletRequest.class);
    when(mockRequest.getReader()).thenThrow(new IOException("Read Error"));

    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> fhirService.getBodyString(mockRequest));

    assertEquals("Error reading request body", exception.getReason());
  }
}
