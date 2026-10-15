/*-
 * #%L
 * Card Terminal Simulator
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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.ti20.simsvc.client.model.EgkIdentifier;
import de.gematik.ti20.simsvc.client.repository.CardImageRepository;
import de.gematik.ti20.simsvc.client.service.EgkInfoService;
import de.gematik.ti20.simsvc.client.service.SmcBInfoService;
import de.gematik.ti20.simsvc.client.service.VirtualCardImageLoader;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

class CardTerminalConfigurationTest {

  private ApplicationContextRunner contextRunner() throws IOException {
    final var propertySources =
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yaml"));
    return new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(CardTerminalConfiguration.class)
        .withInitializer(
            context ->
                propertySources.forEach(
                    source -> context.getEnvironment().getPropertySources().addLast(source)));
  }

  @Test
  void bindsDefaultPathsFromApplicationYaml() throws IOException {
    contextRunner()
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(CardTerminalConfiguration.class).getCardImagePaths())
                  .containsExactly(
                      "attached_assets",
                      "cardimages",
                      "file:no-publish/test-data/cards",
                      "file:test/vsdm-testsuite/src/test/resources/private");
            });
  }

  @Test
  void overriddenPathsAreUsedForRepositoryInitialization() throws IOException {
    contextRunner()
        .withPropertyValues("card.terminal.card-image-paths[0]=attached_assets")
        .withUserConfiguration(
            CardImageRepository.class,
            VirtualCardImageLoader.class,
            EgkInfoService.class,
            SmcBInfoService.class)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(CardTerminalConfiguration.class).getCardImagePaths())
                  .containsExactly("attached_assets");
              assertThat(
                      context
                          .getBean(CardImageRepository.class)
                          .findByEgkIdentifier(new EgkIdentifier("109500969", "X110639491")))
                  .isNotEmpty()
                  .allSatisfy(card -> assertThat(card.path()).startsWith("attached_assets/"));
            });
  }

  @Test
  void emptyConfiguredPathsDisableStartupScanning() throws IOException {
    contextRunner()
        .withPropertyValues("card.terminal.card-image-paths=")
        .withUserConfiguration(
            CardImageRepository.class,
            VirtualCardImageLoader.class,
            EgkInfoService.class,
            SmcBInfoService.class)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(CardTerminalConfiguration.class).getCardImagePaths())
                  .isEmpty();
              assertThat(
                      context
                          .getBean(CardImageRepository.class)
                          .findByEgkIdentifier(new EgkIdentifier("109500969", "X110639491")))
                  .isEmpty();
            });
  }

  @Test
  void missingPathsDefaultToEmptyList() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
        .withUserConfiguration(CardTerminalConfiguration.class)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(CardTerminalConfiguration.class).getCardImagePaths())
                  .isEmpty();
            });
  }
}
