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
package de.gematik.ti20.simsvc.client.service.vsdm;

import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import de.gematik.ti20.simsvc.client.repository.VsdmDataRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VsdmDataFromCacheStrategy {

  private final VsdmDataRepository vsdmDataRepository;

  public VsdmDataFromCacheStrategy(final VsdmDataRepository vsdmDataRepository) {
    this.vsdmDataRepository = vsdmDataRepository;
  }

  public Optional<VsdmReadResult> get(
      final AttachedCard attachedCard, final String terminalId, final Integer egkSlotId) {
    if (attachedCard != null) {
      final VsdmCachedValue vsdmCachedValue =
          vsdmDataRepository.get(terminalId, egkSlotId, attachedCard.getId());

      if (vsdmCachedValue != null) {
        log.debug("VSDM data found in repository: {}", vsdmCachedValue);
        return Optional.of(vsdmCachedValue).map(VsdmReadResult::from);
      }
    }

    return Optional.empty();
  }
}
