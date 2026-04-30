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
package de.gematik.ti20.simsvc.client.service;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.simsvc.client.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CardImageService}.
 *
 * <p>The central test is a roundtrip:
 *
 * <ol>
 *   <li>Read a real {@link CardImage} from XML
 *   <li>Extract {@link EgkInfoDto} via {@link EgkInfoService}
 *   <li>Re-create a {@link CardImage} via {@link CardImageService}
 *   <li>Extract {@link EgkInfoDto} again from the re-created card
 *   <li>Assert that all relevant patient fields are identical
 * </ol>
 */
class CardImageServiceTest {

  private CardImageParser cardImageParser;
  private EgkInfoService egkInfoService;
  private CardImageService cardImageService;

  @BeforeEach
  void setUp() {
    cardImageParser = new CardImageParser();
    egkInfoService = new EgkInfoService();
    cardImageService = new CardImageService();
  }

  // -------------------------------------------------------------------------
  // Roundtrip test
  // -------------------------------------------------------------------------

  /**
   * Full roundtrip: real XML → EgkInfoDto → synthetic CardImage → EgkInfoDto. The two EgkInfoDto
   * instances must contain identical patient data.
   */
  @Test
  void roundtrip_extractedEgkInfoSurvivesCardImageConversion() throws Exception {
    // 1. Load original CardImage from test resource
    CardImage originalCard = loadCardImageFromResource("egkCardImage.xml");

    // 2. Extract EgkInfoDto from the original card
    EgkInfoDto originalDto = egkInfoService.extractEgkInfo(originalCard);
    assertNotNull(originalDto.getKvnr(), "Original EgkInfoDto must contain a KVNR");
    assertNotEquals(
        "EXTRACTION_ERROR",
        originalDto.getKvnr(),
        "EgkInfoService must successfully parse the test XML");

    // 3. Create a synthetic CardImage from the extracted DTO
    CardImage syntheticCard = cardImageService.createCardImage(originalDto);
    assertNotNull(syntheticCard);
    assertNotNull(syntheticCard.getEgk(), "Synthetic card must be an EGK card");

    // 4. Extract EgkInfoDto from the synthetic card
    EgkInfoDto roundtrippedDto = egkInfoService.extractEgkInfo(syntheticCard);

    // 5. All relevant patient fields must be preserved
    assertEquals(originalDto.getKvnr(), roundtrippedDto.getKvnr(), "KVNR must be preserved");
    assertEquals(originalDto.getIknr(), roundtrippedDto.getIknr(), "IKNR must be preserved");
    assertEquals(
        originalDto.getFirstName(), roundtrippedDto.getFirstName(), "First name must be preserved");
    assertEquals(
        originalDto.getLastName(), roundtrippedDto.getLastName(), "Last name must be preserved");
    assertEquals(
        originalDto.getPatientName(),
        roundtrippedDto.getPatientName(),
        "Patient name must be preserved");
    assertEquals(
        originalDto.getDateOfBirth(),
        roundtrippedDto.getDateOfBirth(),
        "Date of birth must be preserved");
    assertEquals(
        originalDto.getInsuranceName(),
        roundtrippedDto.getInsuranceName(),
        "Insurance name must be preserved");
    assertEquals(
        originalDto.getValidUntil(),
        roundtrippedDto.getValidUntil(),
        "Valid-until date must be preserved");
    assertEquals(
        originalDto.getValid(), roundtrippedDto.getValid(), "Validity flag must be preserved");
  }

  // -------------------------------------------------------------------------
  // CardImageService unit tests
  // -------------------------------------------------------------------------

  @Test
  void createCardImage_setsCorrectId() {
    EgkInfoDto dto = createTestDto();
    CardImage card = cardImageService.createCardImage(dto);
    assertTrue(card.getId().startsWith("card-"), "Card ID must start with 'card-'");
  }

  @Test
  void createCardImage_setsLabel() {
    EgkInfoDto dto = createTestDto();
    CardImage card = cardImageService.createCardImage(dto);
    assertEquals("eGK Card", card.getLabel());
  }

  @Test
  void createCardImage_createsEgkCard() {
    CardImage card = cardImageService.createCardImage(createTestDto());
    assertNotNull(card.getEgk());
    assertNull(card.getHpc());
    assertNull(card.getHpic());
  }

  @Test
  void createCardImage_egkContainsSimFiles() {
    CardImage card = cardImageService.createCardImage(createTestDto());
    var files = card.getAllFiles();
    assertTrue(files.stream().anyMatch(f -> "SIM.KVNR".equals(f.getName())));
    assertTrue(files.stream().anyMatch(f -> "SIM.IKNR".equals(f.getName())));
    assertTrue(files.stream().anyMatch(f -> "SIM.FIRSTNAME".equals(f.getName())));
    assertTrue(files.stream().anyMatch(f -> "SIM.LASTNAME".equals(f.getName())));
    assertTrue(files.stream().anyMatch(f -> "SIM.DATE_OF_BIRTH".equals(f.getName())));
    assertTrue(files.stream().anyMatch(f -> "SIM.INSURANCE_NAME".equals(f.getName())));
    assertTrue(files.stream().anyMatch(f -> "SIM.VALID_UNTIL".equals(f.getName())));
    assertTrue(files.stream().anyMatch(f -> "SIM.VALID".equals(f.getName())));
  }

  @Test
  void createCardImage_extractionReturnsOriginalData() {
    EgkInfoDto original = createTestDto();
    CardImage card = cardImageService.createCardImage(original);
    EgkInfoDto extracted = egkInfoService.extractEgkInfo(card);

    assertEquals(original.getKvnr(), extracted.getKvnr());
    assertEquals(original.getIknr(), extracted.getIknr());
    assertEquals(original.getFirstName(), extracted.getFirstName());
    assertEquals(original.getLastName(), extracted.getLastName());
    assertEquals(original.getDateOfBirth(), extracted.getDateOfBirth());
    assertEquals(original.getInsuranceName(), extracted.getInsuranceName());
    assertEquals(original.getValidUntil(), extracted.getValidUntil());
    assertEquals(original.getValid(), extracted.getValid());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private CardImage loadCardImageFromResource(String resourceName) throws Exception {
    InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
    assertNotNull(is, "Test resource not found: " + resourceName);
    String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
    return cardImageParser.parseCardImage(xml);
  }

  private EgkInfoDto createTestDto() {
    EgkInfoDto dto = new EgkInfoDto();
    dto.setKvnr("X110639491");
    dto.setIknr("109500969");
    dto.setFirstName("Kriemhild");
    dto.setLastName("Muster");
    dto.setPatientName("Kriemhild Muster");
    dto.setDateOfBirth("19900717");
    dto.setInsuranceName("Test GKV-SV");
    dto.setValidUntil("20261231");
    dto.setValid(true);
    dto.setCardType("EGK");
    return dto;
  }
}
