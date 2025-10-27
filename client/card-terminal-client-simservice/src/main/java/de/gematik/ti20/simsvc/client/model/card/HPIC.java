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
import java.util.ArrayList;
import java.util.List;

/** Represents an HPIC (SMC-B Card / Institution Card) card's data structure. */
@XmlRootElement(name = "hpic")
@XmlAccessorType(XmlAccessType.FIELD)
public class HPIC {

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

  /** Container class for applications within an HPIC. */
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
  public HPIC() {}

  /**
   * Get all keys from the card, regardless of application.
   *
   * @return List of all keys
   */
  public List<Key> getAllKeys() {
    List<Key> allKeys = new ArrayList<>();
    if (applications != null && applications.getApplicationList() != null) {
      for (Application app : applications.getApplicationList()) {
        if (app.getContainers() != null && app.getContainers().getKeys() != null) {
          allKeys.addAll(app.getContainers().getKeys());
        }
      }
    }
    return allKeys;
  }

  /**
   * Find a specific key by type. For HPIC cards, we need special handling as the naming patterns
   * differ from other card types.
   *
   * @param keyType Key type (AUT, ENC, QES, etc.)
   * @return Matching key or null if not found
   */
  public Key findKeyByType(String keyType) {
    List<Key> keys = getAllKeys();

    // Special patterns for HPIC keys - check in priority order
    if ("AUT".equals(keyType)) {
      // First check for elliptic curve AUT keys
      for (Key key : keys) {
        String name = key.getName();
        if (name != null
            && (name.contains("PRK_HCI_AUT_E256")
                || name.contains("PRK_AUTR_CVC_E256")
                || name.contains("PRK_AUTD_RPE_CVC_E256"))) {
          return key;
        }
      }

      // Then check for RSA AUT keys
      for (Key key : keys) {
        String name = key.getName();
        if (name != null && name.contains("PRK_HCI_AUT_R2048")) {
          return key;
        }
      }
    } else if ("ENC".equals(keyType)) {
      // First check for elliptic curve encryption keys
      for (Key key : keys) {
        String name = key.getName();
        if (name != null && name.contains("PRK_HCI_ENC_E256")) {
          return key;
        }
      }

      // Then check for RSA encryption keys
      for (Key key : keys) {
        String name = key.getName();
        if (name != null && name.contains("PRK_HCI_ENC_R2048")) {
          return key;
        }
      }
    } else if ("QES".equals(keyType) || "OSIG".equals(keyType)) {
      // First check for elliptic curve keys
      for (Key key : keys) {
        String name = key.getName();
        if (name != null
            && (name.contains("PRK_HCI_OSIG_E256") || name.contains("PRK_HCI_QES_E256"))) {
          return key;
        }
      }

      // Then check for RSA keys
      for (Key key : keys) {
        String name = key.getName();
        if (name != null
            && (name.contains("PRK_HCI_OSIG_R2048") || name.contains("PRK_HCI_QES_R2048"))) {
          return key;
        }
      }
    }

    // No specific match found, do generic search
    for (Key key : keys) {
      String name = key.getName();
      if (name != null && name.contains(keyType)) {
        return key;
      }
    }

    return null;
  }

  /**
   * Get all files from the card, regardless of application.
   *
   * @return List of all files
   */
  public List<FileData> getAllFiles() {
    List<FileData> allFiles = new ArrayList<>();
    if (applications != null && applications.getApplicationList() != null) {
      for (Application app : applications.getApplicationList()) {
        if (app.getContainers() != null && app.getContainers().getFiles() != null) {
          allFiles.addAll(app.getContainers().getFiles());
        }
      }
    }
    return allFiles;
  }

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
