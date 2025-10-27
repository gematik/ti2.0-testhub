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

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.rest.fd.MediaType;
import de.gematik.ti20.vsdm.fhir.service.CodecServiceR4;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class FhirService extends CodecServiceR4 {

  public Resource parsePostRequest(HttpServletRequest request) {
    return parseString(getBodyString(request), request.getHeader("content-type"));
  }

  public <T extends Resource> T parsePostRequest(
      HttpServletRequest request, Class<T> expectedClass) {
    return parseString(getBodyString(request), request.getHeader("content-type"), expectedClass);
  }

  public Resource parseString(String body, String contentType) {
    // validate(body);
    return codec.decode(body, EncodingType.fromString(contentType));
  }

  public <T extends Resource> T parseString(String body, String contentType, Class<T> cls) {
    // validate(body);
    return codec.decode(cls, body, EncodingType.fromString(contentType));
  }

  private String getBodyString(HttpServletRequest request) {
    StringBuilder requestBody = new StringBuilder();
    try (BufferedReader reader = request.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        requestBody.append(line);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error reading request body");
    }

    return requestBody.toString();
  }

  public void validate(String body) {
    if (!codec.isValid(body)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid FHIR resource: " + body);
    }
  }

  private EncodingType getEndodingType(final HttpServletRequest request) {
    var accept = request.getHeader("accept");
    EncodingType encodingType = EncodingType.JSON;
    try {
      if (EncodingType.fromString(accept) == EncodingType.XML) {
        encodingType = EncodingType.XML;
      }
    } catch (Exception e) {
      // do nothing
    }

    return encodingType;
  }

  public String encodeResponse(
      final Resource fhirResourceOut,
      final HttpServletRequest request,
      final HttpHeaders responseHeaders) {
    final EncodingType encodingType = getEndodingType(request);

    responseHeaders.add(
        "Content-Type",
        encodingType == EncodingType.JSON
            ? MediaType.FHIR_JSON.asString()
            : MediaType.FHIR_XML.asString());

    return codec.encode(fhirResourceOut, encodingType);
  }
}
