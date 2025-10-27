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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.apdu.ApduScenario;
import de.gematik.ti20.simsvc.client.model.apdu.ApduStep;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScenarioParserTest {

  private ScenarioParser scenarioParser;

  @BeforeEach
  void setUp() {
    scenarioParser = new ScenarioParser();
  }

  @Test
  void testParseScenarios_WithContactBasedScenarios() {
    String yamlContent =
        """
            scenario-vars:
              cardType: "EGK"
              version: "2.0"
            scenario-names:
              select: "Select Application"
            step-names:
              selectCmd: "Select Command"
            contact-based-scenarios:
              scenarios:
                - name: "${scenario-names.select}"
                  stepDefinitions:
                    - name: "${step-names.selectCmd}"
                      description: "Select application ${scenario-vars.cardType}"
                      commandApdu: "00A4040C"
                      expectedStatusWord: "9000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertEquals(1, scenarios.size());
    assertTrue(scenarios.containsKey("Select Application"));

    ApduScenario scenario = scenarios.get("Select Application");
    assertEquals("Select Application", scenario.getName());
    assertEquals(1, scenario.getSteps().size());

    ApduStep step = scenario.getSteps().get(0);
    assertEquals("Select Command", step.getName());
    assertEquals("Select application EGK", step.getDescription());
    assertEquals("00A4040C", step.getCommandApdu());
    assertEquals(List.of("9000"), step.getExpectedStatusWords());
  }

  @Test
  void testParseScenarios_WithOpenEgkScenarios() {
    String yamlContent =
        """
            open-egk-scenarios:
              scenarios:
                - name: "Open EGK Test"
                  stepDefinitions:
                    - name: "Test Step"
                      description: "Test description"
                      commandApdu: "00A40000"
                      expectedStatusWord:
                        - "9000"
                        - "6282"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertEquals(1, scenarios.size());
    assertTrue(scenarios.containsKey("Open EGK Test"));

    ApduScenario scenario = scenarios.get("Open EGK Test");
    ApduStep step = scenario.getSteps().get(0);
    assertEquals(List.of("9000", "6282"), step.getExpectedStatusWords());
  }

  @Test
  void testParseScenarios_WithMultipleScenarios() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios:
                - name: "Scenario 1"
                  stepDefinitions:
                    - name: "Step 1"
                      description: "First step"
                      commandApdu: "00A40000"
                      expectedStatusWord: "9000"
                - name: "Scenario 2"
                  stepDefinitions:
                    - name: "Step 2"
                      description: "Second step"
                      commandApdu: "00A40001"
                      expectedStatusWord: "9000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertEquals(2, scenarios.size());
    assertTrue(scenarios.containsKey("Scenario 1"));
    assertTrue(scenarios.containsKey("Scenario 2"));
  }

  @Test
  void testParseScenarios_WithMultipleSteps() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios:
                - name: "Multi Step Scenario"
                  stepDefinitions:
                    - name: "Step 1"
                      description: "First step"
                      commandApdu: "00A40000"
                      expectedStatusWord: "9000"
                    - name: "Step 2"
                      description: "Second step"
                      commandApdu: "00A40001"
                      expectedStatusWord: "9000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertEquals(1, scenarios.size());
    ApduScenario scenario = scenarios.get("Multi Step Scenario");
    assertEquals(2, scenario.getSteps().size());

    assertEquals("Step 1", scenario.getSteps().get(0).getName());
    assertEquals("Step 2", scenario.getSteps().get(1).getName());
  }

  @Test
  void testParseScenarios_WithAllVariableTypes() {
    String yamlContent =
        """
            scenario-vars:
              cardType: "HBA"
            scenario-names:
              auth: "Authentication"
            step-names:
              verify: "Verify PIN"
            contact-based-scenarios:
              scenarios:
                - name: "${scenario-names.auth} for ${scenario-vars.cardType}"
                  stepDefinitions:
                    - name: "${step-names.verify}"
                      description: "Verify PIN for ${scenario-vars.cardType}"
                      commandApdu: "002000"
                      expectedStatusWord: "9000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertEquals(1, scenarios.size());
    assertTrue(scenarios.containsKey("Authentication for HBA"));

    ApduScenario scenario = scenarios.get("Authentication for HBA");
    ApduStep step = scenario.getSteps().get(0);
    assertEquals("Verify PIN", step.getName());
    assertEquals("Verify PIN for HBA", step.getDescription());
  }

  @Test
  void testParseScenarios_EmptyYaml() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios: []
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertTrue(scenarios.isEmpty());
  }

  @Test
  void testParseScenarios_NoScenarios() {
    String yamlContent =
        """
            scenario-vars:
              cardType: "EGK"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertTrue(scenarios.isEmpty());
  }

  @Test
  void testParseScenarios_WithSingleStatusWord() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios:
                - name: "Test Scenario"
                  stepDefinitions:
                    - name: "Test Step"
                      description: "Test description"
                      commandApdu: "00A40000"
                      expectedStatusWord: "9000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    ApduScenario scenario = scenarios.get("Test Scenario");
    ApduStep step = scenario.getSteps().get(0);
    assertEquals(List.of("9000"), step.getExpectedStatusWords());
  }

  @Test
  void testParseScenarios_WithoutStepDefinitions() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios:
                - name: "Empty Scenario"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertEquals(1, scenarios.size());
    ApduScenario scenario = scenarios.get("Empty Scenario");
    assertTrue(scenario.getSteps().isEmpty());
  }

  @Test
  void testParseScenarios_WithoutExpectedStatusWord() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios:
                - name: "Test Scenario"
                  stepDefinitions:
                    - name: "Test Step"
                      description: "Test description"
                      commandApdu: "00A40000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    ApduScenario scenario = scenarios.get("Test Scenario");
    ApduStep step = scenario.getSteps().get(0);
    assertTrue(step.getExpectedStatusWords().isEmpty());
  }

  @Test
  void testSubstituteVariables_WithNullInput() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios:
                - name: "Test Scenario"
                  stepDefinitions:
                    - name: "Test Step"
                      commandApdu: "00A40000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    ApduScenario scenario = scenarios.get("Test Scenario");
    ApduStep step = scenario.getSteps().get(0);
    assertNull(step.getDescription()); // Description war null im YAML
  }

  @Test
  void testParseScenarios_BothContactAndOpenEgk() {
    String yamlContent =
        """
            contact-based-scenarios:
              scenarios:
                - name: "Contact Scenario"
                  stepDefinitions:
                    - name: "Contact Step"
                      description: "Contact description"
                      commandApdu: "00A40000"
                      expectedStatusWord: "9000"
            open-egk-scenarios:
              scenarios:
                - name: "Open EGK Scenario"
                  stepDefinitions:
                    - name: "Open Step"
                      description: "Open description"
                      commandApdu: "00A40001"
                      expectedStatusWord: "9000"
            """;

    InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes());
    Map<String, ApduScenario> scenarios = scenarioParser.parseScenarios(inputStream);

    assertEquals(2, scenarios.size());
    assertTrue(scenarios.containsKey("Contact Scenario"));
    assertTrue(scenarios.containsKey("Open EGK Scenario"));
  }
}
