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
package de.gematik.ti20.vsdm.fhir.builder;

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.ti20.vsdm.fhir.def.VsdmPayorOrganization;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.jupiter.api.Test;

class VsdmPayorOrganizationBuilderTest {

  @Test
  void testBuildPayorOrganization() {
    String iknr = "123456789";
    String name = "Test Krankenkasse";

    VsdmPayorOrganization payorOrg =
        VsdmPayorOrganizationBuilder.create().iknr(iknr).name(name).build();

    assertNotNull(payorOrg);
    assertEquals(name, payorOrg.getName());

    assertNotNull(payorOrg.getIdentifier());
    assertEquals(1, payorOrg.getIdentifier().size());

    Identifier identifier = payorOrg.getIdentifier().get(0);
    assertEquals("http://fhir.de/sid/arge-ik/iknr", identifier.getSystem());
    assertEquals(iknr, identifier.getValue());
  }
}
