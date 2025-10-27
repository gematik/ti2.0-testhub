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
package de.gematik.ti20.simsvc.client.service.protocol;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CardProtocolManagerTest {

  private CardProtocol protocol1;
  private CardProtocol protocol2;
  private CardProtocolManager manager;

  @BeforeEach
  void setUp() {
    protocol1 = mock(CardProtocol.class);
    when(protocol1.getProtocolName()).thenReturn("PACE");
    protocol2 = mock(CardProtocol.class);
    when(protocol2.getProtocolName()).thenReturn("SIGNATURE");
    manager = new CardProtocolManager(Arrays.asList(protocol1, protocol2));
  }

  @Test
  void testProcessCommand_delegatesToCorrectProtocol() {
    ApduCommand command = mock(ApduCommand.class);
    CardImage card = mock(CardImage.class);
    ApduResponse response = mock(ApduResponse.class);

    when(protocol1.canHandle(command)).thenReturn(false);
    when(protocol2.canHandle(command)).thenReturn(true);
    when(protocol2.processCommand(card, command)).thenReturn(response);

    ApduResponse result = manager.processCommand(card, command);

    assertEquals(response, result);
    verify(protocol2).processCommand(card, command);
    verify(protocol1, never()).processCommand(any(), any());
  }

  @Test
  void testProcessCommand_noProtocolHandlesCommand_returnsNull() {
    ApduCommand command = mock(ApduCommand.class);
    CardImage card = mock(CardImage.class);

    when(protocol1.canHandle(command)).thenReturn(false);
    when(protocol2.canHandle(command)).thenReturn(false);

    assertNull(manager.processCommand(card, command));
  }

  @Test
  void testResetAllProtocols_callsResetOnAll() {
    manager.resetAllProtocols();
    verify(protocol1).reset();
    verify(protocol2).reset();
  }

  @Test
  void testGetProtocolByName_caseInsensitive() {
    assertEquals(protocol1, manager.getProtocolByName("pace"));
    assertEquals(protocol2, manager.getProtocolByName("SIGNATURE"));
    assertNull(manager.getProtocolByName("UNKNOWN"));
  }

  @Test
  void testEmptyProtocolList() {
    CardProtocolManager emptyManager = new CardProtocolManager(Collections.emptyList());
    assertNull(emptyManager.getProtocolByName("PACE"));
    assertNull(emptyManager.getPaceProtocol());
  }
}
