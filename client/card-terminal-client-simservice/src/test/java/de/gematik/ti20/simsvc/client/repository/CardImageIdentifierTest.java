/*-
 * #%L
 * Card Terminal Simulator
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
package de.gematik.ti20.simsvc.client.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.ti20.simsvc.client.model.EgkIdentifier;
import de.gematik.ti20.simsvc.client.model.SmcbIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CardImageIdentifierTest {

  @ParameterizedTest
  @CsvSource({
    "109500969, X110639491, true",
    "109500968, X110639491, false",
    "109500969, X110639492, false",
    "109500968, X110639492, false"
  })
  void matches_requiresBothIknrAndKvnr(
      final String iknr, final String kvnr, final boolean expected) {
    final EgkCardImageIdentifier identifier =
        new EgkCardImageIdentifier("109500969", "X110639491", "cards/egk.xml");

    assertEquals(expected, identifier.matches(new EgkIdentifier(iknr, kvnr)));
  }

  @ParameterizedTest
  @CsvSource({"actor-1, true", "actor-2, false"})
  void smcbIdentifier_matchesActorId(final String actorId, final boolean expected) {
    final SmcbCardImageIdentifier identifier =
        new SmcbCardImageIdentifier("actor-1", "cards/smcb.xml");

    assertEquals(expected, identifier.matches(new SmcbIdentifier(actorId)));
  }

  @Test
  void bothIdentifiersExposeTheirPathThroughTheInterface() {
    final CardImageIdentifier egk =
        new EgkCardImageIdentifier("109500969", "X110639491", "cards/egk.xml");
    final CardImageIdentifier smcb = new SmcbCardImageIdentifier("actor-1", "cards/smcb.xml");

    assertTrue(egk.path().endsWith("egk.xml"));
    assertTrue(smcb.path().endsWith("smcb.xml"));
  }
}
