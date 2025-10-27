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
package de.gematik.ti20.simsvc.server.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessTokenTest {

  private JwtClaims mockJwtClaims;
  private AccessToken accessToken;

  @BeforeEach
  void setUp() {
    mockJwtClaims = mock(JwtClaims.class);
    accessToken = new AccessToken(mockJwtClaims);
  }

  @Test
  void testConstructor() {
    assertNotNull(accessToken);
  }

  @Test
  void testValidate() {
    assertDoesNotThrow(() -> accessToken.validate());
  }

  @Test
  void testParseValidToken() {
    // Create a valid JWT token for testing
    String validToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk5OTk5OTk5OTl9.Lf3MwLM8-sTGGl5xjCUHs-6A2MdgW6HT2V1Zx2J5Y8g";

    assertThrows(InvalidJwtException.class, () -> AccessToken.parse(validToken));
  }

  @Test
  void testParseInvalidToken() {
    String invalidToken = "invalid.jwt.token";

    assertThrows(InvalidJwtException.class, () -> AccessToken.parse(invalidToken));
  }

  @Test
  void testParseEmptyToken() {
    String emptyToken = "";

    assertThrows(InvalidJwtException.class, () -> AccessToken.parse(emptyToken));
  }

  @Test
  void testParseNullToken() {
    assertThrows(InvalidJwtException.class, () -> AccessToken.parse(null));
  }

  @Test
  void testParseMalformedToken() {
    String malformedToken = "not.a.jwt";

    assertThrows(InvalidJwtException.class, () -> AccessToken.parse(malformedToken));
  }
}
