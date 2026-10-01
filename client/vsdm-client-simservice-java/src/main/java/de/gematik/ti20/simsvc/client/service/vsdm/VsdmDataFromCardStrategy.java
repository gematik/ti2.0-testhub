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

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.card.EgkInfo;
import de.gematik.ti20.simsvc.client.exception.CardTerminalException;
import de.gematik.ti20.simsvc.client.service.CardTerminalService;
import de.gematik.ti20.simsvc.client.service.FhirService;
import de.gematik.ti20.vsdm.fhir.builder.VsdmBundleBuilder;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPatientBuilder;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import java.net.HttpURLConnection;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
public class VsdmDataFromCardStrategy {

  private final CardTerminalService cardTerminalService;
  private final FhirService fhirService;

  public VsdmDataFromCardStrategy(
      final CardTerminalService cardTerminalService, final FhirService fhirService) {
    this.cardTerminalService = cardTerminalService;
    this.fhirService = fhirService;
  }

  public Optional<VsdmReadResult> get(final AttachedCard attachedCard) {
    if (attachedCard == null) {
      // No fallback available when using provided token
      throw new ResponseStatusException(
          HttpStatusCode.valueOf(HttpURLConnection.HTTP_INTERNAL_ERROR));
    }
    // Fallback to card data only if attachedCard is available
    try {
      final String responseToCaller = loadTruncatedDataFromCard(attachedCard);
      if (responseToCaller == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "eGK data is invalid");
      }

      log.debug("VSDM data loaded from card: {}", responseToCaller);

      return Optional.of(new VsdmReadResult(HttpStatus.OK, null, null, responseToCaller, null));
    } catch (final CardTerminalException cardEx) {
      log.error("Error while loading truncated data from card: {}", cardEx.getMessage(), cardEx);
      throw new ResponseStatusException(
          HttpStatusCode.valueOf(HttpURLConnection.HTTP_INTERNAL_ERROR));
    }
  }

  public String loadTruncatedDataFromCard(final AttachedCard attachedCard)
      throws CardTerminalException {
    final EgkInfo egkInfo = cardTerminalService.getEgkInfo(attachedCard);

    if (Boolean.FALSE.equals(egkInfo.getValid())) {
      return null;
    }

    // send 401, falls nicht valid
    final VsdmBundle truncatedDataBundle =
        VsdmBundleBuilder.create()
            .addEntry(
                VsdmPatientBuilder.create()
                    .withKvnr(egkInfo.getKvnr())
                    .withNames(egkInfo.getLastName(), egkInfo.getFirstName())
                    .build())
            .build();

    return fhirService.encodeResponse(truncatedDataBundle, EncodingType.JSON);
  }
}
