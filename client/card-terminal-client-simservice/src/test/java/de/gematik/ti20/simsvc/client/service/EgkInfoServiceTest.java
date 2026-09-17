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

import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EgkInfoServiceTest {

  private EgkInfoService egkInfoService = new EgkInfoService();

  private final VirtualCardImageLoader loader = new VirtualCardImageLoader();

  @Test
  void testShouldParseInvalidEgk() throws Exception {
    String xml = loadResourceAsString("egkCardImageInvalid.xml");
    VirtualCardImageData imageData = loader.load(xml);
    EgkInfoDto egkInfoDto = egkInfoService.extractEgkInfo(imageData);

    assertEquals("X110639491", egkInfoDto.getKvnr());
    assertEquals("Kriemhild Amelie Abigail Hannelore", egkInfoDto.getFirstName());
    assertEquals("19900717", egkInfoDto.getDateOfBirth());
    assertEquals("Test GKV-SV", egkInfoDto.getInsuranceName());
    assertEquals(false, egkInfoDto.getValid());
  }

  @Test
  void testShouldParseValidEgk1() throws Exception {
    String xml = loadResourceAsString("egkCardImage.xml");
    VirtualCardImageData imageData = loader.load(xml);
    EgkInfoDto egkInfoDto = egkInfoService.extractEgkInfo(imageData);

    assertEquals("X110639491", egkInfoDto.getKvnr());
    assertEquals("109500969", egkInfoDto.getIknr());
    assertEquals("Kriemhild Amelie Abigail Hannelore", egkInfoDto.getFirstName());
    assertEquals("19900717", egkInfoDto.getDateOfBirth());
    assertEquals("Test GKV-SV", egkInfoDto.getInsuranceName());
    assertEquals(true, egkInfoDto.getValid());
  }

  @Test
  void testShouldParseValidEgk2() throws Exception {
    String xml = loadResourceAsString("attached_assets/EGK_80276883110000168583_gema5.xml");
    VirtualCardImageData imageData = loader.load(xml);
    EgkInfoDto egkInfoDto = egkInfoService.extractEgkInfo(imageData);

    assertEquals("X110639491", egkInfoDto.getKvnr());
    assertEquals("109500969", egkInfoDto.getIknr());
    assertEquals("Kriemhild Amelie Abigail Hannelore", egkInfoDto.getFirstName());
    assertEquals("19900717", egkInfoDto.getDateOfBirth());
    assertEquals("Test GKV-SV", egkInfoDto.getInsuranceName());
    assertEquals(true, egkInfoDto.getValid());
  }

  private String loadResourceAsString(String resourceName) throws Exception {
    return Files.readString(Path.of(ClassLoader.getSystemResource(resourceName).toURI()));
  }
}
