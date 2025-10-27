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
package de.gematik.ti20.client.card.card;

/** Class representing card certificate information from the card or card image */
public class CardCertInfoEgk extends CardCertInfo {

  private String kvnr;
  private String iknr;

  private String patientName;
  private String firstName;
  private String lastName;

  /** Default constructor for JSON deserialization. */
  public CardCertInfoEgk() {
    super(CardType.EGK);
  }

  public CardCertInfoEgk(
      String kvnr, String iknr, String patientName, String firstName, String lastName) {
    super(CardType.EGK);
    this.kvnr = kvnr;
    this.iknr = iknr;
    this.patientName = patientName;
    this.firstName = firstName;
    this.lastName = lastName;
  }

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
}
