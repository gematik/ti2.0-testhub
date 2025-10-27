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
package de.gematik.ti20.simsvc.client.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object for APDU command transmission requests. Includes validation for proper APDU
 * command format.
 */
public class TransmitRequestDto {

  @NotBlank(message = "APDU command cannot be empty")
  @Pattern(
      regexp = "^([0-9A-Fa-f]{2}\\s)*[0-9A-Fa-f]{2}$",
      message = "Command must be a valid hexadecimal string with bytes separated by spaces")
  private String command;

  /** Default constructor. */
  public TransmitRequestDto() {}

  /**
   * Constructor with command.
   *
   * @param command The APDU command as a hex string
   */
  public TransmitRequestDto(String command) {
    this.command = command;
  }

  /**
   * Get the APDU command.
   *
   * @return The APDU command as a hex string
   */
  public String getCommand() {
    return command;
  }

  /**
   * Set the APDU command.
   *
   * @param command The APDU command as a hex string
   */
  public void setCommand(String command) {
    this.command = command;
  }
}
