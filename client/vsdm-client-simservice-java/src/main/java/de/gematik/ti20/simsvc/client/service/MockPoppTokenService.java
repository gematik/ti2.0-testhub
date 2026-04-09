/*-
 * #%L
 * VSDM Client Simulator Service
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
package de.gematik.ti20.simsvc.client.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class MockPoppTokenService {

  private final String POPP_TOKEN_GENERATOR_ENDPOINT = "/popp/test/api/v1/token-generator";

  private final RestTemplate restTemplate = new RestTemplate();
  private final ObjectMapper mapper = new ObjectMapper();

  public String requestPoppToken(
      final VsdmClientConfig config, final String iknr, final String kvnr) {
    log.info("requestMockPoppToken for iknr: {}, kvnr: {}", iknr, kvnr);

    final String body = getPoppTokenJsonBody(iknr, kvnr);

    final HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<String> entity = new HttpEntity<>(body, headers);

    final ResponseEntity<String> response =
        restTemplate.exchange(
            config.getPoppTokenGeneratorUrl() + POPP_TOKEN_GENERATOR_ENDPOINT,
            HttpMethod.POST,
            entity,
            String.class);

    final String poppToken = extractPoppToken(response.getBody());
    return poppToken;
  }

  private String getPoppTokenJsonBody(final String iknr, final String kvnr) {
    final Map<String, List<Map<String, String>>> tokenArgs =
        Map.of(
            "tokenParamsList",
            List.of(
                Map.of(
                    "proofMethod",
                    "ehc-practitioner-trustedchannel",
                    "patientId",
                    kvnr,
                    "insurerId",
                    iknr,
                    "actorId",
                    "883110000168650",
                    "actorProfessionOid",
                    "1.2.276.0.76.4.32")));

    try {
      return mapper.writeValueAsString(tokenArgs);
    } catch (JsonProcessingException e) {
      return null;
    }
  }

  private String extractPoppToken(final String json) {
    try {
      JsonNode root = mapper.readTree(json);
      JsonNode tokenResults = root.path("tokenResults");
      if (tokenResults.isArray() && tokenResults.size() > 0) {
        JsonNode first = tokenResults.get(0);
        return first.isTextual() ? first.asText() : first.toString();
      }
      return null;
    } catch (final IOException e) {
      throw new RuntimeException("Ungültiges JSON beim Parsen des Tokens", e);
    }
  }
}
