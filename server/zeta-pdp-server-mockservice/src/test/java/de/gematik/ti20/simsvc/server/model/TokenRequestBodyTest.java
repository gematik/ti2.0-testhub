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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenRequestBodyTest {

  @Test
  void testNoArgsConstructor() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();

    assertNotNull(tokenRequestBody);
    assertNull(tokenRequestBody.getGrant_type());
    assertNull(tokenRequestBody.getRequested_token_type());
    assertNull(tokenRequestBody.getSubject_token());
    assertNull(tokenRequestBody.getSubject_token_type());
  }

  @Test
  void testAllArgsConstructor() {
    String grantType = "urn:ietf:params:oauth:grant-type:token-exchange";
    String requestedTokenType = "urn:ietf:params:oauth:token-type:access_token";
    String subjectToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    String subjectTokenType = "urn:ietf:params:oauth:token-type:access_token";

    TokenRequestBody tokenRequestBody =
        new TokenRequestBody(grantType, requestedTokenType, subjectToken, subjectTokenType);

    assertEquals(grantType, tokenRequestBody.getGrant_type());
    assertEquals(requestedTokenType, tokenRequestBody.getRequested_token_type());
    assertEquals(subjectToken, tokenRequestBody.getSubject_token());
    assertEquals(subjectTokenType, tokenRequestBody.getSubject_token_type());
  }

  @Test
  void testGetterAndSetterGrantType() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();
    String grantType = "urn:ietf:params:oauth:grant-type:token-exchange";

    tokenRequestBody.setGrant_type(grantType);

    assertEquals(grantType, tokenRequestBody.getGrant_type());
  }

  @Test
  void testGetterAndSetterRequestedTokenType() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();
    String requestedTokenType = "urn:ietf:params:oauth:token-type:access_token";

    tokenRequestBody.setRequested_token_type(requestedTokenType);

    assertEquals(requestedTokenType, tokenRequestBody.getRequested_token_type());
  }

  @Test
  void testGetterAndSetterSubjectToken() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();
    String subjectToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";

    tokenRequestBody.setSubject_token(subjectToken);

    assertEquals(subjectToken, tokenRequestBody.getSubject_token());
  }

  @Test
  void testGetterAndSetterSubjectTokenType() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();
    String subjectTokenType = "urn:ietf:params:oauth:token-type:access_token";

    tokenRequestBody.setSubject_token_type(subjectTokenType);

    assertEquals(subjectTokenType, tokenRequestBody.getSubject_token_type());
  }

  @Test
  void testSettersWithNullValues() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();

    tokenRequestBody.setGrant_type(null);
    tokenRequestBody.setRequested_token_type(null);
    tokenRequestBody.setSubject_token(null);
    tokenRequestBody.setSubject_token_type(null);

    assertNull(tokenRequestBody.getGrant_type());
    assertNull(tokenRequestBody.getRequested_token_type());
    assertNull(tokenRequestBody.getSubject_token());
    assertNull(tokenRequestBody.getSubject_token_type());
  }

  @Test
  void testToString() {
    TokenRequestBody tokenRequestBody =
        new TokenRequestBody(
            "grant_type_value",
            "requested_token_type_value",
            "subject_token_value",
            "subject_token_type_value");

    String toString = tokenRequestBody.toString();

    assertNotNull(toString);
    assertTrue(toString.contains("grant_type_value"));
    assertTrue(toString.contains("requested_token_type_value"));
    assertTrue(toString.contains("subject_token_value"));
    assertTrue(toString.contains("subject_token_type_value"));
  }

  @Test
  void testToStringWithNullValues() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();

    String toString = tokenRequestBody.toString();

    assertNotNull(toString);
    assertTrue(toString.contains("TokenRequestBody"));
  }

  @Test
  void testPackageStructure() {
    assertEquals(
        "de.gematik.ti20.simsvc.server.model", TokenRequestBody.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(TokenRequestBody.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            TokenRequestBody.class.getDeclaredField("grant_type").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            TokenRequestBody.class.getDeclaredField("requested_token_type").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            TokenRequestBody.class.getDeclaredField("subject_token").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            TokenRequestBody.class.getDeclaredField("subject_token_type").getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, TokenRequestBody.class.getDeclaredField("grant_type").getType());
    assertEquals(
        String.class, TokenRequestBody.class.getDeclaredField("requested_token_type").getType());
    assertEquals(String.class, TokenRequestBody.class.getDeclaredField("subject_token").getType());
    assertEquals(
        String.class, TokenRequestBody.class.getDeclaredField("subject_token_type").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(4, TokenRequestBody.class.getDeclaredFields().length);
  }

  @Test
  void testCompleteTokenRequestBodyUsage() {
    TokenRequestBody tokenRequestBody = new TokenRequestBody();

    tokenRequestBody.setGrant_type("urn:ietf:params:oauth:grant-type:token-exchange");
    tokenRequestBody.setRequested_token_type("urn:ietf:params:oauth:token-type:access_token");
    tokenRequestBody.setSubject_token("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");
    tokenRequestBody.setSubject_token_type("urn:ietf:params:oauth:token-type:access_token");

    assertEquals(
        "urn:ietf:params:oauth:grant-type:token-exchange", tokenRequestBody.getGrant_type());
    assertEquals(
        "urn:ietf:params:oauth:token-type:access_token",
        tokenRequestBody.getRequested_token_type());
    assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9", tokenRequestBody.getSubject_token());
    assertEquals(
        "urn:ietf:params:oauth:token-type:access_token", tokenRequestBody.getSubject_token_type());
  }
}
