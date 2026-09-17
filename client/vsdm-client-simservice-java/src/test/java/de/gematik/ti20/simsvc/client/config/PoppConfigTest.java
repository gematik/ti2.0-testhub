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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.simsvc.client.service.popp.PoppClientAdapter;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class PoppConfigTest {

  private PoppConfig poppConfig;

  @BeforeEach
  void setUp() {
    poppConfig = new PoppConfig();
    PoppConfig.Http http = new PoppConfig.Http();
    http.setUrl("http://example.com/http");
    PoppConfig.Ws ws = new PoppConfig.Ws();
    ws.setUrl("ws://example.com/ws");
    poppConfig.setHttp(http);
    poppConfig.setWs(ws);
  }

  @Test
  void testGetHttpUrl() {
    assertEquals("http://example.com/http", poppConfig.getHttp().getUrl());
  }

  @Test
  void testGetWsUrl() {
    assertEquals("ws://example.com/ws", poppConfig.getWs().getUrl());
  }

  @Test
  void shouldExposeTokenTypeAndCreateBeans() throws ReflectiveOperationException {
    poppConfig.setTokenType(PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR);
    WebClient webClient = poppConfig.webClient();

    assertNotNull(webClient);
    assertEquals(PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR, poppConfig.getTokenType());

    PoppClientAdapter adapter = poppConfig.poppClientAdapter(webClient);
    assertNotNull(adapter);

    PoppClientConfig config = getPrivateField(adapter, "poppClientConfig", PoppClientConfig.class);
    assertEquals("http://example.com/http", config.getUrlPoppServerHttp());
    assertEquals(PoppClientConfig.TokenType.CONTACTLESS_CONNECTOR, config.getTokenType());
    assertSame(webClient, getPrivateField(adapter, "webClient", WebClient.class));
  }

  private static <T> T getPrivateField(Object instance, String fieldName, Class<T> type)
      throws ReflectiveOperationException {
    Field field = instance.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return type.cast(field.get(instance));
  }
}
