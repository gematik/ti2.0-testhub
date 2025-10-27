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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenParams {

  private final String proofMethod;
  private final Long patientProofTime;
  private final Long iat;
  private final String patientId;
  private final String insurerId;
  private final String actorId;
  private final String actorProfessionOid;

  @JsonCreator
  public TokenParams(
      @JsonProperty(value = "proofMethod") final String proofMethod,
      @JsonProperty(value = "patientProofTime") final String patientProofTimeStr,
      @JsonProperty(value = "iat") final String iatStr,
      @JsonProperty(value = "patientId") final String patientId,
      @JsonProperty(value = "insurerId") final String insurerId,
      @JsonProperty(value = "actorId") final String actorId,
      @JsonProperty(value = "actorProfessionOid") final String actorProfessionOid) {

    this.proofMethod = proofMethod != null ? proofMethod : "ehc-provider-user-x509";

    this.patientProofTime =
        patientProofTimeStr != null
            ? Long.parseLong(patientProofTimeStr)
            : System.currentTimeMillis() / 1000;
    this.iat = iatStr != null ? Long.parseLong(iatStr) : System.currentTimeMillis() / 1000;

    this.patientId = patientId;
    this.insurerId = insurerId;
    this.actorId = actorId;
    this.actorProfessionOid = actorProfessionOid;
  }

  public String getProofMethod() {
    return proofMethod;
  }

  public Long getPatientProofTime() {
    return patientProofTime;
  }

  public Long getIat() {
    return iat;
  }

  public String getPatientId() {
    return patientId;
  }

  public String getInsurerId() {
    return insurerId;
  }

  public String getActorId() {
    return actorId;
  }

  public String getActorProfessionOid() {
    return actorProfessionOid;
  }
}
