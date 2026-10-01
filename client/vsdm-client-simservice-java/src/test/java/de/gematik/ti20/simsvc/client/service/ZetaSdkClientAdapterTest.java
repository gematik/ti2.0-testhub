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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import de.gematik.zeta.sdk.ZetaSdkClient;
import de.gematik.zeta.sdk.network.http.client.HttpClientExtension;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClient;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClientBuilder;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpResponse;
import io.ktor.http.HttpStatusCode;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;

class ZetaSdkClientAdapterTest {

  @ParameterizedTest
  @MethodSource("requestParameterCombinations")
  void httpGet_setsHeadersForAllParameterCombinations(
      final boolean isFhirXml,
      final boolean skipPoppTokenHeader,
      final String ifNoneMatch,
      final boolean expectPoppHeader,
      final boolean expectIfNoneMatchHeader)
      throws Exception {
    final ZetaSdkClient zetaSdkClient = mock(ZetaSdkClient.class);
    final ZetaHttpClient httpClient = mock(ZetaHttpClient.class);
    final ZetaHttpResponse zetaHttpResponse = mock(ZetaHttpResponse.class);
    final String body = "{\"resourceType\":\"Bundle\"}";

    when(zetaSdkClient.httpClient(any()))
        .thenAnswer(
            invocation -> {
              final Function1<ZetaHttpClientBuilder, Unit> config = invocation.getArgument(0);
              config.invoke(new ZetaHttpClientBuilder());
              return httpClient;
            });
    when(zetaHttpResponse.getStatus()).thenReturn(HttpStatusCode.Companion.getOK());
    when(zetaHttpResponse.getHeaders()).thenReturn(Map.of("etag", "etag-1"));

    final String url = "https://example.org/vsdm";
    try (MockedStatic<HttpClientExtension> httpClientExtension =
        mockStatic(HttpClientExtension.class)) {
      httpClientExtension
          .when(() -> HttpClientExtension.getAsync(eq(httpClient), eq(url), anyMap()))
          .thenAnswer(
              invocation -> {
                final Map<String, String> headers = invocation.getArgument(2);
                assertThat(headers).containsEntry("x-trace-id", "trace-123");
                assertThat(headers)
                    .containsEntry(
                        "Accept", isFhirXml ? "application/fhir+xml" : "application/fhir+json");
                if (expectPoppHeader) {
                  assertThat(headers).containsEntry("PoPP", "token-123");
                } else {
                  assertThat(headers).doesNotContainKey("PoPP");
                }
                if (expectIfNoneMatchHeader) {
                  assertThat(headers).containsEntry("If-None-Match", ifNoneMatch);
                } else {
                  assertThat(headers).doesNotContainKey("If-None-Match");
                }
                return CompletableFuture.completedFuture(zetaHttpResponse);
              });
      httpClientExtension
          .when(() -> HttpClientExtension.bodyAsText(zetaHttpResponse))
          .thenReturn(CompletableFuture.completedFuture(body));

      final ZetaSdkClientAdapter adapter = new ZetaSdkClientAdapter(zetaSdkClient);
      final ZetaSdkClientAdapter.Response response =
          adapter.httpGet(
              url,
              new ZetaSdkClientAdapter.RequestParameters(
                  "trace-123", "token-123", isFhirXml, skipPoppTokenHeader, ifNoneMatch));

      assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.body()).isEqualTo(body);
    }
  }

  @Test
  void httpGet_handlesNullIfNoneMatchHeader() throws Exception {
    final ZetaSdkClient zetaSdkClient = mock(ZetaSdkClient.class);
    final ZetaHttpClient httpClient = mock(ZetaHttpClient.class);
    final ZetaHttpResponse zetaHttpResponse = mock(ZetaHttpResponse.class);
    final String body = "{\"resourceType\":\"Bundle\"}";

    when(zetaSdkClient.httpClient(any())).thenReturn(httpClient);
    when(zetaHttpResponse.getStatus()).thenReturn(HttpStatusCode.Companion.getOK());
    when(zetaHttpResponse.getHeaders()).thenReturn(Map.of("etag", "etag-1"));

    try (MockedStatic<HttpClientExtension> httpClientExtension =
        mockStatic(HttpClientExtension.class)) {
      httpClientExtension
          .when(
              () ->
                  HttpClientExtension.getAsync(
                      eq(httpClient), eq("https://example.org/vsdm"), anyMap()))
          .thenAnswer(
              invocation -> {
                final Map<String, String> headers = invocation.getArgument(2);
                assertThat(headers).doesNotContainKey("If-None-Match");
                return CompletableFuture.completedFuture(zetaHttpResponse);
              });
      httpClientExtension
          .when(() -> HttpClientExtension.bodyAsText(zetaHttpResponse))
          .thenReturn(CompletableFuture.completedFuture(body));

      final ZetaSdkClientAdapter adapter = new ZetaSdkClientAdapter(zetaSdkClient);
      final ZetaSdkClientAdapter.Response response =
          adapter.httpGet(
              "https://example.org/vsdm",
              new ZetaSdkClientAdapter.RequestParameters(
                  "trace-123", "token-123", false, false, null));

      assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.body()).isEqualTo(body);
    }
  }

  private static Stream<Arguments> requestParameterCombinations() {
    return Stream.of(
        Arguments.of(false, false, null, true, false),
        Arguments.of(false, false, "etag-42", true, true),
        Arguments.of(false, true, null, false, false),
        Arguments.of(false, true, "etag-42", false, true),
        Arguments.of(true, false, null, true, false),
        Arguments.of(true, false, "etag-42", true, true),
        Arguments.of(true, true, null, false, false),
        Arguments.of(true, true, "etag-42", false, true));
  }
}
