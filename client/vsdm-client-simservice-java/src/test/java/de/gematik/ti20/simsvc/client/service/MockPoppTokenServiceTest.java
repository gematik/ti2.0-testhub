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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class MockPoppTokenServiceTest {

  private MockPoppTokenService service;
  private RestTemplate mockRestTemplate;
  private VsdmClientConfig mockConfig;

  @BeforeEach
  void setUp() {
    service = new MockPoppTokenService();
    mockRestTemplate = mock(RestTemplate.class);
    mockConfig = mock(VsdmClientConfig.class);
    when(mockConfig.getPoppTokenGeneratorUrl()).thenReturn("http://localhost:8080");

    ReflectionTestUtils.setField(service, "restTemplate", mockRestTemplate);
  }

  @Test
  void requestPoppToken_returnsTextToken() {
    String json = "{\"tokenResults\":[\"the-token-value\"]}";
    when(mockRestTemplate.exchange(
            anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

    String token = service.requestPoppToken(mockConfig, "iknr", "kvnr");

    assertEquals("the-token-value", token);
  }

  @Test
  void requestPoppToken_throwsRuntimeException_onInvalidJson() {
    when(mockRestTemplate.exchange(
            anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>("not a json", HttpStatus.OK));

    assertThrows(
        RuntimeException.class, () -> service.requestPoppToken(mockConfig, "iknr", "kvnr"));
  }
}
