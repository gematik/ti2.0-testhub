/*-
 * #%L
 * VSDM Server Simservice
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.server.config;

import de.gematik.test.testdata.TestDataManager;
import java.nio.file.Path;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TestDataConfiguration {
  @Bean
  public TestDataManager testDataManager(
      @Value("${vsdm.path-to-test-data}") final String pathToTestData) {
    final Path rawPath = Path.of(pathToTestData);
    final Path testDataPath;
    if (rawPath.isAbsolute()) {
      testDataPath = rawPath;
    } else {
      // In case we get passed a relative like ../../data we concatenate that with the current
      // directory . and navigate from there.
      testDataPath = Path.of(".", pathToTestData).toAbsolutePath().normalize();
    }

    log.info("Loading test data from '{}'", testDataPath);
    return TestDataManager.initializeWithClasspath(Set.of(testDataPath));
  }
}
