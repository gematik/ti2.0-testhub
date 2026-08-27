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
import de.gematik.zeta.sdk.network.http.client.HttpClientExtension;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClient;
import io.gatling.commons.stats.KO$;
import io.gatling.commons.stats.OK$;
import io.gatling.commons.util.Clock;
import io.gatling.core.action.Action;
import io.gatling.core.stats.StatsEngine;
import io.gatling.core.structure.ScenarioContext;
import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.Session;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kotlin.Unit;
import lombok.extern.slf4j.Slf4j;
import scala.Option;
import scala.Option$;

@Slf4j
public final class ZetaDsl {

  private static final Pattern SESSION_PLACEHOLDER = Pattern.compile("#\\{([^}]+)}");
  private static final String ZETA_STATUS_KEY = "zeta_status";
  private static final String ZETA_RESPONSE_BODY_KEY = "zeta_response_body";
  private static final String ZETA_REQUEST_NAME_KEY = "zeta_request_name";
  private static final String ZETA_SUCCESS_KEY = "zeta_success";
  private static final String ZETA_DURATION_MS_KEY = "zeta_duration_ms";
  private static final String ZETA_ERROR_MESSAGE_KEY = "zeta_error_message";
  private static final String ACTOR_ID_KEY = "actorId";

  private ZetaDsl() {}

  public static void configureZetaClientPool(
      final SimulationConfigBean.ZetaClientPoolConfig zetaClientPoolConfig) {
    Objects.requireNonNull(zetaClientPoolConfig, "zetaClientPoolConfig");
    configureZetaClientPool(zetaClientPoolConfig.getCapacity(), zetaClientPoolConfig.getSmcbs());
  }

  public static void configureZetaClientPool(
      final int poolSize, final List<SimulationConfigBean.SmcbData> smcbs) {
    ZetaClientFactory.configure(poolSize, smcbs);
  }

  public static ZetaRequestBuilder zeta(final String requestName) {
    return new ZetaRequestBuilder(requestName);
  }

  public static ZetaStatusCheckBuilder status() {
    return new ZetaStatusCheckBuilder();
  }

  public static ZetaBodyContainsCheckBuilder bodyContains(final String expectedFragment) {
    return new ZetaBodyContainsCheckBuilder(expectedFragment);
  }

  public static ZetaBodyEmptyCheckBuilder bodyEmpty() {
    return new ZetaBodyEmptyCheckBuilder();
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

  public static final class ZetaBodyEmptyCheckBuilder {

    private ZetaBodyEmptyCheckBuilder() {}
  }

  public static final class ZetaRequestBuilder implements ActionBuilder {
    private final String requestName;
    private String rawUrl;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private ZetaStatusCheckBuilder statusCheck;
    private ZetaBodyContainsCheckBuilder bodyContainsCheck;
    private ZetaBodyEmptyCheckBuilder bodyEmptyCheck;

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

    public ZetaRequestBuilder check(final ZetaBodyEmptyCheckBuilder bodyEmptyCheck) {
      this.bodyEmptyCheck = bodyEmptyCheck;
      return this;
    }

    @Override
    public io.gatling.core.action.builder.ActionBuilder asScala() {
      return new io.gatling.core.action.builder.ActionBuilder() {
        @Override
        public Action build(final ScenarioContext ctx, final Action next) {
          return new ZetaRequestAction(
              requestName,
              ZetaRequestBuilder.this,
              ctx.coreComponents().statsEngine(),
              ctx.coreComponents().clock(),
              next);
        }
      };
    }

    private Session executeWithZetaClient(final Session session) {
      final long startedAtNanos = System.nanoTime();
      ZetaSdkClient zetaSdkClient = null;
      String targetUrl = null;
      String actorId = null;
      try {
        if (rawUrl == null) {
          throw new IllegalStateException(
              "Missing URL: call get(url) before executing zeta request.");
        }

        final String resolvedUrl = resolveSessionValues(rawUrl, session);
        targetUrl = appendQueryParams(resolvedUrl, session);

        final Map<String, String> resolvedHeaders = resolveHeaders(session);

        actorId = resolveActorId(session);
        zetaSdkClient = ZetaClientFactory.acquireFor(targetUrl, actorId);
        log.info(
            "Acquired ZetaSdkClient for actorId '{}': {} ({})",
            actorId,
            zetaSdkClient.hashCode(),
            ZetaSdkClientExtension.status(zetaSdkClient));

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
                  .set(ZETA_SUCCESS_KEY, actualStatus == 200 || actualStatus == 304)
                  .remove(ZETA_ERROR_MESSAGE_KEY);

          if (statusCheck != null && !statusCheck.matches(actualStatus)) {
            final String errorMessage =
                "Status check failed: expected="
                    + statusCheck.expectedDescription()
                    + ", actual="
                    + actualStatus;
            log.warn(
                "Zeta request '{}' failed status check. expected={}, actual={}, url={}",
                requestName,
                statusCheck.expectedDescription(),
                actualStatus,
                targetUrl);
            return updatedSession
                .set(ZETA_SUCCESS_KEY, false)
                .set(ZETA_ERROR_MESSAGE_KEY, errorMessage)
                .markAsFailed();
          }

          if (bodyContainsCheck != null
              && !responseBody.contains(bodyContainsCheck.expectedFragment())) {
            final String errorMessage =
                "Body fragment missing: expected fragment='"
                    + bodyContainsCheck.expectedFragment()
                    + "'";
            log.error(
                "Zeta request '{}' failed body check. expected fragment='{}', url={}",
                requestName,
                bodyContainsCheck.expectedFragment(),
                targetUrl);
            return updatedSession
                .set(ZETA_SUCCESS_KEY, false)
                .set(ZETA_ERROR_MESSAGE_KEY, errorMessage)
                .markAsFailed();
          }

          if (bodyEmptyCheck != null && !responseBody.isEmpty()) {
            final String errorMessage = "Body empty check failed: response body was not empty";
            log.error(
                "Zeta request '{}' failed body empty check. actual body='{}', url={}",
                requestName,
                responseBody,
                targetUrl);
            return updatedSession
                .set(ZETA_SUCCESS_KEY, false)
                .set(ZETA_ERROR_MESSAGE_KEY, errorMessage)
                .markAsFailed();
          }

          return updatedSession.markAsSucceeded();
        }
      } catch (Exception e) {
        final int durationMs = (int) ((System.nanoTime() - startedAtNanos) / 1_000_000L);
        final String errorMessage =
            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.error("Zeta request '{}' failed", requestName, e);
        return session
            .set(ZETA_REQUEST_NAME_KEY, requestName)
            .set(ZETA_DURATION_MS_KEY, durationMs)
            .set(ZETA_SUCCESS_KEY, false)
            .set(ZETA_ERROR_MESSAGE_KEY, errorMessage)
            .markAsFailed();
      } finally {
        if (zetaSdkClient != null && targetUrl != null) {
          ZetaClientFactory.releaseFor(targetUrl, actorId, zetaSdkClient);
          log.info(
              "Released ZetaSdkClient for actorId '{}': {}", actorId, zetaSdkClient.hashCode());
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

  private static final class ZetaRequestAction implements Action {
    private com.typesafe.scalalogging.Logger logger =
        com.typesafe.scalalogging.Logger$.MODULE$.apply(ZetaRequestAction.class);
    private final String fallbackRequestName;
    private final ZetaRequestBuilder requestBuilder;
    private final StatsEngine statsEngine;
    private final Clock clock;
    private final Action next;
    private final String name;

    private ZetaRequestAction(
        final String fallbackRequestName,
        final ZetaRequestBuilder requestBuilder,
        final StatsEngine statsEngine,
        final Clock clock,
        final Action next) {
      this.fallbackRequestName = fallbackRequestName;
      this.requestBuilder = requestBuilder;
      this.statsEngine = statsEngine;
      this.clock = clock;
      this.next = next;
      this.name = "zeta-" + fallbackRequestName;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public com.typesafe.scalalogging.Logger logger() {
      return logger;
    }

    @Override
    public void com$typesafe$scalalogging$StrictLogging$_setter_$logger_$eq(
        final com.typesafe.scalalogging.Logger logger) {
      this.logger = logger;
    }

    @Override
    public void execute(final io.gatling.core.session.Session scalaSession) {
      final long startedAtMillis = clock.nowMillis();
      final Session javaSession = new Session(scalaSession);
      final Session updatedJavaSession = requestBuilder.executeWithZetaClient(javaSession);
      final io.gatling.core.session.Session updatedScalaSession = updatedJavaSession.asScala();
      final long endedAtMillis = clock.nowMillis();

      final boolean success =
          Boolean.TRUE.equals(updatedJavaSession.getBooleanWrapper(ZETA_SUCCESS_KEY));
      final String resolvedRequestName =
          Objects.requireNonNullElse(
              updatedJavaSession.getString(ZETA_REQUEST_NAME_KEY), fallbackRequestName);
      final Option<String> errorMessage = resolveErrorMessageOption(updatedJavaSession, success);

      statsEngine.logResponse(
          updatedScalaSession.scenario(),
          updatedScalaSession.groups(),
          resolvedRequestName,
          startedAtMillis,
          endedAtMillis,
          success ? OK$.MODULE$ : KO$.MODULE$,
          Option$.MODULE$.empty(),
          errorMessage);

      next.$bang(updatedScalaSession.logGroupRequestTimings(startedAtMillis, endedAtMillis));
    }

    private Option<String> resolveErrorMessageOption(
        final Session updatedJavaSession, final boolean success) {
      if (success) {
        return Option$.MODULE$.empty();
      }
      final String message =
          Objects.requireNonNullElse(
              updatedJavaSession.getString(ZETA_ERROR_MESSAGE_KEY), "Zeta request failed");
      return Option.apply(message);
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

  private static String resolveActorId(final Session session) {
    final String actorId = session.getString(ACTOR_ID_KEY);
    if (actorId == null || actorId.isBlank()) {
      throw new IllegalStateException("Missing actorId in session");
    }
    return actorId;
  }

  static final class ZetaClientFactory {
    private static final Map<String, ZetaClientPool> POOLS_BY_RESOURCE = new ConcurrentHashMap<>();
    private static volatile ZetaClientPoolConfiguration configuration = loadDefaultConfiguration();

    private ZetaClientFactory() {}

    static synchronized void configure(
        final int capacity, final List<SimulationConfigBean.SmcbData> smcbs) {
      configuration = new ZetaClientPoolConfiguration(capacity, smcbs);
    }

    static ZetaSdkClient acquireFor(final String targetUrl, final String actorId)
        throws InterruptedException {
      return poolFor(targetUrl).acquire(actorId);
    }

    static void releaseFor(
        final String targetUrl, final String actorId, final ZetaSdkClient client) {
      poolFor(targetUrl).release(actorId, client);
    }

    static void shutdown() {
      POOLS_BY_RESOURCE.values().forEach(ZetaClientPool::cleanup);
      POOLS_BY_RESOURCE.clear();
    }

    private static ZetaClientPool poolFor(final String resourceBase) {
      final ZetaClientPoolConfiguration currentConfiguration = configuration;
      return POOLS_BY_RESOURCE.computeIfAbsent(
          resourceBase,
          key ->
              new ZetaClientPool(
                  key, currentConfiguration.capacity(), currentConfiguration.smcbs()));
    }

    private static ZetaClientPoolConfiguration loadDefaultConfiguration() {
      final SimulationConfigBean.ZetaClientPoolConfig zetaSdkPool =
          SimulationConfigProvider.getInstance().getZetaSdkPool();
      return new ZetaClientPoolConfiguration(zetaSdkPool.getCapacity(), zetaSdkPool.getSmcbs());
    }

    private static final class ZetaClientPoolConfiguration {
      private final int capacity;
      private final List<SimulationConfigBean.SmcbData> smcbs;

      private ZetaClientPoolConfiguration(
          final int capacity, final List<SimulationConfigBean.SmcbData> smcbs) {
        if (capacity <= 0) {
          throw new IllegalArgumentException("zetaSdkPool.capacity must be greater than 0");
        }
        this.capacity = capacity;
        this.smcbs = List.copyOf(Objects.requireNonNull(smcbs, "smcbs"));
        if (this.smcbs.isEmpty()) {
          throw new IllegalArgumentException("zetaSdkPool.smcbs must not be empty");
        }
      }

      private int capacity() {
        return capacity;
      }

      private List<SimulationConfigBean.SmcbData> smcbs() {
        return smcbs;
      }
    }
  }
}
