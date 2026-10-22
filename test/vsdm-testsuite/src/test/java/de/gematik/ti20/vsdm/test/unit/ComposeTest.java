/*-
 * #%L
 * VSDM 2.0 Testsuite
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
package de.gematik.ti20.vsdm.test.unit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class ComposeTest {

  private static final Pattern VARIABLE_DEFAULT =
      Pattern.compile("\\$\\{[A-Za-z_][A-Za-z0-9_]*(?::-|-)([^{}]*)}");
  private static final Map<String, Map<?, ?>> SERVICES = new LinkedHashMap<>();
  private static final Set<String> NETWORKS = new LinkedHashSet<>();
  private static final Set<String> VOLUMES = new LinkedHashSet<>();
  private static final Set<Path> COMPOSE_FILES = new LinkedHashSet<>();

  @BeforeAll
  static void readComposeFiles() throws IOException {
    Path root = Path.of("").toAbsolutePath();
    while (root != null && !Files.isRegularFile(root.resolve("infra/docker/compose-local.yaml"))) {
      root = root.getParent();
    }
    assertNotNull(root, "Could not locate infra/docker/compose-local.yaml from the test directory");
    readComposeFile(root.resolve("infra/docker/compose-local.yaml"));
  }

  private static Set<String> activeServices(String profile) {
    Set<String> activeServices = new LinkedHashSet<>();
    SERVICES.forEach(
        (name, service) -> {
          List<?> profiles = list(serviceProperty(name, "profiles", new LinkedHashSet<>()));
          if (profiles.isEmpty() || profiles.contains(profile)) {
            activeServices.add(name);
          }
        });
    return activeServices;
  }

  @ParameterizedTest(name = "{0}: service selection")
  @ValueSource(strings = {"backend-only", "full", "ps-only"})
  void profileSelectsExpectedServices(String profile) {
    assertEquals(
        4, COMPOSE_FILES.size(), "Read the local Compose file and all three backend includes");
    Set<String> backendServices =
        Set.of(
            "popp-token-generator",
            "popp-server",
            "popp-server-db",
            "popp-zeta-ingress",
            "popp-zeta-pep",
            "popp-zeta-pdp",
            "popp-zeta-pdp-db",
            "popp-zeta-pdp-opa",
            "vsdm-server",
            "vsdm-zeta-ingress",
            "vsdm-zeta-pep",
            "vsdm-zeta-pdp",
            "vsdm-zeta-pdp-db",
            "vsdm-zeta-pdp-opa",
            "otel-collector",
            "prometheus",
            "grafana",
            "resolv-writer",
            "canopy",
            "docker-tiger-proxy");
    Set<String> expected = new LinkedHashSet<>();
    switch (profile) {
      case "backend-only" -> {
        expected.addAll(backendServices);
        expected.removeAll(Set.of("popp-zeta-pep", "vsdm-zeta-pep"));
        expected.addAll(Set.of("popp-zeta-pep-backend-only", "vsdm-zeta-pep-backend-only"));
      }
      case "full" -> {
        expected.addAll(backendServices);
        expected.addAll(
            Set.of("card-terminal-client", "vsdm-client", "popp-client", "docker-tiger-proxy"));
      }
      case "ps-only" ->
          expected.addAll(
              Set.of(
                  "popp-token-generator",
                  "card-terminal-client",
                  "vsdm-client",
                  "popp-client",
                  "docker-tiger-proxy",
                  "resolv-writer",
                  "canopy",
                  "otel-collector",
                  "prometheus"));
      default -> fail("Unexpected profile: " + profile);
    }
    assertEquals(expected, activeServices(profile));
  }

  @ParameterizedTest(name = "{0}: profile-specific protected resource metadata")
  @ValueSource(strings = {"backend-only", "full", "ps-only"})
  void profileSelectsExactlyOneMetadataMountPerPep(String profile) throws IOException {
    Path backend =
        COMPOSE_FILES.stream()
            .filter(path -> path.getFileName().toString().equals("compose-backend.yaml"))
            .findFirst()
            .orElseThrow()
            .getParent();
    Set<String> active = activeServices(profile);
    for (String domain : List.of("popp", "vsdm")) {
      String pep = domain + "-zeta-pep";
      List<String> selected =
          active.stream()
              .filter(name -> name.equals(pep) || name.equals(pep + "-backend-only"))
              .toList();
      assertEquals("ps-only".equals(profile) ? 0 : 1, selected.size());
      if (selected.isEmpty()) {
        continue;
      }
      String service = selected.getFirst();
      String directory = "popp".equals(domain) ? "zeta-popp" : "zeta";
      directory += "/well-known" + ("backend-only".equals(profile) ? "-backend-only" : "");
      List<String> mounts =
          list(serviceProperty(service, "volumes", new LinkedHashSet<>())).stream()
              .map(ComposeTest::string)
              .filter(mount -> mount.contains(":/srv/.well-known/"))
              .toList();
      assertEquals(List.of("./" + directory + "/:/srv/.well-known/:ro"), mounts);
      assertTrue(
          Files.isRegularFile(backend.resolve(directory).resolve("oauth-protected-resource")));
      assertTrue(dependencies(domain + "-zeta-ingress").containsKey(service));
      if ("backend-only".equals(profile)) {
        Map<?, ?> network =
            map(
                map(serviceProperty(service, "networks", new LinkedHashSet<>()))
                    .get("testhub_network"));
        assertTrue(
            list(network.get("aliases")).contains(pep), service + " must keep the PEP DNS name");
      }
    }
  }

  @ParameterizedTest(name = "{0}: required dependencies")
  @ValueSource(strings = {"backend-only", "full", "ps-only"})
  void allRequiredDependenciesAreEnabledByProfile(String profile) {
    Set<String> activeServices = activeServices(profile);
    List<Executable> checks = new ArrayList<>();
    activeServices.forEach(
        name ->
            dependencies(name)
                .forEach(
                    (dependency, options) -> {
                      // Compose permits an unavailable dependency only with required: false.
                      if (!Boolean.FALSE.equals(options.get("required"))) {
                        checks.add(
                            () ->
                                assertTrue(
                                    activeServices.contains(dependency),
                                    name
                                        + " requires "
                                        + dependency
                                        + ", which is not enabled by "
                                        + profile));
                      }
                    }));
    assertAll(profile + " dependency closure", checks);
  }

  @ParameterizedTest(name = "{0}: healthcheck prerequisites")
  @ValueSource(strings = {"backend-only", "full", "ps-only"})
  void dependenciesAwaitingHealthyServicesHaveEnabledHealthchecks(String profile) {
    Set<String> activeServices = activeServices(profile);
    List<Executable> checks = new ArrayList<>();
    activeServices.forEach(
        name ->
            dependencies(name)
                .forEach(
                    (dependency, options) -> {
                      if ("service_healthy".equals(options.get("condition"))
                          && activeServices.contains(dependency)) {
                        checks.add(
                            () -> {
                              Map<?, ?> healthcheck =
                                  map(
                                      serviceProperty(
                                          dependency, "healthcheck", new LinkedHashSet<>()));
                              List<?> command = list(healthcheck.get("test"));
                              assertFalse(
                                  Boolean.TRUE.equals(healthcheck.get("disable")),
                                  name + " waits for disabled healthcheck on " + dependency);
                              assertFalse(command.isEmpty(), dependency + " needs a healthcheck");
                              assertNotEquals(
                                  "NONE",
                                  command.getFirst(),
                                  dependency + " healthcheck is disabled");
                            });
                      }
                    }));
    assertAll(profile + " healthcheck prerequisites", checks);
  }

  @ParameterizedTest(name = "{0}: startup dependency cycles")
  @ValueSource(strings = {"backend-only", "full", "ps-only"})
  void startupDependenciesHaveNoCycles(String profile) {
    Set<String> activeServices = activeServices(profile);
    Set<String> visited = new LinkedHashSet<>();
    for (String name : activeServices) {
      visitDependencies(name, activeServices, new LinkedHashSet<>(), visited);
    }
  }

  @ParameterizedTest(name = "{0}: resource declarations")
  @ValueSource(strings = {"backend-only", "full", "ps-only"})
  void activeServicesReferenceDeclaredNetworksAndNamedVolumes(String profile) {
    List<Executable> checks = new ArrayList<>();
    activeServices(profile)
        .forEach(
            name -> {
              Object networks = serviceProperty(name, "networks", new LinkedHashSet<>());
              Set<String> references = new LinkedHashSet<>();
              if (networks instanceof Map<?, ?> networkMap) {
                networkMap.keySet().forEach(key -> references.add(string(key)));
              } else {
                list(networks).forEach(network -> references.add(string(network)));
              }
              references.forEach(
                  network ->
                      checks.add(
                          () ->
                              assertTrue(
                                  NETWORKS.contains(network),
                                  name + " references undeclared network " + network)));
              for (Object volume : list(serviceProperty(name, "volumes", new LinkedHashSet<>()))) {
                String source;
                if (volume instanceof Map<?, ?> volumeMap) {
                  if (!"volume".equals(volumeMap.get("type")) || !volumeMap.containsKey("source")) {
                    continue;
                  }
                  source = resolveVariableDefaults(string(volumeMap.get("source")));
                  if (source.contains("${")) {
                    continue; // No fallback: the source requires environment interpolation.
                  }
                } else {
                  String mount = resolveVariableDefaults(string(volume));
                  if (mount.contains("${")) {
                    continue; // No fallback: the mount requires environment interpolation.
                  }
                  if (!mount.contains(":")) {
                    continue; // Anonymous volume.
                  }
                  source = mount.substring(0, mount.indexOf(':'));
                  if (source.startsWith(".") || source.startsWith("/") || source.startsWith("~")) {
                    continue; // Bind mount; host files are not prerequisites for this static check.
                  }
                }
                String namedVolume = source;
                checks.add(
                    () ->
                        assertTrue(
                            VOLUMES.contains(namedVolume),
                            name + " references undeclared volume " + namedVolume));
              }
            });
    assertAll(profile + " resource declarations", checks);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "${CONNECTOR_KEYSTORE_DIR:-./zeta/connector}/${CONNECTOR_KEYSTORE_FILE:-keystore.p12}:/app/connector/keystore.p12:ro|./zeta/connector/keystore.p12:/app/connector/keystore.p12:ro",
        "${CONNECTOR_TRUSTSTORE_DIR:-./zeta/connector}/${CONNECTOR_TRUSTSTORE_FILE:-truststore.p12}:/app/connector/truststore.p12:ro|./zeta/connector/truststore.p12:/app/connector/truststore.p12:ro",
        "${DATA_VOLUME:-postgres_data}:/data|postgres_data:/data",
        "${DATA_VOLUME-postgres_data}:/data|postgres_data:/data",
        "${DATA_VOLUME}:/data|${DATA_VOLUME}:/data",
        "${DATA_DIR:-/tmp/a$1}:/data|/tmp/a$1:/data"
      })
  void volumeDefaultsAreResolvedBeforeParsingMountSeparators(String mount, String expected) {
    assertEquals(expected, resolveVariableDefaults(mount));
  }

  private static String resolveVariableDefaults(String value) {
    // Validate the no-environment configuration, independently of the developer's environment.
    return VARIABLE_DEFAULT
        .matcher(value)
        .replaceAll(match -> Matcher.quoteReplacement(match.group(1)));
  }

  private static void readComposeFile(Path path) throws IOException {
    path = path.toAbsolutePath().normalize();
    assertTrue(COMPOSE_FILES.add(path), "Duplicate or circular Compose include: " + path);
    LoaderOptions options = new LoaderOptions();
    options.setAllowDuplicateKeys(false);
    Map<?, ?> document;
    try (var input = Files.newInputStream(path)) {
      document = map(new Yaml(new SafeConstructor(options)).load(input));
    }
    map(document.get("services"))
        .forEach(
            (name, service) ->
                assertNull(
                    SERVICES.putIfAbsent(string(name), map(service)),
                    "Duplicate service in Compose includes: " + name));
    map(document.get("networks")).keySet().forEach(name -> NETWORKS.add(string(name)));
    map(document.get("volumes")).keySet().forEach(name -> VOLUMES.add(string(name)));
    for (Object include : list(document.get("include"))) {
      Object includePath = include instanceof Map<?, ?> entry ? entry.get("path") : include;
      List<?> paths = includePath instanceof List<?> entries ? entries : List.of(includePath);
      for (Object child : paths) {
        readComposeFile(path.getParent().resolve(string(child)));
      }
    }
  }

  private static Map<String, Map<?, ?>> dependencies(String name) {
    Object dependsOn = serviceProperty(name, "depends_on", new LinkedHashSet<>());
    Map<String, Map<?, ?>> result = new LinkedHashMap<>();
    if (dependsOn instanceof Map<?, ?> entries) {
      entries.forEach((dependency, options) -> result.put(string(dependency), map(options)));
    } else {
      list(dependsOn).forEach(dependency -> result.put(string(dependency), Map.of()));
    }
    return result;
  }

  private static Object serviceProperty(String name, String property, Set<String> visiting) {
    assertTrue(visiting.add(name), "Circular service inheritance: " + visiting + " -> " + name);
    Map<?, ?> service = SERVICES.get(name);
    assertNotNull(service, "Unknown service in extends: " + name);
    Map<?, ?> parent = map(service.get("extends"));
    if (parent.isEmpty()) {
      return service.get(property);
    }
    assertFalse(parent.containsKey("file"), "External extends files are not supported: " + name);
    Object inherited = serviceProperty(string(parent.get("service")), property, visiting);
    Object own = service.get(property);
    if (inherited instanceof Map<?, ?> inheritedMap && own instanceof Map<?, ?> ownMap) {
      Map<Object, Object> merged = new LinkedHashMap<>(inheritedMap);
      merged.putAll(ownMap);
      return merged;
    }
    // The current client bases have no sequence resources requiring Compose's target-based merging.
    if (inherited instanceof List<?> inheritedList
        && own instanceof List<?>
        && Set.of("networks", "volumes", "depends_on").contains(property)) {
      assertTrue(
          inheritedList.isEmpty(), "Sequence merging is required for " + name + "." + property);
    }
    return own == null ? inherited : own;
  }

  private static void visitDependencies(
      String name, Set<String> activeServices, Set<String> visiting, Set<String> visited) {
    assertFalse(visiting.contains(name), "Startup dependency cycle: " + visiting + " -> " + name);
    if (!visited.add(name)) {
      return;
    }
    visiting.add(name);
    for (String dependency : dependencies(name).keySet()) {
      if (activeServices.contains(dependency)) {
        visitDependencies(dependency, activeServices, visiting, visited);
      }
    }
    visiting.remove(name);
  }

  private static Map<?, ?> map(Object value) {
    return value == null ? Map.of() : assertInstanceOf(Map.class, value);
  }

  private static List<?> list(Object value) {
    return value == null ? List.of() : assertInstanceOf(List.class, value);
  }

  private static String string(Object value) {
    return assertInstanceOf(String.class, value);
  }
}
