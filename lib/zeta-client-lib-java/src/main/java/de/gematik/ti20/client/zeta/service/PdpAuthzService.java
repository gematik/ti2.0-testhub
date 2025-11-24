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
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest.HeaderName;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class PdpAuthzService extends HttpService {

  public PdpAuthzService(final ZetaClientService zetaClientService) {
    super(zetaClientService);
  }

  public void requestAccessToken(final AuthContext ac) throws ZetaHttpException {
    String url;
    try {
      URI urlAS = new URI(ac.getWellKnownFromPep().getAuthorization_endpoint());
      URI urlToken =
          new URI(
              urlAS.getScheme(),
              null,
              urlAS.getHost(),
              urlAS.getPort(),
              ZetaClientConfig.PATH_TOKEN_AS,
              null,
              null);
      url = urlToken.toString();
    } catch (URISyntaxException e) {
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
