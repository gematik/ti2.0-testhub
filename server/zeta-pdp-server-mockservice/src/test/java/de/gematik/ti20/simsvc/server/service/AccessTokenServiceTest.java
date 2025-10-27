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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.server.config.AuthzConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {

  @Mock private HttpServletRequest httpServletRequest;

  private AccessTokenService accessTokenService;

  @BeforeEach
  void setUp() {

    AuthzConfig.KeyConfig keyConfig = new AuthzConfig.KeyConfig();
    keyConfig.setPass("testpassword");
    keyConfig.setAlias("zetamock");

    AuthzConfig.StoreConfig storeConfig = new AuthzConfig.StoreConfig();
    storeConfig.setPass("testpassword");
    storeConfig.setPath("./zetakeystore.p12");

    AuthzConfig.SecurityConfig securityConfig = new AuthzConfig.SecurityConfig();
    securityConfig.setKey(keyConfig);
    securityConfig.setStore(storeConfig);

    AuthzConfig authzConfig = new AuthzConfig();
    authzConfig.setSec(securityConfig);

    accessTokenService = new AccessTokenService(authzConfig);
  }

  @Test
  void testCreateTokenSuccess() throws Exception {
    String smcBAccessToken =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0LXN1YmplY3QiLCJwcm9mZXNzaW9uT2lkIjoidGVzdC1wcm9mZXNzaW9uLW9pZCJ9.signature";

    String result = accessTokenService.createToken(httpServletRequest, smcBAccessToken);

    assertNotNull(result);
    assertTrue(
        result.startsWith(
            "eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsuYWNjZXNzK2p3dCIsImtpZCI6InpldGFtb2NrIn0"));
  }

  @Test
  void testCreateTokenWithInvalidJwt() throws Exception {
    String invalidJwt = "invalid-jwt-token";

    assertThrows(
        InvalidJwtException.class,
        () -> {
          accessTokenService.createToken(httpServletRequest, invalidJwt);
        });
    //    }
  }
}
