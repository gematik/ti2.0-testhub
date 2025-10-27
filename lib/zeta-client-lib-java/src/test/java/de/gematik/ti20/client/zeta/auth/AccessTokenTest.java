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

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.Test;

class AccessTokenTest {

  @Test
  void testCreateReturnsValidJwt() throws Exception {
    String token = AccessToken.create();
    assertNotNull(token);
    assertTrue(token.split("\\.").length == 3, "Token ist kein JWT");

    // JWT validieren und Claims prüfen
    JwtConsumer consumer =
        new JwtConsumerBuilder()
            .setSkipSignatureVerification() // Nur Struktur und Claims prüfen
            .setSkipAllValidators()
            .build();

    JwtClaims claims = consumer.processToClaims(token);

    assertEquals("http://test.authz.server.local", claims.getIssuer());
    assertEquals("read write todo", claims.getStringClaimValue("scope"));
    assertNotNull(claims.getJwtId());
    assertNotNull(claims.getExpirationTime());
    assertNotNull(claims.getIssuedAt());
    assertTrue(claims.getAudience().contains("http://zeta.guard.popp.local"));
    assertTrue(claims.getAudience().contains("https://zeta.guard.vsdm.local"));
  }
}
