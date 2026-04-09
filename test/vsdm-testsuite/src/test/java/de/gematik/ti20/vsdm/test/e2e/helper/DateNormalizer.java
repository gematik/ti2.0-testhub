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
package de.gematik.ti20.vsdm.test.e2e.helper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public class DateNormalizer {
  private static final List<DateTimeFormatter> DATE_FORMATS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE, // 2025-12-19
          DateTimeFormatter.ofPattern("dd.MM.uuuu"),
          DateTimeFormatter.ofPattern("d.M.uuuu"),
          DateTimeFormatter.ofPattern("uuuuMMdd"),
          DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z uuuu", Locale.ENGLISH));

  public static LocalDate normalizeToLocalDate(String input) {
    for (var fmt : DATE_FORMATS) {
      try {
        return LocalDate.parse(input, fmt);
      } catch (DateTimeParseException ignored) {
      }
    }
    throw new IllegalArgumentException("Unbekanntes Datumsformat: " + input);
  }
}
