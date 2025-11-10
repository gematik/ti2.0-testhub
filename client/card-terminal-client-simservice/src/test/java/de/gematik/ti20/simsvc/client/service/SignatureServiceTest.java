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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.card.FileData;
import de.gematik.ti20.simsvc.client.model.card.Key;
import de.gematik.ti20.simsvc.client.model.dto.SignRequestDto;
import de.gematik.ti20.simsvc.client.model.dto.SignResponseDto;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignatureServiceTest {

  private SignatureService signatureService;

  @Mock private SlotManager slotManager;

  @Mock private CardManager cardManager;

  @Mock private CardImage cardImage;

  @Mock private Key key;

  @Mock private FileData certificateFile;

  private static final String CARD_HANDLE = "test-card-handle";
  private static final String TEST_DATA = "test data to sign";

  @BeforeEach
  void setUp() {
    signatureService = new SignatureService(slotManager, cardManager);
  }

  @Test
  void testSignData_WithSpecificKeyReference() throws Exception {
    when(cardImage.getId()).thenReturn(CARD_HANDLE);
    // Arrange
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048);
    KeyPair keyPair = kpg.generateKeyPair();
    String privateKeyBase64 = Base64.encodeBase64String(keyPair.getPrivate().getEncoded());

    when(key.getKeyIdentifier()).thenReturn("KEY_01");
    setupCardForSigning(CardType.SMCB, "AUT", "SHA256withRSA", privateKeyBase64);

    SignRequestDto request = new SignRequestDto();
    request.setData(Base64.encodeBase64String(TEST_DATA.getBytes()));
    request.setOptions(Map.of("keyReference", "KEY_01", "algorithm", "SHA256withRSA"));

    // Act
    SignResponseDto response = signatureService.signData(CARD_HANDLE, request);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.getSignature()).isNotEmpty();
  }

  @Test
  void testSignData_CardNotFound() {
    // Arrange
    when(slotManager.getSlotCount()).thenReturn(0);
    when(cardManager.findCardByHandle(CARD_HANDLE))
        .thenThrow(new IllegalArgumentException("Card not found"));

    SignRequestDto request = new SignRequestDto();
    request.setData(Base64.encodeBase64String(TEST_DATA.getBytes()));

    // Act & Assert
    assertThatThrownBy(() -> signatureService.signData(CARD_HANDLE, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Card not found");
  }

  @Test
  void testSignData_InvalidData() {
    when(cardImage.getId()).thenReturn(CARD_HANDLE);
    // Arrange
    setupBasicCard();

    SignRequestDto request = new SignRequestDto();
    request.setData("");

    // Act & Assert
    assertThatThrownBy(() -> signatureService.signData(CARD_HANDLE, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid data to sign");
  }

  @Test
  void testSignData_SHA1AlgorithmRejected() {
    when(cardImage.getId()).thenReturn(CARD_HANDLE);
    // Arrange
    setupBasicCard();

    SignRequestDto request = new SignRequestDto();
    request.setData(Base64.encodeBase64String(TEST_DATA.getBytes()));
    request.setOptions(Map.of("algorithm", "SHA1withRSA"));

    // Act & Assert
    assertThatThrownBy(() -> signatureService.signData(CARD_HANDLE, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SHA1 algorithms are deprecated and insecure");
  }

  @Test
  void testSignData_UnsupportedAlgorithm() {
    when(cardImage.getId()).thenReturn(CARD_HANDLE);
    // Arrange
    setupBasicCard();

    SignRequestDto request = new SignRequestDto();
    request.setData(Base64.encodeBase64String(TEST_DATA.getBytes()));
    request.setOptions(Map.of("algorithm", "MD5withRSA"));

    // Act & Assert
    assertThatThrownBy(() -> signatureService.signData(CARD_HANDLE, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported algorithm");
  }

  @Test
  void testSignData_KeyNotFound() {
    when(cardImage.getId()).thenReturn(CARD_HANDLE);
    // Arrange
    setupBasicCard();
    when(cardImage.getAllKeys()).thenReturn(Collections.emptyList());

    SignRequestDto request = new SignRequestDto();
    request.setData(Base64.encodeBase64String(TEST_DATA.getBytes()));
    request.setOptions(Map.of("keyReference", "NONEXISTENT"));

    // Act & Assert
    assertThatThrownBy(() -> signatureService.signData(CARD_HANDLE, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void testGetCertificate_Success() throws Exception {
    when(cardImage.getId()).thenReturn(CARD_HANDLE);
    // Arrange
    setupCertificateData("AUT", CardType.SMCB);

    // Act
    Map<String, String> result = signatureService.getCertificate(CARD_HANDLE, "AUT");

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.get("certificate")).isNotEmpty();
    assertThat(result.get("keyType")).isEqualTo("AUT");
    assertThat(result.get("cardType")).isEqualTo("SMCB");
  }

  @Test
  void testGetCertificate_CardNotFound() {
    // Arrange
    when(slotManager.getSlotCount()).thenReturn(0);
    when(cardManager.findCardByHandle(CARD_HANDLE))
        .thenThrow(new IllegalArgumentException("Card not found"));

    // Act & Assert
    assertThatThrownBy(() -> signatureService.getCertificate(CARD_HANDLE, "AUT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Card not found");
  }

  @Test
  void testGetCertificate_NoCertificateFound() {
    when(cardImage.getId()).thenReturn(CARD_HANDLE);
    // Arrange
    setupBasicCard();
    when(cardImage.getAllFiles()).thenReturn(Collections.emptyList());

    // Act & Assert
    assertThatThrownBy(() -> signatureService.getCertificate(CARD_HANDLE, "AUT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("No certificate found");
  }

  private void setupBasicCard() {
    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(cardImage);
    when(cardImage.getCardType()).thenReturn(CardType.SMCB);
  }

  private void setupCardForSigning(
      CardType cardType, String keyType, String algorithm, String privateKeyBase64) {
    setupBasicCard();
    when(cardImage.getCardType()).thenReturn(cardType);

    // Setup key
    String keyName = "PRK_" + keyType + (algorithm.contains("ECDSA") ? "_E256" : "_R2048");
    when(key.getName()).thenReturn(keyName);
    when(key.getPrivateKey()).thenReturn(privateKeyBase64);
    when(key.getKeyIdentifier()).thenReturn("KEY_01");
    when(cardImage.getAllKeys()).thenReturn(List.of(key));

    // Setup certificate
    when(certificateFile.getData()).thenReturn("test-certificate-data");
    when(certificateFile.getName())
        .thenReturn("C." + keyType + (algorithm.contains("ECDSA") ? ".E256" : ".R2048"));
    when(certificateFile.getFileId()).thenReturn("C500");
    when(cardImage.getAllFiles()).thenReturn(List.of(certificateFile));
  }

  private void setupCertificateData(String keyType, CardType cardType) {
    setupBasicCard();
    when(cardImage.getCardType()).thenReturn(cardType);

    when(certificateFile.getData()).thenReturn("test-certificate-data");
    when(certificateFile.getName()).thenReturn("C." + keyType + ".R2048");
    when(certificateFile.getFileId()).thenReturn("C500");
    when(cardImage.getAllFiles()).thenReturn(List.of(certificateFile));
  }
}
