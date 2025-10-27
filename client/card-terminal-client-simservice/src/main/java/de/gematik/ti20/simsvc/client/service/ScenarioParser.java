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
package de.gematik.ti20.simsvc.client.service;

import de.gematik.ti20.simsvc.client.model.apdu.ApduScenario;
import de.gematik.ti20.simsvc.client.model.apdu.ApduStep;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/** Service for parsing APDU scenarios from YAML files. */
@Service
public class ScenarioParser {

  /**
   * Parse scenarios from a YAML file.
   *
   * @param yamlInputStream Input stream for the YAML file
   * @return Map of scenario names to ApduScenario objects
   */
  public Map<String, ApduScenario> parseScenarios(InputStream yamlInputStream) {
    Map<String, ApduScenario> scenarios = new HashMap<>();

    Yaml yaml = new Yaml();
    Map<String, Object> yamlData = yaml.load(yamlInputStream);

    // Get variables and names for substitution
    Map<String, String> variableMap = getVariableMap(yamlData);

    // Process scenarios
    processScenarios(yamlData, scenarios, variableMap);

    return scenarios;
  }

  /**
   * Extract variables from YAML data for substitution.
   *
   * @param yamlData YAML data
   * @return Map of variable names to values
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> getVariableMap(Map<String, Object> yamlData) {
    Map<String, String> variableMap = new HashMap<>();

    // Extract scenario-vars
    if (yamlData.containsKey("scenario-vars")) {
      Map<String, Object> scenarioVars = (Map<String, Object>) yamlData.get("scenario-vars");
      for (Map.Entry<String, Object> entry : scenarioVars.entrySet()) {
        variableMap.put("${scenario-vars." + entry.getKey() + "}", entry.getValue().toString());
      }
    }

    // Extract scenario-names
    if (yamlData.containsKey("scenario-names")) {
      Map<String, Object> scenarioNames = (Map<String, Object>) yamlData.get("scenario-names");
      for (Map.Entry<String, Object> entry : scenarioNames.entrySet()) {
        variableMap.put("${scenario-names." + entry.getKey() + "}", entry.getValue().toString());
      }
    }

    // Extract step-names
    if (yamlData.containsKey("step-names")) {
      Map<String, Object> stepNames = (Map<String, Object>) yamlData.get("step-names");
      for (Map.Entry<String, Object> entry : stepNames.entrySet()) {
        variableMap.put("${step-names." + entry.getKey() + "}", entry.getValue().toString());
      }
    }

    return variableMap;
  }

  /**
   * Process scenarios from YAML data.
   *
   * @param yamlData YAML data
   * @param scenarios Map to store parsed scenarios
   * @param variableMap Map of variable names to values for substitution
   */
  @SuppressWarnings("unchecked")
  private void processScenarios(
      Map<String, Object> yamlData,
      Map<String, ApduScenario> scenarios,
      Map<String, String> variableMap) {
    // Find the scenarios container
    if (yamlData.containsKey("contact-based-scenarios")) {
      Map<String, Object> contactBasedScenarios =
          (Map<String, Object>) yamlData.get("contact-based-scenarios");
      processScenariosContainer(contactBasedScenarios, scenarios, variableMap);
    }

    if (yamlData.containsKey("open-egk-scenarios")) {
      Map<String, Object> openEgkScenarios =
          (Map<String, Object>) yamlData.get("open-egk-scenarios");
      processScenariosContainer(openEgkScenarios, scenarios, variableMap);
    }
  }

  /**
   * Process a scenarios container.
   *
   * @param scenariosContainer Container of scenarios
   * @param scenarios Map to store parsed scenarios
   * @param variableMap Map of variable names to values for substitution
   */
  @SuppressWarnings("unchecked")
  private void processScenariosContainer(
      Map<String, Object> scenariosContainer,
      Map<String, ApduScenario> scenarios,
      Map<String, String> variableMap) {
    if (scenariosContainer.containsKey("scenarios")) {
      List<Map<String, Object>> scenariosList =
          (List<Map<String, Object>>) scenariosContainer.get("scenarios");

      for (Map<String, Object> scenarioData : scenariosList) {
        String name = substituteVariables((String) scenarioData.get("name"), variableMap);
        ApduScenario scenario = new ApduScenario(name);

        // Process steps
        if (scenarioData.containsKey("stepDefinitions")) {
          List<Map<String, Object>> stepDefinitions =
              (List<Map<String, Object>>) scenarioData.get("stepDefinitions");

          for (Map<String, Object> stepData : stepDefinitions) {
            String stepName = substituteVariables((String) stepData.get("name"), variableMap);
            String description =
                substituteVariables((String) stepData.get("description"), variableMap);
            String commandApdu =
                substituteVariables((String) stepData.get("commandApdu"), variableMap);

            // Process expected status words
            List<String> expectedStatusWords = new ArrayList<>();
            if (stepData.containsKey("expectedStatusWord")) {
              Object statusWordObj = stepData.get("expectedStatusWord");
              if (statusWordObj instanceof List) {
                List<String> statusWords = (List<String>) statusWordObj;
                for (String statusWord : statusWords) {
                  expectedStatusWords.add(substituteVariables(statusWord, variableMap));
                }
              } else if (statusWordObj instanceof String) {
                expectedStatusWords.add(substituteVariables((String) statusWordObj, variableMap));
              }
            }

            ApduStep step = new ApduStep(stepName, description, commandApdu, expectedStatusWords);
            scenario.addStep(step);
          }
        }

        scenarios.put(name, scenario);
      }
    }
  }

  /**
   * Substitute variables in a string.
   *
   * @param input Input string potentially containing variables
   * @param variableMap Map of variable names to values
   * @return String with variables substituted
   */
  private String substituteVariables(String input, Map<String, String> variableMap) {
    if (input == null) {
      return null;
    }

    String result = input;

    for (Map.Entry<String, String> entry : variableMap.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }

    return result;
  }
}
