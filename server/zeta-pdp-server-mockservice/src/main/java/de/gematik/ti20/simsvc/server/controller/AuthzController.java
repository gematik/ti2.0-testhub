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
package de.gematik.ti20.simsvc.server.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.gematik.ti20.simsvc.server.model.TokenRequestBody;
import de.gematik.ti20.simsvc.server.service.AccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthzController {

  private final AccessTokenService accessTokenService;

  public AuthzController(AccessTokenService accessTokenService) {
    this.accessTokenService = accessTokenService;
  }

  @PostMapping(path = "/token")
  public ResponseEntity<?> getAccessToken(
      HttpServletRequest request, @ModelAttribute TokenRequestBody tokenRequestBody) {

    String accessToken = null;
    try {
      accessToken = accessTokenService.createToken(request, tokenRequestBody.getSubject_token());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // TODO: hier sollte die korrekte Struktur der Antwort zurückgegeben werden

    return ResponseEntity.ok().body(new TokenData(accessToken, "TODO REFRESHTOKEN"));
  }

  @Getter
  @Setter
  @AllArgsConstructor
  public static class TokenData {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;
  }
}
