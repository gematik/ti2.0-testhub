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

import de.gematik.zeta.sdk.BuildConfig;
import de.gematik.zeta.sdk.StorageConfig;
import de.gematik.zeta.sdk.TpmConfig;
import de.gematik.zeta.sdk.ZetaSdk;
import de.gematik.zeta.sdk.ZetaSdkClient;
import de.gematik.zeta.sdk.authentication.AuthConfig;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VsdmZetaSdkClientConfig {

  @Bean
  public ZetaSdkClient vsdmServiceClient(final VsdmConfig vsdmConfig) {
    return ZetaSdk.INSTANCE.build(
        vsdmConfig.getResourceServerUrl(),
        new BuildConfig(
            new StorageConfig(),
            new TpmConfig() {},
            new AuthConfig(
                List.of("vsdservice"),
                "http://connector-address:9210/popp/test/api/v1/token-generator",
                "sub",
                1,
                vsdmConfig.getPdpUrl()),
            null,
            null));
  }
}
