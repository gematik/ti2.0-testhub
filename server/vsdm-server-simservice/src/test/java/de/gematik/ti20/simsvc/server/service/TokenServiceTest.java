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
package de.gematik.ti20.simsvc.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.model.PoppToken;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  private TokenService tokenService;

  @BeforeEach
  void setUp() {
    tokenService = new TokenService();
  }

  @Test
  void testParsePoppToken_ValidToken() throws InvalidJwtException {
    String validToken = "valid.jwt.token";
    PoppToken mockPoppToken = mock(PoppToken.class);

    try (MockedStatic<PoppToken> poppTokenMock = mockStatic(PoppToken.class)) {
      poppTokenMock.when(() -> PoppToken.parse(validToken)).thenReturn(mockPoppToken);

      PoppToken result = tokenService.parsePoppToken(validToken);

      assertNotNull(result);
      assertEquals(mockPoppToken, result);
      poppTokenMock.verify(() -> PoppToken.parse(validToken));
    }
  }

  @Test
  void testParsePoppToken_InvalidToken() throws InvalidJwtException {
    String invalidToken = "invalid.jwt.token";
    InvalidJwtException exception = mock(InvalidJwtException.class);

    try (MockedStatic<PoppToken> poppTokenMock = mockStatic(PoppToken.class)) {
      poppTokenMock.when(() -> PoppToken.parse(invalidToken)).thenThrow(exception);

      ResponseStatusException thrown =
          assertThrows(
              ResponseStatusException.class, () -> tokenService.parsePoppToken(invalidToken));

      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, thrown.getStatusCode());
      assertEquals("Invalid PoPP auth", thrown.getReason());
      poppTokenMock.verify(() -> PoppToken.parse(invalidToken));
    }
  }

  @Test
  void testParsePoppToken_EmptyToken() {
    String emptyToken = "";

    PoppToken result = tokenService.parsePoppToken(emptyToken);

    assertNull(result);
  }

  @Test
  void testParsePoppToken_NullToken() {
    PoppToken result = tokenService.parsePoppToken(null);

    assertNull(result);
  }

  @Test
  void testParsePoppToken_WhitespaceToken() {
    String whitespaceToken = "   ";

    PoppToken result = tokenService.parsePoppToken(whitespaceToken);

    assertNull(result);
  }

  @Test
  void testParsePoppToken_BlankToken() {
    String blankToken = "\t\n ";

    PoppToken result = tokenService.parsePoppToken(blankToken);

    assertNull(result);
  }
}
