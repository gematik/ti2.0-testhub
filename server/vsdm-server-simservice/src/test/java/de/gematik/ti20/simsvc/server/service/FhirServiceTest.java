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
package de.gematik.ti20.simsvc.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.bbriccs.rest.fd.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FhirServiceTest {

  @Mock private HttpServletRequest request;

  private FhirService fhirService;

  @BeforeEach
  void setUp() {
    fhirService = new FhirService();
  }

  @Test
  void testParsePostRequest_ValidJson() throws IOException {
    String jsonBody = "{\"resourceType\":\"Patient\"}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(jsonBody)));
    when(request.getHeader("content-type")).thenReturn("application/json");

    assertDoesNotThrow(() -> fhirService.parsePostRequest(request));
  }

  @Test
  void testParsePostRequest_WithExpectedClass() throws IOException {
    String jsonBody = "{\"resourceType\":\"Patient\"}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(jsonBody)));
    when(request.getHeader("content-type")).thenReturn("application/json");

    assertDoesNotThrow(() -> fhirService.parsePostRequest(request, Patient.class));
  }

  @Test
  void testParsePostRequest_IoException() throws IOException {
    when(request.getReader()).thenThrow(new IOException("Read error"));

    assertThrows(ResponseStatusException.class, () -> fhirService.parsePostRequest(request));
  }

  @Test
  void testParseString_ValidJson() {
    String jsonBody = "{\"resourceType\":\"Patient\"}";
    String contentType = "application/json";

    assertDoesNotThrow(() -> fhirService.parseString(jsonBody, contentType));
  }

  @Test
  void testParseString_WithClass() {
    String jsonBody = "{\"resourceType\":\"Patient\"}";
    String contentType = "application/json";

    assertDoesNotThrow(() -> fhirService.parseString(jsonBody, contentType, Patient.class));
  }

  @Test
  void testEncodeResponse_JsonAccept() {
    Resource resource = new Patient();
    HttpHeaders headers = new HttpHeaders();
    when(request.getHeader("accept")).thenReturn("application/json");

    String result = fhirService.encodeResponse(resource, request, headers);

    assertNotNull(result);
    assertEquals(MediaType.FHIR_JSON.asString(), headers.getFirst("Content-Type"));
  }

  @Test
  void testEncodeResponse_XmlAccept() {
    Resource resource = new Patient();
    HttpHeaders headers = new HttpHeaders();
    when(request.getHeader("accept")).thenReturn("application/xml");

    String result = fhirService.encodeResponse(resource, request, headers);

    assertNotNull(result);
    assertEquals(MediaType.FHIR_XML.asString(), headers.getFirst("Content-Type"));
  }

  @Test
  void testEncodeResponse_NoAcceptHeader() {
    Resource resource = new Patient();
    HttpHeaders headers = new HttpHeaders();
    when(request.getHeader("accept")).thenReturn(null);

    String result = fhirService.encodeResponse(resource, request, headers);

    assertNotNull(result);
    assertEquals(MediaType.FHIR_JSON.asString(), headers.getFirst("Content-Type"));
  }

  @Test
  void testEncodeResponse_InvalidAcceptHeader() {
    Resource resource = new Patient();
    HttpHeaders headers = new HttpHeaders();
    when(request.getHeader("accept")).thenReturn("invalid/type");

    String result = fhirService.encodeResponse(resource, request, headers);

    assertNotNull(result);
    assertEquals(MediaType.FHIR_JSON.asString(), headers.getFirst("Content-Type"));
  }

  @Test
  void testGetBodyString_EmptyBody() throws IOException {
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));
    when(request.getHeader("content-type")).thenReturn("application/json");

    assertDoesNotThrow(() -> fhirService.parsePostRequest(request));
  }

  @Test
  void testGetBodyString_MultilineContent() throws IOException {
    String multilineBody = "{\n  \"resourceType\": \"Patient\",\n  \"id\": \"test\"\n}";
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(multilineBody)));
    when(request.getHeader("content-type")).thenReturn("application/json");

    assertDoesNotThrow(() -> fhirService.parsePostRequest(request));
  }

  @Test
  void testValidate_ValidResource() {
    String validBody = "{\"resourceType\":\"Patient\"}";

    assertDoesNotThrow(() -> fhirService.validate(validBody));
  }

  @Test
  void testEncodeResponse_WithBundle() {
    Resource resource = new Bundle();
    HttpHeaders headers = new HttpHeaders();
    when(request.getHeader("accept")).thenReturn("application/json");

    String result = fhirService.encodeResponse(resource, request, headers);

    assertNotNull(result);
    assertTrue(headers.containsKey("Content-Type"));
  }
}
