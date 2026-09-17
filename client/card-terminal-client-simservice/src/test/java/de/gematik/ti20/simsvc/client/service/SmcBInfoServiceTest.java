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

import static org.assertj.core.api.Assertions.*;

import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SmcBInfoServiceTest {
  private SmcBInfoService smcBInfoService;
  private VirtualCardImageLoader loader = new VirtualCardImageLoader();

  @BeforeEach
  void setUp() {
    smcBInfoService = new SmcBInfoService();
  }

  @Test
  void shouldHandleRealCardImage1() throws Exception {
    String xml = loadResourceAsString("SMC_B_80276883110000168650_gema5.xml");
    VirtualCardImageData imageData = loader.load(xml);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(imageData);

    assertThat(result).isNotNull();
    assertThat(result.getCardType()).isEqualTo("SMCB");
    assertThat(result.getTelematikId()).isEqualTo("1-SMC-B-Testkarte--883110000168650");
    assertThat(result.getProfessionOid()).isEqualTo("1.2.276.0.76.4.50");
  }

  @Test
  void shouldHandleRealCardImage2() throws Exception {
    String xml = loadResourceAsString("SMC_B_80276883110000180834_gema5_INVALID.xml");
    VirtualCardImageData imageData = loader.load(xml);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(imageData);

    assertThat(result).isNotNull();
    assertThat(result.getCardType()).isEqualTo("SMCB");
    assertThat(result.getHolderName()).isEqualTo("Praxis Maximilian Graf NötherTEST-ONLY");
    assertThat(result.getOrganizationName()).isEqualTo("Praxis Maximilian Graf NötherNOT-VALID");
    assertThat(result.getTelematikId()).isEqualTo("1-20.TK--883110000180834");
    assertThat(result.getProfessionOid()).isEqualTo("1.2.276.0.54.4.9");
  }

  @Test
  void shouldHandleRealCardImage3() throws Exception {
    String xml = loadResourceAsString("attached_assets/SMC_B_80276883110000168650_gema5.xml");
    VirtualCardImageData imageData = loader.load(xml);

    // When
    SmcBInfoDto result = smcBInfoService.extractSmcBInfo(imageData);

    assertThat(result).isNotNull();
    assertThat(result.getCardType()).isEqualTo("SMCB");
    assertThat(result.getHolderName()).isEqualTo("Praxis Münchhausen-HohenfelsTEST-ONLY");
    assertThat(result.getOrganizationName()).isEqualTo("Praxis Münchhausen-HohenfelsNOT-VALID");
    assertThat(result.getTelematikId()).isEqualTo("1-SMC-B-Testkarte--883110000168650");
    assertThat(result.getProfessionOid()).isEqualTo("1.2.276.0.76.4.50");
  }

  private String loadResourceAsString(String resourceName) throws Exception {
    return Files.readString(Path.of(ClassLoader.getSystemResource(resourceName).toURI()));
  }
}
