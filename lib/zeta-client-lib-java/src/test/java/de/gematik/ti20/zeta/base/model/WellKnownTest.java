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
package de.gematik.ti20.zeta.base.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class WellKnownTest {

  @Test
  void testGettersAndSetters() {
    WellKnown wellKnown = new WellKnown();

    wellKnown.setIssuer("issuer");
    wellKnown.setAuthorization_endpoint("auth_endpoint");
    wellKnown.setToken_endpoint("token_endpoint");
    wellKnown.setJwks_uri("jwks_uri");
    wellKnown.setNonce_endpoint("nonce_endpoint");
    wellKnown.setOpenid_providers_endpoint("openid_endpoint");
    wellKnown.setScopes_supported(List.of("scope1", "scope2"));
    wellKnown.setResponse_types_supported(List.of("code", "id_token"));
    wellKnown.setResponse_modes_supported(List.of("query", "fragment"));
    wellKnown.setGrant_types_supported(List.of("authorization_code"));
    wellKnown.setToken_endpoint_auth_methods_supported(List.of("client_secret_basic"));
    wellKnown.setToken_endpoint_auth_signing_alg_values_supported(List.of("RS256"));
    wellKnown.setService_documentation("service_doc");
    wellKnown.setUi_locales_supported(List.of("de", "en"));
    wellKnown.setCode_challenge_methods_supported(List.of("S256"));

    assertEquals("issuer", wellKnown.getIssuer());
    assertEquals("auth_endpoint", wellKnown.getAuthorization_endpoint());
    assertEquals("token_endpoint", wellKnown.getToken_endpoint());
    assertEquals("jwks_uri", wellKnown.getJwks_uri());
    assertEquals("nonce_endpoint", wellKnown.getNonce_endpoint());
    assertEquals("openid_endpoint", wellKnown.getOpenid_providers_endpoint());
    assertEquals(List.of("scope1", "scope2"), wellKnown.getScopes_supported());
    assertEquals(List.of("code", "id_token"), wellKnown.getResponse_types_supported());
    assertEquals(List.of("query", "fragment"), wellKnown.getResponse_modes_supported());
    assertEquals(List.of("authorization_code"), wellKnown.getGrant_types_supported());
    assertEquals(
        List.of("client_secret_basic"), wellKnown.getToken_endpoint_auth_methods_supported());
    assertEquals(List.of("RS256"), wellKnown.getToken_endpoint_auth_signing_alg_values_supported());
    assertEquals("service_doc", wellKnown.getService_documentation());
    assertEquals(List.of("de", "en"), wellKnown.getUi_locales_supported());
    assertEquals(List.of("S256"), wellKnown.getCode_challenge_methods_supported());
  }
}
