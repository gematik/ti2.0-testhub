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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.ti20.simsvc.client.card.AttachedCard;
import de.gematik.ti20.simsvc.client.card.EgkInfo;
import de.gematik.ti20.simsvc.client.exception.CardTerminalException;
import de.gematik.ti20.simsvc.client.service.CardTerminalService;
import de.gematik.ti20.simsvc.client.service.FhirService;
import de.gematik.ti20.vsdm.fhir.def.VsdmBundle;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VsdmDataFromCardStrategyTest {

  @Mock private CardTerminalService cardTerminalService;
  @Mock private FhirService fhirService;
  @Mock private AttachedCard attachedCard;

  private VsdmDataFromCardStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new VsdmDataFromCardStrategy(cardTerminalService, fhirService);
  }

  @Test
  void shouldReturnTruncatedVsdmDataFromValidCard() throws CardTerminalException {
    final EgkInfo egkInfo = new EgkInfo("X123456789", "123456789", "Erika", "Mustermann", "true");
    final String encodedBundle = "{\"resourceType\":\"Bundle\"}";
    when(cardTerminalService.getEgkInfo(attachedCard)).thenReturn(egkInfo);
    when(fhirService.encodeResponse(any(VsdmBundle.class), eq(EncodingType.JSON)))
        .thenReturn(encodedBundle);

    final Optional<VsdmReadResult> result = strategy.get(attachedCard);

    assertThat(result).contains(new VsdmReadResult(HttpStatus.OK, null, null, encodedBundle, null));
    verify(cardTerminalService).getEgkInfo(attachedCard);
    verify(fhirService).encodeResponse(any(VsdmBundle.class), eq(EncodingType.JSON));
  }

  @Test
  void shouldThrowUnauthorizedWhenCardDataIsInvalid() throws CardTerminalException {
    final EgkInfo egkInfo = new EgkInfo("X123456789", "123456789", "Erika", "Mustermann", "false");
    when(cardTerminalService.getEgkInfo(attachedCard)).thenReturn(egkInfo);

    final ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> strategy.get(attachedCard));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(exception.getReason()).isEqualTo("eGK data is invalid");
    verifyNoInteractions(fhirService);
  }

  @Test
  void shouldThrowInternalServerErrorWhenNoCardIsAttached() {
    final ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> strategy.get(null));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    verifyNoInteractions(cardTerminalService, fhirService);
  }

  @Test
  void shouldThrowInternalServerErrorWhenCardDataCannotBeLoaded() throws CardTerminalException {
    when(cardTerminalService.getEgkInfo(attachedCard))
        .thenThrow(new CardTerminalException("card terminal unavailable"));

    final ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> strategy.get(attachedCard));

    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    verifyNoInteractions(fhirService);
  }
}
