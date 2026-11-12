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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class PolicyRejectionStepsTest {

  @ParameterizedTest
  @ValueSource(strings = {"vsdm-client", "zeta-ci-42-vsdm-client"})
  void resetsOnlyConfiguredClientAndUsesConfiguredDockerHost(String containerName)
      throws Exception {
    var steps = new PolicyRejectionSteps();
    var restTemplate = (RestTemplate) ReflectionTestUtils.getField(steps, "restTemplate");
    var server = MockRestServiceServer.createServer(restTemplate);
    String pdpUrl = "http://docker.internal:29122";
    String statusUrl = "http://docker.internal:26100/service/status";
    server
        .expect(requestTo(pdpUrl + "/auth/realms/master/protocol/openid-connect/token"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"access_token\":\"test-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(pdpUrl + "/auth/admin/realms/zeta-guard/clients"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo(statusUrl))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("UP", MediaType.TEXT_PLAIN));

    Process process = mock(Process.class);
    when(process.waitFor(60, TimeUnit.SECONDS)).thenReturn(true);
    when(process.exitValue()).thenReturn(0);
    when(process.getInputStream()).thenReturn(InputStream.nullInputStream());
    List<List<?>> commands = new ArrayList<>();
    try (var configuration = mockStatic(TigerGlobalConfiguration.class);
        var builders =
            mockConstruction(
                ProcessBuilder.class,
                (builder, context) -> {
                  commands.add((List<?>) context.arguments().getFirst());
                  when(builder.redirectErrorStream(true)).thenReturn(builder);
                  when(builder.start()).thenReturn(process);
                })) {
      configuration
          .when(
              () -> TigerGlobalConfiguration.resolvePlaceholders("${zeta.paths.vsdm.pdp.baseUrl}"))
          .thenReturn(pdpUrl);
      configuration
          .when(
              () ->
                  TigerGlobalConfiguration.resolvePlaceholders(
                      "${zeta.containers.vsdmClient|vsdm-client}"))
          .thenReturn(containerName);
      configuration
          .when(
              () ->
                  TigerGlobalConfiguration.resolvePlaceholders(
                      "http://${ports.host}:${ports.vsdmClientPort}/service/status"))
          .thenReturn(statusUrl);

      steps.resetZetaPdpRegistration();

      assertThat(commands)
          .containsExactly(
              List.of("docker", "restart", containerName),
              List.of("docker", "port", containerName, "80"));
      server.verify();
    }
  }
}
