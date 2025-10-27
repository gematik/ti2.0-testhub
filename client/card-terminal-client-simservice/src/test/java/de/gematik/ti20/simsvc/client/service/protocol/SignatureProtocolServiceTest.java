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
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.card.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SignatureProtocolServiceTest {

  private SignatureProtocolService service;
  private CardImage card;

  @BeforeEach
  void setUp() {
    service = new SignatureProtocolService();
    card = mock(CardImage.class);
  }

  @Test
  void testCanHandle_MseSetAndPsoCds() {
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getCla()).thenReturn((byte) 0x00);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getP1()).thenReturn((byte) 0x41);
    when(mse.getP2()).thenReturn((byte) 0xA6);

    ApduCommand pso = mock(ApduCommand.class);
    when(pso.getCla()).thenReturn((byte) 0x00);
    when(pso.getIns()).thenReturn((byte) 0x2A);
    when(pso.getP1()).thenReturn((byte) 0x9E);
    when(pso.getP2()).thenReturn((byte) 0x9A);

    assertTrue(service.canHandle(mse));
    assertTrue(service.canHandle(pso));
  }

  @Test
  void testCanHandle_Unsupported() {
    ApduCommand cmd = mock(ApduCommand.class);
    when(cmd.getCla()).thenReturn((byte) 0x00);
    when(cmd.getIns()).thenReturn((byte) 0xA4);
    when(cmd.getP1()).thenReturn((byte) 0x00);
    when(cmd.getP2()).thenReturn((byte) 0x00);

    assertFalse(service.canHandle(cmd));
  }

  @Test
  void testProcessCommand_MseSetSignature_Success() {
    // Tag 0x84, Länge 1, Wert 0x01
    byte[] data = new byte[] {(byte) 0x84, 0x01, 0x01};
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(data);

    ApduResponse resp = service.processCommand(card, mse);
    assertEquals(0x9000, resp.getStatusWord());
  }

  @Test
  void testProcessCommand_MseSetSignature_Invalid() {
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(new byte[] {0x01, 0x02}); // zu kurz

    ApduResponse resp = service.processCommand(card, mse);
    assertEquals(0x6A80, resp.getStatusWord());
  }

  @Test
  void testProcessCommand_PsoCds_Success_RSA() throws Exception {
    // Vorbereiten: MSE:SET ausführen
    byte[] mseData = new byte[] {(byte) 0x84, 0x01, 0x01};
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(mseData);
    service.processCommand(card, mse);

    // RSA Key generieren
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair kp = kpg.generateKeyPair();
    String privKeyB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

    Key key = mock(Key.class);
    when(key.getPrivateKey()).thenReturn(privKeyB64);
    when(key.getName()).thenReturn("SIG_RSA");

    when(card.getCardType()).thenReturn(CardType.SMCB);
    when(card.getAllKeys()).thenReturn(Collections.singletonList(key));

    byte[] data = "test".getBytes();
    ApduCommand pso = mock(ApduCommand.class);
    when(pso.getIns()).thenReturn((byte) 0x2A);
    when(pso.getP1()).thenReturn((byte) 0x9E);
    when(pso.getP2()).thenReturn((byte) 0x9A);
    when(pso.getData()).thenReturn(data);

    ApduResponse resp = service.processCommand(card, pso);
    assertEquals(0x9000, resp.getStatusWord());
    assertNotNull(resp.getData());
    assertTrue(resp.getData().length > 0);
  }

  @Test
  void testProcessCommand_PsoCds_Success_EC() throws Exception {
    // Vorbereiten: MSE:SET ausführen
    byte[] mseData = new byte[] {(byte) 0x84, 0x01, 0x01};
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(mseData);
    service.processCommand(card, mse);

    // EC Key generieren
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
    kpg.initialize(256);
    KeyPair kp = kpg.generateKeyPair();
    String privKeyB64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

    Key key = mock(Key.class);
    when(key.getPrivateKey()).thenReturn(privKeyB64);
    when(key.getName()).thenReturn("SIG_E256");

    when(card.getCardType()).thenReturn(CardType.HBA);
    when(card.getAllKeys()).thenReturn(Collections.singletonList(key));

    byte[] data = "test".getBytes();
    ApduCommand pso = mock(ApduCommand.class);
    when(pso.getIns()).thenReturn((byte) 0x2A);
    when(pso.getP1()).thenReturn((byte) 0x9E);
    when(pso.getP2()).thenReturn((byte) 0x9A);
    when(pso.getData()).thenReturn(data);

    ApduResponse resp = service.processCommand(card, pso);
    assertEquals(0x9000, resp.getStatusWord());
    assertNotNull(resp.getData());
    assertTrue(resp.getData().length > 0);
  }

  @Test
  void testProcessCommand_PsoCds_NotPrepared() {
    when(card.getCardType()).thenReturn(CardType.SMCB);
    when(card.getAllKeys()).thenReturn(Collections.emptyList());

    ApduCommand pso = mock(ApduCommand.class);
    when(pso.getIns()).thenReturn((byte) 0x2A);
    when(pso.getP1()).thenReturn((byte) 0x9E);
    when(pso.getP2()).thenReturn((byte) 0x9A);
    when(pso.getData()).thenReturn("test".getBytes());

    ApduResponse resp = service.processCommand(card, pso);
    assertEquals(0x6985, resp.getStatusWord());
  }

  @Test
  void testProcessCommand_PsoCds_UnsupportedCardType() {
    when(card.getCardType()).thenReturn(CardType.HPC); // nicht SMCB/HBA
    ApduCommand pso = mock(ApduCommand.class);
    when(pso.getIns()).thenReturn((byte) 0x2A);
    when(pso.getP1()).thenReturn((byte) 0x9E);
    when(pso.getP2()).thenReturn((byte) 0x9A);
    when(pso.getData()).thenReturn("test".getBytes());

    ApduResponse resp = service.processCommand(card, pso);
    assertEquals(0x6A81, resp.getStatusWord());
  }

  @Test
  void testProcessCommand_PsoCds_NoKeyFound() {
    // Vorbereiten: MSE:SET ausführen
    byte[] mseData = new byte[] {(byte) 0x84, 0x01, 0x01};
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(mseData);
    service.processCommand(card, mse);

    when(card.getCardType()).thenReturn(CardType.SMCB);
    when(card.getAllKeys()).thenReturn(Collections.emptyList());

    ApduCommand pso = mock(ApduCommand.class);
    when(pso.getIns()).thenReturn((byte) 0x2A);
    when(pso.getP1()).thenReturn((byte) 0x9E);
    when(pso.getP2()).thenReturn((byte) 0x9A);
    when(pso.getData()).thenReturn("test".getBytes());

    ApduResponse resp = service.processCommand(card, pso);
    assertEquals(0x6A88, resp.getStatusWord());
  }

  @Test
  void testReset() {
    // Vorbereiten: MSE:SET ausführen
    byte[] mseData = new byte[] {(byte) 0x84, 0x01, 0x01};
    ApduCommand mse = mock(ApduCommand.class);
    when(mse.getIns()).thenReturn((byte) 0x22);
    when(mse.getData()).thenReturn(mseData);
    service.processCommand(card, mse);

    service.reset();
    // Nach Reset sollte ein PSO:CDS fehlschlagen (nicht vorbereitet)
    when(card.getCardType()).thenReturn(CardType.SMCB);
    ApduCommand pso = mock(ApduCommand.class);
    when(pso.getIns()).thenReturn((byte) 0x2A);
    when(pso.getP1()).thenReturn((byte) 0x9E);
    when(pso.getP2()).thenReturn((byte) 0x9A);
    when(pso.getData()).thenReturn("test".getBytes());

    ApduResponse resp = service.processCommand(card, pso);
    assertEquals(0x6985, resp.getStatusWord());
  }

  @Test
  void testGetProtocolName() {
    assertEquals("SIGNATURE", service.getProtocolName());
  }
}
