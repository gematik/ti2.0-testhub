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
package de.gematik.ti20.client.card.card;

/** Enumeration of supported card types in the German healthcare system. */
public enum CardType {

  /** Elektronische Gesundheitskarte (electronic health card for patients). */
  EGK("EGK"),

  /** Heilberufsausweis (health professional card). */
  HBA("HBA"),

  /** Security Module Card Type B (for institutions). */
  SMC_B("SMC-B"),

  /** Unknown card type. */
  UNKNOWN("UNKNOWN");

  private final String description;

  /**
   * Constructor.
   *
   * @param description the human-readable description of the card type
   */
  CardType(String description) {
    this.description = description;
  }

  /**
   * Returns the human-readable description of the card type.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this card type supports signing operations.
   *
   * @return true if the card type can sign data, false otherwise
   */
  public boolean canSign() {
    switch (this) {
      case HBA:
      case SMC_B:
        return true;
      case EGK:
      case UNKNOWN:
      default:
        return false;
    }
  }

  public static boolean isSet(CardType cardType) {
    if (cardType == null || cardType == UNKNOWN) {
      return false;
    }
    return true;
  }
}
