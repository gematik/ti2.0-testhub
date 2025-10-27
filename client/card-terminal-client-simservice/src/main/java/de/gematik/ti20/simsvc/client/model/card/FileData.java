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

/** Represents a file on a smart card. Contains the file ID, name, and binary data. */
@XmlRootElement(name = "file")
@XmlAccessorType(XmlAccessType.FIELD)
public class FileData {

  @XmlAttribute(name = "fileId")
  private String fileId;

  @XmlAttribute(name = "name")
  private String name;

  @XmlElement(name = "data")
  private String data;

  /** Default constructor for JAXB. */
  public FileData() {}

  /**
   * Constructor with all fields.
   *
   * @param fileId File ID
   * @param name File name
   * @param data File data (Base64 encoded)
   */
  public FileData(String fileId, String name, String data) {
    this.fileId = fileId;
    this.name = name;
    this.data = data;
  }

  /**
   * Get the file ID.
   *
   * @return File ID
   */
  public String getFileId() {
    return fileId;
  }

  /**
   * Set the file ID.
   *
   * @param fileId File ID
   */
  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  /**
   * Get the file name.
   *
   * @return File name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the file name.
   *
   * @param name File name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the file data (Base64 encoded).
   *
   * @return File data
   */
  public String getData() {
    return data;
  }

  /**
   * Set the file data (Base64 encoded).
   *
   * @param data File data
   */
  public void setData(String data) {
    this.data = data;
  }
}
