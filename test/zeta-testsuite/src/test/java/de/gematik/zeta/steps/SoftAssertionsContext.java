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

import java.util.stream.Collectors;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.model.exceptions.TestCompromisedException;
import net.thucydides.core.steps.StepEventBus;
import net.thucydides.model.domain.TestResult;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

/**
 * Thread-local container for AssertJ {@link SoftAssertions} bound to the current Cucumber scenario.
 */
public final class SoftAssertionsContext {

  private static final ThreadLocal<SoftAssertions> SOFT_ASSERTIONS = new InheritableThreadLocal<>();

  private SoftAssertionsContext() {}

  /**
   * Provides the {@link SoftAssertions} instance for the current scenario, creating it lazily.
   *
   * @return soft assertions instance
   */
  public static SoftAssertions softly() {
    var softly = SOFT_ASSERTIONS.get();
    if (softly == null) {
      softly = new SoftAssertions();
      SOFT_ASSERTIONS.set(softly);
    }
    return softly;
  }

  /** Clears any stored soft assertions to avoid leaking state between scenarios. */
  public static void reset() {
    SOFT_ASSERTIONS.remove();
  }

  /** Verifies all collected soft assertions and resets the context afterward. */
  public static void assertAll() {
    var softly = SOFT_ASSERTIONS.get();
    try {
      if (softly == null || softly.errorsCollected().isEmpty()) {
        return;
      }

      var summary =
          softly.errorsCollected().stream()
              .map(Throwable::getMessage)
              .filter(msg -> msg != null && !msg.isBlank())
              .map(String::trim)
              .collect(
                  Collectors.joining("\n - ", "Soft assertions (non-blocking) failed:\n - ", ""));

      throw new TestCompromisedException(summary);
    } finally {
      reset();
    }
  }

  /**
   * Records a soft assertion failure without interrupting the current step execution.
   *
   * @param description human readable context
   * @param cause root cause to summarize
   */
  public static void recordSoftFailure(String description, Throwable cause) {
    var message = summarize(cause);
    softly().fail("%s: %s", description, message);
    markCurrentStepCompromised();
    Serenity.recordReportData()
        .withTitle("Soft assertion (non-blocking)")
        .andContents(description + "\n" + message);
  }

  /**
   * Marks the currently executing Serenity step as compromised and stores a short reason for
   * visibility in the report while letting execution continue.
   */
  private static void markCurrentStepCompromised() {
    var listener = StepEventBus.getEventBus().getBaseStepListener();
    if (listener == null) {
      return;
    }
    var outcome = listener.getCurrentTestOutcome();
    if (outcome == null) {
      return;
    }
    outcome.currentStep().ifPresent(step -> step.setResult(TestResult.COMPROMISED));
  }

  /**
   * Reduces an exception to a concise, single-line description suitable for report output.
   *
   * @param throwable the exception to summarize, may be {@code null}
   * @return trimmed summary string
   */
  private static @NotNull String summarize(Throwable throwable) {
    if (throwable == null) {
      return "unknown failure";
    }
    var root = throwable;
    while (root.getCause() != null) {
      root = root.getCause();
    }
    var message = root.getMessage();
    var type = root.getClass().getSimpleName();
    var summary = (message == null || message.isBlank()) ? type : type + ": " + message;
    summary = summary.replaceAll("\\s+", " ").trim();
    return summary.length() > 500 ? summary.substring(0, 500) + " ..." : summary;
  }
}
