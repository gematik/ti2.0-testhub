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
package de.gematik.ti20.simsvc.client.model.card;

import jakarta.xml.bind.annotation.*;
import java.util.List;

/** Represents an HPC (Health Professional Card / Heilberufsausweis) card's data structure. */
@XmlRootElement(name = "hpc")
@XmlAccessorType(XmlAccessType.FIELD)
public class HPC {

  @XmlElement(name = "applications")
  private Applications applications;

  @XmlElement(name = "cardGeneration")
  private String cardGeneration;

  @XmlElement(name = "commonName")
  private String commonName;

  @XmlElement(name = "expirationDate")
  private String expirationDate;

  @XmlElement(name = "iccsn")
  private String iccsn;

  @XmlElement(name = "keyDerivation")
  private String keyDerivation;

  @XmlElement(name = "objectSystemIds")
  private String objectSystemIds;

  /** Container class for applications within an HPC. */
  @XmlRootElement(name = "applications")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Applications {
    @XmlElement(name = "application")
    private List<Application> applicationList;

    /**
     * Get the list of applications.
     *
     * @return List of applications
     */
    public List<Application> getApplicationList() {
      return applicationList;
    }

    /**
     * Set the list of applications.
     *
     * @param applicationList List of applications
     */
    public void setApplicationList(List<Application> applicationList) {
      this.applicationList = applicationList;
    }
  }

  /** Default constructor for JAXB. */
  public HPC() {}

  /**
   * Get the applications container.
   *
   * @return Applications container
   */
  public Applications getApplications() {
    return applications;
  }

  /**
   * Set the applications container.
   *
   * @param applications Applications container
   */
  public void setApplications(Applications applications) {
    this.applications = applications;
  }

  /**
   * Get the card generation.
   *
   * @return Card generation string
   */
  public String getCardGeneration() {
    return cardGeneration;
  }

  /**
   * Set the card generation.
   *
   * @param cardGeneration Card generation string
   */
  public void setCardGeneration(String cardGeneration) {
    this.cardGeneration = cardGeneration;
  }

  /**
   * Get the common name.
   *
   * @return Common name string
   */
  public String getCommonName() {
    return commonName;
  }

  /**
   * Set the common name.
   *
   * @param commonName Common name string
   */
  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  /**
   * Get the expiration date.
   *
   * @return Expiration date string
   */
  public String getExpirationDate() {
    return expirationDate;
  }

  /**
   * Set the expiration date.
   *
   * @param expirationDate Expiration date string
   */
  public void setExpirationDate(String expirationDate) {
    this.expirationDate = expirationDate;
  }

  /**
   * Get the ICCSN (Integrated Circuit Card Serial Number).
   *
   * @return ICCSN string
   */
  public String getIccsn() {
    return iccsn;
  }

  /**
   * Set the ICCSN (Integrated Circuit Card Serial Number).
   *
   * @param iccsn ICCSN string
   */
  public void setIccsn(String iccsn) {
    this.iccsn = iccsn;
  }

  /**
   * Get the key derivation.
   *
   * @return Key derivation string
   */
  public String getKeyDerivation() {
    return keyDerivation;
  }

  /**
   * Set the key derivation.
   *
   * @param keyDerivation Key derivation string
   */
  public void setKeyDerivation(String keyDerivation) {
    this.keyDerivation = keyDerivation;
  }

  /**
   * Get the object system IDs.
   *
   * @return Object system IDs string
   */
  public String getObjectSystemIds() {
    return objectSystemIds;
  }

  /**
   * Set the object system IDs.
   *
   * @param objectSystemIds Object system IDs string
   */
  public void setObjectSystemIds(String objectSystemIds) {
    this.objectSystemIds = objectSystemIds;
  }
}
