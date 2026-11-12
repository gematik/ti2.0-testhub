/*-
 * #%L
 * ZeTA Testsuite
 * %%
 * Copyright (C) 2026 gematik GmbH
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
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BundleRegistryClientTest {
  private static final String CONTAINER = "abcdef0123456789";

  @Test
  void copyStreamContainsReadableCompressedBundleWithIsolatedRoot() throws IOException {
    String policy = "package zeta.bundletest.authz\n";
    try (var copy =
        new TarArchiveInputStream(
            new ByteArrayInputStream(BundleRegistryClient.copyArchive(policy)))) {
      var file = copy.getNextEntry();
      assertThat(file.getName()).isEqualTo("bundle.tar.gz");
      assertThat(file.getMode()).isEqualTo(UnixStat.DEFAULT_FILE_PERM);
      byte[] compressedBundle = copy.readAllBytes();
      assertThat(copy.getNextEntry()).isNull();
      try (var bundle =
          new TarArchiveInputStream(
              new GzipCompressorInputStream(new ByteArrayInputStream(compressedBundle)))) {
        assertThat(bundle.getNextEntry().getName()).isEqualTo(".manifest");
        assertThat(new String(bundle.readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo("{\"roots\":[\"zeta/bundletest\"]}");
        var rego = bundle.getNextEntry();
        assertThat(rego.getName()).isEqualTo("authz.rego");
        assertThat(rego.getMode()).isEqualTo(UnixStat.DEFAULT_FILE_PERM);
        assertThat(new String(bundle.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(policy);
        assertThat(bundle.getNextEntry()).isNull();
      }
    }
  }

  @Test
  void publishingUsesCiProjectAndOverlayThenRenamesWithinVolume() {
    List<List<String>> commands = new ArrayList<>();
    var client =
        new BundleRegistryClient(
            (command, input) -> {
              commands.add(command);
              if (command.getFirst().equals("cp")) {
                assertThat(input).isNotEmpty();
                assertThat(command.get(1)).isEqualTo("-");
                assertThat(command.get(2))
                    .startsWith(CONTAINER + ":/usr/share/nginx/html/.bundle-staging-");
              } else {
                assertThat(input).isEmpty();
              }
              return command.getFirst().equals("compose") ? CONTAINER : "";
            },
            Map.of(
                "COMPOSE_FILES", "-f compose-local.yaml -f compose-ci-vsdm.yaml",
                "COMPOSE_PROJECT_NAME", "testhub-ci-123"));
    client.publish("policy");
    assertThat(commands.getFirst())
        .containsExactly(
            "compose",
            "-f",
            "compose-local.yaml",
            "-f",
            "compose-ci-vsdm.yaml",
            "-p",
            "testhub-ci-123",
            "--profile",
            "full",
            "ps",
            "-q",
            "vsdm-zeta-bundle-registry");
    assertThat(commands).hasSize(5);
    assertThat(commands.get(3).get(4)).contains("chmod 644", "mv -f");
    assertThat(commands.get(3).getLast())
        .isEqualTo("/usr/share/nginx/html/bundles/zeta-updateability-bundle-test.tar.gz");
    assertThat(commands.getLast().get(4)).contains("rmdir");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abcdef0123456789\n0123456789abcdef", "unexpected"})
  void missingOrAmbiguousRegistryDoesNotFallBackToAnotherStack(String result) {
    List<List<String>> commands = new ArrayList<>();
    var client =
        new BundleRegistryClient(
            (command, input) -> {
              commands.add(command);
              return result;
            },
            Map.of());
    assertThatThrownBy(client::delete)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("exactly one running bundle registry");
    assertThat(commands).hasSize(1);
    assertThat(commands.getFirst()).contains("compose-local.yaml");
  }

  @Test
  void cleanupErrorsAreNotSilentlyIgnored() {
    var client =
        new BundleRegistryClient(
            (command, input) -> {
              if (command.getFirst().equals("exec")) {
                throw new IllegalStateException("Docker exec failed");
              }
              return CONTAINER;
            },
            Map.of());
    assertThatThrownBy(client::delete).hasMessage("Docker exec failed");
  }

  @Test
  void failedCleanupDoesNotHidePublishingFailure() {
    var failure = new IllegalStateException("Docker copy failed");
    var cleanupFailure = new IllegalStateException("Docker cleanup failed");
    var client =
        new BundleRegistryClient(
            (command, input) -> {
              if (command.getFirst().equals("cp")) {
                throw failure;
              }
              if (command.contains("rm -f -- \"$1/bundle.tar.gz\"; rmdir -- \"$1\"")) {
                throw cleanupFailure;
              }
              return command.getFirst().equals("compose") ? CONTAINER : "";
            },
            Map.of());
    assertThatThrownBy(() -> client.publish("policy"))
        .isSameAs(failure)
        .satisfies(error -> assertThat(error.getSuppressed()).containsExactly(cleanupFailure));
  }
}
