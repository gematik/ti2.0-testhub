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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaceProtocolServiceTest {

  private PaceProtocolService paceService;
  private CardImage card;

  @BeforeEach
  void setUp() {
    paceService = new PaceProtocolService();
    card = mock(CardImage.class);
  }

  @Test
  void testCanHandle_MseSetAtAndGeneralAuthenticate() {
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getCla()).thenReturn((byte) 0x00);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getP1()).thenReturn((byte) 0xC1);
    when(mse.getP2()).thenReturn((byte) 0xA4);

    ApduCommand ga = mock(ApduCommand.class);
    when(ga.getCla()).thenReturn((byte) 0x00);
    when(ga.getIns()).thenReturn((byte) 0x86);
    when(ga.getP1()).thenReturn((byte) 0x00);
    when(ga.getP2()).thenReturn((byte) 0x00);

    assertTrue(paceService.canHandle(mse));
    assertTrue(paceService.canHandle(ga));
  }

  @Test
  void testCanHandle_Unsupported() {
    ApduCommand cmd = mock(ApduCommand.class);
    when(cmd.getCla()).thenReturn((byte) 0x00);
    when(cmd.getIns()).thenReturn((byte) 0xA4);
    when(cmd.getP1()).thenReturn((byte) 0x00);
    when(cmd.getP2()).thenReturn((byte) 0x00);

    assertFalse(paceService.canHandle(cmd));
  }

  @Test
  void testProcessCommand_MseSetAt_Success() {
    // Tag 0x80, Länge 1, Wert 0x01; Tag 0x83, Länge 1, Wert 0x03
    byte[] data = new byte[] {(byte) 0x80, 0x01, 0x01, (byte) 0x83, 0x01, 0x03};
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(data);

    ApduResponse resp = paceService.processCommand(card, mse);
    assertEquals(0x9000, resp.getStatusWord());
    assertFalse(paceService.isPaceAuthenticated());
    assertFalse(paceService.isTrustedChannelEstablished());
  }

  @Test
  void testProcessCommand_MseSetAt_Invalid() {
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(new byte[] {0x01, 0x02}); // zu kurz

    ApduResponse resp = paceService.processCommand(card, mse);
    assertEquals(0x6A80, resp.getStatusWord());
  }

  @Test
  void testProcessCommand_GeneralAuthenticate_Initial() {
    ApduCommand ga = mock(ApduCommand.class);
    when(ga.getIns()).thenReturn((byte) 0x86);
    when(ga.getP1()).thenReturn((byte) 0x00);
    when(ga.getP2()).thenReturn((byte) 0x00);
    when(ga.getData()).thenReturn(null);

    ApduResponse resp = paceService.processCommand(card, ga);
    assertEquals(0x9000, resp.getStatusWord());
  }

  @Test
  void testProcessCommand_GeneralAuthenticate_TlvSteps() {
    // 7C 05 85 03 01 02 03 (Mapping Step)
    byte[] data = new byte[] {0x7C, 0x05, (byte) 0x85, 0x03, 0x01, 0x02, 0x03};
    ApduCommand ga = mock(ApduCommand.class);
    when(ga.getIns()).thenReturn((byte) 0x86);
    when(ga.getData()).thenReturn(data);

    ApduResponse resp = paceService.processCommand(card, ga);
    assertEquals(0x9000, resp.getStatusWord());
    assertArrayEquals(data, resp.getData());
  }

  @Test
  void testProcessCommand_GeneralAuthenticate_MutualAuth() {
    // 7C 04 8E 02 01 02 (Mutual Auth)
    byte[] data = new byte[] {0x7C, 0x04, (byte) 0x8E, 0x02, 0x01, 0x02};
    ApduCommand ga = mock(ApduCommand.class);
    when(ga.getIns()).thenReturn((byte) 0x86);
    when(ga.getData()).thenReturn(data);

    assertFalse(paceService.isPaceAuthenticated());
    assertFalse(paceService.isTrustedChannelEstablished());

    ApduResponse resp = paceService.processCommand(card, ga);
    assertEquals(0x9000, resp.getStatusWord());
    assertTrue(paceService.isPaceAuthenticated());
    assertTrue(paceService.isTrustedChannelEstablished());
  }

  @Test
  void testReset() {
    // Setze Zustand
    paceService.reset();
    assertFalse(paceService.isPaceAuthenticated());
    assertFalse(paceService.isTrustedChannelEstablished());
  }

  @Test
  void testGetProtocolName() {
    assertEquals("PACE", paceService.getProtocolName());
  }
}
