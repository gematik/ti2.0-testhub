/*-
 * #%L
 * VSDM2 FHIR Library
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
package de.gematik.ti20.vsdm.fhir.def;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import de.gematik.test.testdata.model.Address;
import de.gematik.test.testdata.model.DeliveryAddress;
import de.gematik.test.testdata.model.PersonData;
import de.gematik.ti20.vsdm.fhir.builder.VsdmPatientBuilder;
import java.io.Serial;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.hl7.fhir.r4.model.Patient;

@Getter
@ResourceDef(profile = "https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMPatient")
public class VsdmPatient extends Patient {

  @Serial private static final long serialVersionUID = -3866131845663337989L;

  @Nonnull
  public static VsdmPatient from(@Nonnull final de.gematik.test.testdata.model.Patient patient) {
    final PersonData personData = patient.getPersonData();
    final VsdmPatientBuilder vsdmPatientBuilder =
        VsdmPatientBuilder.create()
            .withKvnr(personData.getKvnr())
            .withNames(personData.getName().getFamily(), personData.getName().getGiven())
            .withBirthDate(personData.getBirthDate());

    final Address address = personData.getAddress();
    if (address != null) {
      final org.hl7.fhir.r4.model.Address postalAddress = AddressAdapter.postalAddress(address);
      vsdmPatientBuilder.addAddress(postalAddress);
    }

    final DeliveryAddress deliveryAddress = personData.getDeliveryAddress();
    if (deliveryAddress != null) {
      final org.hl7.fhir.r4.model.Address fhirDeliveryAddress =
          AddressAdapter.physicalAddress(deliveryAddress);
      vsdmPatientBuilder.addAddress(fhirDeliveryAddress);
    }

    return vsdmPatientBuilder.build();
  }

  public VsdmPatient() {
    super();
  }
}
