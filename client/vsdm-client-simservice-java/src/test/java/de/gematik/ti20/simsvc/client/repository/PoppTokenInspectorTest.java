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
package de.gematik.ti20.simsvc.client.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PoppTokenInspectorTest {

  final String POPP_TOKEN =
      "eyJhbGciOiJFUzI1NiIsInR5cCI6InZuZC50ZWxlbWF0aWsucG9wcCtqd3QiLCJraWQiOiJwb3BwbW9jayIsIng1YyI6WyJNSUlCM1RDQ0FZR2dBd0lCQWdJRUJ3R0pSekFNQmdncWhrak9QUVFEQWdVQU1HTXhDekFKQmdOVkJBWVRBa1JGTVE0d0RBWURWUVFJRXdWVGRHRjBaVEVOTUFzR0ExVUVCeE1FUTJsMGVURVFNQTRHQTFVRUNoTUhSWGhoYlhCc1pURVVNQklHQTFVRUN4TUxSR1YyWld4dmNHMWxiblF4RFRBTEJnTlZCQU1UQkZSbGMzUXdIaGNOTWpVd05USXpNVEl6TWpVd1doY05Nall3TlRJek1USXpNalV3V2pCak1Rc3dDUVlEVlFRR0V3SkVSVEVPTUF3R0ExVUVDQk1GVTNSaGRHVXhEVEFMQmdOVkJBY1RCRU5wZEhreEVEQU9CZ05WQkFvVEIwVjRZVzF3YkdVeEZEQVNCZ05WQkFzVEMwUmxkbVZzYjNCdFpXNTBNUTB3Q3dZRFZRUURFd1JVWlhOME1Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMERBUWNEUWdBRVhwR00wL3ZjUnNjbWl4eEl0bjdLNjI0Y3dOdVFBUGc3djJCNWJrSmh2RUJWOVUvOVlyQXI3NjJDWnFPRTdSM2NqLzRDVjVwamdHNW45RTFRT2RScU1LTWhNQjh3SFFZRFZSME9CQllFRk5xSSt0NDZDMFo1SXJhbThKWnhXV3N2SGlKbE1Bd0dDQ3FHU000OUJBTUNCUUFEU0FBd1JRSWhBSXJUa2pjck1ZMDBMOU1VWDdNajc4OGhzL1c0aFNnWnNua2Y1M2hwSUZyQkFpQjlEWnQzNzlGOXRKbHArajRCN3Bsb3BybU5sT1hvRnh2ZnlObWNsVlVVVUE9PSJdfQ.eyJ2ZXJzaW9uIjoiMS4wLjAiLCJpc3MiOiJodHRwczovL3BvcHAuZXhhbXBsZS5jb20iLCJpYXQiOjE3NTM0MzM1MjUsInByb29mTWV0aG9kIjoiZWhjLXByb3ZpZGVyLXVzZXIteDUwOSIsInBhdGllbnRQcm9vZlRpbWUiOjE3MzU2ODYwMDAsInBhdGllbnRJZCI6IlgxMTA2Mzk0OTEiLCJpbnN1cmVySWQiOiIxMDk1MDA5NjkiLCJhY3RvcklkIjoiODgzMTEwMDAwMTY4NjUwIiwiYWN0b3JQcm9mZXNzaW9uT2lkIjoiMS4yLjI3Ni4wLjc2LjQuMzIifQ.9kI_Q_YUIhWNETONIyXRBwNu0Vo64jg3aE-kwrig8I-O99oDPXOubU2Q_8cej0kaM2d0gIBeqE5yUfJpKuop0A";

  final PoppTokenInspector poppTokenInspector = new PoppTokenInspector();

  @Test
  void testExtractsPatientProofTime() {
    final Long patientProofTime = poppTokenInspector.getPatientProofTime(POPP_TOKEN);
    assertNotNull(patientProofTime);
    assertEquals(1735686000L, patientProofTime);
  }

  @Test
  void testExtractIKNr() {
    final String ikNr = poppTokenInspector.getIkNr(POPP_TOKEN);
    assertNotNull(ikNr);
    assertEquals("109500969", ikNr);
  }
}
