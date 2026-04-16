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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.de.Und;
import io.cucumber.java.en.And;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

/**
 * Step definitions for validating timestamps. Mirrors the tiger-testsuite TimestampValidationSteps
 * for use in the zeta-testsuite.
 */
@Slf4j
public class TimestampValidationSteps {

  /**
   * Validates that a given epoch-seconds timestamp is in the future.
   *
   * @param timestamp the epoch seconds
   * @throws AssertionError if the value is invalid or the timestamp is not in the future
   */
  @Und("validiere, dass der Zeitstempel {tigerResolvedString} in der Zukunft liegt")
  @And("validate that the timestamp {tigerResolvedString} is in the future")
  public void validateTimestampIsInTheFuture(String timestamp) {
    Instant futureTimestamp;
    try {
      futureTimestamp = Instant.ofEpochSecond(Long.parseLong(timestamp));
    } catch (NumberFormatException e) {
      throw new AssertionError("Ungültiges Zeitstempel Format: " + timestamp);
    }

    assertThat(Instant.now())
        .as("Zeitstempel muss in der Zukunft liegen")
        .isBefore(futureTimestamp);

    log.info("Validierung erfolgreich: Zeitstempel {} ist noch nicht abgelaufen.", futureTimestamp);
  }

  /**
   * Validates that a given epoch-seconds timestamp is in the past.
   *
   * @param timestamp the epoch seconds
   * @throws AssertionError if the value is invalid or the timestamp is not in the past
   */
  @Und("validiere, dass der Zeitstempel {tigerResolvedString} in der Vergangenheit liegt")
  @And("validate that the timestamp {tigerResolvedString} is in the past")
  public void validateTimestampIsInThePast(String timestamp) {
    Instant earlierTimestamp;
    try {
      earlierTimestamp = Instant.ofEpochSecond(Long.parseLong(timestamp));
    } catch (NumberFormatException e) {
      throw new AssertionError("Ungültiges Zeitstempel Format: " + timestamp);
    }

    assertThat(Instant.now())
        .as("Zeitstempel muss in der Vergangenheit liegen oder jetzt sein")
        .isAfterOrEqualTo(earlierTimestamp);

    log.info("Validierung erfolgreich: Zeitstempel {} ist bereits abgelaufen.", earlierTimestamp);
  }

  /**
   * Validates that one epoch-seconds timestamp is later than another.
   *
   * @param after the epoch seconds of the later timestamp
   * @param before the epoch seconds of the earlier timestamp
   * @throws AssertionError if either value is invalid or the first is not after the second
   */
  @Und(
      "validiere, dass der Zeitstempel {tigerResolvedString} später als {tigerResolvedString} liegt")
  @And("validate that the timestamp {tigerResolvedString} is after {tigerResolvedString}")
  public void validateTimestampIsLaterThan(String after, String before) {
    Instant later;
    Instant earlier;
    try {
      later = Instant.ofEpochSecond(Long.parseLong(after));
    } catch (NumberFormatException e) {
      throw new AssertionError("Ungültiges Zeitstempel Format: " + after);
    }
    try {
      earlier = Instant.ofEpochSecond(Long.parseLong(before));
    } catch (NumberFormatException e) {
      throw new AssertionError("Ungültiges Zeitstempel Format: " + before);
    }

    assertThat(later).as("Zeitstempel 2 ist nicht später als Zeitstempel 1.").isAfter(earlier);

    log.info("Validierung erfolgreich: Zeitstempel {} ist später als {}.", after, before);
  }
}
