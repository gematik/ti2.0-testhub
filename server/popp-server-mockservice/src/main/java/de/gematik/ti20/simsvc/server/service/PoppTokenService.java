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

import de.gematik.ti20.simsvc.server.config.PoppConfig;
import de.gematik.ti20.simsvc.server.model.PoppToken;
import de.gematik.ti20.simsvc.server.model.SecurityParams;
import de.gematik.ti20.simsvc.server.model.TokenParams;
import java.io.InputStream;
import java.security.KeyStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PoppTokenService {

  private PoppConfig poppConfig;
  private static KeyStore keyStore;

  public PoppTokenService(final PoppConfig poppConfig) {
    this.poppConfig = poppConfig;
    init();
  }

  private void init() {
    try {
      log.debug("Initializing KeyStore from path: {}", poppConfig.getSec().getStore().getPath());

      keyStore = KeyStore.getInstance("PKCS12");
      final String keyStoryPath = poppConfig.getSec().getStore().getPath();
      final String keyStoryPass = poppConfig.getSec().getStore().getPass();

      try (final InputStream fis = getClass().getClassLoader().getResourceAsStream(keyStoryPath)) {
        keyStore.load(fis, keyStoryPass.toCharArray());
      }

      log.debug("KeyStore successfully loaded.");
    } catch (final Exception e) {
      log.error("Error initializing KeyStore: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to initialize KeyStore", e);
    }
  }

  public String createToken(final TokenParams tokenParams) throws Exception {
    return createToken(tokenParams, null);
  }

  public String createToken(final TokenParams tokenParams, final SecurityParams securityParams)
      throws Exception {
    log.debug(
        "Creating POPP token for patientId: {}, insurerId: {}, actorId: {}, actorProfessionOid: {}",
        tokenParams.getPatientId(),
        tokenParams.getInsurerId(),
        tokenParams.getActorId(),
        tokenParams.getActorProfessionOid());

    final PoppToken.TokenClaims claims =
        new PoppToken.TokenClaims(
            tokenParams.getProofMethod(),
            tokenParams.getPatientProofTime(),
            tokenParams.getIat(),
            tokenParams.getPatientId(),
            tokenParams.getInsurerId(),
            tokenParams.getActorId(),
            tokenParams.getActorProfessionOid());

    String result = null;
    if (securityParams == null) {
      PoppConfig.KeyConfig keyConfig = poppConfig.getSec().getKey();
      PoppToken.TokenHeader header = new PoppToken.TokenHeader(keyConfig.getAlias());
      final PoppToken poppToken = new PoppToken(header, claims);

      result = poppToken.toJwt(keyStore, keyConfig.getAlias(), keyConfig.getPass().toCharArray());
    } else {
      PoppToken.TokenHeader header = new PoppToken.TokenHeader(securityParams.getKeyAlias());
      final PoppToken poppToken = new PoppToken(header, claims);
      final KeyStore tmpKeyStore = buildKeyStore(securityParams);
      result =
          poppToken.toJwt(
              tmpKeyStore, securityParams.getKeyAlias(), securityParams.getKeyPass().toCharArray());
    }

    return result;
  }

  private KeyStore buildKeyStore(final SecurityParams securityParams) throws Exception {
    final byte[] decoded = java.util.Base64.getDecoder().decode(securityParams.getStoreContent());
    final InputStream kis = new java.io.ByteArrayInputStream(decoded);

    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(kis, securityParams.getKeyPass().toCharArray());

    return keyStore;
  }
}
