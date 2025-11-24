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
package de.gematik.ti20.client.zeta.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class ZetaClientConfigTest {

  @Test
  void testUserAgentConfig() {
    ZetaClientConfig.UserAgentConfig userAgent =
        new ZetaClientConfig.UserAgentConfig("MyApp", "1.0.0");
    assertEquals("MyApp", userAgent.getName());
    assertEquals("1.0.0", userAgent.getVersion());
    assertEquals("MyApp/1.0.0", userAgent.getUserAgent());
  }

  @Test
  void testAddTerminalConnectionConfig() {
    ZetaClientConfig config =
        new ZetaClientConfig(new ZetaClientConfig.UserAgentConfig("App", "1"));
    CardTerminalConnectionConfig terminalConfig = mock(CardTerminalConnectionConfig.class);
    config.addTerminalConnectionConfig(terminalConfig);

    assertEquals(1, config.getTerminalConnectionConfigs().size());
    assertTrue(config.getTerminalConnectionConfigs().contains(terminalConfig));
  }

  @Test
  void testSetTerminalConnectionConfigs() {
    ZetaClientConfig config =
        new ZetaClientConfig(new ZetaClientConfig.UserAgentConfig("App", "1"));
    CardTerminalConnectionConfig terminal1 = mock(CardTerminalConnectionConfig.class);
    CardTerminalConnectionConfig terminal2 = mock(CardTerminalConnectionConfig.class);

    config.addTerminalConnectionConfig(terminal1);
    assertEquals(1, config.getTerminalConnectionConfigs().size());

    config.setTerminalConnectionConfigs(List.of(terminal2));
    assertEquals(1, config.getTerminalConnectionConfigs().size());
    assertTrue(config.getTerminalConnectionConfigs().contains(terminal2));
    assertFalse(config.getTerminalConnectionConfigs().contains(terminal1));
  }

  @Test
  void testSetTerminalConnectionConfigsWithNull() {
    ZetaClientConfig config =
        new ZetaClientConfig(new ZetaClientConfig.UserAgentConfig("App", "1"));
    config.addTerminalConnectionConfig(mock(CardTerminalConnectionConfig.class));
    config.setTerminalConnectionConfigs(null);
    // Liste bleibt unverändert, da null ignoriert wird
    assertEquals(1, config.getTerminalConnectionConfigs().size());
  }
}
