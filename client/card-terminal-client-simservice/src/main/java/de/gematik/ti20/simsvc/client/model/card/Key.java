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

/**
 * Represents a cryptographic key on a smart card. Contains the key reference, name, and private key
 * data.
 */
@XmlRootElement(name = "key")
@XmlAccessorType(XmlAccessType.FIELD)
public class Key {

  @XmlAttribute(name = "keyRef")
  private String keyRef;

  @XmlAttribute(name = "name")
  private String name;

  @XmlAttribute(name = "keyIdentifier")
  private String keyIdentifier;

  @XmlElement(name = "privateKey")
  private String privateKey;

  /** Default constructor for JAXB. */
  public Key() {}

  /**
   * Constructor with all fields.
   *
   * @param keyRef Key reference
   * @param name Key name
   * @param keyIdentifier Key identifier
   * @param privateKey Private key data (Base64 encoded)
   */
  public Key(String keyRef, String name, String keyIdentifier, String privateKey) {
    this.keyRef = keyRef;
    this.name = name;
    this.keyIdentifier = keyIdentifier;
    this.privateKey = privateKey;
  }

  /**
   * Get the key reference.
   *
   * @return Key reference
   */
  public String getKeyRef() {
    return keyRef;
  }

  /**
   * Set the key reference.
   *
   * @param keyRef Key reference
   */
  public void setKeyRef(String keyRef) {
    this.keyRef = keyRef;
  }

  /**
   * Get the key name.
   *
   * @return Key name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the key name.
   *
   * @param name Key name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the key identifier.
   *
   * @return Key identifier
   */
  public String getKeyIdentifier() {
    return keyIdentifier;
  }

  /**
   * Set the key identifier.
   *
   * @param keyIdentifier Key identifier
   */
  public void setKeyIdentifier(String keyIdentifier) {
    this.keyIdentifier = keyIdentifier;
  }

  /**
   * Get the private key data (Base64 encoded).
   *
   * @return Private key data
   */
  public String getPrivateKey() {
    return privateKey;
  }

  /**
   * Set the private key data (Base64 encoded).
   *
   * @param privateKey Private key data
   */
  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }
}
