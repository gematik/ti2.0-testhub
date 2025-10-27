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
package de.gematik.ti20.client.popp.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ScenarioStep {

  @JsonProperty("commandApdu")
  private String commandApdu;

  @JsonProperty("expectedStatusWords")
  private List<String> expectedStatusWords;

  public ScenarioStep() {}

  public ScenarioStep(String commandApdu, List<String> expectedStatusWords) {
    this.commandApdu = commandApdu;
    this.expectedStatusWords = expectedStatusWords;
  }

  public String getCommandApdu() {
    return commandApdu;
  }

  public void setCommandApdu(String commandApdu) {
    this.commandApdu = commandApdu;
  }

  public List<String> getExpectedStatusWords() {
    return expectedStatusWords;
  }

  public void setExpectedStatusWords(List<String> expectedStatusWords) {
    this.expectedStatusWords = expectedStatusWords;
  }
}
