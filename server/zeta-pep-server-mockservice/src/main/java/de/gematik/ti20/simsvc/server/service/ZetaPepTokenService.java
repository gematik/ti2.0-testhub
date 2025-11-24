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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.server.config.PoppConfig;
import de.gematik.ti20.simsvc.server.config.SecurityConfig;
import de.gematik.ti20.simsvc.server.model.AccessToken;
import de.gematik.ti20.simsvc.server.model.PoppToken;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ZetaPepTokenService {

  private final SecurityConfig secConfig;
  private final PoppConfig poppConfig;

  private static KeyStore keyStore;

  private static final ObjectMapper jsonMapper = new ObjectMapper();

  public ZetaPepTokenService(
      @Autowired SecurityConfig secConfig, @Autowired PoppConfig poppConfig) {
    this.secConfig = secConfig;
    this.poppConfig = poppConfig;
    init();
  }

  private void init() {
    try {
      log.debug("Initializing KeyStore from path: {}", secConfig.getStore().getPath());

      keyStore = KeyStore.getInstance("PKCS12");
      try (InputStream is =
          getClass().getClassLoader().getResourceAsStream(secConfig.getStore().getPath())) {
        keyStore.load(is, secConfig.getStore().getPass().toCharArray());
      }

      log.debug("KeyStore successfully loaded.");
    } catch (Exception e) {
      log.error("Error initializing KeyStore: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to initialize KeyStore", e);
    }
  }

  public AccessToken validateAccessToken(String authzHeader) throws ResponseStatusException {
    if (!StringUtils.hasText(authzHeader)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AccessToken is empty");
    }

    try {
      String jwt = authzHeader.substring(authzHeader.indexOf(" ") + 1);
      return AccessToken.fromJwt(jwt, keyStore);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
  }

  public PoppToken validatePoppToken(String poppToken, AccessToken at)
      throws ResponseStatusException {
    if (!poppConfig.isRequired()) {
      return null;
    }

    if (!StringUtils.hasText(poppToken)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing PoPP auth");
    }

    try {
      PoppToken pt = PoppToken.fromJwt(poppToken, keyStore);

      if (!pt.getClaims().getActorId().equals(at.getClaims().getClientId())) {
        log.error(
            "PoPP token actorId does not match AccessToken clientId: {} != {}",
            pt.getClaims().getActorId(),
            at.getClaims().getClientId());
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "PoPP token actorId does not match AccessToken clientId");
      }

      return pt;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
  }

  public String convertToBase64Json(Object object) throws JsonProcessingException {
    if (object == null) {
      return null;
    }

    String jsonString = jsonMapper.writeValueAsString(object);

    return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonString.getBytes());
  }
}
