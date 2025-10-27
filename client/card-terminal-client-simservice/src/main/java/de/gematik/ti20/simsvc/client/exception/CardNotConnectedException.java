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

/**
 * Exception thrown when an operation is attempted on a card that has not been connected to. This is
 * different from CardNotFoundException, which is thrown when the card itself is not found.
 */
@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
public class CardNotConnectedException extends CardException {

  private final String cardId;

  /**
   * Create a new card not connected exception.
   *
   * @param cardId The ID of the card that is not connected
   */
  public CardNotConnectedException(String cardId) {
    super(
        "Card not connected: "
            + cardId
            + ". Please establish a connection first using GET /cards/{cardId}");
    this.cardId = cardId;
  }

  /**
   * Get the ID of the card that is not connected.
   *
   * @return The card ID
   */
  public String getCardId() {
    return cardId;
  }
}
