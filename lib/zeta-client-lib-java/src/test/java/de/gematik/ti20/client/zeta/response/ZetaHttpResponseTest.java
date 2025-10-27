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
package de.gematik.ti20.client.zeta.response;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

class ZetaHttpResponseTest {

  @Test
  void testConstructorWithStringBody() {
    ZetaHttpResponse resp = new ZetaHttpResponse(200, "Hallo Welt");
    assertEquals(200, resp.getStatusCode());
    assertTrue(resp.isSuccessful());
    assertEquals("Hallo Welt", resp.getBody().orElse(null));
  }

  @Test
  void testConstructorWithObjectBody() {
    var obj = Map.of("foo", "bar");
    ZetaHttpResponse resp = new ZetaHttpResponse(201, obj);
    assertEquals(201, resp.getStatusCode());
    assertTrue(resp.getBody().orElse("").contains("\"foo\":\"bar\""));
    var map = resp.getBodyFromJson(Map.class).orElseThrow();
    assertEquals("bar", map.get("foo"));
  }

  @Test
  void testGetHeaders() {
    Headers headers =
        new Headers.Builder().add("X-Test", "abc").add("Content-Type", "text/plain").build();
    Response okResp =
        new Response.Builder()
            .request(new okhttp3.Request.Builder().url("https://example.org").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .headers(headers)
            .body(ResponseBody.create("body", MediaType.get("text/plain")))
            .build();

    ZetaHttpResponse resp = new ZetaHttpResponse(okResp);
    Map<String, List<String>> headerMap = resp.getHeaders();
    assertEquals(List.of("abc"), headerMap.get("X-Test"));
    assertEquals(List.of("text/plain"), headerMap.get("Content-Type"));
  }

  @Test
  void testIsSuccessful() {
    assertTrue(new ZetaHttpResponse(200, "").isSuccessful());
    assertFalse(new ZetaHttpResponse(404, "").isSuccessful());
  }

  @Test
  void testGetBodyFromJsonReturnsEmptyOnError() {
    ZetaHttpResponse resp = new ZetaHttpResponse(200, "not a json");
    assertTrue(resp.getBodyFromJson(Map.class).isEmpty());
  }
}
