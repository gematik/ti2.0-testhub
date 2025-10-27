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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a scenario containing a sequence of APDU steps. Each scenario has a name and a list of
 * steps that define a complete card interaction.
 */
public class ApduScenario {

  private final String name;
  private final List<ApduStep> steps;

  /**
   * Constructor with name and steps.
   *
   * @param name Scenario name
   * @param steps List of APDU steps
   */
  public ApduScenario(String name, List<ApduStep> steps) {
    this.name = name;
    this.steps = new ArrayList<>(steps);
  }

  /**
   * Constructor with name only.
   *
   * @param name Scenario name
   */
  public ApduScenario(String name) {
    this(name, new ArrayList<>());
  }

  /**
   * Get the scenario name.
   *
   * @return Scenario name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the list of steps.
   *
   * @return List of APDU steps
   */
  public List<ApduStep> getSteps() {
    return new ArrayList<>(steps);
  }

  /**
   * Add a step to the scenario.
   *
   * @param step APDU step to add
   */
  public void addStep(ApduStep step) {
    steps.add(step);
  }

  /**
   * Find a step by name.
   *
   * @param stepName Name of the step to find
   * @return The step if found, null otherwise
   */
  public ApduStep findStepByName(String stepName) {
    return steps.stream()
        .filter(step -> Objects.equals(step.getName(), stepName))
        .findFirst()
        .orElse(null);
  }

  /**
   * Get the number of steps in the scenario.
   *
   * @return Number of steps
   */
  public int getStepCount() {
    return steps.size();
  }

  @Override
  public String toString() {
    return "ApduScenario{" + "name='" + name + '\'' + ", steps=" + steps.size() + '}';
  }
}
