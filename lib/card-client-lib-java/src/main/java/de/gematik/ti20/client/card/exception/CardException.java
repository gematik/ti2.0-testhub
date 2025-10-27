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
package de.gematik.ti20.client.card.exception;

import de.gematik.ti20.client.card.message.CardMessage;
import jakarta.annotation.Nullable;
import java.io.Serial;

public class CardException extends RuntimeException {

  @Serial private static final long serialVersionUID = 6535524282113239307L;
  private CardMessage request;

  public CardException(String message, CardMessage request) {
    super(message);
    this.request = request;
  }

  public CardException(String message, Throwable cause, CardMessage request) {
    super(message, cause);
    this.request = request;
  }

  public CardException(Throwable cause, CardMessage request) {
    super(cause.getMessage(), cause);
    this.request = request;
  }

  public CardException(Throwable cause) {
    super(cause.getMessage(), cause);
  }

  @Nullable
  public CardMessage getRequest() {
    return request;
  }
}
