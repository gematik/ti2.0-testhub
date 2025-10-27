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
package de.gematik.ti20.client.zeta.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Map;
import org.jose4j.json.JsonUtil;
import org.junit.jupiter.api.Test;

class ClientAssertionTokenTest {

  @Test
  void testCreateSetsClaimsAndJkt() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
    kpg.initialize(256);
    KeyPair keyPair = kpg.generateKeyPair();

    System.setProperty("org.jose4j.jws.getPayload-skip-verify", "true");
    ClientAssertionToken token = ClientAssertionToken.create("nonce", keyPair);
    assertNotNull(token.getPayload());

    Map<String, Object> claims = JsonUtil.parseJson(token.getPayload());

    assertEquals("Client Instance Name", claims.get("name"));
    assertEquals("client-123", claims.get("client_id"));
    assertEquals("product-456", claims.get("product_id"));
    assertEquals("Product Name", claims.get("product_name"));
    assertEquals("1.0.0", claims.get("product_version"));
    assertEquals("manufacturer-789", claims.get("manufacturer_id"));
    assertEquals("Manufacturer Name", claims.get("manufacturer_name"));
    assertEquals("Owner Information", claims.get("owner"));
    assertEquals("owner@example.com", claims.get("owner_mail"));
    assertNotNull(claims.get("registration_timestamp"));
    assertEquals("Platform Name", claims.get("platform"));
    assertEquals("platform-product-id", claims.get("platform_product_id"));
    assertEquals("Posture Information", claims.get("posture"));
    assertEquals("Attestation Information", claims.get("attestation"));
    assertNotNull(claims.get("jkt"));
  }
}
