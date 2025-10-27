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
package de.gematik.ti20.client.zeta.request;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

class ZetaWsRequestTest {

  @Test
  void testConstructorAndGetters() {
    ZetaWsRequest req = new ZetaWsRequest("wss://ws.example.org", "https://api.example.org");
    assertEquals("wss://ws.example.org", req.getUrl());
    assertEquals("https://api.example.org", req.getUrlHttpEndpoint());
  }

  @Test
  void testValidateWithValidUrls() throws Exception {
    ZetaWsRequest req = new ZetaWsRequest("wss://ws.example.org", "https://api.example.org");
    req.setHeaderUserAgent(new ZetaClientConfig.UserAgentConfig("App", "1.0"));
    // Keine Exception erwartet
    req.validate();
  }

  @Test
  void testValidateWithInvalidHttpUrl() {
    ZetaWsRequest req = new ZetaWsRequest("wss://ws.example.org", "not-a-url");
    req.setHeaderUserAgent(new ZetaClientConfig.UserAgentConfig("App", "1.0"));
    URISyntaxException ex = assertThrows(URISyntaxException.class, req::validate);
    assertTrue(ex.getMessage().contains("no protocol"));
  }
}
