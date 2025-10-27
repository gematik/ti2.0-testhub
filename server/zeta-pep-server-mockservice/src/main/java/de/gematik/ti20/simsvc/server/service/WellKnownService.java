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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.server.config.WellKnownConfig;
import de.gematik.ti20.simsvc.server.model.WellKnown;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class WellKnownService {

  private static final String WELL_KNOWN_FILE = "well-known.json";

  private final WellKnownConfig wellKnownConfig;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public WellKnownService(WellKnownConfig wellKnownConfig, ResourceLoader resourceLoader) {
    this.wellKnownConfig = wellKnownConfig;
    this.resourceLoader = resourceLoader;
  }

  public String getWellKnown(HttpServletRequest request) {
    log.debug("Loading well-known configuration from file: {}", WELL_KNOWN_FILE);
    Resource resource = resourceLoader.getResource("classpath:" + WELL_KNOWN_FILE);
    WellKnown wellKnown;
    try (InputStream inputStream = resource.getInputStream()) {
      wellKnown = objectMapper.readValue(inputStream, WellKnown.class);
    } catch (IOException e) {
      try {
        log.error(
            "Failed to load well-known configuration from resource: {}",
            resource.getURI().toString(),
            e);
      } catch (IOException ex) {
        log.error("Failed to load well-known configuration from resource: {}", WELL_KNOWN_FILE, e);
      }
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load well-known configuration");
    }

    wellKnown.setIssuer(wellKnownConfig.getIssuer());
    wellKnown.setToken_endpoint(wellKnownConfig.getToken_ep());
    wellKnown.setAuthorization_endpoint(wellKnownConfig.getAuth_ep());
    wellKnown.setNonce_endpoint(wellKnownConfig.getNonce_ep());

    try {
      return objectMapper.writeValueAsString(wellKnown);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize well-known configuration");
    }
  }
}
