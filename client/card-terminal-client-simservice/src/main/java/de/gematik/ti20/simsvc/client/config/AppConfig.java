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
package de.gematik.ti20.simsvc.client.config;

import de.gematik.ti20.simsvc.client.model.apdu.ApduScenario;
import de.gematik.ti20.simsvc.client.service.ScenarioParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Configuration class for the Card Terminal Simulator application. Defines beans and initial setup
 * for the application.
 */
@Configuration
public class AppConfig {

  /**
   * Load APDU scenarios from YAML files.
   *
   * @return Map of scenario names to ApduScenario objects
   */
  @Bean
  public Map<String, ApduScenario> apduScenarios(ScenarioParser scenarioParser) {
    Map<String, ApduScenario> scenarios = new HashMap<>();

    try {
      // Load contact-based scenarios from file or attached resource
      try {
        InputStream contactScenarios =
            new ClassPathResource("scenarios/contact-based-scenarios.yaml").getInputStream();
        scenarios.putAll(scenarioParser.parseScenarios(contactScenarios));
      } catch (IOException e) {
        // Try to load from attached_assets if not found in resources
        File contactFile = new File("attached_assets/contact-based-scenarios.yaml");
        if (contactFile.exists()) {
          try (InputStream contactScenarios = new FileInputStream(contactFile)) {
            scenarios.putAll(scenarioParser.parseScenarios(contactScenarios));
          }
        } else {
          throw new IOException(
              "Could not find contact-based-scenarios.yaml in resources or attached_assets");
        }
      }

      // Load open EGK scenarios from file or attached resource
      try {
        InputStream egkScenarios =
            new ClassPathResource("scenarios/open-egk-scenarios.yaml").getInputStream();
        scenarios.putAll(scenarioParser.parseScenarios(egkScenarios));
      } catch (IOException e) {
        // Try to load from attached_assets if not found in resources
        File egkFile = new File("attached_assets/open-egk-scenarios.yaml");
        if (egkFile.exists()) {
          try (InputStream egkScenarios = new FileInputStream(egkFile)) {
            scenarios.putAll(scenarioParser.parseScenarios(egkScenarios));
          }
        } else {
          throw new IOException(
              "Could not find open-egk-scenarios.yaml in resources or attached_assets");
        }
      }

    } catch (IOException e) {
      throw new RuntimeException("Failed to load APDU scenarios: " + e.getMessage(), e);
    }

    return scenarios;
  }
}
