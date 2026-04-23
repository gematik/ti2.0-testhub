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

import de.gematik.ti20.client.card.terminal.CardTerminalService;
import de.gematik.ti20.simsvc.client.service.PoppClientAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@ConfigurationProperties(prefix = "popp")
@Getter
@Setter
public class PoppConfig {

  private Http http;
  private Ws ws;
  private PoppClientConfig.TokenType tokenType;

  @Getter
  @Setter
  public static class Http {

    private String url;
  }

  @Getter
  @Setter
  public static class Ws {

    private String url;
  }

  @Bean
  public CardTerminalService getCardTerminalService() {
    return new CardTerminalService(null);
  }

  @Bean
  public PoppClientAdapter poppClientAdapter(WebClient webClient) {
    return new PoppClientAdapter(
        new PoppClientConfig(this.tokenType, this.getWs().getUrl(), this.getHttp().getUrl()),
        webClient);
  }

  @Bean
  WebClient webClient(WebClient.Builder builder) {
    return builder.build();
  }
}
