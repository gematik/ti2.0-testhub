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
import java.util.Map;
import okhttp3.Request;
import org.junit.jupiter.api.Test;

class ZetaHttpRequestTest {

  @Test
  void testConstructorAndTraceId() {
    ZetaHttpRequest req = new ZetaHttpRequest("https://example.org");
    assertNotNull(req.getTraceId());
    assertEquals("https://example.org", req.getUrl());
    assertTrue(req.hasHeader("X-Trace-Id"));
    assertEquals(req.getTraceId(), req.getHeader("X-Trace-Id"));
  }

  @Test
  void testHeaderHandling() {
    ZetaHttpRequest req = new ZetaHttpRequest("https://example.org");
    req.setHeader("Test", "123");
    assertTrue(req.hasHeader("Test"));
    assertEquals("123", req.getHeader("Test"));
    req.removeHeader("Test");
    assertFalse(req.hasHeader("Test"));
  }

  @Test
  void testSetHeaderAuthorization() {
    ZetaHttpRequest req = new ZetaHttpRequest("https://example.org");
    req.setHeaderAuthorization(ZetaHttpRequest.AuthorizationType.BEARER, "token123");
    assertEquals("Bearer token123", req.getHeader("Authorization"));
  }

  @Test
  void testSetHeaderUserAgentWithConfig() {
    ZetaHttpRequest req = new ZetaHttpRequest("https://example.org");
    ZetaClientConfig.UserAgentConfig config = new ZetaClientConfig.UserAgentConfig("App", "1.2");
    req.setHeaderUserAgent(config);
    assertEquals("App/1.2", req.getHeader("User-Agent"));
  }

  @Test
  void testPostJsonSetsMethodAndBody() throws Exception {
    ZetaHttpRequest req = new ZetaHttpRequest("https://example.org");
    req.setHeaderUserAgent("App", "1.0");
    req.postJson(Map.of("foo", "bar"));
    assertEquals(ZetaHttpRequest.HttpMethod.POST, req.getMethod());
    assertTrue(req.hasBody());
    assertEquals("application/json; charset=utf-8", req.getHeader("Content-Type"));
  }

  @Test
  void testPostValuesSetsFormContentType() throws Exception {
    ZetaHttpRequest req = new ZetaHttpRequest("https://example.org");
    req.setHeaderUserAgent("App", "1.0");
    req.postValues(Map.of("a", "b", "c", "d"));
    assertEquals(ZetaHttpRequest.HttpMethod.POST, req.getMethod());
    assertTrue(req.hasBody());
    assertEquals("application/x-www-form-urlencoded", req.getHeader("Content-Type"));
  }

  @Test
  void testBuildValidRequest() throws Exception {
    ZetaHttpRequest req = new ZetaHttpRequest("https://example.org");
    req.setHeaderUserAgent("App", "1.0");
    req.setHeader("Content-Type", "application/json");
    req.post("body", ZetaHttpRequest.ContentType.TEXT);
    Request okReq = req.build();
    assertEquals("https://example.org/", okReq.url().toString());
    assertEquals("POST", okReq.method());
    assertEquals("App/1.0", okReq.header("User-Agent"));
  }
}
