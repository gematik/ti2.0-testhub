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
package de.gematik.ti20.simsvc.client.model.apdu;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a step in an APDU scenario. Each step has a name, description, command, and expected
 * status words.
 */
public class ApduStep {

  private final String name;
  private final String description;
  private final String commandApdu;
  private final List<String> expectedStatusWords;

  /**
   * Constructor with all fields.
   *
   * @param name Step name
   * @param description Step description
   * @param commandApdu APDU command as a hex string
   * @param expectedStatusWords List of expected status words as hex strings
   */
  public ApduStep(
      String name, String description, String commandApdu, List<String> expectedStatusWords) {
    this.name = name;
    this.description = description;
    this.commandApdu = commandApdu;
    this.expectedStatusWords = expectedStatusWords;
  }

  /**
   * Constructor with name, description, and command. The expected status word is defaulted to
   * "9000" (success).
   *
   * @param name Step name
   * @param description Step description
   * @param commandApdu APDU command as a hex string
   */
  public ApduStep(String name, String description, String commandApdu) {
    this(name, description, commandApdu, Arrays.asList("9000"));
  }

  /**
   * Get the step name.
   *
   * @return Step name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the step description.
   *
   * @return Step description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the APDU command as a hex string.
   *
   * @return APDU command hex string
   */
  public String getCommandApdu() {
    return commandApdu;
  }

  /**
   * Get the list of expected status words.
   *
   * @return List of expected status words as hex strings
   */
  public List<String> getExpectedStatusWords() {
    return expectedStatusWords;
  }

  /**
   * Check if a status word is expected for this step.
   *
   * @param statusWord Status word to check (as a hex string, e.g., "9000")
   * @return true if the status word is expected, false otherwise
   */
  public boolean isStatusWordExpected(String statusWord) {
    return expectedStatusWords.contains(statusWord.toUpperCase());
  }

  /**
   * Parse the APDU command to an ApduCommand object.
   *
   * @return Parsed ApduCommand
   */
  public ApduCommand parseCommand() {
    return ApduCommand.fromHex(commandApdu);
  }

  @Override
  public String toString() {
    return "ApduStep{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", commandApdu='"
        + commandApdu
        + '\''
        + ", expectedStatusWords="
        + expectedStatusWords
        + '}';
  }
}
