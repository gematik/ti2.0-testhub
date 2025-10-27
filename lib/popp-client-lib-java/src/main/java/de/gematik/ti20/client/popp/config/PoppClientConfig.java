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
package de.gematik.ti20.client.popp.config;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import java.util.ArrayList;
import java.util.List;

public class PoppClientConfig {

  private final String urlPoppServerHttp;
  private final String urlPoppServerWs;
  private final String urlPoppServerMockHttp;
  private final String urlPoppServerMockWs;

  private List<CardTerminalConnectionConfig> terminalConnectionConfigs = new ArrayList<>();

  public PoppClientConfig(String urlPoppServerWs, String urlPoppServerHttp) {
    this.urlPoppServerWs = urlPoppServerWs;
    this.urlPoppServerHttp = urlPoppServerHttp;
    this.urlPoppServerMockWs = urlPoppServerWs;
    this.urlPoppServerMockHttp = urlPoppServerHttp;
  }

  /**
   * Use this constructor in cases when you use the card-terminal-client-mockservice with card
   * images.
   *
   * <p>In this case you must provide additionally the URLs where the popp-server-mockservice runs.
   *
   * @param urlPoppServerWs WebSocket URL to the real PoPP Service instance
   * @param urlPoppServerHttp HTTP URL to the real PoPP Service instance
   * @param urlPoppServerMockWs WebSocket URL to the popp-server-mockservice instance
   * @param urlPoppServerMockHttp HTTP URL to the popp-server-mockservice instance
   */
  public PoppClientConfig(
      String urlPoppServerWs,
      String urlPoppServerHttp,
      String urlPoppServerMockWs,
      String urlPoppServerMockHttp) {
    this.urlPoppServerWs = urlPoppServerWs;
    this.urlPoppServerHttp = urlPoppServerHttp;
    this.urlPoppServerMockWs = urlPoppServerMockWs;
    this.urlPoppServerMockHttp = urlPoppServerMockHttp;
  }

  public String getUrlPoppServerHttp(final AttachedCard card) {
    if (card instanceof SimulatorAttachedCard) {
      return urlPoppServerMockHttp;
    }
    return urlPoppServerHttp;
  }

  public String getUrlPoppServerWs(final AttachedCard card) {
    if (card instanceof SimulatorAttachedCard) {
      return urlPoppServerMockWs;
    }
    return urlPoppServerWs;
  }

  public List<CardTerminalConnectionConfig> getTerminalConnectionConfigs() {
    return terminalConnectionConfigs;
  }

  public void addTerminalConnectionConfig(CardTerminalConnectionConfig terminalConnectionConfig) {
    terminalConnectionConfigs.add(terminalConnectionConfig);
  }

  public void setTerminalConnectionConfigs(
      final List<CardTerminalConnectionConfig> terminalConnectionConfigs) {
    if (terminalConnectionConfigs != null) {
      this.terminalConnectionConfigs.clear();
      this.terminalConnectionConfigs.addAll(terminalConnectionConfigs);
    }
  }
}
