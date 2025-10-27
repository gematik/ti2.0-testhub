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
package de.gematik.ti20.simsvc.server.model.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StandardScenarioMessage extends BasePoppMessage {

  @JsonProperty("version")
  private String version = "1.0.0";

  @JsonProperty("clientSessionId")
  private String clientSessionId;

  @JsonProperty("sequenceCounter")
  private int sequenceCounter;

  @JsonProperty("timeSpan")
  private int timeSpan;

  @JsonProperty("steps")
  private List<ScenarioStep> steps;

  public StandardScenarioMessage() {
    super(BasePoppMessageType.STANDARD_SCENARIO);
  }

  public StandardScenarioMessage(
      String clientSessionId, int sequenceCounter, int timeSpan, List<ScenarioStep> steps) {
    super(BasePoppMessageType.STANDARD_SCENARIO);
    this.clientSessionId = clientSessionId;
    this.sequenceCounter = sequenceCounter;
    this.timeSpan = timeSpan;
    this.steps = steps;
  }
}
