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
 * Represents a PIN (Personal Identification Number) on a smart card. Contains PIN information like
 * reference, value, and try counter.
 */
@XmlRootElement(name = "pin")
@XmlAccessorType(XmlAccessType.FIELD)
public class Pin {

  @XmlAttribute(name = "pinRef")
  private String pinRef;

  @XmlAttribute(name = "name")
  private String name;

  @XmlElement(name = "value")
  private String value;

  @XmlElement(name = "tryCounter")
  private Integer tryCounter;

  /** Default constructor for JAXB. */
  public Pin() {}

  /**
   * Constructor with all fields.
   *
   * @param pinRef PIN reference
   * @param name PIN name
   * @param value PIN value (encrypted or hashed)
   * @param tryCounter Try counter value
   */
  public Pin(String pinRef, String name, String value, Integer tryCounter) {
    this.pinRef = pinRef;
    this.name = name;
    this.value = value;
    this.tryCounter = tryCounter;
  }

  /**
   * Get the PIN reference.
   *
   * @return PIN reference
   */
  public String getPinRef() {
    return pinRef;
  }

  /**
   * Set the PIN reference.
   *
   * @param pinRef PIN reference
   */
  public void setPinRef(String pinRef) {
    this.pinRef = pinRef;
  }

  /**
   * Get the PIN name.
   *
   * @return PIN name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the PIN name.
   *
   * @param name PIN name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the PIN value (encrypted or hashed).
   *
   * @return PIN value
   */
  public String getValue() {
    return value;
  }

  /**
   * Set the PIN value (encrypted or hashed).
   *
   * @param value PIN value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Get the try counter value.
   *
   * @return Try counter value
   */
  public Integer getTryCounter() {
    return tryCounter;
  }

  /**
   * Set the try counter value.
   *
   * @param tryCounter Try counter value
   */
  public void setTryCounter(Integer tryCounter) {
    this.tryCounter = tryCounter;
  }
}
