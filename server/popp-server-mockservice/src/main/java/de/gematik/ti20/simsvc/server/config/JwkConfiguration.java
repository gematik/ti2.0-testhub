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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwkConfiguration {

  /**
   * We are using a key pair to sign PoPP tokens. This method returns the public key portion as a
   * JWK. Using the JWK our PoPP tokens can be verified by external systems.
   */
  @Bean(name = "publicJwk")
  public JsonWebKeySet jwkSource(final PoppConfig poppConfig) {
    final PoppConfig.SecurityConfig securityConfig = poppConfig.getSec();
    final String keyStoryPath = securityConfig.getStore().getPath();
    final String keyStoryPass = securityConfig.getStore().getPass();
    final String keyId = securityConfig.getKey().getAlias();
    try {
      final KeyStore keyStore = KeyStore.getInstance("PKCS12");
      try (final InputStream fis = getClass().getClassLoader().getResourceAsStream(keyStoryPath)) {
        keyStore.load(fis, keyStoryPass.toCharArray());
      }

      final X509Certificate certificate = (X509Certificate) keyStore.getCertificate(keyId);
      final PublicKey publicKey = certificate.getPublicKey();
      final JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk(publicKey);
      jsonWebKey.setKeyId(keyId);
      return new JsonWebKeySet(jsonWebKey);
    } catch (final GeneralSecurityException | IOException | JoseException e) {
      throw new IllegalStateException("Failed to initialize KeyStore", e);
    }
  }
}
