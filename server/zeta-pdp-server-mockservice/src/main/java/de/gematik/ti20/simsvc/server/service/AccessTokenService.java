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

import de.gematik.ti20.simsvc.server.config.AuthzConfig;
import de.gematik.ti20.simsvc.server.model.AccessToken;
import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccessTokenService {

  private AuthzConfig authzConfig;
  private static KeyStore keyStore;

  public AccessTokenService(final AuthzConfig authzConfig) {
    this.authzConfig = authzConfig;
    init();
  }

  private void init() {
    try {
      log.debug("Initializing KeyStore from path: {}", authzConfig.getSec().getStore().getPath());

      keyStore = KeyStore.getInstance("PKCS12");
      try (InputStream is =
          getClass()
              .getClassLoader()
              .getResourceAsStream(authzConfig.getSec().getStore().getPath())) {
        keyStore.load(is, authzConfig.getSec().getStore().getPass().toCharArray());
      }

      log.debug("KeyStore successfully loaded.");
    } catch (Exception e) {
      log.error("Error initializing KeyStore: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to initialize KeyStore", e);
    }
  }

  public String createToken(final HttpServletRequest request, final String smcBAccessToken)
      throws Exception {
    AuthzConfig.KeyConfig keyConfig = authzConfig.getSec().getKey();

    AccessToken.TokenHeader header = new AccessToken.TokenHeader(keyConfig.getAlias());

    // TODO: hier müsste die Signatur richtig verifiziert werden, aber für den Mock-Service
    // überspringen wir das
    JwtConsumer jwtConsumer =
        new JwtConsumerBuilder()
            .setSkipSignatureVerification()
            .setSkipDefaultAudienceValidation()
            .build();

    JwtClaims smcbClaims = jwtConsumer.processToClaims(smcBAccessToken);

    AccessToken.TokenClaims claims =
        new AccessToken.TokenClaims(
            "PDP-MockService",
            List.of("PEP-MockService"),
            UUID.randomUUID().toString(),
            smcbClaims.getSubject(),
            "",
            "",
            smcbClaims.getStringClaimValue("professionOid"));

    final AccessToken token = new AccessToken(header, claims);
    final String jwt =
        token.toJwt(keyStore, keyConfig.getAlias(), keyConfig.getPass().toCharArray());

    return jwt;
  }
}
