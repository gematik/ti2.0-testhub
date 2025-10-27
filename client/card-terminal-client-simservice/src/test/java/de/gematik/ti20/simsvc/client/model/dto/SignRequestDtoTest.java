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
package de.gematik.ti20.simsvc.client.model.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SignRequestDtoTest {

  @Test
  void testDefaultConstructorAndSetters() {
    SignRequestDto dto = new SignRequestDto();
    dto.setData("dGVzdA==");
    Map<String, String> opts = new HashMap<>();
    opts.put("alg", "RS256");
    dto.setOptions(opts);

    assertEquals("dGVzdA==", dto.getData());
    assertEquals(1, dto.getOptions().size());
    assertEquals("RS256", dto.getOption("alg"));
  }

  @Test
  void testConstructorWithData() {
    SignRequestDto dto = new SignRequestDto("YmFzZTY0");
    assertEquals("YmFzZTY0", dto.getData());
    assertTrue(dto.getOptions().isEmpty());
  }

  @Test
  void testConstructorWithDataAndOptions() {
    Map<String, String> opts = new HashMap<>();
    opts.put("alg", "ES256");
    SignRequestDto dto = new SignRequestDto("ZGF0YQ==", opts);

    assertEquals("ZGF0YQ==", dto.getData());
    assertEquals(1, dto.getOptions().size());
    assertEquals("ES256", dto.getOption("alg"));
  }

  @Test
  void testAddAndGetOption() {
    SignRequestDto dto = new SignRequestDto();
    dto.addOption("format", "DER");
    assertEquals("DER", dto.getOption("format"));
    assertNull(dto.getOption("missing"));
    assertEquals("default", dto.getOption("missing", "default"));
  }

  @Test
  void testOptionsAreDefensiveCopies() {
    Map<String, String> opts = new HashMap<>();
    opts.put("foo", "bar");
    SignRequestDto dto = new SignRequestDto("abc", opts);

    Map<String, String> returned = dto.getOptions();
    returned.put("foo", "changed");
    assertEquals("bar", dto.getOption("foo"));

    opts.put("foo", "baz");
    assertEquals("bar", dto.getOption("foo"));
  }
}
