/*-
 * #%L
 * VSDM Client Simulator Service
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
package de.gematik.ti20.simsvc.client.service.popp;

import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.card.EgkInfo;
import de.gematik.ti20.simsvc.client.card.SmcbInfo;
import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import de.gematik.ti20.simsvc.client.exception.CardTerminalException;
import de.gematik.ti20.simsvc.client.repository.PoppTokenRepository;
import de.gematik.ti20.simsvc.client.service.CardTerminalService;
import de.gematik.ti20.simsvc.client.service.MockPoppTokenService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PoppTokenFromMockedStrategy {

  final VsdmClientConfig vsdmClientConfig;
  final CardTerminalService cardTerminalService;
  final MockPoppTokenService mockPoppTokenService;
  final PoppTokenRepository poppTokenRepository;

  public PoppTokenFromMockedStrategy(
      final VsdmClientConfig vsdmClientConfig,
      final CardTerminalService cardTerminalService,
      final MockPoppTokenService mockPoppTokenService,
      final PoppTokenRepository poppTokenRepository) {
    this.vsdmClientConfig = vsdmClientConfig;
    this.cardTerminalService = cardTerminalService;
    this.mockPoppTokenService = mockPoppTokenService;
    this.poppTokenRepository = poppTokenRepository;
  }

  public Optional<PoppToken> get(
      final String terminalId, final Integer egkSlotId, final AttachedCard attachedCard) {
    if (vsdmClientConfig.isUseMockPoppToken()) {
      log.info("Load mocked PoPP token");
      final String mockPoppToken = loadMockPoppToken(vsdmClientConfig, attachedCard);
      poppTokenRepository.put(terminalId, egkSlotId, attachedCard.getId(), mockPoppToken);
      return Optional.ofNullable(mockPoppToken).map(PoppToken::new);
    }
    return Optional.empty();
  }

  private String loadMockPoppToken(final VsdmClientConfig config, final AttachedCard attachedCard) {
    try {
      final EgkInfo egkInfo =
          requireNonNull(cardTerminalService.getEgkInfo(attachedCard), "egkInfo");
      final SmcbInfo smcbInfo = requireNonNull(cardTerminalService.getSmcbInfo(), "smcbInfo");

      final String iknr = requireNonNull(egkInfo.getIknr(), "egkInfo.iknr");
      final String kvnr = requireNonNull(egkInfo.getKvnr(), "egkInfo.kvnr");
      final String telematikId = requireNonNull(smcbInfo.getTelematikId(), "smcbInfo.telematikId");
      final String professionOid =
          requireNonNull(smcbInfo.getProfessionOid(), "smcbInfo.professionOid");

      log.info(
          "Creating mock token for iknr: {}, kvnr: {}, telematikId: {}, professionOid: {}",
          iknr,
          kvnr,
          telematikId,
          professionOid);

      return mockPoppTokenService.requestPoppToken(config, iknr, kvnr, telematikId, professionOid);
    } catch (final CardTerminalException cardEx) {
      return null;
    }
  }

  private <T> T requireNonNull(final T value, final String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " must not be null");
    }
    return value;
  }
}
