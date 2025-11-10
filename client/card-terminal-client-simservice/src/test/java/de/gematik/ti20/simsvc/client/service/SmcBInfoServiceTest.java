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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.ti20.simsvc.client.exception.CardNotFoundException;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.card.FileData;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmcBInfoServiceTest {

  @Mock private SlotManager slotManager;

  private SmcBInfoService smcBInfoService;

  @BeforeEach
  void setUp() {
    smcBInfoService = new SmcBInfoService(slotManager);
  }

  @Test
  void extractSmcBInfo_whenCardNotFound_throwsCardNotFoundException() {
    // Given
    String cardHandle = "unknown-card";
    when(slotManager.getSlotCount()).thenReturn(2);
    when(slotManager.isCardPresent(anyInt())).thenReturn(false);

    // When/Then
    assertThatThrownBy(() -> smcBInfoService.extractSmcBInfo(cardHandle))
        .isInstanceOf(CardNotFoundException.class)
        .hasMessageContaining(cardHandle);
  }

  @Test
  void extractSmcBInfo_whenCardIsNotSmcB_throwsIllegalArgumentException() {
    // Given
    String cardHandle = "egk-card";
    CardImage card = mock(CardImage.class);
    when(card.getId()).thenReturn(cardHandle);
    when(card.getCardType()).thenReturn(CardType.EGK);

    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);

    // When/Then
    assertThatThrownBy(() -> smcBInfoService.extractSmcBInfo(cardHandle))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not an SMC-B card");
  }

  @Test
  void extractSmcBInfo_whenNoAuthenticDataFound_returnsExtractionError() {
    // Given
    String cardHandle = "smcb-no-data";
    CardImage smcbCard = createMockCard(cardHandle, CardType.HPIC, new ArrayList<>());

    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(smcbCard);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(cardHandle);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTelematikId()).isEqualTo("DATA_EXTRACTION_FAILED");
    assertThat(result.getProfessionOid()).isEqualTo("DATA_EXTRACTION_FAILED");
    assertThat(result.getHolderName()).isEqualTo("EXTRACTION_ERROR");
    assertThat(result.getOrganizationName()).contains("DATA EXTRACTION FAILED");
  }

  @Test
  void extractSmcBInfo_whenIncompleteCertificateData_returnsExtractionError() {
    // Given
    String cardHandle = "smcb-incomplete";
    List<FileData> files = new ArrayList<>();
    FileData fileData = mock(FileData.class);
    when(fileData.getName()).thenReturn("invalid-cert");
    when(fileData.getData()).thenReturn("not-a-certificate");
    files.add(fileData);
    CardImage smcbCard = createMockCard(cardHandle, CardType.HPIC, files);

    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(smcbCard);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(cardHandle);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getTelematikId()).isEqualTo("DATA_EXTRACTION_FAILED");
  }

  @Test
  void extractSmcBInfo_withHPCCardType_isRecognizedAsSmcB() {
    // Given
    String cardHandle = "hpc-card";
    CardImage hpcCard = createMockCard(cardHandle, CardType.HPC, new ArrayList<>());

    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(hpcCard);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(cardHandle);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getCardType()).isEqualTo(CardType.HPC.toString());
  }

  @Test
  void extractSmcBInfo_withSmcBLabel_isRecognizedAsSmcB() {
    // Given
    String cardHandle = "smcb-labeled";
    CardImage card = mock(CardImage.class);
    when(card.getId()).thenReturn(cardHandle);
    when(card.getCardType()).thenReturn(CardType.EGK);
    when(card.getLabel()).thenReturn("SMC-B Test Card");
    when(card.getAllFiles()).thenReturn(new ArrayList<FileData>());

    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(card);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(cardHandle);

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  void extractSmcBInfo_whenCardInSecondSlot_findsCard() {
    // Given
    String cardHandle = "smcb-slot2";
    CardImage smcbCard = createMockCard(cardHandle, CardType.HPIC, new ArrayList<>());

    when(slotManager.getSlotCount()).thenReturn(3);
    when(slotManager.isCardPresent(0)).thenReturn(false);
    when(slotManager.isCardPresent(1)).thenReturn(true);
    when(slotManager.getCardInSlot(1)).thenReturn(smcbCard);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(cardHandle);

    // Then
    assertThat(result).isNotNull();
  }

  @Test
  void extractSmcBInfo_withNullFileData_handlesGracefully() {
    // Given
    String cardHandle = "smcb-null-files";
    List<FileData> files = new ArrayList<>();
    files.add(mock(FileData.class));
    files.add(mock(FileData.class));
    CardImage smcbCard = createMockCard(cardHandle, CardType.HPIC, files);

    when(slotManager.getSlotCount()).thenReturn(1);
    when(slotManager.isCardPresent(0)).thenReturn(true);
    when(slotManager.getCardInSlot(0)).thenReturn(smcbCard);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(cardHandle);

    // Then
    assertThat(result).isNotNull();
  }

  private CardImage createMockCard(String id, CardType type, List<FileData> files) {
    CardImage card = mock(CardImage.class);
    when(card.getId()).thenReturn(id);
    when(card.getCardType()).thenReturn(type);
    when(card.getAllFiles()).thenReturn(files);
    return card;
  }
}
