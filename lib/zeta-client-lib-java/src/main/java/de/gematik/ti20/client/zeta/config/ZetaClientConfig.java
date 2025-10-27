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

import de.gematik.ti20.client.card.config.CardTerminalConnectionConfig;
import java.util.ArrayList;
import java.util.List;

public class ZetaClientConfig {

  private String pathWellKnownRS = "/.well-known/oauth-protected-resource";
  private String pathWellKnownAS = "/.well-known/oauth-authorization-server";
  private String pathTokenAS = "/token";

  private List<CardTerminalConnectionConfig> terminalConnectionConfigs = new ArrayList<>();

  private final UserAgentConfig userAgent;

  public static class UserAgentConfig {

    private String name;
    private String version;

    public UserAgentConfig(String name, String version) {
      this.name = name;
      this.version = version;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }

    public String getUserAgent() {
      return getName() + "/" + getVersion();
    }
  }

  public ZetaClientConfig(UserAgentConfig userAgent) {
    this.userAgent = userAgent;
  }

  public String getPathWellKnownRS() {
    return pathWellKnownRS;
  }

  public String getPathWellKnownAS() {
    return pathWellKnownAS;
  }

  public String getPathTokenAS() {
    return pathTokenAS;
  }

  public UserAgentConfig getUserAgent() {
    return userAgent;
  }

  public List<CardTerminalConnectionConfig> getTerminalConnectionConfigs() {
    return terminalConnectionConfigs;
  }

  public void addTerminalConnectionConfig(CardTerminalConnectionConfig terminalConnectionConfig) {
    terminalConnectionConfigs.add(terminalConnectionConfig);
  }

  public void setTerminalConnectionConfigs(
      List<CardTerminalConnectionConfig> terminalConnectionConfigs) {
    if (terminalConnectionConfigs != null) {
      this.terminalConnectionConfigs.clear();
      this.terminalConnectionConfigs.addAll(terminalConnectionConfigs);
    }
  }
}
