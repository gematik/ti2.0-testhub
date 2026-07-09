/*-
 * #%L
 * RBEL Fluent API
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
package de.gematik.ti20.rbel.fluent;

import de.gematik.test.tiger.LocalProxyRbelMessageListener;
import de.gematik.test.tiger.common.data.config.tigerproxy.TigerProxyConfiguration;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import de.gematik.test.tiger.lib.rbel.RbelValidator;
import de.gematik.test.tiger.proxy.TigerProxy;
import de.gematik.test.tiger.testenvmgr.TigerTestEnvMgr;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RbelFluentApiTigerProxyExampleTest {
  @Test
  void shouldValidateRecordedTrafficFromBackendTgrFile() {
    final var config = TigerProxyConfiguration.builder().proxyPort(0).adminPort(0).build();
    final var trafficFile = findTrafficFile("frontend.tgr");
    try (var tigerProxy = new TigerProxy(config)) {
      tigerProxy.readTrafficFromTgrFile(trafficFile.toString());
      tigerProxy.waitForAllCurrentMessagesToBeParsed();

      final var tigerTestEnvMgr = new TigerTestEnvMgr();
      final var retriever =
          new RbelMessageRetriever(
              tigerTestEnvMgr, tigerProxy, new LocalProxyRbelMessageListener(tigerProxy));
      final var components = RbelFluentApi.RbelComponents.of(retriever, new RbelValidator());

      RbelFluentApi.expectRequests(
              ".*/auth/realms/zeta-guard/protocol/openid-connect/token", components)
          .nextResponse(
              response ->
                  response
                      .hasValueAtPathEqualTo("$.responseCode", "200")
                      .hasValueAtPathEqualTo("$.body.token_type", "DPoP")
                      .hasValueAtPathMatching("$.body.access_token", "eyJ[a-zA-Z0-9_-]+\\..+")
                      .hasValueAtPathMatching("$.body.refresh_token", "eyJ[a-zA-Z0-9_-]+\\..+")
                      .hasValueAtPathMatching("$.body.scope", ".*profile.*")
                      .hasValueAtPathMatching("$.body.session_state", "[A-Za-z0-9_-]{8,}"));

      RbelFluentApi.expectRequests(".*/auth/realms/zeta-guard/zeta-guard-nonce", components)
          .nextResponse(
              response ->
                  response
                      .hasValueAtPathEqualTo("$.responseCode", "200")
                      .hasValueAtPathMatching("$.header.content-type", "(?i)text/plain.*")
                      .hasValueAtPathMatching("$.body", "[A-Za-z0-9_-]{8,}"));

      RbelFluentApi.expectRequests(
              ".*/auth/realms/zeta-guard/protocol/openid-connect/token", components)
          .nextResponse(
              response ->
                  response
                      .hasValueAtPathEqualTo("$.responseCode", "200")
                      .hasValueAtPathEqualTo("$.body.token_type", "DPoP")
                      .hasValueAtPathMatching("$.body.scope", ".*profile.*")
                      .hasValueAtPathMatching("$.body.access_token", ".{100,}"));

      RbelFluentApi.expectRequests(".*/ws", components)
          .nextResponse(
              response ->
                  response
                      .hasValueAtPathEqualTo("$.responseCode", "101")
                      .hasValueAtPathMatching("$.header.Upgrade", "(?i)websocket")
                      .hasValueAtPathMatching("$.header.Connection", "(?i).*upgrade.*"));
    }
  }

  private static Path findTrafficFile(final String fileName) {
    Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (current != null) {
      final var candidate = current.resolve("doc").resolve("traffic").resolve(fileName);
      if (Files.exists(candidate)) {
        return candidate;
      }
      current = current.getParent();
    }
    throw new IllegalStateException(
        "Could not locate doc/traffic/" + fileName + " from current project path.");
  }
}
