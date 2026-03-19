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
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.de.Und;
import io.cucumber.java.en.And;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

/** Step definitions for validating Unix timestamps in JWT claims. */
@Slf4j
public class TimestampValidationSteps {

  @Und("validiere, dass der Zeitstempel {tigerResolvedString} in der Vergangenheit liegt")
  @And("validate that timestamp {tigerResolvedString} is in the past")
  public void validateTimestampInPast(String timestampStr) {
    long timestamp = Long.parseLong(timestampStr.trim());
    long now = Instant.now().getEpochSecond();

    log.info("Validating timestamp {} (now: {})", timestamp, now);

    assertThat(timestamp)
        .as("Timestamp %d should be in the past (now: %d)", timestamp, now)
        .isLessThanOrEqualTo(now);
  }
}
