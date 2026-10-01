/*-
 * #%L
 * VSDM Client Simulator Service
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
package de.gematik.ti20.simsvc.client.config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;

class StartupConfigurationLoggerTest {

  @Test
  void shouldLogConfigurationProperties() {
    ConfigurationPropertiesReportEndpoint endpoint =
        mock(ConfigurationPropertiesReportEndpoint.class);
    ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor descriptor =
        mock(ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor.class);
    when(descriptor.getContexts()).thenReturn(Map.of());
    when(endpoint.configurationProperties()).thenReturn(descriptor);

    Logger logger = (Logger) LoggerFactory.getLogger(StartupConfigurationLogger.class);
    Level previousLevel = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.INFO);
    try {
      new StartupConfigurationLogger(endpoint).logConfiguration();
    } finally {
      logger.setLevel(previousLevel);
      logger.detachAppender(appender);
    }

    assertTrue(
        appender.list.stream()
            .anyMatch(
                event ->
                    event.getLevel() == Level.INFO
                        && event.getFormattedMessage().contains("\"contexts\"")));
  }
}
