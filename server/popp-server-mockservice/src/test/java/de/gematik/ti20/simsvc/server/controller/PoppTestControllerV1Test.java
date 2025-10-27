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
package de.gematik.ti20.simsvc.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.model.SecurityParams;
import de.gematik.ti20.simsvc.server.model.TokenGenerationParams;
import de.gematik.ti20.simsvc.server.model.TokenParams;
import de.gematik.ti20.simsvc.server.service.PoppTokenService;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PoppTestControllerV1Test {

  private PoppTokenService poppTokenService;
  private PoppTestControllerV1 controller;

  @BeforeEach
  void setUp() {
    poppTokenService = mock(PoppTokenService.class);
    controller = new PoppTestControllerV1(poppTokenService);
  }

  @Test
  void testGenerateReturnsTokens() throws Exception {
    TokenParams tokenParams =
        new TokenParams("ehc-practitioner-trustedchannel", "1000", "999", "p1", "i1", "a1", "oid1");
    when(poppTokenService.createToken(tokenParams, null)).thenReturn("token123");

    List<TokenParams> tokenParamsList = List.of(tokenParams);
    TokenGenerationParams tokenGenerationParams = new TokenGenerationParams(tokenParamsList, null);
    ResponseEntity<?> response = controller.generate(tokenGenerationParams);

    assertEquals(200, response.getStatusCode().value());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("tokenResults"));
    List<?> results = (List<?>) body.get("tokenResults");
    assertEquals(1, results.size());
    assertEquals("token123", results.get(0));
    verify(poppTokenService).createToken(tokenParams, null);
  }

  @Test
  void testGenerateReturnsTokensDefaultValues() throws Exception {
    TokenParams tokenParams = new TokenParams(null, null, null, "p1", "i1", "a1", "oid1");
    when(poppTokenService.createToken(tokenParams, null)).thenReturn("token123");

    List<TokenParams> tokenParamsList = List.of(tokenParams);
    TokenGenerationParams tokenGenerationParams = new TokenGenerationParams(tokenParamsList, null);
    ResponseEntity<?> response = controller.generate(tokenGenerationParams);

    assertEquals(200, response.getStatusCode().value());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("tokenResults"));
    List<?> results = (List<?>) body.get("tokenResults");
    assertEquals(1, results.size());
    assertEquals("token123", results.get(0));
    verify(poppTokenService).createToken(eq(tokenParams), eq(null));
  }

  @Test
  void testGenerateEmptyTokenArgs() {
    List<TokenParams> tokenParamsList = List.of();
    SecurityParams securityParams = null;

    TokenGenerationParams tokenGenerationParams =
        new TokenGenerationParams(tokenParamsList, securityParams);
    ResponseEntity<?> response = controller.generate(tokenGenerationParams);

    assertEquals(400, response.getStatusCode().value());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals("tokenParams is missing or empty.", body.get("error"));
  }

  @Test
  void testGenerateMissingFields() throws Exception {
    TokenParams tokenParams =
        new TokenParams("ehc-practitioner-trustedchannel", "1000", "999", null, "i1", "a1", "oid1");

    List<TokenParams> tokenParamsList = List.of(tokenParams);
    TokenGenerationParams tokenGenerationParams = new TokenGenerationParams(tokenParamsList, null);
    ResponseEntity<?> response = controller.generate(tokenGenerationParams);

    assertEquals(400, response.getStatusCode().value());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals("One or more required fields are missing.", body.get("error"));
  }

  @Test
  void testGenerateUnknownProofmethod() throws Exception {
    TokenParams tokenParams = new TokenParams("unknown", "1000", "999", "p1", "i1", "a1", "oid1");

    List<TokenParams> tokenParamsList = List.of(tokenParams);
    TokenGenerationParams tokenGenerationParams = new TokenGenerationParams(tokenParamsList, null);
    ResponseEntity<?> response = controller.generate(tokenGenerationParams);

    assertEquals(400, response.getStatusCode().value());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals("Unknown proofMethod: unknown", body.get("error"));
  }

  @Test
  void testGenerateWithSecurityParams() throws Exception {
    TokenParams tokenParams =
        new TokenParams("ehc-practitioner-trustedchannel", "1000", "999", "p1", "i1", "a1", "oid1");

    final InputStream fis =
        PoppTestControllerV1.class.getClassLoader().getResourceAsStream("alt_keystore.p12");
    final byte[] content = fis.readAllBytes();
    final String base64 = java.util.Base64.getEncoder().encodeToString(content);
    final SecurityParams securityParams = new SecurityParams(base64, "00", "alias", "00");

    when(poppTokenService.createToken(tokenParams, securityParams)).thenReturn("token123");

    List<TokenParams> tokenParamsList = List.of(tokenParams);
    TokenGenerationParams tokenGenerationParams =
        new TokenGenerationParams(tokenParamsList, securityParams);
    ResponseEntity<?> response = controller.generate(tokenGenerationParams);

    assertEquals(200, response.getStatusCode().value());
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertNotNull(body);
    assertTrue(body.containsKey("tokenResults"));
    List<?> results = (List<?>) body.get("tokenResults");
    assertEquals(1, results.size());
    assertEquals("token123", results.get(0));
    verify(poppTokenService).createToken(tokenParams, securityParams);
  }
}
