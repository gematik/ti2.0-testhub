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
package de.gematik.ti20.simsvc.client.card;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class AttachedCardTest {

  @Test
  void shouldCreateAttachedCardFromMap() {
    final Map<String, Object> values =
        Map.of("cardHandle", "card-123", "cardType", "egk", "slotId", 2);

    final AttachedCard attachedCard = AttachedCard.from("http://terminal-a/", values);

    assertEquals("http://terminal-a/", attachedCard.getTerminalUrl());
    assertEquals("card-123", attachedCard.getId());
    assertEquals("egk", attachedCard.getCardType());
    assertEquals(2, attachedCard.getSlotId());
  }
}
