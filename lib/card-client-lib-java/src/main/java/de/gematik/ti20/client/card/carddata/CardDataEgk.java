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
package de.gematik.ti20.client.card.carddata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CardDataEgk extends CardData {

  private String kvnr;
  private PersonalData personalData;
  private InsuranceData insuranceData;

  @JsonCreator
  public CardDataEgk(
      @JsonProperty("number") String cardNumber,
      @JsonProperty("expires") String cardExpires,
      @JsonProperty("kvnr") String kvnr) {
    super(Type.EGK, cardNumber, cardExpires);
    this.kvnr = kvnr;
    this.personalData = new PersonalData();
    this.insuranceData = new InsuranceData();
  }

  public static class PersonalData {

    private String name;
    private String birthDate;
    private String gender;
    private String address;

    public PersonalData() {}

    @JsonCreator
    public PersonalData(
        @JsonProperty("name") String name,
        @JsonProperty("birthDate") String birthDate,
        @JsonProperty("gender") String gender,
        @JsonProperty("address") String address) {
      this.name = name;
      this.birthDate = birthDate;
      this.gender = gender;
      this.address = address;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getBirthDate() {
      return birthDate;
    }

    public void setBirthDate(String birthDate) {
      this.birthDate = birthDate;
    }

    public String getGender() {
      return gender;
    }

    public void setGender(String gender) {
      this.gender = gender;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }
  }

  public static class InsuranceData {

    private String insuranceName;
    private String insuredStatus;

    public InsuranceData() {}

    @JsonCreator
    public InsuranceData(
        @JsonProperty("insuranceName") String insuranceName,
        @JsonProperty("insuredStatus") String insuredStatus) {
      this.insuranceName = insuranceName;
      this.insuredStatus = insuredStatus;
    }

    public String getInsuranceName() {
      return insuranceName;
    }

    public void setInsuranceName(String insuranceName) {
      this.insuranceName = insuranceName;
    }

    public String getInsuredStatus() {
      return insuredStatus;
    }

    public void setInsuredStatus(String insuredStatus) {
      this.insuredStatus = insuredStatus;
    }
  }

  public String getKvnr() {
    return kvnr;
  }

  public PersonalData getPersonalData() {
    return personalData;
  }

  public InsuranceData getInsuranceData() {
    return insuranceData;
  }
}
