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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WellKnownTest {

  private WellKnown wellKnown;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    wellKnown = new WellKnown();
    objectMapper = new ObjectMapper();
  }

  @Test
  void testNoArgsConstructor() {
    assertNotNull(wellKnown);
    assertNull(wellKnown.getIssuer());
    assertNull(wellKnown.getAuthorization_endpoint());
    assertNull(wellKnown.getToken_endpoint());
    assertNull(wellKnown.getJwks_uri());
    assertNull(wellKnown.getNonce_endpoint());
    assertNull(wellKnown.getOpenid_providers_endpoint());
    assertNull(wellKnown.getScopes_supported());
    assertNull(wellKnown.getResponse_types_supported());
    assertNull(wellKnown.getResponse_modes_supported());
    assertNull(wellKnown.getGrant_types_supported());
    assertNull(wellKnown.getToken_endpoint_auth_methods_supported());
    assertNull(wellKnown.getToken_endpoint_auth_signing_alg_values_supported());
    assertNull(wellKnown.getService_documentation());
    assertNull(wellKnown.getUi_locales_supported());
    assertNull(wellKnown.getCode_challenge_methods_supported());
  }

  @Test
  void testAllArgsConstructor() {
    List<String> scopes = Arrays.asList("openid", "profile");
    List<String> responseTypes = Arrays.asList("code", "token");
    List<String> responseModes = Arrays.asList("query", "fragment");
    List<String> grantTypes = Arrays.asList("authorization_code", "refresh_token");
    List<String> authMethods = Arrays.asList("client_secret_basic", "client_secret_post");
    List<String> signingAlgs = Arrays.asList("RS256", "ES256");
    List<String> locales = Arrays.asList("de", "en");
    List<String> challengeMethods = Arrays.asList("S256", "plain");

    WellKnown wellKnown =
        new WellKnown(
            "https://example.com",
            "https://example.com/auth",
            "https://example.com/token",
            "https://example.com/jwks",
            "https://example.com/nonce",
            "https://example.com/providers",
            scopes,
            responseTypes,
            responseModes,
            grantTypes,
            authMethods,
            signingAlgs,
            "https://example.com/docs",
            locales,
            challengeMethods);

    assertEquals("https://example.com", wellKnown.getIssuer());
    assertEquals("https://example.com/auth", wellKnown.getAuthorization_endpoint());
    assertEquals("https://example.com/token", wellKnown.getToken_endpoint());
    assertEquals("https://example.com/jwks", wellKnown.getJwks_uri());
    assertEquals("https://example.com/nonce", wellKnown.getNonce_endpoint());
    assertEquals("https://example.com/providers", wellKnown.getOpenid_providers_endpoint());
    assertEquals(scopes, wellKnown.getScopes_supported());
    assertEquals(responseTypes, wellKnown.getResponse_types_supported());
    assertEquals(responseModes, wellKnown.getResponse_modes_supported());
    assertEquals(grantTypes, wellKnown.getGrant_types_supported());
    assertEquals(authMethods, wellKnown.getToken_endpoint_auth_methods_supported());
    assertEquals(signingAlgs, wellKnown.getToken_endpoint_auth_signing_alg_values_supported());
    assertEquals("https://example.com/docs", wellKnown.getService_documentation());
    assertEquals(locales, wellKnown.getUi_locales_supported());
    assertEquals(challengeMethods, wellKnown.getCode_challenge_methods_supported());
  }

  @Test
  void testGettersAndSetters() {
    wellKnown.setIssuer("https://test.com");
    wellKnown.setAuthorization_endpoint("https://test.com/auth");
    wellKnown.setToken_endpoint("https://test.com/token");
    wellKnown.setJwks_uri("https://test.com/jwks");
    wellKnown.setNonce_endpoint("https://test.com/nonce");
    wellKnown.setOpenid_providers_endpoint("https://test.com/providers");
    wellKnown.setService_documentation("https://test.com/docs");

    assertEquals("https://test.com", wellKnown.getIssuer());
    assertEquals("https://test.com/auth", wellKnown.getAuthorization_endpoint());
    assertEquals("https://test.com/token", wellKnown.getToken_endpoint());
    assertEquals("https://test.com/jwks", wellKnown.getJwks_uri());
    assertEquals("https://test.com/nonce", wellKnown.getNonce_endpoint());
    assertEquals("https://test.com/providers", wellKnown.getOpenid_providers_endpoint());
    assertEquals("https://test.com/docs", wellKnown.getService_documentation());
  }

  @Test
  void testListGettersAndSetters() {
    List<String> scopes = Arrays.asList("openid", "profile");
    List<String> responseTypes = Arrays.asList("code");
    List<String> responseModes = Arrays.asList("query");
    List<String> grantTypes = Arrays.asList("authorization_code");
    List<String> authMethods = Arrays.asList("client_secret_basic");
    List<String> signingAlgs = Arrays.asList("RS256");
    List<String> locales = Arrays.asList("de");
    List<String> challengeMethods = Arrays.asList("S256");

    wellKnown.setScopes_supported(scopes);
    wellKnown.setResponse_types_supported(responseTypes);
    wellKnown.setResponse_modes_supported(responseModes);
    wellKnown.setGrant_types_supported(grantTypes);
    wellKnown.setToken_endpoint_auth_methods_supported(authMethods);
    wellKnown.setToken_endpoint_auth_signing_alg_values_supported(signingAlgs);
    wellKnown.setUi_locales_supported(locales);
    wellKnown.setCode_challenge_methods_supported(challengeMethods);

    assertEquals(scopes, wellKnown.getScopes_supported());
    assertEquals(responseTypes, wellKnown.getResponse_types_supported());
    assertEquals(responseModes, wellKnown.getResponse_modes_supported());
    assertEquals(grantTypes, wellKnown.getGrant_types_supported());
    assertEquals(authMethods, wellKnown.getToken_endpoint_auth_methods_supported());
    assertEquals(signingAlgs, wellKnown.getToken_endpoint_auth_signing_alg_values_supported());
    assertEquals(locales, wellKnown.getUi_locales_supported());
    assertEquals(challengeMethods, wellKnown.getCode_challenge_methods_supported());
  }

  @Test
  void testNullValues() {
    wellKnown.setIssuer(null);
    wellKnown.setAuthorization_endpoint(null);
    wellKnown.setToken_endpoint(null);
    wellKnown.setJwks_uri(null);
    wellKnown.setNonce_endpoint(null);
    wellKnown.setOpenid_providers_endpoint(null);
    wellKnown.setScopes_supported(null);
    wellKnown.setResponse_types_supported(null);
    wellKnown.setResponse_modes_supported(null);
    wellKnown.setGrant_types_supported(null);
    wellKnown.setToken_endpoint_auth_methods_supported(null);
    wellKnown.setToken_endpoint_auth_signing_alg_values_supported(null);
    wellKnown.setService_documentation(null);
    wellKnown.setUi_locales_supported(null);
    wellKnown.setCode_challenge_methods_supported(null);

    assertNull(wellKnown.getIssuer());
    assertNull(wellKnown.getAuthorization_endpoint());
    assertNull(wellKnown.getToken_endpoint());
    assertNull(wellKnown.getJwks_uri());
    assertNull(wellKnown.getNonce_endpoint());
    assertNull(wellKnown.getOpenid_providers_endpoint());
    assertNull(wellKnown.getScopes_supported());
    assertNull(wellKnown.getResponse_types_supported());
    assertNull(wellKnown.getResponse_modes_supported());
    assertNull(wellKnown.getGrant_types_supported());
    assertNull(wellKnown.getToken_endpoint_auth_methods_supported());
    assertNull(wellKnown.getToken_endpoint_auth_signing_alg_values_supported());
    assertNull(wellKnown.getService_documentation());
    assertNull(wellKnown.getUi_locales_supported());
    assertNull(wellKnown.getCode_challenge_methods_supported());
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    wellKnown.setIssuer("https://example.com");
    wellKnown.setAuthorization_endpoint("https://example.com/auth");
    wellKnown.setToken_endpoint("https://example.com/token");
    wellKnown.setScopes_supported(Arrays.asList("openid", "profile"));

    String json = objectMapper.writeValueAsString(wellKnown);

    assertNotNull(json);
    assertTrue(json.contains("\"issuer\":\"https://example.com\""));
    assertTrue(json.contains("\"authorization_endpoint\":\"https://example.com/auth\""));
    assertTrue(json.contains("\"token_endpoint\":\"https://example.com/token\""));
    assertTrue(json.contains("\"scopes_supported\":[\"openid\",\"profile\"]"));
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    String json =
        "{\"issuer\":\"https://example.com\",\"authorization_endpoint\":\"https://example.com/auth\",\"token_endpoint\":\"https://example.com/token\",\"scopes_supported\":[\"openid\",\"profile\"]}";

    WellKnown deserializedWellKnown = objectMapper.readValue(json, WellKnown.class);

    assertEquals("https://example.com", deserializedWellKnown.getIssuer());
    assertEquals("https://example.com/auth", deserializedWellKnown.getAuthorization_endpoint());
    assertEquals("https://example.com/token", deserializedWellKnown.getToken_endpoint());
    assertEquals(Arrays.asList("openid", "profile"), deserializedWellKnown.getScopes_supported());
  }

  @Test
  void testJsonIgnoreUnknownProperties() throws JsonProcessingException {
    String json =
        "{\"issuer\":\"https://example.com\",\"unknown_field\":\"ignored\",\"authorization_endpoint\":\"https://example.com/auth\"}";

    WellKnown deserializedWellKnown = objectMapper.readValue(json, WellKnown.class);

    assertEquals("https://example.com", deserializedWellKnown.getIssuer());
    assertEquals("https://example.com/auth", deserializedWellKnown.getAuthorization_endpoint());
  }

  @Test
  void testJsonIgnorePropertiesAnnotation() {
    assertTrue(WellKnown.class.isAnnotationPresent(JsonIgnoreProperties.class));
    JsonIgnoreProperties annotation = WellKnown.class.getAnnotation(JsonIgnoreProperties.class);
    assertTrue(annotation.ignoreUnknown());
  }

  @Test
  void testPackageStructure() {
    assertEquals("de.gematik.ti20.simsvc.server.model", WellKnown.class.getPackage().getName());
  }

  @Test
  void testClassIsPublic() {
    assertTrue(java.lang.reflect.Modifier.isPublic(WellKnown.class.getModifiers()));
  }

  @Test
  void testFieldsArePrivate() throws NoSuchFieldException {
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("issuer").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("authorization_endpoint").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("token_endpoint").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("jwks_uri").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("nonce_endpoint").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("openid_providers_endpoint").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("scopes_supported").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("response_types_supported").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("response_modes_supported").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("grant_types_supported").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class
                .getDeclaredField("token_endpoint_auth_methods_supported")
                .getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class
                .getDeclaredField("token_endpoint_auth_signing_alg_values_supported")
                .getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("service_documentation").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("ui_locales_supported").getModifiers()));
    assertTrue(
        java.lang.reflect.Modifier.isPrivate(
            WellKnown.class.getDeclaredField("code_challenge_methods_supported").getModifiers()));
  }

  @Test
  void testFieldTypes() throws NoSuchFieldException {
    assertEquals(String.class, WellKnown.class.getDeclaredField("issuer").getType());
    assertEquals(
        String.class, WellKnown.class.getDeclaredField("authorization_endpoint").getType());
    assertEquals(String.class, WellKnown.class.getDeclaredField("token_endpoint").getType());
    assertEquals(String.class, WellKnown.class.getDeclaredField("jwks_uri").getType());
    assertEquals(String.class, WellKnown.class.getDeclaredField("nonce_endpoint").getType());
    assertEquals(
        String.class, WellKnown.class.getDeclaredField("openid_providers_endpoint").getType());
    assertEquals(List.class, WellKnown.class.getDeclaredField("scopes_supported").getType());
    assertEquals(
        List.class, WellKnown.class.getDeclaredField("response_types_supported").getType());
    assertEquals(
        List.class, WellKnown.class.getDeclaredField("response_modes_supported").getType());
    assertEquals(List.class, WellKnown.class.getDeclaredField("grant_types_supported").getType());
    assertEquals(
        List.class,
        WellKnown.class.getDeclaredField("token_endpoint_auth_methods_supported").getType());
    assertEquals(
        List.class,
        WellKnown.class
            .getDeclaredField("token_endpoint_auth_signing_alg_values_supported")
            .getType());
    assertEquals(String.class, WellKnown.class.getDeclaredField("service_documentation").getType());
    assertEquals(List.class, WellKnown.class.getDeclaredField("ui_locales_supported").getType());
    assertEquals(
        List.class, WellKnown.class.getDeclaredField("code_challenge_methods_supported").getType());
  }

  @Test
  void testFieldCount() {
    assertEquals(15, WellKnown.class.getDeclaredFields().length);
  }

  @Test
  void testEmptyListValues() {
    List<String> emptyList = Arrays.asList();

    wellKnown.setScopes_supported(emptyList);
    wellKnown.setResponse_types_supported(emptyList);
    wellKnown.setResponse_modes_supported(emptyList);
    wellKnown.setGrant_types_supported(emptyList);
    wellKnown.setToken_endpoint_auth_methods_supported(emptyList);
    wellKnown.setToken_endpoint_auth_signing_alg_values_supported(emptyList);
    wellKnown.setUi_locales_supported(emptyList);
    wellKnown.setCode_challenge_methods_supported(emptyList);

    assertEquals(emptyList, wellKnown.getScopes_supported());
    assertEquals(emptyList, wellKnown.getResponse_types_supported());
    assertEquals(emptyList, wellKnown.getResponse_modes_supported());
    assertEquals(emptyList, wellKnown.getGrant_types_supported());
    assertEquals(emptyList, wellKnown.getToken_endpoint_auth_methods_supported());
    assertEquals(emptyList, wellKnown.getToken_endpoint_auth_signing_alg_values_supported());
    assertEquals(emptyList, wellKnown.getUi_locales_supported());
    assertEquals(emptyList, wellKnown.getCode_challenge_methods_supported());
  }

  @Test
  void testConstructorCount() {
    assertEquals(2, WellKnown.class.getDeclaredConstructors().length);
  }
}
