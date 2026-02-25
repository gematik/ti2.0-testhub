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
import de.gematik.zeta.sdk.attestation.model.AttestationConfig;
import de.gematik.zeta.sdk.attestation.model.PlatformProductId;
import de.gematik.zeta.sdk.authentication.AuthConfig;
import de.gematik.zeta.sdk.authentication.SubjectTokenProvider;
import de.gematik.zeta.sdk.authentication.smb.SmbTokenProvider;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClientBuilder;
import io.ktor.client.plugins.logging.LogLevel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConfigurationProperties(prefix = "zetasdk")
@Getter
@Setter
public class VsdmZetaSdkClientConfig {

  private String smcbAliasPath;

  @SuppressWarnings({"java:S2068"}) // This is not a hardcoded password, but a path to a local file
  private String smcbPrivateKeyPasswordPath;

  private String smcbPrivateKeyPath;

  @Bean
  public ZetaSdkClient vsdmServiceClient(final VsdmClientConfig vsdmConfig) {
    boolean disableServerValidation = true;

    return ZetaSdk.INSTANCE.build(
        vsdmConfig.getResourceServerUrl(),
        new BuildConfig(
            "demo-client",
            "0.2.0",
            "sdk-client",
            new StorageConfig(),
            new TpmConfig() {},
            new AuthConfig(
                List.of("zero:audience"),
                30,
                true,
                getTokenProvider(),
                AttestationConfig.software()),
            new PlatformProductId.AppleProductId("apple", "macos", List.of("bundleX")),
            new ZetaHttpClientBuilder("")
                .disableServerValidation(disableServerValidation)
                .logging(LogLevel.ALL, message -> log.debug("Ktor HttpClient: {}", message)),
            null,
            null));
  }

  private SubjectTokenProvider getTokenProvider() {
    try {
      String alias =
          FileUtils.readFileToString(new File(getSmcbAliasPath()), StandardCharsets.UTF_8);
      String pw =
          FileUtils.readFileToString(
              new File(getSmcbPrivateKeyPasswordPath()), StandardCharsets.UTF_8);
      return new SmbTokenProvider(
          new SmbTokenProvider.Credentials(getSmcbPrivateKeyPath(), alias, pw));
    } catch (IOException e) {
      throw new IllegalStateException(
          "Could not load SMCB certificate for ZetaSDK. Check the configured file location exists and is readable",
          e);
    }
  }
}
