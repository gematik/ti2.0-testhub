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
package de.gematik.ti20.simsvc.client.model.dto;

/**
 * Data Transfer Object for SMC-B card information. Contains Telematik-ID and ProfessionOID
 * extracted from the card certificate.
 */
public class SmcBInfoDto {

  private String telematikId;
  private String professionOid;
  private String holderName;
  private String organizationName;
  private String cardType;

  /** Default constructor. */
  public SmcBInfoDto() {}

  /**
   * Constructor with all fields.
   *
   * @param telematikId The Telematik-ID
   * @param professionOid The ProfessionOID
   * @param holderName The card holder name
   * @param organizationName The organization name
   * @param cardType The card type
   */
  public SmcBInfoDto(
      String telematikId,
      String professionOid,
      String holderName,
      String organizationName,
      String cardType) {
    this.telematikId = telematikId;
    this.professionOid = professionOid;
    this.holderName = holderName;
    this.organizationName = organizationName;
    this.cardType = cardType;
  }

  /**
   * Get the Telematik-ID.
   *
   * @return The Telematik-ID
   */
  public String getTelematikId() {
    return telematikId;
  }

  /**
   * Set the Telematik-ID.
   *
   * @param telematikId The Telematik-ID
   */
  public void setTelematikId(String telematikId) {
    this.telematikId = telematikId;
  }

  /**
   * Get the ProfessionOID.
   *
   * @return The ProfessionOID
   */
  public String getProfessionOid() {
    return professionOid;
  }

  /**
   * Set the ProfessionOID.
   *
   * @param professionOid The ProfessionOID
   */
  public void setProfessionOid(String professionOid) {
    this.professionOid = professionOid;
  }

  /**
   * Get the card holder name.
   *
   * @return The card holder name
   */
  public String getHolderName() {
    return holderName;
  }

  /**
   * Set the card holder name.
   *
   * @param holderName The card holder name
   */
  public void setHolderName(String holderName) {
    this.holderName = holderName;
  }

  /**
   * Get the organization name.
   *
   * @return The organization name
   */
  public String getOrganizationName() {
    return organizationName;
  }

  /**
   * Set the organization name.
   *
   * @param organizationName The organization name
   */
  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  /**
   * Get the card type.
   *
   * @return The card type
   */
  public String getCardType() {
    return cardType;
  }

  /**
   * Set the card type.
   *
   * @param cardType The card type
   */
  public void setCardType(String cardType) {
    this.cardType = cardType;
  }
}
