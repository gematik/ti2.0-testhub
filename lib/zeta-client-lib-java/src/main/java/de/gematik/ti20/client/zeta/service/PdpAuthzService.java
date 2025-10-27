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
package de.gematik.ti20.client.zeta.service;

import de.gematik.ti20.client.zeta.auth.AuthContext;
import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest.HeaderName;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class PdpAuthzService extends HttpService {

  public PdpAuthzService(final ZetaClientService zetaClientService) {
    super(zetaClientService);
  }

  public void requestAccessToken(final AuthContext ac) throws ZetaHttpException {
    String url;
    try {
      URL urlAS = new URL(ac.getWellKnownFromPep().getAuthorization_endpoint());
      URL urlToken =
          new URL(
              urlAS.getProtocol(),
              urlAS.getHost(),
              urlAS.getPort(),
              zetaClientService.getZetaClientConfig().getPathTokenAS());
      url = urlToken.toString();
    } catch (MalformedURLException e) {
      throw new ZetaHttpException("Failed to create /token URL", e, ac.getRequest());
    }

    ZetaHttpRequest request = new ZetaHttpRequest(url);
    request.setTraceId(ac.getRequest().getTraceId());
    request.copyHeaderFrom(ac.getRequest(), HeaderName.UserAgent.getName());
    try {
      request.postValues(
          Map.of(
              "grant_type", "urn:ietf:params:oauth:grant-type:token-exchange",
              "requested_token_type", "urn:ietf:params:oauth:token-type:access_token",
              "subject_token", ac.getSmcbAccessToken(),
              "subject_token_type", "urn:ietf:params:oauth:token-type:jwt"));
    } catch (UnsupportedEncodingException e) {
      throw new ZetaHttpException(
          "Failed to serialize POST values to /token URL", e, ac.getRequest());
    }
    var response = send(request);
    ac.setAccessToken(response.getBodyFromJson(Map.class).orElse(null));
  }
}
