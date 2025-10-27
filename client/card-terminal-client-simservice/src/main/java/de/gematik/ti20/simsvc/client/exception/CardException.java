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
 * Base exception class for card-related errors. This exception is thrown when an error occurs
 * during card operations.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CardException extends RuntimeException {

  private final int statusWord;
  private final String message;
  private final String statusMessage;

  /**
   * Create a new card exception with a status word and message.
   *
   * @param statusWord The ISO 7816 status word (SW) indicating the error
   * @param message The error message
   */
  public CardException(int statusWord, String message) {
    super(String.format("Card error (SW=%04X): %s", statusWord, message));
    this.statusWord = statusWord;
    this.message = message;
    this.statusMessage = getDefaultStatusMessage(statusWord);
  }

  /**
   * Create a new card exception with a status word, message, and status message.
   *
   * @param statusWord The ISO 7816 status word (SW) indicating the error
   * @param message The error message
   * @param statusMessage The status message explaining the status word
   */
  public CardException(int statusWord, String message, String statusMessage) {
    super(String.format("Card error (SW=%04X): %s", statusWord, message));
    this.statusWord = statusWord;
    this.message = message;
    this.statusMessage = statusMessage;
  }

  /**
   * Create a new card exception with only a message.
   *
   * @param message The error message
   */
  public CardException(String message) {
    super(message);
    this.statusWord = 0;
    this.message = message;
    this.statusMessage = "Unknown error";
  }

  /**
   * Get the status word.
   *
   * @return The ISO 7816 status word
   */
  public int getStatusWord() {
    return statusWord;
  }

  /**
   * Get the status word as a hex string.
   *
   * @return The status word as a hex string (e.g., "9000")
   */
  public String getStatusWordHex() {
    return String.format("%04X", statusWord);
  }

  /**
   * Get the status message explaining the status word.
   *
   * @return The status message
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Get the error message.
   *
   * @return The error message
   */
  @Override
  public String getMessage() {
    return message;
  }

  /**
   * Get a default status message for common ISO 7816 status words.
   *
   * @param sw The status word
   * @return A human-readable explanation of the status word
   */
  private String getDefaultStatusMessage(int sw) {
    switch (sw) {
      case 0x9000:
        return "Success";
      case 0x6100:
        return "More data available";
      case 0x6200:
        return "Warning: State of non-volatile memory unchanged";
      case 0x6281:
        return "Warning: Part of returned data may be corrupted";
      case 0x6282:
        return "Warning: End of file reached before reading Le bytes";
      case 0x6283:
        return "Warning: Selected file invalidated";
      case 0x6300:
        return "Warning: State of non-volatile memory changed";
      case 0x6381:
        return "Warning: File filled up by the last write";
      case 0x6400:
        return "Error: State of non-volatile memory unchanged";
      case 0x6500:
        return "Error: State of non-volatile memory changed";
      case 0x6700:
        return "Error: Wrong length";
      case 0x6800:
        return "Error: Functions in CLA not supported";
      case 0x6881:
        return "Error: Logical channel not supported";
      case 0x6882:
        return "Error: Secure messaging not supported";
      case 0x6900:
        return "Error: Command not allowed";
      case 0x6981:
        return "Error: Command incompatible with file structure";
      case 0x6982:
        return "Error: Security status not satisfied";
      case 0x6983:
        return "Error: Authentication method blocked";
      case 0x6984:
        return "Error: Referenced data invalidated";
      case 0x6985:
        return "Error: Conditions of use not satisfied";
      case 0x6986:
        return "Error: Command not allowed (no current EF)";
      case 0x6A80:
        return "Error: Incorrect parameters in the data field";
      case 0x6A81:
        return "Error: Function not supported";
      case 0x6A82:
        return "Error: File not found";
      case 0x6A83:
        return "Error: Record not found";
      case 0x6A86:
        return "Error: Incorrect parameters P1-P2";
      case 0x6C00:
        return "Error: Wrong Le field";
      case 0x6D00:
        return "Error: Instruction code not supported or invalid";
      case 0x6E00:
        return "Error: Class not supported";
      case 0x6F00:
        return "Error: Technical problem";
      default:
        return "Unknown status word";
    }
  }
}
