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
package de.gematik.ti20.client.card.terminal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardCertInfo;
import de.gematik.ti20.client.card.card.CardCertInfoEgk;
import de.gematik.ti20.client.card.card.CardCertInfoSmcb;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.CardType;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.card.SignOptions.HashAlgorithm;
import de.gematik.ti20.client.card.card.SignOptions.SignatureType;
import de.gematik.ti20.client.card.config.SimulatorConnectionConfig;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class CardTerminalSimSvcIT {

  private CardTerminalService cardService;

  @BeforeEach
  void setUp() {
    cardService =
        new CardTerminalService(
            List.of(
                new SimulatorConnectionConfig("Card Terminal SimSvc", "http://localhost:8000")
                //        ,
                //        new ConnectorConnectionConfig(
                //            "KoCoBox MED+",
                //            "https://192.168.1.100",
                //            "Mandant123",
                //            "PoppClientTest",
                //            "Workplace789",
                //            "User001"
                //        )
                ));
  }

  @AfterEach
  void tearDown() {}

  @SneakyThrows
  @Test
  void testCardTerminalSimvc() {
    var cards = cardService.getAttachedCards();
    assertNotNull(cards);
    assertFalse(cards.isEmpty());

    AttachedCard smcb =
        cards.stream().filter(card -> card.getType() == CardType.SMC_B).findFirst().orElse(null);
    assertNotNull(smcb);

    CardConnection smcbConnection = smcb.getTerminal().connect(smcb);
    assertNotNull(smcbConnection);

    CardCertInfo certInfo = smcbConnection.getCertInfo();
    assertInstanceOf(CardCertInfoSmcb.class, certInfo);
    var certInfoSmcb = (CardCertInfoSmcb) certInfo;
    assertNotNull(certInfoSmcb.getTelematikId());
    assertNotNull(certInfoSmcb.getProfessionOid());
    assertNotNull(certInfoSmcb.getHolderName());
    assertNotNull(certInfoSmcb.getOrganizationName());

    byte[] signResult =
        smcbConnection.sign(
            "Test Data for Sign()", new SignOptions(HashAlgorithm.SHA256, SignatureType.ECDSA));
    assertNotNull(signResult);

    smcbConnection.disconnect();

    AttachedCard egk =
        cards.stream().filter(card -> card.getType() == CardType.EGK).findFirst().orElse(null);

    assertNotNull(egk);
    CardConnection egkConnection = egk.getTerminal().connect(egk);
    assertNotNull(egkConnection);

    certInfo = egkConnection.getCertInfo();
    assertInstanceOf(CardCertInfoEgk.class, certInfo);
    var certInfoEgk = (CardCertInfoEgk) certInfo;
    assertNotNull(certInfoEgk.getKvnr());
    assertNotNull(certInfoEgk.getIknr());
    assertNotNull(certInfoEgk.getPatientName());
    assertNotNull(certInfoEgk.getFirstName());
    assertNotNull(certInfoEgk.getLastName());

    egkConnection.disconnect();
  }
}
