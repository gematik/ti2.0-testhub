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

import de.gematik.ti20.simsvc.client.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.card.Application;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.EGK;
import de.gematik.ti20.simsvc.client.model.card.FileData;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Service for creating {@link CardImage} instances from EGK patient data. The generated card image
 * stores patient information in simulator-specific files (prefixed with {@code SIM.}) so that
 * {@link EgkInfoService} can reliably extract the data back without requiring real PKI certificates
 * or compressed EF.PD/EF.VD data.
 */
@Service
public class CardImageService {

  /**
   * Create a synthetic {@link CardImage} from the given {@link EgkInfoDto}.
   *
   * <p>The resulting card image represents an EGK card whose patient data is stored in plaintext
   * simulator files ({@code SIM.KVNR}, {@code SIM.IKNR}, etc.) inside the card's application
   * container.
   *
   * @param dto patient data to embed in the card image
   * @return a new {@link CardImage} that can be round-tripped back to an equivalent {@link
   *     EgkInfoDto} via {@link EgkInfoService#extractEgkInfo(CardImage)}
   */
  public CardImage createCardImage(EgkInfoDto dto) {
    final CardImage cardImage = new CardImage();
    cardImage.setId("card-" + System.currentTimeMillis());
    cardImage.setLabel("eGK Card");

    EGK egk = new EGK();
    egk.setCommonName(dto.getFirstName() + " " + dto.getLastName());
    egk.setIccsn(dto.getKvnr());
    egk.setExpirationDate(dto.getValidUntil());

    EGK.Applications applications = new EGK.Applications();
    List<Application> appList = new ArrayList<>();

    Application app = new Application();
    app.setApplicationId("SIM");
    app.setDeactivated(false);

    Application.Containers containers = new Application.Containers();
    List<FileData> files = new ArrayList<>();
    containers.setFiles(files);
    app.setContainers(containers);

    addFile(files, "SIM.KVNR", dto.getKvnr());
    addFile(files, "SIM.IKNR", dto.getIknr());
    addFile(files, "SIM.FIRSTNAME", dto.getFirstName());
    addFile(files, "SIM.LASTNAME", dto.getLastName());
    addFile(files, "SIM.PATIENT_NAME", dto.getPatientName());
    addFile(files, "SIM.DATE_OF_BIRTH", dto.getDateOfBirth());
    addFile(files, "SIM.INSURANCE_NAME", dto.getInsuranceName());
    addFile(files, "SIM.VALID_UNTIL", dto.getValidUntil());
    if (dto.getValid() != null) {
      addFile(files, "SIM.VALID", String.valueOf(dto.getValid()));
    }

    appList.add(app);
    applications.setApplicationList(appList);
    egk.setApplications(applications);

    cardImage.setEgk(egk);
    return cardImage;
  }

  private void addFile(List<FileData> files, String name, String value) {
    if (value == null) {
      return;
    }
    FileData file = new FileData();
    file.setFileId(name);
    file.setName(name);
    file.setData(value);
    files.add(file);
  }
}
