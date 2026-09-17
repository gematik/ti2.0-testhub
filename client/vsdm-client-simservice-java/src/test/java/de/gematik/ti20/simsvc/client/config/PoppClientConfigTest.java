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
package de.gematik.ti20.simsvc.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PoppClientConfigTest {

  @Test
  void shouldExposeConfiguredValues() {
    PoppClientConfig config =
        new PoppClientConfig(
            PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR, "http://example.com/http");

    assertEquals(PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR, config.getTokenType());
    assertEquals("http://example.com/http", config.getUrlPoppServerHttp());
  }

  @Test
  void shouldSupportHttpOnlyConstructor() {
    PoppClientConfig config =
        new PoppClientConfig(
            PoppClientConfig.TokenType.CONTACT_VIRTUAL, "http://example.com/http-only");

    assertEquals(PoppClientConfig.TokenType.CONTACT_VIRTUAL, config.getTokenType());
    assertEquals("http://example.com/http-only", config.getUrlPoppServerHttp());
  }

  @Test
  void shouldExposeAllTokenTypeIdentifiers() {
    assertEquals("contact-connector", PoppClientConfig.TokenType.CONTACT_CONNECTOR.getType());
    assertEquals(
        "contactless-connector", PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR.getType());
    assertEquals("contact-virtual", PoppClientConfig.TokenType.CONTACT_VIRTUAL.getType());
  }

  @Test
  void shouldContainExpectedTokenTypes() {
    assertEquals(3, PoppClientConfig.TokenType.values().length);
    assertEquals(
        PoppClientConfig.TokenType.CONTACT_CONNECTOR,
        PoppClientConfig.TokenType.valueOf("CONTACT_CONNECTOR"));
    assertEquals(
        PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR,
        PoppClientConfig.TokenType.valueOf("CONTACTLESS_CONNECTOR"));
    assertEquals(
        PoppClientConfig.TokenType.CONTACT_VIRTUAL,
        PoppClientConfig.TokenType.valueOf("CONTACT_VIRTUAL"));
  }
}
