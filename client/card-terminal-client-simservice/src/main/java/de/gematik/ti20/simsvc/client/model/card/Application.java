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

/**
 * Represents an application on a smart card. Applications contain files, keys, and other data
 * elements.
 */
@XmlRootElement(name = "application")
@XmlAccessorType(XmlAccessType.FIELD)
public class Application {

  @XmlElement(name = "applicationId")
  private String applicationId;

  @XmlElement(name = "deactivated")
  private boolean deactivated;

  @XmlElement(name = "containers")
  private Containers containers;

  /** Container class for the containers within an application. */
  @XmlRootElement(name = "containers")
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Containers {
    @XmlElement(name = "file")
    private List<FileData> files;

    @XmlElement(name = "key")
    private List<Key> keys;

    @XmlElement(name = "pin")
    private List<Pin> pins;

    /**
     * Get the list of files.
     *
     * @return List of files
     */
    public List<FileData> getFiles() {
      return files;
    }

    /**
     * Set the list of files.
     *
     * @param files List of files
     */
    public void setFiles(List<FileData> files) {
      this.files = files;
    }

    /**
     * Get the list of keys.
     *
     * @return List of keys
     */
    public List<Key> getKeys() {
      return keys;
    }

    /**
     * Set the list of keys.
     *
     * @param keys List of keys
     */
    public void setKeys(List<Key> keys) {
      this.keys = keys;
    }

    /**
     * Get the list of PINs.
     *
     * @return List of PINs
     */
    public List<Pin> getPins() {
      return pins;
    }

    /**
     * Set the list of PINs.
     *
     * @param pins List of PINs
     */
    public void setPins(List<Pin> pins) {
      this.pins = pins;
    }
  }

  /** Default constructor for JAXB. */
  public Application() {}

  /**
   * Get the application ID.
   *
   * @return Application ID
   */
  public String getApplicationId() {
    return applicationId;
  }

  /**
   * Set the application ID.
   *
   * @param applicationId Application ID
   */
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  /**
   * Check if the application is deactivated.
   *
   * @return true if deactivated, false otherwise
   */
  public boolean isDeactivated() {
    return deactivated;
  }

  /**
   * Set the deactivated status.
   *
   * @param deactivated Deactivated status
   */
  public void setDeactivated(boolean deactivated) {
    this.deactivated = deactivated;
  }

  /**
   * Get the containers.
   *
   * @return Containers
   */
  public Containers getContainers() {
    return containers;
  }

  /**
   * Set the containers.
   *
   * @param containers Containers
   */
  public void setContainers(Containers containers) {
    this.containers = containers;
  }
}
