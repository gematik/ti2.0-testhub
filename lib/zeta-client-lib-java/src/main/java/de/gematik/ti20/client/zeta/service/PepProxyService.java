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
import de.gematik.ti20.client.zeta.request.ZetaWsRequest;
import de.gematik.ti20.zeta.base.model.WellKnown;
import java.net.MalformedURLException;
import java.net.URL;

public class PepProxyService extends HttpService {

  public PepProxyService(final ZetaClientService zetaClientService) {
    super(zetaClientService);
  }

  public void requestWellKnown(final AuthContext ac) throws ZetaHttpException {
    String url = ac.getRequest().getUrl();
    if (ac.getRequest() instanceof ZetaWsRequest) {
      url = ((ZetaWsRequest) ac.getRequest()).getUrlHttpEndpoint();
    }

    try {
      URL urlBase = new URL(url);
      URL urlWellKnown =
          new URL(
              urlBase.getProtocol(),
              urlBase.getHost(),
              urlBase.getPort(),
              zetaClientService.getZetaClientConfig().getPathWellKnownRS());
      url = urlWellKnown.toString();
    } catch (MalformedURLException e) {
      throw new ZetaHttpException("Failed to create well-known URL", e, ac.getRequest());
    }

    ZetaHttpRequest request = new ZetaHttpRequest(url);
    request.setTraceId(ac.getRequest().getTraceId());
    request.copyHeaderFrom(ac.getRequest(), HeaderName.UserAgent.getName());
    var response = send(request);
    ac.setWellKnownFromPep(response.getBodyFromJson(WellKnown.class).orElse(null));
  }
}
