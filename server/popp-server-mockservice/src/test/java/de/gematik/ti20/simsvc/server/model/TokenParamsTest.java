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

package de.gematik.ti20.simsvc.server.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenParamsTest {

  @Test
  void testConstructorWithAllValues() {
    // Arrange
    String proofMethod = "custom-method";
    String patientProofTimeStr = "1640995200";
    String iatStr = "1640995300";
    String patientId = "patient123";
    String insurerId = "insurer456";
    String actorId = "actor789";
    String actorProfessionOid = "1.2.3.4.5";

    // Act
    TokenParams params =
        new TokenParams(
            proofMethod,
            patientProofTimeStr,
            iatStr,
            patientId,
            insurerId,
            actorId,
            actorProfessionOid);

    // Assert
    assertEquals(proofMethod, params.getProofMethod());
    assertEquals(1640995200L, params.getPatientProofTime());
    assertEquals(1640995300L, params.getIat());
    assertEquals(patientId, params.getPatientId());
    assertEquals(insurerId, params.getInsurerId());
    assertEquals(actorId, params.getActorId());
    assertEquals(actorProfessionOid, params.getActorProfessionOid());
  }

  @Test
  void testConstructorWithNullValues() {
    // Act
    TokenParams params = new TokenParams(null, null, null, null, null, null, null);

    // Assert
    assertEquals("ehc-provider-user-x509", params.getProofMethod());
    assertNotNull(params.getPatientProofTime());
    assertNotNull(params.getIat());
    assertNull(params.getPatientId());
    assertNull(params.getInsurerId());
    assertNull(params.getActorId());
    assertNull(params.getActorProfessionOid());
  }

  @Test
  void testDefaultTimestamps() {
    // Arrange
    long beforeCreation = System.currentTimeMillis() / 1000;

    // Act
    TokenParams params = new TokenParams(null, null, null, null, null, null, null);

    // Assert
    long afterCreation = System.currentTimeMillis() / 1000;
    assertTrue(params.getPatientProofTime() >= beforeCreation);
    assertTrue(params.getPatientProofTime() <= afterCreation);
    assertTrue(params.getIat() >= beforeCreation);
    assertTrue(params.getIat() <= afterCreation);
  }

  @Test
  void testMixedValues() {
    // Arrange
    String patientProofTimeStr = "1640995200";
    String patientId = "patient123";

    // Act
    TokenParams params =
        new TokenParams(null, patientProofTimeStr, null, patientId, null, null, null);

    // Assert
    assertEquals("ehc-provider-user-x509", params.getProofMethod());
    assertEquals(1640995200L, params.getPatientProofTime());
    assertNotNull(params.getIat());
    assertEquals(patientId, params.getPatientId());
    assertNull(params.getInsurerId());
    assertNull(params.getActorId());
    assertNull(params.getActorProfessionOid());
  }
}
