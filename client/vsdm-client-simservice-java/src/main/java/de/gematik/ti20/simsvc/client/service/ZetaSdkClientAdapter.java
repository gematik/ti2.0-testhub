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
package de.gematik.ti20.simsvc.client.service;

import com.google.common.base.Strings;
import de.gematik.ti20.simsvc.client.config.VsdmConfig;
import de.gematik.zeta.sdk.ZetaSdkClient;
import io.ktor.client.HttpClient;
import io.ktor.client.request.BuildersKt;
import io.ktor.client.request.HttpRequestBuilder;
import io.ktor.client.request.HttpRequestKt;
import io.ktor.client.statement.HttpResponse;
import io.ktor.client.statement.HttpResponseKt;
import io.ktor.http.Headers;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Hide the Kotlin implementation details from the code */
@Service
@RequiredArgsConstructor
public class ZetaSdkClientAdapter {

  @Nonnull private final ZetaSdkClient zetaClient;
  @Nonnull private final VsdmConfig vsdmConfig;

  public record RequestParameters(
      @Nonnull String traceId,
      @Nonnull String poppToken,
      boolean isFhirXml,
      @CheckForNull String ifNoneMatch) {}

  /** Wraps the response from the Zeta client for convenience */
  public record Response(
      @Nonnull HttpStatus statusCode, @Nonnull Headers headers, @Nonnull String body) {}

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
    try (HttpClient httpClient = zetaClient.httpClient(b -> Unit.INSTANCE)) {
      final HttpResponse response =
          kotlinx.coroutines.BuildersKt.runBlocking(
              EmptyCoroutineContext.INSTANCE,
              (scope, cont) ->
                  BuildersKt.request(
                      httpClient,
                      req -> {
                        return executeRequest(url, parameters, req);
                      },
                      cont));
      final HttpStatus httpStatusCode = HttpStatus.valueOf(response.getStatus().getValue());
      final Headers headers = response.getHeaders();
      final String bodyAsString = getBodyAsString(response);
      return new Response(httpStatusCode, headers, bodyAsString);
    }
  }

  @Nonnull
  private Unit executeRequest(
      @Nonnull String urlPath, @Nonnull RequestParameters parameters, HttpRequestBuilder req) {
    final URI uri = URI.create(vsdmConfig.getResourceServerUrl());
    HttpRequestKt.url(req, "http", uri.getHost(), uri.getPort(), urlPath, x -> Unit.INSTANCE);
    HttpRequestKt.headers(
        req,
        headers -> {
          headers.set("x-trace-id", parameters.traceId());
          headers.set("PoPP", parameters.poppToken());
          headers.set(
              "Accept", (parameters.isFhirXml) ? "application/fhir+xml" : "application/fhir+json");
          if (!Strings.isNullOrEmpty(parameters.ifNoneMatch())) {
            headers.set("If-None-Match", parameters.ifNoneMatch());
          }
          return Unit.INSTANCE;
        });
    return Unit.INSTANCE;
  }

  @Nonnull
  private String getBodyAsString(@Nonnull final HttpResponse response) throws InterruptedException {
    return kotlinx.coroutines.BuildersKt.runBlocking(
        EmptyCoroutineContext.INSTANCE,
        (scope, cont) -> HttpResponseKt.bodyAsText(response, StandardCharsets.UTF_8, cont));
  }
}
