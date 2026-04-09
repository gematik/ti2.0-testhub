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

import de.gematik.bbriccs.fhir.de.builder.AddressBuilder;
import de.gematik.bbriccs.fhir.de.valueset.Country;
import de.gematik.test.testdata.model.DeliveryAddress;
import javax.annotation.Nonnull;
import org.hl7.fhir.r4.model.Address;

/**
 * Create {@link org.hl7.fhir.r4.model.Address} from {@link de.gematik.test.testdata.model.Address}
 * and {@link DeliveryAddress}
 */
public class AddressAdapter {

  private AddressAdapter() {}

  @Nonnull
  public static Address postalAddress(@Nonnull de.gematik.test.testdata.model.Address original) {
    final AddressBuilder addressBuilder = AddressBuilder.ofPostalType();
    if (original.getCountry() != null) {
      addressBuilder.country(Country.fromCode(original.getCountry()));
    }

    return addressBuilder
        .city(original.getCity())
        .postal(original.getPostalCode())
        .street(original.getStreetName() + " " + original.getHouseNumber())
        .build();
  }

  @Nonnull
  public static Address physicalAddress(@Nonnull final DeliveryAddress original) {
    final AddressBuilder addressBuilder = AddressBuilder.ofPhysicalType();
    if (original.getCountry() != null) {
      addressBuilder.country(Country.fromCode(original.getCountry()));
    }

    return addressBuilder
        .city(original.getCity())
        .street(original.getStreetName() + " " + original.getHouseNumber())
        .postal(original.getPostalCode())
        .build();
  }
}
