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
 * Represents a container of elements (files, keys, PINs) within an application. This is a utility
 * class for XML parsing.
 */
@XmlRootElement(name = "containers")
@XmlAccessorType(XmlAccessType.FIELD)
public class Container {

  @XmlElement(name = "file")
  private List<FileData> files;

  @XmlElement(name = "key")
  private List<Key> keys;

  @XmlElement(name = "pin")
  private List<Pin> pins;

  /** Default constructor for JAXB. */
  public Container() {}

  /**
   * Constructor with all elements.
   *
   * @param files List of files
   * @param keys List of keys
   * @param pins List of PINs
   */
  public Container(List<FileData> files, List<Key> keys, List<Pin> pins) {
    this.files = files;
    this.keys = keys;
    this.pins = pins;
  }

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

  /**
   * Find a file by its fileId.
   *
   * @param fileId The fileId to search for
   * @return The FileData object if found, null otherwise
   */
  public FileData findFileById(String fileId) {
    if (files == null) return null;

    return files.stream()
        .filter(file -> fileId.equalsIgnoreCase(file.getFileId()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Find a key by its keyRef.
   *
   * @param keyRef The keyRef to search for
   * @return The Key object if found, null otherwise
   */
  public Key findKeyByRef(String keyRef) {
    if (keys == null) return null;

    return keys.stream()
        .filter(key -> keyRef.equalsIgnoreCase(key.getKeyRef()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Find a PIN by its pinRef.
   *
   * @param pinRef The pinRef to search for
   * @return The Pin object if found, null otherwise
   */
  public Pin findPinByRef(String pinRef) {
    if (pins == null) return null;

    return pins.stream()
        .filter(pin -> pinRef.equalsIgnoreCase(pin.getPinRef()))
        .findFirst()
        .orElse(null);
  }
}
