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
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PoppTokenTest {

  private JwtClaims mockJwtClaims;
  private PoppToken poppToken;

  @BeforeEach
  void setUp() {
    mockJwtClaims = mock(JwtClaims.class);
    poppToken = new PoppToken(mockJwtClaims);
  }

  @Test
  void testConstructor() {
    assertNotNull(poppToken);
  }

  @Test
  void testValidate() {
    assertDoesNotThrow(() -> poppToken.validate());
  }

  @Test
  void testGetClaimValue_Success() throws MalformedClaimException {
    String claimName = "testClaim";
    String expectedValue = "testValue";
    when(mockJwtClaims.getStringClaimValue(claimName)).thenReturn(expectedValue);

    String result = poppToken.getClaimValue(claimName);

    assertEquals(expectedValue, result);
    verify(mockJwtClaims).getStringClaimValue(claimName);
  }

  @Test
  void testGetClaimValue_ThrowsException() throws MalformedClaimException {
    String claimName = "invalidClaim";
    when(mockJwtClaims.getStringClaimValue(claimName))
        .thenThrow(new MalformedClaimException("Invalid claim"));

    assertThrows(MalformedClaimException.class, () -> poppToken.getClaimValue(claimName));
  }

  @Test
  void testParseValidToken() {
    String validToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjk5OTk5OTk5OTl9.Lf3MwLM8-sTGGl5xjCUHs-6A2MdgW6HT2V1Zx2J5Y8g";

    assertDoesNotThrow(() -> PoppToken.parse(validToken));
  }

  @Test
  void testParseInvalidToken() {
    String invalidToken = "invalid.jwt.token";

    assertThrows(InvalidJwtException.class, () -> PoppToken.parse(invalidToken));
  }

  @Test
  void testParseEmptyToken() {
    String emptyToken = "";

    assertThrows(InvalidJwtException.class, () -> PoppToken.parse(emptyToken));
  }

  @Test
  void testParseNullToken() {
    assertThrows(InvalidJwtException.class, () -> PoppToken.parse(null));
  }

  @Test
  void testParseMalformedToken() {
    String malformedToken = "not.a.jwt";

    assertThrows(InvalidJwtException.class, () -> PoppToken.parse(malformedToken));
  }
}
