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
package de.gematik.ti20.simsvc.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for EGK (elektronische Gesundheitskarte) information extracted from real card images.
 * Contains authentic patient data including KVNR, IKNR, and personal information.
 */
public class EgkInfoDto {

  @JsonProperty("kvnr")
  private String kvnr;

  @JsonProperty("iknr")
  private String iknr;

  @JsonProperty("patientName")
  private String patientName;

  @JsonProperty("firstName")
  private String firstName;

  @JsonProperty("lastName")
  private String lastName;

  @JsonProperty("dateOfBirth")
  private String dateOfBirth;

  @JsonProperty("insuranceName")
  private String insuranceName;

  @JsonProperty("cardType")
  private String cardType;

  @JsonProperty("validUntil")
  private String validUntil;

  @JsonProperty("valid")
  private Boolean valid;

  public EgkInfoDto() {
    this.cardType = "EGK";
  }

  // Getters and Setters
  public String getKvnr() {
    return kvnr;
  }

  public void setKvnr(String kvnr) {
    this.kvnr = kvnr;
  }

  public String getIknr() {
    return iknr;
  }

  public void setIknr(String iknr) {
    this.iknr = iknr;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String patientName) {
    this.patientName = patientName;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(String dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public String getInsuranceName() {
    return insuranceName;
  }

  public void setInsuranceName(String insuranceName) {
    this.insuranceName = insuranceName;
  }

  public String getCardType() {
    return cardType;
  }

  public void setCardType(String cardType) {
    this.cardType = cardType;
  }

  public String getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(String validUntil) {
    this.validUntil = validUntil;
  }

  public Boolean getValid() {
    return valid;
  }

  public void setValid(Boolean valid) {
    this.valid = valid;
  }
}
