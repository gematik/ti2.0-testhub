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

/** Exception thrown when an APDU command fails to execute correctly. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CardCommandException extends CardException {

  private final String command;

  /**
   * Create a new card command exception.
   *
   * @param statusWord The ISO 7816 status word (SW) indicating the error
   * @param message The error message
   * @param command The APDU command that caused the error
   */
  public CardCommandException(int statusWord, String message, String command) {
    super(statusWord, message);
    this.command = command;
  }

  /**
   * Get the APDU command that caused the error.
   *
   * @return The APDU command
   */
  public String getCommand() {
    return command;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + " (Command: " + command + ")";
  }
}
