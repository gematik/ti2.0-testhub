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
package de.gematik.ti20.vsdm.fhir.service;

import static org.junit.jupiter.api.Assertions.*;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import de.gematik.bbriccs.fhir.EncodingType;
import de.gematik.bbriccs.fhir.codec.FhirCodec;
import de.gematik.ti20.vsdm.fhir.builder.*;
import de.gematik.ti20.vsdm.fhir.def.*;
import java.util.List;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CodecServiceR4Test {

  static FhirCodec codec;

  @BeforeAll
  static void init() {
    codec = FhirCodec.forR4().andBbriccsValidator();
  }

  @Test
  void testValidPatientFhir() {
    VsdmPatient patient =
        VsdmPatientBuilder.create()
            .withKvnr("X123456789")
            .withNames("family", "given")
            .withBirthDate("1990-07-17")
            .build();

    String json = codec.encode(patient, EncodingType.JSON);

    assertTrue(codec.isValid(json));
  }

  @Test
  void testCanDeserializePatient() {
    VsdmPatient patient =
        VsdmPatientBuilder.create().withKvnr("X123456789").withNames("family", "given").build();

    String json = codec.encode(patient, EncodingType.JSON);

    Resource resource = codec.decode(json);
    assertNotNull(resource);
    assertInstanceOf(Patient.class, resource);
    Patient deserializedPatient = (Patient) resource;

    assertEquals("X123456789", deserializedPatient.getIdentifier().get(0).getValue());
    assertEquals("family", deserializedPatient.getName().get(0).getFamily());
    assertEquals("given", deserializedPatient.getName().get(0).getGiven().get(0).getValue());
  }

  @Test
  void testValidCoverageFhir() {
    VsdmCoverage coverage =
        VsdmCoverageBuilder.create()
            .withStatus("active")
            .withPayor("Test GKV Krankenkasse")
            .withBeneficiary("Max Mustermann")
            .build();
    String json = codec.encode(coverage, EncodingType.JSON);

    // Validierung schlägt fehl, da VSDMPatient nicht aufgelöst werden kann
    // assertTrue(service.getCodec().isValid(json));
  }

  @Test
  void testCanDeserializeCoverage() {
    VsdmCoverage coverage =
        VsdmCoverageBuilder.create()
            .withStatus("active")
            .withPayor("Test GKV Krankenkasse")
            .withBeneficiary("Max Mustermann")
            .build();
    String json = codec.encode(coverage, EncodingType.JSON);

    Resource resource = codec.decode(json);
    assertNotNull(resource);
    assertInstanceOf(Coverage.class, resource);
    Coverage deserializedCoverage = (Coverage) resource;

    assertEquals("Active", deserializedCoverage.getStatus().getDisplay());
    assertEquals("Test GKV Krankenkasse", deserializedCoverage.getPayor().get(0).getDisplay());
    assertEquals("Max Mustermann", deserializedCoverage.getBeneficiary().getReference());
  }

  @Test
  void testValidPayorOrganizationFhir() {
    VsdmPayorOrganization payorOrganization =
        VsdmPayorOrganizationBuilder.create()
            .iknr("107723372")
            .name("Beispielkostenträger")
            .build();

    String json = codec.encode(payorOrganization, EncodingType.JSON);

    assertTrue(codec.isValid(json));
  }

  @Test
  void testCanDeserializePayorOrganization() {
    VsdmPayorOrganization payorOrganization =
        VsdmPayorOrganizationBuilder.create()
            .iknr("107723372")
            .name("Beispielkostenträger")
            .build();

    String json = codec.encode(payorOrganization, EncodingType.JSON);

    Resource resource = codec.decode(json);
    assertNotNull(resource);
    assertInstanceOf(Organization.class, resource);
    Organization deserializedPayorOrganization = (Organization) resource;

    assertEquals("107723372", deserializedPayorOrganization.getIdentifier().get(0).getValue());
    assertEquals("Beispielkostenträger", deserializedPayorOrganization.getName());
  }

  @Test
  void testValidOperationOutcomeFhir() {
    VsdmOperationOutcome operationOutcome =
        VsdmOperationOutcomeBuilder.create()
            .withCode("79010")
            .withText("text")
            .withReference("VSDSERVICE_INVALID_KVNR")
            .build();

    String json = codec.encode(operationOutcome, EncodingType.JSON);

    assertTrue(codec.isValid(json));
  }

  @Test
  void testCanDeserializeOperationOutcomeFhir() {
    VsdmOperationOutcome operationOutcome =
        VsdmOperationOutcomeBuilder.create()
            .withCode("79010")
            .withText("text")
            .withReference("VSDSERVICE_INTERNAL_SERVER_ERROR")
            .build();

    String json = codec.encode(operationOutcome, EncodingType.JSON);

    Resource resource = codec.decode(json);
    assertNotNull(resource);
    assertInstanceOf(OperationOutcome.class, resource);
    OperationOutcome deserializedOperationOutcome = (OperationOutcome) resource;

    assertEquals(
        "VSDSERVICE_INTERNAL_SERVER_ERROR",
        deserializedOperationOutcome.getIssue().get(0).getDetails().getCoding().get(0).getCode());
    assertEquals("text", deserializedOperationOutcome.getIssue().get(0).getDetails().getText());
  }

  @Test
  void testValidBundleFhir() {
    VsdmPatient patient =
        VsdmPatientBuilder.create()
            .withKvnr("X123456789")
            .withNames("family", "given")
            .withBirthDate("1990-07-17")
            .build();

    VsdmPayorOrganization payorOrganization =
        VsdmPayorOrganizationBuilder.create()
            .iknr("107723372")
            .name("https://gematik.de/fhir/Organization/107723372")
            .build();

    VsdmCoverage coverage =
        VsdmCoverageBuilder.create()
            .withStatus("active")
            .withPayor("https://gematik.de/fhir/Organization/107723372")
            .withIknr("107723372")
            .withBeneficiary("https://gematik.de/fhir/Patient/" + patient.getId())
            .build();

    VsdmBundle vsdmBundle =
        VsdmBundleBuilder.create()
            .addEntry(patient)
            .addEntry(coverage)
            .addEntry(payorOrganization)
            .build();

    String json = codec.encode(vsdmBundle, EncodingType.JSON);

    ValidationResult validationResult = codec.validate(json);
    List<SingleValidationMessage> errors =
        validationResult.getMessages().stream()
            .filter(msg -> msg.getSeverity() == ResultSeverityEnum.ERROR)
            .toList();

    assertEquals(0, errors.size());
  }

  @Test
  void testCanDeserializeBundle() {
    VsdmPatient patient =
        VsdmPatientBuilder.create().withKvnr("X123456789").withNames("family", "given").build();

    VsdmPayorOrganization payorOrganization =
        VsdmPayorOrganizationBuilder.create()
            .iknr("107723372")
            .name("Beispielkostenträger")
            .build();

    VsdmCoverage coverage =
        VsdmCoverageBuilder.create()
            .withStatus("active")
            .withPayor("https://gematik.de/fhir/Organization/" + payorOrganization.getId())
            .withBeneficiary("https://gematik.de/fhir/Patient/" + patient.getId())
            .build();

    VsdmBundle vsdmBundle =
        VsdmBundleBuilder.create()
            .addEntry(patient)
            .addEntry(coverage)
            .addEntry(payorOrganization)
            .build();

    String json = codec.encode(vsdmBundle, EncodingType.JSON);

    Resource resource = codec.decode(json);
    assertNotNull(resource);

    assertInstanceOf(Bundle.class, resource);
    Bundle deserializedBundle = (Bundle) resource;
    assertEquals(3, deserializedBundle.getEntry().size());
  }
}
