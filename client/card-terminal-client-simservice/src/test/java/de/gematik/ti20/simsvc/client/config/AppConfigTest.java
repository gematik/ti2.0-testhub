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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.apdu.ApduScenario;
import de.gematik.ti20.simsvc.client.service.ScenarioParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.core.io.ClassPathResource;

class AppConfigTest {

  @Test
  void apduScenarios_loadsFromResources() throws Exception {
    // Arrange
    ScenarioParser parser = mock(ScenarioParser.class);
    InputStream dummyStream = new ByteArrayInputStream(new byte[] {});
    Map<String, ApduScenario> parsedContact = Map.of("contact", mock(ApduScenario.class));
    Map<String, ApduScenario> parsedEgk = Map.of("egk", mock(ApduScenario.class));

    // Mock ClassPathResource
    MockedConstruction<ClassPathResource> resourceMock =
        mockConstruction(
            ClassPathResource.class,
            (mock, context) -> {
              when(mock.getInputStream()).thenReturn(dummyStream);
            });

    when(parser.parseScenarios(dummyStream)).thenReturn(parsedContact).thenReturn(parsedEgk);

    AppConfig config = new AppConfig();

    // Act
    Map<String, ApduScenario> result = config.apduScenarios(parser);

    // Assert
    assertTrue(result.containsKey("contact"));
    assertTrue(result.containsKey("egk"));
    resourceMock.close();
  }
}
