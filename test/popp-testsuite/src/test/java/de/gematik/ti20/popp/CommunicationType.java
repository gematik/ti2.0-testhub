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
package de.gematik.ti20.popp;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

public enum CommunicationType {
  CONTACT_STANDARD("contact-standard", "Standardleser", "kontaktbehaftet"),
  CONTACTLESS_STANDARD("contactless-standard", "Standardleser", "kontaktlos"),
  CONTACT_CONNECTOR("contact-connector", "eH-KT", "kontaktbehaftet"),
  CONTACTLESS_CONNECTOR("contactless-connector", "eH-KT", "kontaktlos"), // Annahme
  CONTACT_VIRTUAL("contact-virtual", "virtuell", "kontaktbehaftet");

  @Getter private final String value;
  private final String readerType;
  private final String commType;

  CommunicationType(String value, String readerType, String commType) {
    this.value = value;
    this.readerType = readerType;
    this.commType = commType;
  }

  private static final Map<String, CommunicationType> MAPPING =
      Stream.of(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  ct -> createKey(ct.readerType, ct.commType), Function.identity()));

  private static String createKey(String readerType, String commType) {
    return readerType + ":" + commType;
  }

  public static CommunicationType from(String readerType, String commType) {
    String key = createKey(readerType, commType);
    return Optional.ofNullable(MAPPING.get(key))
        .orElseThrow(() -> new IllegalArgumentException("Unbekannte Kombination: " + key));
  }
}
