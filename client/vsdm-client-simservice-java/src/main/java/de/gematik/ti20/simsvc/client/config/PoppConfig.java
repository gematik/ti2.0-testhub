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

import de.gematik.ti20.client.popp.config.PoppClientConfig;
import de.gematik.ti20.client.popp.service.PoppClientService;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig;
import de.gematik.ti20.client.zeta.config.ZetaClientConfig.UserAgentConfig;
import de.gematik.ti20.client.zeta.service.ZetaClientService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConfigurationProperties(prefix = "popp")
@Getter
@Setter
public class PoppConfig {

  private Http http;
  private Ws ws;

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
  public ZetaClientService getZetaClientService() {
    return new ZetaClientService(new ZetaClientConfig(new UserAgentConfig("VsdmClient", "1.0.0")));
  }

  @Bean
  public PoppClientService poppClientService(final ZetaClientService zetaClientService) {
    return new PoppClientService(
        new PoppClientConfig(this.getWs().getUrl(), this.getHttp().getUrl()), zetaClientService);
  }
}
