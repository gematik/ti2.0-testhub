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
package de.gematik.ti20.simsvc.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.client.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.FileData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApduProcessorTest {

  @Mock private EgkInfoService egkInfoService;

  private ApduProcessor apduProcessor;
  private CardImage testCard;

  @BeforeEach
  void setUp() {
    apduProcessor = new ApduProcessor(Map.of(), egkInfoService);
    testCard = createTestCard();
  }

  private CardImage createTestCard() {
    CardImage card = new CardImage();
    card.setId("TEST-CARD-001");
    card.setCardTypeString("EGK");

    List<FileData> files = new ArrayList<>();

    // Add GDO file
    FileData gdoFile = new FileData();
    gdoFile.setFileId("2F02");
    gdoFile.setData("47656D617469632047323120412E312E3020362E31352E30");
    files.add(gdoFile);

    // Add version file
    FileData versionFile = new FileData();
    versionFile.setFileId("2F11");
    versionFile.setData("47656D617469632047323120412E312E3020362E31352E30");
    files.add(versionFile);

    //    card.setFiles(files);
    return card;
  }

  @Test
  void testSelectMasterFile() throws Exception {
    // SELECT MF command: 00 A4 04 0C 07 D2760001448000
    byte[] aid = Hex.decodeHex("D2760001448000");
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x0C, aid, aid.length);
    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);
  }

  @Test
  void testSelectDfEsign() throws Exception {
    // SELECT DF.ESIGN: 00 A4 04 0C 0A A000000167455349474E
    byte[] aid = Hex.decodeHex("A000000167455349474E");
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x0C, aid, aid.length);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);
  }

  @Test
  void testSelectApplicationNotFound() throws Exception {
    // SELECT with unknown AID
    byte[] aid = Hex.decodeHex("AABBCCDD");
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x0C, aid, aid.length);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getStatusWord()).isEqualTo(0x6A82); // File not found
  }

  @Test
  void testReadBinaryFileNotFound() throws Exception {
    // READ BINARY with unknown file ID
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0xB0, (byte) 0xFF, (byte) 0xFF, new byte[0], 0);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getStatusWord()).isEqualTo(0x6A82); // File not found
  }

  @Test
  void testManageSecurityEnvironmentForPace() throws Exception {
    // MSE:SET for PACE (P1=C1, P2=A4)
    byte[] data = Hex.decodeHex("8001038301");
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0x22, (byte) 0xC1, (byte) 0xA4, data, data.length);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);
  }

  @Test
  void testManageSecurityEnvironmentForSignature() throws Exception {
    // MSE:SET for digital signature (P1=41, P2=A6)
    byte[] data = Hex.decodeHex("8401098301");
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0x22, (byte) 0x41, (byte) 0xA6, data, data.length);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);
  }

  @Test
  void testPerformSecurityOperationComputeSignature() throws Exception {
    // First set MSE for signature
    byte[] mseData = Hex.decodeHex("8401098301");
    ApduCommand mseCommand =
        new ApduCommand(
            (byte) 0x00, (byte) 0x22, (byte) 0x41, (byte) 0xA6, mseData, mseData.length);
    apduProcessor.processCommand(testCard, mseCommand);

    // PSO:CDS (P1=9E, P2=9A) with hash
    byte[] hash = new byte[32]; // SHA-256 hash
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0x2A, (byte) 0x9E, (byte) 0x9A, hash, hash.length);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);
    assertThat(response.getData()).isNotEmpty(); // Should contain signature
  }

  @Test
  void testPerformSecurityOperationWithoutMse() throws Exception {
    // PSO:CDS without prior MSE should fail
    byte[] hash = new byte[32];
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0x2A, (byte) 0x9E, (byte) 0x9A, hash, hash.length);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getStatusWord()).isEqualTo(0x6985); // Conditions of use not satisfied
  }

  @Test
  void testGetData() throws Exception {
    // GET DATA for public key identifiers (P1P2=0100)
    ApduCommand command =
        new ApduCommand((byte) 0x80, (byte) 0xCA, (byte) 0x01, (byte) 0x00, new byte[0], 0);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);
    assertThat(response.getData()).isNotEmpty();
  }

  @Test
  void testGetDataNotFound() throws Exception {
    // GET DATA with unsupported P1P2
    ApduCommand command =
        new ApduCommand((byte) 0x80, (byte) 0xCA, (byte) 0xFF, (byte) 0xFF, new byte[0], 0);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getStatusWord()).isEqualTo(0x6A88); // Referenced data not found
  }

  @Test
  void testGetEgkInfo() throws Exception {
    // Mock EGK info service
    EgkInfoDto expectedInfo = new EgkInfoDto();
    expectedInfo.setKvnr("X123456789");
    expectedInfo.setIknr("987654321");
    expectedInfo.setPatientName("Mustermann, Max");

    when(egkInfoService.extractEgkInfo(any(CardImage.class))).thenReturn(expectedInfo);

    // GET EGK INFO command (0x80EE)
    ApduCommand command =
        new ApduCommand((byte) 0x80, (byte) 0xEE, (byte) 0x00, (byte) 0x00, new byte[0], 0);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);

    String responseStr = new String(response.getData(), "UTF-8");
    assertThat(responseStr).contains("KVNR:X123456789");
    assertThat(responseStr).contains("IKNR:987654321");
    assertThat(responseStr).contains("NAME:Mustermann, Max");
  }

  @Test
  void testGeneralAuthenticatePaceStep1() throws Exception {
    // First set MSE for PACE
    byte[] mseData = Hex.decodeHex("8001038301");
    ApduCommand mseCommand =
        new ApduCommand(
            (byte) 0x00, (byte) 0x22, (byte) 0xC1, (byte) 0xA4, mseData, mseData.length);
    apduProcessor.processCommand(testCard, mseCommand);

    // PACE Step 1: ECDH Key Exchange (Tag 0x81)
    byte[] authData =
        Hex.decodeHex("7C228120041E5AE49B8D5BD8D62A0F349B5FD1D56F6F8FD10DD69F5BD4DD6DC69C8C9FCE3B");
    ApduCommand command =
        new ApduCommand(
            (byte) 0x00, (byte) 0x86, (byte) 0x00, (byte) 0x00, authData, authData.length);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getSw1()).isEqualTo((byte) 0x90);
    assertThat(response.getSw2()).isEqualTo((byte) 0x00);
    assertThat(response.getData()).isNotEmpty();
  }

  @Test
  void testUnsupportedInstruction() throws Exception {
    // Command with unsupported instruction
    ApduCommand command =
        new ApduCommand((byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0x00, new byte[0], 0);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getStatusWord()).isEqualTo(0x6D00); // Instruction code not supported
  }

  @Test
  void testUnsupportedClass() throws Exception {
    // Command with unsupported class
    ApduCommand command =
        new ApduCommand((byte) 0xFF, (byte) 0xA4, (byte) 0x00, (byte) 0x00, new byte[0], 0);

    ApduResponse response = apduProcessor.processCommand(testCard, command);

    assertThat(response).isNotNull();
    assertThat(response.getStatusWord()).isEqualTo(0x6E00); // Class not supported
  }
}
