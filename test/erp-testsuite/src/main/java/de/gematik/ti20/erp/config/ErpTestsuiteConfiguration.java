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

import de.gematik.bbriccs.cardterminal.CardTerminal;
import de.gematik.bbriccs.cats.CatsClient;
import de.gematik.bbriccs.smartcards.DummyEgk;
import de.gematik.bbriccs.smartcards.Egk;
import de.gematik.bbriccs.smartcards.SmartcardArchive;
import de.gematik.bbriccs.smartcards.SmcB;
import de.gematik.bbriccs.smartcards.cfg.SmartcardConfigDto;
import de.gematik.test.erezept.client.ErpClient;
import de.gematik.test.erezept.client.cfg.ErpClientFactory;
import de.gematik.test.erezept.config.dto.actor.PharmacyConfiguration;
import de.gematik.test.erezept.config.dto.erpclient.BackendRouteConfiguration;
import de.gematik.test.erezept.config.dto.erpclient.EnvironmentConfiguration;
import java.util.Optional;
import kong.unirest.core.ContentType;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestInstance;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErpTestsuiteConfiguration {

  private final ErpEnvironmentConfiguration environment;
  private final ErpActorsConfiguration actors;
  private final CatsConfiguration cats;
  private final SmartcardArchive sca;

  private final String poppClientUrl;

  @Autowired
  public ErpTestsuiteConfiguration(
      ErpEnvironmentConfiguration environment,
      ErpActorsConfiguration actors,
      CatsConfiguration cats,
      @Value("${erp.smartcards}") String smartcardsPath,
      @Value("${erp.popp-client.url}") String poppClientUrl) {
    this.environment = environment;
    this.actors = actors;
    this.cats = cats;
    this.sca = SmartcardArchive.from(smartcardsPath);
    this.poppClientUrl = poppClientUrl;
  }

  public SmcB getSmcbFor(String name) {
    val actor =
        this.actors.getPharmacies().stream()
            .filter(it -> name.equalsIgnoreCase(it.name()))
            .findAny()
            .orElseThrow();

    return sca.getSmcbByICCSN(actor.iccsn());
  }

  public ErpClient createPharmacyErpClient(String name) {
    val envConfig = createEnvironmentConfiguration();

    val smcb = getSmcbFor(name);

    val actorCfg = new PharmacyConfiguration();
    actorCfg.setKonnektor("Soft-Konn"); // TODO: take from spring-boot config
    actorCfg.setFhirValidator("NONE"); // TODO: take from spring-boot config

    Unirest.config().verifySsl(false);
    val client = ErpClientFactory.createErpClient(envConfig, actorCfg);

    client.authenticateWith(smcb);

    return client;
  }

  public Egk getPatientEgk(String name) {
    // Note: eGK has no p12, hence is not in SmartcardArchive. But MUST be configured in CATS
    val actor =
        this.actors.getPatients().stream()
            .filter(it -> name.equalsIgnoreCase(it.name()))
            .findAny()
            .orElseThrow();

    val egkConf = new SmartcardConfigDto();
    egkConf.setIccsn(actor.iccsn());
    egkConf.setOwnerName(actor.name());

    // create a Dummy eGK which is enough for CATS operations
    return DummyEgk.fromConfig(egkConf);
  }

  public CardTerminal getCardTerminal() {
    val catsUrl = cats.getUrl();
    val catsTid = cats.getTerminalId();

    return CatsClient.create(catsUrl).withTerminalId(catsTid).connect();
  }

  public UnirestInstance createPoppClient() {
    val client = Unirest.spawnInstance();

    client
        .config()
        .defaultBaseUrl(poppClientUrl)
        .setDefaultHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
        .verifySsl(false);

    // set the proxy
    // this MUST be the remoteTigerProxy from TestHub
    Optional.ofNullable(System.getProperty("https.proxyHost"))
        .ifPresent(
            ph -> {
              val port = Integer.parseInt(System.getProperty("https.proxyPort", "6350"));
              client.config().proxy(ph, port);
            });

    return client;
  }

  private EnvironmentConfiguration createEnvironmentConfiguration() {
    val env = new EnvironmentConfiguration();

    val envRoute = new BackendRouteConfiguration();
    env.setTi(envRoute);
    env.setInternet(envRoute);
    envRoute.setClientId(this.environment.getIdpClientId());
    envRoute.setRedirectUrl(this.environment.getIdpRedirectUrl());
    envRoute.setDiscoveryDocumentUrl(this.environment.getIdpDiscoveryDocument());

    envRoute.setFdBaseUrl(this.environment.getFdUrl());
    envRoute.setUserAgent(this.environment.getFdUserAgent());

    return env;
  }
}
