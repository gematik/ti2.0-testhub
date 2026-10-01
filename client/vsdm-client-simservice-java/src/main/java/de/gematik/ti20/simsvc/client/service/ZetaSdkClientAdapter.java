/*-
 * #%L
 * VSDM Client Simulator Service
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.service;

import com.google.common.base.Strings;
import de.gematik.zeta.sdk.ZetaSdkClient;
import de.gematik.zeta.sdk.network.http.client.HttpClientExtension;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClient;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpResponse;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Hide the Kotlin implementation details from the code */
@Service
@RequiredArgsConstructor
@Setter
public class ZetaSdkClientAdapter {

  @Nonnull private final ZetaSdkClient zetaClient;

  public record RequestParameters(
      @Nonnull String traceId,
      @Nonnull String poppToken,
      boolean isFhirXml,
      boolean skipPoppTokenHeader,
      @CheckForNull String ifNoneMatch) {}

  /** Wraps the response from the Zeta client for convenience */
  public record Response(
      @Nonnull HttpStatus statusCode, @Nonnull Map<String, String> headers, @Nonnull String body) {}

  /**
   * Perform a get request using the Zeta client.
   *
   * @param url The url to call
   * @param parameters Additional parameters required for the request.
   * @return The response body as string
   */
  @Nonnull
  public Response httpGet(
      @Nonnull final String url, @Nonnull final ZetaSdkClientAdapter.RequestParameters parameters)
      throws InterruptedException {

    try (final ZetaHttpClient httpClient =
        zetaClient.httpClient(
            it -> {
              it.disableServerValidation(true);
              return Unit.INSTANCE;
            })) {

      final Map<String, String> headers = new HashMap<>();
      headers.put("x-trace-id", parameters.traceId());
      if (!parameters.skipPoppTokenHeader) {
        headers.put("PoPP", parameters.poppToken());
      }
      headers.put(
          "Accept", (parameters.isFhirXml) ? "application/fhir+xml" : "application/fhir+json");
      if (!Strings.isNullOrEmpty(parameters.ifNoneMatch())) {
        headers.put("If-None-Match", parameters.ifNoneMatch());
      }
      ZetaHttpResponse response = HttpClientExtension.getAsync(httpClient, url, headers).join();

      final HttpStatus httpStatusCode = HttpStatus.valueOf(response.getStatus().getValue());
      final Map<String, String> responseHeaders = response.getHeaders();
      final String bodyAsString = HttpClientExtension.bodyAsText(response).join();
      return new Response(httpStatusCode, responseHeaders, bodyAsString);
    }
  }
}
