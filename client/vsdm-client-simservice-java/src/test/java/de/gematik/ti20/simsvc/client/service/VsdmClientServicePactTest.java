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
package de.gematik.ti20.simsvc.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.simsvc.client.config.VsdmConfig;
import de.gematik.ti20.simsvc.client.pact.PactConfig;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ExtendWith(PactConsumerTestExt.class)
@TestPropertySource(value = "classpath:pactconfig.properties")
@MockServerConfig(hostInterface = "localhost")
@Slf4j
class VsdmClientServicePactTest {

  @Autowired private VsdmConfig vsdmConfig;
  @Autowired private VsdmClientService vsdmClientService;
  @Autowired private PactConfig pactConfig;
  private SimulatorAttachedCard mockEgkCard;

  @BeforeEach
  void beforeEach() {
    // We set the properties in this way, so that we can use the configuration in the
    // @Pact annotation. Otherwise it does not see the values of the spring configuration bean
    System.setProperty("pact.consumer.name", pactConfig.getPactConsumerName());
    System.setProperty("pact.provider.name", pactConfig.getPactProviderName());
    mockEgkCard = mock(SimulatorAttachedCard.class);
    when(mockEgkCard.isEgk()).thenReturn(true);
    when(mockEgkCard.getSlotId()).thenReturn(1);
    when(mockEgkCard.getId()).thenReturn("card1");
  }

  @AfterEach
  void afterEach() {
    System.clearProperty("pact.consumer.name");
    System.clearProperty("pact.provider.name");
  }

  @SneakyThrows
  @Pact(provider = "${pact.provider.name}", consumer = "${pact.consumer.name}")
  public RequestResponsePact getPatientBundle(PactDslWithProvider builder) {
    String expectedResponse =
        new String(
            getClass()
                .getResourceAsStream("/expectedResponses/getVSDMBundle_200_0.json")
                .readAllBytes(),
            StandardCharsets.UTF_8);

    return builder
        .given(" a patient with ID 10 exists")
        .uponReceiving("a request to get a patient")
        .path("/vsdservice/v1/vsdmbundle")
        .method("GET")
        .willRespondWith()
        .status(200)
        .matchHeader("Content-Type", "application/json; charset=utf-8")
        .body(expectedResponse)
        .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getPatientBundle", pactVersion = PactSpecVersion.V3)
  void testGetPatientBundle(MockServer mockServer) {
    Integer egkSlotId = 1;
    Integer smcBSlotId = 2;

    configureVsdmUrl(mockServer.getUrl());

    var patientBundleAsString =
        vsdmClientService.requestVsd(
            "terminalId", egkSlotId, smcBSlotId, mockEgkCard, "token123", "etag123", false, false);

    assertThat(patientBundleAsString.getBody()).isNotEmpty();
  }

  void configureVsdmUrl(String url) {
    vsdmConfig.setUrl(url);
  }
}
