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

import ca.uhn.fhir.validation.ValidationResult;
import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.ti20.vsdm.fhir.service.CodecServiceR4;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FhirService extends CodecServiceR4 {

  public Resource parsePostRequest(final HttpServletRequest request) {
    return parseString(getBodyString(request), request.getHeader("content-type"));
  }

  public Resource parseString(final String body, final String contentType) {
    try {
      validate(body);
      return codec.decode(body, EncodingType.fromString(contentType));
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid FHIR resource: " + body, e);
    }
  }

  public <T extends Resource> T parseString(String body, String contentType, Class<T> cls) {
    validate(body);
    return codec.decode(cls, body, EncodingType.fromString(contentType));
  }

  protected String getBodyString(HttpServletRequest request) {
    final StringBuilder requestBody = new StringBuilder();
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

  public void validate(final String body) {
    ValidationResult result = codec.validate(body);
    result.getMessages().forEach(System.out::println);

    if (!codec.isValid(body)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid FHIR resource: " + body);
    }
  }

  public String encodeResponse(final Resource fhirResourceOut, final EncodingType encodeTo) {
    return codec.encode(fhirResourceOut, encodeTo);
  }
}
