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

import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest.AuthorizationType;
import de.gematik.ti20.zeta.base.model.WellKnown;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;

public class AuthContext {

  private ZetaHttpRequest request;

  private String accessToken;
  private String refreshToken;

  private WellKnown wellKnownFromPep;
  private WellKnown wellKnownFromPdp;
  private String nonce;
  private ClientAssertionToken clientAssertionToken;
  private KeyPair dpopKeyPair;
  private DpopToken dpopToken;

  private String smcbAccessToken;

  public AuthContext(ZetaHttpRequest request) {
    this.request = request;
  }

  public ZetaHttpRequest getRequest() {
    return request;
  }

  public ZetaHttpRequest getRequestAuthorized() {
    request.setHeaderAuthorization(AuthorizationType.DPOP, this.accessToken);
    return request;
  }

  public WellKnown getWellKnownFromPep() {
    return wellKnownFromPep;
  }

  public void setWellKnownFromPep(WellKnown wellKnownFromPep) {
    this.wellKnownFromPep = wellKnownFromPep;
  }

  public void createDpopKeyPair()
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
    this.dpopKeyPair = DpopToken.createKeyPair();
  }

  public void setAccessToken(Map<String, String> tokenData) {
    this.accessToken = tokenData.get("accessToken");
  }

  public String getSmcbAccessToken() {
    return smcbAccessToken;
  }

  public void setSmcbAccessToken(String smcbAccessToken) {
    this.smcbAccessToken = smcbAccessToken;
  }
}
