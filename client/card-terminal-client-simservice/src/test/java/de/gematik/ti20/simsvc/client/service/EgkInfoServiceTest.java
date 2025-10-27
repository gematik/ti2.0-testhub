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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.simsvc.client.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class EgkInfoServiceTest {

  private CardImageParser cardImageParser = new CardImageParser();
  private EgkInfoService egkInfoService = new EgkInfoService();

  @Test
  public void testShouldParseEgk() throws Exception {
    InputStream is =
        EgkInfoServiceTest.class.getClassLoader().getResourceAsStream("egkCardImage.xml");
    String xmlString = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    CardImage cardImage = cardImageParser.parseCardImage(xmlString);
    EgkInfoDto egkInfoDto = egkInfoService.extractEgkInfo(cardImage);

    assertEquals("X110639491", egkInfoDto.getKvnr());
    assertEquals("Kriemhild", egkInfoDto.getFirstName());
    assertEquals("19900717", egkInfoDto.getDateOfBirth());
    assertEquals("Test GKV-SV", egkInfoDto.getInsuranceName());
    assertEquals(true, egkInfoDto.getValid());
  }

  @Test
  public void testShouldParseInvalidEgk() throws Exception {
    InputStream is =
        EgkInfoServiceTest.class.getClassLoader().getResourceAsStream("egkCardImageInvalid.xml");
    String xmlString = new String(is.readAllBytes(), StandardCharsets.UTF_8);

    CardImage cardImage = cardImageParser.parseCardImage(xmlString);
    EgkInfoDto egkInfoDto = egkInfoService.extractEgkInfo(cardImage);

    assertEquals("X110639491", egkInfoDto.getKvnr());
    assertEquals("Kriemhild", egkInfoDto.getFirstName());
    assertEquals("19900717", egkInfoDto.getDateOfBirth());
    assertEquals("Test GKV-SV", egkInfoDto.getInsuranceName());
    assertEquals(false, egkInfoDto.getValid());
  }
}
