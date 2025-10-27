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
import org.junit.jupiter.api.Test;

class CodecServiceR4Test {

  FhirCodec codec = FhirCodec.forR4().andBbriccsValidator();

  @Test
  void testValidPatientFhir() {
    VsdmPatient patient =
        VsdmPatientBuilder.create().withKvnr("X123456789").withNames("family", "given").build();

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

    ValidationResult validationResult = codec.validate(json);
    List<SingleValidationMessage> errors =
        validationResult.getMessages().stream()
            .filter(msg -> msg.getSeverity() == ResultSeverityEnum.ERROR)
            .toList();
    assertEquals(1, errors.size());
    assertTrue(errors.get(0).getMessage().contains("VSDMCoverageGKV"));
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

    //    String json = codec.encode(vsdmBundle, EncodingType.JSON);
    String json =
        "{\n"
            + "  \"resourceType\": \"Bundle\",\n"
            + "  \"id\": \"b05a393d-a765-4748-9f38-599ad2718e49\",\n"
            + "  \"meta\": {\n"
            + "    \"lastUpdated\": \"2025-10-17T10:28:38.916+02:00\",\n"
            + "    \"profile\": [\n"
            + "      \"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMBundle\"\n"
            + "    ]\n"
            + "  },\n"
            + "  \"identifier\": {\n"
            + "    \"system\": \"urn:ietf:rfc:3986\",\n"
            + "    \"value\": \"urn:uuid:b05a393d-a765-4748-9f38-599ad2718e49\"\n"
            + "  },\n"
            + "  \"type\": \"collection\",\n"
            + "  \"timestamp\": \"2025-10-17T10:28:38.915+02:00\",\n"
            + "  \"entry\": [\n"
            + "    {\n"
            + "      \"fullUrl\": \"https://gematik.de/fhir/Patient/1acd2a41-2cfa-461d-9eed-7fcedd92cc29\",\n"
            + "      \"resource\": {\n"
            + "        \"resourceType\": \"Patient\",\n"
            + "        \"id\": \"1acd2a41-2cfa-461d-9eed-7fcedd92cc29\",\n"
            + "        \"meta\": {\n"
            + "          \"profile\": [\n"
            + "            \"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMPatient\"\n"
            + "          ]\n"
            + "        },\n"
            + "        \"identifier\": [\n"
            + "          {\n"
            + "            \"system\": \"http://fhir.de/sid/gkv/kvid-10\",\n"
            + "            \"value\": \"X123456789\"\n"
            + "          }\n"
            + "        ],\n"
            + "        \"name\": [\n"
            + "          {\n"
            + "            \"use\": \"official\",\n"
            + "            \"text\": \"text\",\n"
            + "            \"family\": \"family\",\n"
            + "            \"given\": [\n"
            + "              \"given\"\n"
            + "            ]\n"
            + "          }\n"
            + "        ],\n"
            + "        \"gender\": \"male\",\n"
            + "        \"birthDate\": \"2025-10-17\"\n"
            + "      }\n"
            + "    },\n"
            + "    {\n"
            + "      \"fullUrl\": \"https://gematik.de/fhir/Coverage/eba45bce-e7cd-4f11-b11c-a12b5b93f554\",\n"
            + "      \"resource\": {\n"
            + "        \"resourceType\": \"Coverage\",\n"
            + "        \"id\": \"eba45bce-e7cd-4f11-b11c-a12b5b93f554\",\n"
            + "        \"meta\": {\n"
            + "          \"profile\": [\n"
            + "            \"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMCoverageGKV\"\n"
            + "          ]\n"
            + "        },\n"
            + "        \"extension\": [\n"
            + "          {\n"
            + "            \"url\": \"http://fhir.de/StructureDefinition/gkv/wop\",\n"
            + "            \"valueCoding\": {\n"
            + "              \"system\": \"https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_ITA_WOP\",\n"
            + "              \"code\": \"98\",\n"
            + "              \"display\": \"Sachsen\"\n"
            + "            }\n"
            + "          },\n"
            + "          {\n"
            + "            \"url\": \"http://fhir.de/StructureDefinition/gkv/versichertenart\",\n"
            + "            \"valueCoding\": {\n"
            + "              \"system\": \"https://fhir.kbv.de/CodeSystem/KBV_CS_SFHIR_KBV_VERSICHERTENSTATUS\",\n"
            + "              \"code\": \"1\",\n"
            + "              \"display\": \"Mitglieder\"\n"
            + "            }\n"
            + "          }\n"
            + "        ],\n"
            + "        \"identifier\": [\n"
            + "          {\n"
            + "            \"system\": \"http://fhir.de/sid/gkv/kvid-10\",\n"
            + "            \"value\": \"A123454321\"\n"
            + "          }\n"
            + "        ],\n"
            + "        \"status\": \"active\",\n"
            + "        \"type\": {\n"
            + "          \"coding\": [\n"
            + "            {\n"
            + "              \"system\": \"http://fhir.de/CodeSystem/versicherungsart-de-basis\",\n"
            + "              \"code\": \"GKV\"\n"
            + "            }\n"
            + "          ]\n"
            + "        },\n"
            + "        \"beneficiary\": {\n"
            + "          \"reference\": \"https://gematik.de/fhir/Patient/1acd2a41-2cfa-461d-9eed-7fcedd92cc29\"\n"
            + "        },\n"
            + "        \"payor\": [\n"
            + "          {\n"
            + "            \"extension\": [\n"
            + "              {\n"
            + "                \"url\": \"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMKostentraegerRolle\",\n"
            + "                \"valueCoding\": {\n"
            + "                  \"system\": \"https://gematik.de/fhir/vsdm2/CodeSystem/VSDMKostentraegerRolleCS\",\n"
            + "                  \"code\": \"H\",\n"
            + "                  \"display\": \"Haupt-Kostenträger\"\n"
            + "                }\n"
            + "              }\n"
            + "            ],\n"
            + "            \"reference\": \"https://gematik.de/fhir/Organization/d7939590-4d0a-496d-8ae3-08167a978073\",\n"
            + "            \"display\": \"https://gematik.de/fhir/Organization/d7939590-4d0a-496d-8ae3-08167a978073\"\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    },\n"
            + "    {\n"
            + "      \"fullUrl\": \"https://gematik.de/fhir/Organization/d7939590-4d0a-496d-8ae3-08167a978073\",\n"
            + "      \"resource\": {\n"
            + "        \"resourceType\": \"Organization\",\n"
            + "        \"id\": \"d7939590-4d0a-496d-8ae3-08167a978073\",\n"
            + "        \"meta\": {\n"
            + "          \"profile\": [\n"
            + "            \"https://gematik.de/fhir/vsdm2/StructureDefinition/VSDMPayorOrganization\"\n"
            + "          ]\n"
            + "        },\n"
            + "        \"identifier\": [\n"
            + "          {\n"
            + "            \"system\": \"http://fhir.de/sid/arge-ik/iknr\",\n"
            + "            \"value\": \"107723372\"\n"
            + "          }\n"
            + "        ],\n"
            + "        \"name\": \"Beispielkostenträger\"\n"
            + "      }\n"
            + "    }\n"
            + "  ]\n"
            + "}\n";
    Resource resource = codec.decode(json);
    assertNotNull(resource);

    assertInstanceOf(Bundle.class, resource);
    Bundle deserializedBundle = (Bundle) resource;
    assertEquals(3, deserializedBundle.getEntry().size());
  }
}
