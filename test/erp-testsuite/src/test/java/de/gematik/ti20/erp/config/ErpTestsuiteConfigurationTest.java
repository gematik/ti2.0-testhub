/*-
 * #%L
 * erp-testsuite
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
package de.gematik.ti20.erp.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.gematik.ti20.erp.TestSuite;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.util.SetSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestSuite.class)
@ActiveProfiles("ru")
class ErpTestsuiteConfigurationTest {

  @Autowired private ErpTestsuiteConfiguration config;
  @Autowired private ErpEnvironmentConfiguration environment;
  @Autowired private ErpActorsConfiguration actors;
  @Autowired private CatsConfiguration cats;

  @Test
  void shouldInstantiateConfigurationFromApplicationYaml() {
    assertNotNull(config);
    assertNotNull(environment);
    assertNotNull(actors);
    assertNotNull(cats);
  }

  @Test
  void shouldBindEnvironmentForRuProfile() {
    List.of(
            environment.getFdUrl(),
            environment.getFdUserAgent(),
            environment.getIdpClientId(),
            environment.getIdpRedirectUrl(),
            environment.getIdpDiscoveryDocument())
        .forEach(
            value -> {
              assertNotNull(value);
              assertFalse(value.isEmpty());
            });
  }

  @Test
  void shouldBindActors() {
    assertFalse(actors.getPatients().isEmpty());
    assertFalse(actors.getPharmacies().isEmpty());
  }

  @Test
  void shouldBindCats() {
    List.of(cats.getUrl(), cats.getTerminalId())
        .forEach(
            value -> {
              assertNotNull(value);
              assertFalse(value.isEmpty());
            });
  }

  @Test
  void shouldInstantiateCardTerminal() {
    val cardTerminal = assertDoesNotThrow(() -> config.getCardTerminal());
    assertNotNull(cardTerminal);
    cardTerminal.connect();
  }

  @Test
  void shouldInstantiatePoppClientWithoutProxy() {
    val poppClient = assertDoesNotThrow(() -> config.createPoppClient());
    assertNotNull(poppClient);
    assertNull(poppClient.config().getProxy());
  }

  @Test
  @SetSystemProperty(key = "https.proxyHost", value = "localhost")
  @SetSystemProperty(key = "https.proxyPort", value = "8080")
  void shouldInstantiatePoppClientWithProxy() {
    val poppClient = assertDoesNotThrow(() -> config.createPoppClient());
    assertNotNull(poppClient);
    assertNotNull(poppClient.config().getProxy());

    val proxy = poppClient.config().getProxy();
    assertEquals("localhost", proxy.getHost());
    assertEquals(8080, proxy.getPort());
  }

  @Test
  void shouldResolveSmcbFromSmartcardArchive() {
    actors
        .getPharmacies()
        .forEach(
            it -> {
              val smcb = assertDoesNotThrow(() -> config.getSmcbFor(it.name()));
              assertEquals(it.iccsn(), smcb.getIccsn());
            });
  }

  @Test
  void shouldCreatePatientEgk() {
    actors
        .getPatients()
        .forEach(
            it -> {
              val egk = assertDoesNotThrow(() -> config.getPatientEgk(it.name()));
              assertEquals(it.iccsn(), egk.getIccsn());
            });
  }
}
