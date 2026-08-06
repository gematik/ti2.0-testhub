/*-
 * #%L
 * VSDM 2.0 Testsuite
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
package de.gematik.ti20.vsdm.test.load;

import de.gematik.zeta.sdk.*;
import de.gematik.zeta.sdk.attestation.model.AttestationConfig;
import de.gematik.zeta.sdk.attestation.model.PlatformProductId;
import de.gematik.zeta.sdk.authentication.AuthConfig;
import de.gematik.zeta.sdk.authentication.SubjectTokenProvider;
import de.gematik.zeta.sdk.authentication.smb.SmbTokenProvider;
import de.gematik.zeta.sdk.network.http.client.HttpClientExtension;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClient;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClientBuilder;
import de.gematik.zeta.sdk.storage.SdkStorage;
import de.gematik.zeta.sdk.storage.StorageConfig;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CoreDsl;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.exec.Executable;
import io.ktor.client.plugins.logging.LogLevel;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Slf4j
public final class ZetaDsl {

  private static final Pattern SESSION_PLACEHOLDER = Pattern.compile("#\\{([^}]+)}");
  private static final String ZETA_STATUS_KEY = "zeta_status";
  private static final String ZETA_RESPONSE_BODY_KEY = "zeta_response_body";
  private static final String ZETA_REQUEST_NAME_KEY = "zeta_request_name";
  private static final String ZETA_SUCCESS_KEY = "zeta_success";
  private static final String ZETA_DURATION_MS_KEY = "zeta_duration_ms";

  private static int ZETA_POOL_SIZE = 20;

  private ZetaDsl() {}

  public static ZetaRequestBuilder zeta(final String requestName, final int zetaPoolSize) {
    ZetaDsl.ZETA_POOL_SIZE = zetaPoolSize;
    return new ZetaRequestBuilder(requestName);
  }

  public static ZetaStatusCheckBuilder status() {
    return new ZetaStatusCheckBuilder();
  }

  public static ZetaBodyContainsCheckBuilder bodyContains(final String expectedFragment) {
    return new ZetaBodyContainsCheckBuilder(expectedFragment);
  }

  public static final class ZetaStatusCheckBuilder {
    private final List<Integer> expectedStatuses = new ArrayList<>();

    public ZetaStatusCheckBuilder is(final int status) {
      expectedStatuses.clear();
      expectedStatuses.add(status);
      return this;
    }

    public ZetaStatusCheckBuilder in(final int... statuses) {
      expectedStatuses.clear();
      for (final int status : statuses) {
        expectedStatuses.add(status);
      }
      return this;
    }

    boolean matches(final int actualStatus) {
      return expectedStatuses.isEmpty() || expectedStatuses.contains(actualStatus);
    }

    String expectedDescription() {
      return expectedStatuses.toString();
    }
  }

  public static final class ZetaBodyContainsCheckBuilder {
    private final String expectedFragment;

    private ZetaBodyContainsCheckBuilder(final String expectedFragment) {
      this.expectedFragment = Objects.requireNonNull(expectedFragment);
    }

    String expectedFragment() {
      return expectedFragment;
    }
  }

  public static final class ZetaRequestBuilder implements Executable {
    private final String requestName;
    private String rawUrl;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private ZetaStatusCheckBuilder statusCheck;
    private ZetaBodyContainsCheckBuilder bodyContainsCheck;

    private ZetaRequestBuilder(final String requestName) {
      this.requestName = requestName;
    }

    public ZetaRequestBuilder get(final String url) {
      this.rawUrl = Objects.requireNonNull(url);
      return this;
    }

    public ZetaRequestBuilder header(final String key, final String value) {
      headers.put(key, value);
      return this;
    }

    public ZetaRequestBuilder queryParam(final String key, final Object value) {
      queryParams.put(key, String.valueOf(value));
      return this;
    }

    public ZetaRequestBuilder check(final ZetaStatusCheckBuilder statusCheck) {
      this.statusCheck = statusCheck;
      return this;
    }

    public ZetaRequestBuilder check(final ZetaBodyContainsCheckBuilder bodyContainsCheck) {
      this.bodyContainsCheck = bodyContainsCheck;
      return this;
    }

    @Override
    public ChainBuilder toChainBuilder() {
      return CoreDsl.exec(this::executeWithZetaClient)
          .exec(
              CoreDsl.dummy(
                      session -> {
                        final String dynamicName = session.getString(ZETA_REQUEST_NAME_KEY);
                        return dynamicName != null ? dynamicName : requestName;
                      },
                      session -> {
                        final Integer duration = session.getIntegerWrapper(ZETA_DURATION_MS_KEY);
                        return duration != null ? duration : 0;
                      })
                  .withSuccess(
                      session -> Boolean.TRUE.equals(session.getBooleanWrapper(ZETA_SUCCESS_KEY))));
    }

    private Session executeWithZetaClient(final Session session) {
      final long startedAtNanos = System.nanoTime();
      ZetaSdkClient zetaSdkClient = null;
      String targetUrl = null;
      try {
        if (rawUrl == null) {
          throw new IllegalStateException(
              "Missing URL: call get(url) before executing zeta request.");
        }

        final String resolvedUrl = resolveSessionValues(rawUrl, session);
        targetUrl = appendQueryParams(resolvedUrl, session);

        final Map<String, String> resolvedHeaders = resolveHeaders(session);

        zetaSdkClient = ZetaClientFactory.acquireFor(targetUrl);
        log.info("Acquired ZetaSdkClient: " + zetaSdkClient.hashCode());

        try (ZetaHttpClient httpClient =
            zetaSdkClient.httpClient(
                builder -> {
                  builder.disableServerValidation(true);
                  return Unit.INSTANCE;
                })) {
          final var response =
              HttpClientExtension.getAsync(httpClient, targetUrl, resolvedHeaders).join();
          final int actualStatus = response.getStatus().getValue();
          final String responseBody = HttpClientExtension.bodyAsText(response).join();

          final int durationMs = (int) ((System.nanoTime() - startedAtNanos) / 1_000_000L);

          Session updatedSession =
              session
                  .set(ZETA_STATUS_KEY, actualStatus)
                  .set(ZETA_RESPONSE_BODY_KEY, responseBody)
                  .set(ZETA_REQUEST_NAME_KEY, requestName)
                  .set(ZETA_DURATION_MS_KEY, durationMs)
                  .set(ZETA_SUCCESS_KEY, actualStatus == 200);

          if (statusCheck != null && !statusCheck.matches(actualStatus)) {
            log.warn(
                "Zeta request '{}' failed status check. expected={}, actual={}, url={}",
                requestName,
                statusCheck.expectedDescription(),
                actualStatus,
                targetUrl);
            return updatedSession.set(ZETA_SUCCESS_KEY, false).markAsFailed();
          }

          if (bodyContainsCheck != null
              && !responseBody.contains(bodyContainsCheck.expectedFragment())) {
            log.warn(
                "Zeta request '{}' failed body check. expected fragment='{}', url={}",
                requestName,
                bodyContainsCheck.expectedFragment(),
                targetUrl);
            return updatedSession.set(ZETA_SUCCESS_KEY, false).markAsFailed();
          }

          return updatedSession.markAsSucceeded();
        }
      } catch (Exception e) {
        final int durationMs = (int) ((System.nanoTime() - startedAtNanos) / 1_000_000L);
        log.warn("Zeta request '{}' failed", requestName, e);
        return session
            .set(ZETA_REQUEST_NAME_KEY, requestName)
            .set(ZETA_DURATION_MS_KEY, durationMs)
            .set(ZETA_SUCCESS_KEY, false)
            .markAsFailed();
      } finally {
        if (zetaSdkClient != null && targetUrl != null) {
          ZetaClientFactory.releaseFor(targetUrl, zetaSdkClient);
          log.info("Released ZetaSdkClient: " + zetaSdkClient.hashCode());
        }
      }
    }

    private Map<String, String> resolveHeaders(final Session session) {
      final Map<String, String> resolvedHeaders = new HashMap<>();
      headers.forEach((k, v) -> resolvedHeaders.put(k, resolveSessionValues(v, session)));
      return resolvedHeaders;
    }

    private String appendQueryParams(final String url, final Session session) {
      if (queryParams.isEmpty()) {
        return url;
      }

      final StringBuilder queryString = new StringBuilder();
      queryParams.forEach(
          (k, v) -> {
            if (!queryString.isEmpty()) {
              queryString.append('&');
            }
            queryString
                .append(URLEncoder.encode(resolveSessionValues(k, session), StandardCharsets.UTF_8))
                .append('=')
                .append(
                    URLEncoder.encode(resolveSessionValues(v, session), StandardCharsets.UTF_8));
          });

      final String separator = url.contains("?") ? "&" : "?";
      return url + separator + queryString;
    }
  }

  private static String resolveSessionValues(final String rawValue, final Session session) {
    final Matcher matcher = SESSION_PLACEHOLDER.matcher(rawValue);
    final StringBuilder resolved = new StringBuilder();
    while (matcher.find()) {
      final String key = matcher.group(1);
      final Object value = session.get(key);
      final String replacement = value == null ? "" : String.valueOf(value);
      matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(resolved);
    return resolved.toString();
  }

  static final class ZetaClientFactory {
    private static final String SCOPE = "vsdservice";

    private static final Map<String, ZetaClientPool> POOLS_BY_RESOURCE = new ConcurrentHashMap<>();

    private ZetaClientFactory() {}

    static ZetaSdkClient acquireFor(final String targetUrl) throws InterruptedException {
      return poolFor(targetUrl).acquire();
    }

    static void releaseFor(final String targetUrl, final ZetaSdkClient client) {
      poolFor(targetUrl).release(client);
    }

    static void shutdown() {
      POOLS_BY_RESOURCE.values().forEach(ZetaClientPool::cleanup);
      POOLS_BY_RESOURCE.clear();
    }

    private static ZetaClientPool poolFor(final String resourceBase) {
      return POOLS_BY_RESOURCE.computeIfAbsent(resourceBase, ZetaClientPool::new);
    }

    static final class ZetaClientPool {
      private final String resourceBase;
      private final int capacity;
      private final BlockingQueue<ZetaSdkClient> available = new LinkedBlockingQueue<>();
      private final AtomicInteger created = new AtomicInteger(0);

      ZetaClientPool(final String resourceBase) {
        this.resourceBase = resourceBase;
        this.capacity = ZetaDsl.ZETA_POOL_SIZE;
        log.info("ZetaClientPool created for '{}' with capacity {}", resourceBase, capacity);
      }

      ZetaSdkClient acquire() throws InterruptedException {
        final ZetaSdkClient existing = available.poll();
        if (existing != null) {
          return existing;
        }

        int current;
        do {
          current = created.get();
          if (current >= capacity) {
            log.debug(
                "ZetaClientPool for '{}' exhausted (capacity={}), waiting for available client",
                resourceBase,
                capacity);
            return available.take();
          }
        } while (!created.compareAndSet(current, current + 1));

        log.debug(
            "ZetaClientPool for '{}': creating client {}/{}", resourceBase, current + 1, capacity);
        return createClient(resourceBase);
      }

      void release(final ZetaSdkClient client) {
        available.offer(client);
      }

      void cleanup() {
        log.info("Clear ZetaClientPool for '{}'", resourceBase);
        for (ZetaSdkClient client : available) {
          SdkStatus status = ZetaSdkClientExtension.status(client);
          log.info("Status 1: {}", status);

          ZetaSdkClientExtension.clearRegistration(client);
          status = ZetaSdkClientExtension.status(client);
          log.info("Status 2: {}", status);

          ZetaSdkClientExtension.close(client);
          status = ZetaSdkClientExtension.status(client);
          log.info("Status 3: {}", status);
        }
      }
    }

    private static ZetaSdkClient createClient(final String resourceBase) {
      return ZetaSdk.INSTANCE.build(
          resourceBase,
          new BuildConfig(
              "demo-client",
              "0.2.0",
              "sdk-client",
              new StorageConfig.Custom(
                  new SdkStorage() {
                    private HashMap<String, String> cache = new HashMap<>();

                    @Override
                    public @Nullable Object put(
                        @NonNull String s,
                        @NonNull String s1,
                        @NonNull Continuation<? super Unit> continuation) {
                      cache.put(s, s1);
                      return continuation;
                    }

                    @Override
                    public @Nullable Object get(
                        @NonNull String s, @NonNull Continuation<? super String> continuation) {
                      return cache.get(s);
                    }

                    @Override
                    public @Nullable Object remove(
                        @NonNull String s, @NonNull Continuation<? super Unit> continuation) {
                      return cache.remove(s);
                    }

                    @Override
                    public @Nullable Object clear(
                        @NonNull Continuation<? super Unit> continuation) {
                      cache.clear();
                      return continuation;
                    }
                  }),
              new TpmConfig() {},
              new AuthConfig(
                  List.of(SCOPE),
                  30L,
                  false,
                  tokenProviderFromEnvironment(),
                  AttestationConfig.software(),
                  ""),
              new PlatformProductId.LinuxProductId(
                  PlatformProductId.PLATFORM_LINUX, "jar", "testhub", "latest"),
              new ZetaHttpClientBuilder().disableServerValidation(true).logging(LogLevel.ALL),
              null,
              null,
              null));
    }

    // FIXME raku
    private static SubjectTokenProvider tokenProviderFromEnvironment() {
      final String keyPath =
          "doc/docker/backend/zeta/smcb-private/smcb_private.p12"; // readRequired("ZETASDK_SMCB_PRIVATE_KEY_PATH");
      final String alias = "alias"; // readRequired("ZETASDK_SMCB_ALIAS");
      final String password = "00"; // readRequired("ZETASDK_SMCB_PRIVATE_KEY_PASSWORD");

      final Path privateKeyPath = Path.of(keyPath);
      if (!Files.exists(privateKeyPath) || !Files.isRegularFile(privateKeyPath)) {
        throw new IllegalStateException("SMCB private key file does not exist: " + keyPath);
      }
      if (!Files.isReadable(privateKeyPath)) {
        throw new IllegalStateException("SMCB private key file is not readable: " + keyPath);
      }

      return new SmbTokenProvider(new SmbTokenProvider.Credentials(keyPath, alias, password, ""));
    }

    private static String readRequired(final String key) {
      final String value = System.getenv(key);
      if (value == null || value.isBlank()) {
        throw new IllegalStateException("Missing required environment variable: " + key);
      }
      return value;
    }
  }
}
