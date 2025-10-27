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
package de.gematik.ti20.simsvc.client.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception thrown when a requested card is not found. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CardNotFoundException extends CardException {

  private final String cardId;

  /**
   * Create a new card not found exception.
   *
   * @param cardId The ID of the card that was not found
   */
  public CardNotFoundException(String cardId) {
    super("Card not found: " + cardId);
    this.cardId = cardId;
  }

  /**
   * Get the ID of the card that was not found.
   *
   * @return The card ID
   */
  public String getCardId() {
    return cardId;
  }
}
