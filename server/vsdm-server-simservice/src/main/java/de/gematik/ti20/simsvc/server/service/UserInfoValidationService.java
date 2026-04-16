/*-
 * #%L
 * VSDM Server Simservice
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import de.gematik.ti20.simsvc.server.exception.ErrorCase;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates the {@code zeta-user-info} header against the JSON Schema for VSDM2
 * (user-info-vsdm2.json). The header value is expected to be a Base64-encoded JSON string.
 */
@Slf4j
@Service
public class UserInfoValidationService {

  private static final String SCHEMA_PATH = "/schemas/user-info-vsdm2.json";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private Schema userInfoSchema;

  @PostConstruct
  public void init() throws IOException {
    try (InputStream schemaStream = getClass().getResourceAsStream(SCHEMA_PATH)) {
      if (schemaStream == null) {
        throw new IllegalStateException("JSON schema not found: " + SCHEMA_PATH);
      }

      SchemaRegistry schemaRegistry =
          SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);
      userInfoSchema = schemaRegistry.getSchema(schemaStream, InputFormat.JSON);
      log.info("user-info-vsdm2 json schema successfully loaded");
    }
  }

  /**
   * Validates the given Base64-encoded JSON string against the user-info-vsdm2 JSON schema.
   *
   * @param userInfoBase64 the Base64-encoded JSON string from the {@code zeta-user-info} header
   * @throws ResponseStatusException with HTTP 400 if the value is missing, malformed, or does not
   *     conform to the schema
   */
  public void validateUserInfo(final String userInfoBase64) {
    final JsonNode jsonNode = decodeAndParse(userInfoBase64);
    final List<Error> errors = userInfoSchema.validate(jsonNode);

    if (!errors.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-user-info"));
    }
  }

  private JsonNode decodeAndParse(final String userInfoBase64) {
    try {
      final byte[] decoded = Base64.getDecoder().decode(userInfoBase64);
      final String json = new String(decoded, StandardCharsets.UTF_8);
      return OBJECT_MAPPER.readTree(json);
    } catch (final IllegalArgumentException e) {
      log.warn("zeta-user-info ist kein gültiges Base64: {}", e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-user-info"));
    } catch (final Exception e) {
      log.warn("zeta-user-info JSON-Parsing fehlgeschlagen: {}", e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          ErrorCase.SERVICE_MISSING_OR_INVALID_HEADER
              .getBdeReference()
              .replaceAll("<header>", "zeta-user-info"));
    }
  }
}
