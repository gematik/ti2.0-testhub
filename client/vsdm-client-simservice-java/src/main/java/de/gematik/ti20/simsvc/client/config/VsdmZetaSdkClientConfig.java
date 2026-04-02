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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@ConfigurationProperties(prefix = "zetasdk")
@Getter
@Setter
public class VsdmZetaSdkClientConfig {

  private String smcbAlias;

  @SuppressWarnings({"java:S2068"}) // This is not production software
  private String smcbPrivateKeyPassword;

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
            new PlatformProductId.LinuxProductId(
                PlatformProductId.PLATFORM_LINUX, "jar", "testhub", "latest"),
            new ZetaHttpClientBuilder("")
                .disableServerValidation(disableServerValidation)
                .logging(LogLevel.ALL, message -> log.debug("Ktor HttpClient: {}", message)),
            null,
            null));
  }

  private SubjectTokenProvider getTokenProvider() {
    String keyPath = getSmcbPrivateKeyPath();
    if (keyPath == null || keyPath.isBlank()) {
      throw new RuntimeException(
          "SMCB private key path is not configured (zetasdk.smcbPrivateKeyPath). Please set the path to the private key file.");
    }

    Path p = Paths.get(keyPath);
    if (!Files.exists(p)) {
      throw new RuntimeException("SMCB private key file does not exist: " + keyPath);
    }
    if (!Files.isRegularFile(p) || !Files.isReadable(p)) {
      throw new RuntimeException("SMCB private key file is not readable: " + keyPath);
    }

    return new SmbTokenProvider(
        new SmbTokenProvider.Credentials(keyPath, smcbAlias, smcbPrivateKeyPassword, ""));
  }
}
