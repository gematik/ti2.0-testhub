/*
 *
 * Copyright 2025-2026 gematik GmbH
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
/*-
 * #%L
 * ZETA Testsuite
 * %%
 * (C) achelos GmbH, 2025, licensed for gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.steps;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Cucumber hooks for scenario lifecycle management. */
@Slf4j
public class Hooks {

  private static final int ORDER_PREPARE_SOFT_ASSERTIONS = Integer.MIN_VALUE;
  private static final int ORDER_DEBUG_PAUSE = ORDER_PREPARE_SOFT_ASSERTIONS + 10;
  private static final int ORDER_VERIFY_SOFT_ASSERTIONS = Integer.MAX_VALUE - 1;

  private static final String DEBUG_PAUSE_PROP = "zeta.debug.pauseBeforeTigerProxyAdminCheck";
  private static final String DEBUG_PAUSE_MS_PROP = DEBUG_PAUSE_PROP + ".ms";

  /** Clears any soft assertions before each scenario to avoid leaking state across scenarios. */
  @Before(order = ORDER_PREPARE_SOFT_ASSERTIONS)
  public void prepareSoftAssertions() {
    SoftAssertionsContext.reset();
  }

  /** Verifies all collected soft assertions at the very end of the scenario lifecycle. */
  @After(order = ORDER_VERIFY_SOFT_ASSERTIONS)
  public void verifySoftAssertions() {
    SoftAssertionsContext.assertAll();
  }

  // Run early (after soft assertions reset), before scenario steps begin.
  @Before(order = ORDER_DEBUG_PAUSE)
  public void debugPauseBeforeTigerProxyAdminCheck(final Scenario scenario) {
    if (!isDebugPauseEnabled()) {
      return;
    }

    ensureTestsuiteTigerConfigLoaded();

    String baseUrl = resolveTigerProxyBaseUrl();
    boolean up = waitForTigerProxyHealth(baseUrl);
    long pauseMs = getDebugPauseMillis();

    logDebugPauseInfo(scenario, baseUrl, up, pauseMs);
    sleepQuietly(pauseMs);
  }

  private boolean isDebugPauseEnabled() {
    return Boolean.parseBoolean(System.getProperty(DEBUG_PAUSE_PROP, "false"));
  }

  private long getDebugPauseMillis() {
    return Long.parseLong(System.getProperty(DEBUG_PAUSE_MS_PROP, "60000"));
  }

  private void ensureTestsuiteTigerConfigLoaded() {
    final String prop = "tiger.testenv.cfgfile";
    if (System.getProperty(prop) == null) {
      System.setProperty(prop, Path.of("tiger.yaml").toAbsolutePath().toString());
    }
  }

  private String resolveTigerProxyBaseUrl() {
    String baseUrl =
        TigerGlobalConfiguration.resolvePlaceholders("${zeta.paths.tigerProxy.baseUrl}");
    if (baseUrl.contains("${")) {
      log.warn("TigerProxy baseUrl still contains placeholders: '{}'", baseUrl);
    }
    return baseUrl;
  }

  private boolean waitForTigerProxyHealth(String baseUrl) {
    int healthCheckSeconds =
        Integer.parseInt(
            TigerGlobalConfiguration.resolvePlaceholders("${zeta.timeouts.healthCheckSeconds}"));
    long pollIntervalMs =
        Long.parseLong(
            TigerGlobalConfiguration.resolvePlaceholders(
                "${zeta.timeouts.healthCheckPollIntervalMs}"));

    var restTemplate = createRestTemplateWithTimeout();
    var deadline = Instant.now().plus(Duration.ofSeconds(healthCheckSeconds));

    while (Instant.now().isBefore(deadline)) {
      try {
        restTemplate.execute(baseUrl + "/actuator/health", HttpMethod.GET, null, r -> null);
        return true;
      } catch (Exception e) {
        if (!sleepQuietly(pollIntervalMs)) {
          break;
        }
      }
    }
    return false;
  }

  private RestTemplate createRestTemplateWithTimeout() {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(1000);
    factory.setReadTimeout(1500);
    return new RestTemplate(factory);
  }

  private void logDebugPauseInfo(Scenario scenario, String baseUrl, boolean up, long pauseMs) {
    String msg =
        String.format(
            "DEBUG pause enabled for %dms. baseUrl='%s', admin up=%s", pauseMs, baseUrl, up);
    log.warn(msg);

    if (scenario != null) {
      scenario.log(msg);
      scenario.log("Try: curl -i " + baseUrl + "/actuator/health");
    }
  }

  private boolean sleepQuietly(long ms) {
    try {
      Thread.sleep(Math.max(0, ms));
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }
}
