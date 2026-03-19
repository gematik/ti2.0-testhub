/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.ti20.simsvc.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.jose4j.jwk.JsonWebKeySet;
import org.junit.jupiter.api.Test;

class JwkConfigurationTest {

  @Test
  void thatAValidJwkSetIsReturned() {
    final PoppConfig.KeyConfig keyConfig = new PoppConfig.KeyConfig();
    keyConfig.setAlias("alias");
    keyConfig.setPass("00");

    final PoppConfig.StoreConfig storeConfig = new PoppConfig.StoreConfig();
    storeConfig.setPass("00");
    storeConfig.setPath("popp-token-Server-Sim-nist-komp61.p12");

    final PoppConfig.SecurityConfig securityConfig = new PoppConfig.SecurityConfig();
    securityConfig.setKey(keyConfig);
    securityConfig.setStore(storeConfig);

    final PoppConfig poppConfig = new PoppConfig();
    poppConfig.setSec(securityConfig);

    final JwkConfiguration jwkConfiguration = new JwkConfiguration();
    final JsonWebKeySet actual = jwkConfiguration.jwkSource(poppConfig);
    assertThat(actual.getJsonWebKeys()).hasSize(1);
    assertThat(actual.getJsonWebKeys().getFirst().getKeyId()).isEqualTo("alias");
  }
}
