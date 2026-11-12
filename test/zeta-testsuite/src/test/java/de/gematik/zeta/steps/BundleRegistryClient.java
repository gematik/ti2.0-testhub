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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

/** Publishes bundles through the Docker API, without writing to the local filesystem. */
final class BundleRegistryClient {
  private static final String REGISTRY_ROOT = "/usr/share/nginx/html";
  private static final String RESOURCE =
      REGISTRY_ROOT + "/bundles/zeta-updateability-bundle-test.tar.gz";
  private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(30);
  private final DockerCommand docker;
  private final Map<String, String> environment;

  @FunctionalInterface
  interface DockerCommand {
    String run(List<String> arguments, byte[] input);
  }

  BundleRegistryClient() {
    this(BundleRegistryClient::runDocker, System.getenv());
  }

  BundleRegistryClient(DockerCommand docker, Map<String, String> environment) {
    this.docker = docker;
    this.environment = Map.copyOf(environment);
  }

  void publish(String rego) {
    byte[] archive = copyArchive(rego);
    String container = containerId();
    String staging = REGISTRY_ROOT + "/.bundle-staging-" + UUID.randomUUID();
    try {
      exec(
          container,
          "mkdir -p \"$1/bundles\"; chmod 755 \"$1\" \"$1/bundles\"; mkdir -m 700 \"$2\"",
          REGISTRY_ROOT,
          staging);
      docker.run(List.of("cp", "-", container + ":" + staging + "/"), archive);
      exec(
          container,
          "chmod 644 \"$1/bundle.tar.gz\"; mv -f \"$1/bundle.tar.gz\" \"$2\"",
          staging,
          RESOURCE);
    } catch (RuntimeException failure) {
      try {
        removeStaging(container, staging);
      } catch (RuntimeException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
    removeStaging(container, staging);
  }

  void delete() {
    exec(containerId(), "rm -f -- \"$1\"", RESOURCE);
  }

  private void removeStaging(String container, String staging) {
    exec(container, "rm -f -- \"$1/bundle.tar.gz\"; rmdir -- \"$1\"", staging);
  }

  private void exec(String container, String script, String... arguments) {
    List<String> command =
        new ArrayList<>(List.of("exec", container, "sh", "-ec", script, "bundle-registry"));
    command.addAll(List.of(arguments));
    docker.run(command, new byte[0]);
  }

  private String containerId() {
    List<String> command = new ArrayList<>(List.of("compose"));
    String files = environment.getOrDefault("COMPOSE_FILES", "-f compose-local.yaml").trim();
    if (files.isEmpty()) {
      throw new IllegalStateException("COMPOSE_FILES must contain Compose file arguments");
    }
    command.addAll(List.of(files.split("\\s+")));
    String project = environment.get("COMPOSE_PROJECT_NAME");
    if (project != null && !project.isBlank()) {
      command.addAll(List.of("-p", project));
    }
    command.addAll(List.of("--profile", "full", "ps", "-q", "vsdm-zeta-bundle-registry"));
    String result = docker.run(command, new byte[0]).trim();
    if (!result.matches("[a-fA-F0-9]{12,64}")) {
      throw new IllegalStateException(
          "Expected exactly one running bundle registry container for this Compose project, but"
              + " got: "
              + result);
    }
    return result;
  }

  static byte[] copyArchive(String rego) {
    try {
      ByteArrayOutputStream bundle = new ByteArrayOutputStream();
      try (var gzip = new GzipCompressorOutputStream(bundle);
          var tar = new TarArchiveOutputStream(gzip)) {
        addEntry(
            tar, ".manifest", "{\"roots\":[\"zeta/bundletest\"]}".getBytes(StandardCharsets.UTF_8));
        addEntry(tar, "authz.rego", rego.getBytes(StandardCharsets.UTF_8));
      }
      ByteArrayOutputStream copy = new ByteArrayOutputStream();
      // docker cp - expects an uncompressed TAR containing the actual bundle file.
      try (var tar = new TarArchiveOutputStream(copy)) {
        addEntry(tar, "bundle.tar.gz", bundle.toByteArray());
      }
      return copy.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to build policy bundle in memory", e);
    }
  }

  private static void addEntry(TarArchiveOutputStream tar, String name, byte[] content)
      throws IOException {
    TarArchiveEntry entry = new TarArchiveEntry(name);
    entry.setSize(content.length);
    entry.setMode(UnixStat.DEFAULT_FILE_PERM);
    tar.putArchiveEntry(entry);
    tar.write(content);
    tar.closeArchiveEntry();
  }

  private static Path composeDirectory() {
    Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (root != null) {
      Path directory = root.resolve("infra/docker");
      if (Files.isRegularFile(directory.resolve("compose-local.yaml"))) {
        return directory;
      }
      root = root.getParent();
    }
    throw new IllegalStateException("Cannot locate infra/docker/compose-local.yaml from user.dir");
  }

  private static String runDocker(List<String> arguments, byte[] input) {
    List<String> command = new ArrayList<>(List.of("docker"));
    command.addAll(arguments);
    Process process;
    try {
      process = new ProcessBuilder(command).directory(composeDirectory().toFile()).start();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot start Docker command: " + command, e);
    }
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      try {
        var writer =
            executor.submit(
                () -> {
                  try (var stdin = process.getOutputStream()) {
                    stdin.write(input);
                  }
                  return null;
                });
        var stdout =
            executor.submit(
                () -> {
                  try (var stream = process.getInputStream()) {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                  }
                });
        var stderr =
            executor.submit(
                () -> {
                  try (var stream = process.getErrorStream()) {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                  }
                });
        if (!process.waitFor(COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
          throw new IllegalStateException("Docker command timed out: " + command);
        }
        String output = stdout.get(5, TimeUnit.SECONDS);
        String errors = stderr.get(5, TimeUnit.SECONDS);
        if (process.exitValue() != 0) {
          throw new IllegalStateException(
              "Docker command failed ("
                  + process.exitValue()
                  + "): "
                  + command
                  + "\n"
                  + output
                  + errors);
        }
        writer.get(5, TimeUnit.SECONDS);
        return output;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted during Docker command: " + command, e);
      } catch (ExecutionException | TimeoutException e) {
        throw new IllegalStateException("Failed to transfer Docker command data: " + command, e);
      } finally {
        if (process.isAlive()) {
          process.destroyForcibly();
        }
      }
    }
  }
}
