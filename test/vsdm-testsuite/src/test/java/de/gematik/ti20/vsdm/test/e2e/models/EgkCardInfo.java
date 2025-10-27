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
package de.gematik.ti20.vsdm.test.e2e.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EgkCardInfo {

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

  @JsonCreator
  public EgkCardInfo(
      @JsonProperty("kvnr") String kvnr,
      @JsonProperty("iknr") String iknr,
      @JsonProperty("patientName") String patientName,
      @JsonProperty("firstName") String firstName,
      @JsonProperty("lastName") String lastName,
      @JsonProperty("dateOfBirth") String dateOfBirth,
      @JsonProperty("insuranceName") String insuranceName,
      @JsonProperty("cardType") String cardType,
      @JsonProperty("validUntil") String validUntil) {
    this.kvnr = kvnr;
    this.iknr = iknr;
    this.patientName = patientName;
    this.firstName = firstName;
    this.lastName = lastName;
    this.dateOfBirth = dateOfBirth;
    this.insuranceName = insuranceName;
    this.cardType = cardType;
    this.validUntil = validUntil;
  }
}
