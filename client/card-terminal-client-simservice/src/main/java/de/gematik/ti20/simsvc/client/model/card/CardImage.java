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

/**
 * Base class for card images. This class represents a smart card's data as loaded from an XML file.
 */
@XmlRootElement(name = "cardImage")
@XmlAccessorType(XmlAccessType.FIELD)
public class CardImage {

  @XmlElement(name = "ehc")
  private EGK egk;

  @XmlElement(name = "hpc")
  private HPC hpc;

  @XmlElement(name = "hpic")
  private HPIC hpic;

  @XmlElement(name = "cardType")
  private String cardTypeString;

  @XmlElement(name = "id")
  private String id;

  @XmlElement(name = "label")
  private String label;

  /** Default constructor for JAXB. */
  public CardImage() {}

  /**
   * Get the card type string.
   *
   * @return The card type string
   */
  public String getCardTypeString() {
    return cardTypeString;
  }

  /**
   * Set the card type string.
   *
   * @param cardTypeString The card type string
   */
  public void setCardTypeString(String cardTypeString) {
    this.cardTypeString = cardTypeString;
  }

  /**
   * Get the card type as an enum.
   *
   * @return The card type enum
   */
  public CardType getCardType() {
    if (cardTypeString != null) {
      try {
        return CardType.valueOf(cardTypeString);
      } catch (IllegalArgumentException e) {
        // Try to map similar card types
        if ("EHC".equals(cardTypeString)) {
          return CardType.EHC;
        } else if ("HPC".equals(cardTypeString)) {
          return CardType.HPC;
        }
        // Default to EGK if not recognized
        return CardType.EGK;
      }
    }

    // Determine type based on which element is present
    if (egk != null) {
      return CardType.EGK;
    } else if (hpc != null) {
      return CardType.HPC;
    } else if (hpic != null) {
      return CardType.HPIC;
    }

    // Default to EGK
    return CardType.EGK;
  }

  /**
   * Get the card's unique identifier.
   *
   * @return Card ID
   */
  public String getId() {
    return id;
  }

  /**
   * Set the card's unique identifier.
   *
   * @param id Card ID
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Get the card's label.
   *
   * @return Card label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Set the card's label.
   *
   * @param label Card label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Get the EGK data if this is an EGK card.
   *
   * @return EGK data or null
   */
  public EGK getEgk() {
    return egk;
  }

  /**
   * Set the EGK data.
   *
   * @param egk EGK data
   */
  public void setEgk(EGK egk) {
    this.egk = egk;
  }

  /**
   * Get the HPC data if this is an HBA card.
   *
   * @return HPC data or null
   */
  public HPC getHpc() {
    return hpc;
  }

  /**
   * Set the HPC data.
   *
   * @param hpc HPC data
   */
  public void setHpc(HPC hpc) {
    this.hpc = hpc;
  }

  /**
   * Get the HPIC data if this is an HPIC (SMC-B) card.
   *
   * @return HPIC data or null
   */
  public HPIC getHpic() {
    return hpic;
  }

  /**
   * Set the HPIC data.
   *
   * @param hpic HPIC data
   */
  public void setHpic(HPIC hpic) {
    this.hpic = hpic;
  }

  /**
   * Get all keys from the card, regardless of which type it is.
   *
   * @return List of all keys
   */
  public List<Key> getAllKeys() {
    List<Key> allKeys = new ArrayList<>();

    // Check EGK keys
    if (egk != null && egk.getApplications() != null) {
      for (Application app : egk.getApplications().getApplicationList()) {
        if (app.getContainers() != null && app.getContainers().getKeys() != null) {
          allKeys.addAll(app.getContainers().getKeys());
        }
      }
    }

    // Check HPC keys
    if (hpc != null && hpc.getApplications() != null) {
      for (Application app : hpc.getApplications().getApplicationList()) {
        if (app.getContainers() != null && app.getContainers().getKeys() != null) {
          allKeys.addAll(app.getContainers().getKeys());
        }
      }
    }

    // Check HPIC keys
    if (hpic != null) {
      allKeys.addAll(hpic.getAllKeys());
    }

    return allKeys;
  }

  /**
   * Get all files from the card, regardless of which type it is.
   *
   * @return List of all files
   */
  public List<FileData> getAllFiles() {
    List<FileData> allFiles = new ArrayList<>();

    // Check EGK files
    if (egk != null && egk.getApplications() != null) {
      for (Application app : egk.getApplications().getApplicationList()) {
        if (app.getContainers() != null && app.getContainers().getFiles() != null) {
          allFiles.addAll(app.getContainers().getFiles());
        }
      }
    }

    // Check HPC files
    if (hpc != null && hpc.getApplications() != null) {
      for (Application app : hpc.getApplications().getApplicationList()) {
        if (app.getContainers() != null && app.getContainers().getFiles() != null) {
          allFiles.addAll(app.getContainers().getFiles());
        }
      }
    }

    // Check HPIC files
    if (hpic != null) {
      allFiles.addAll(hpic.getAllFiles());
    }

    return allFiles;
  }
}
