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
 * Data Transfer Object (DTO) for sign responses. Contains the signature and related information.
 */
public class SignResponseDto {

  private String signature;
  private String algorithm;
  private String certificate;

  /** Default constructor. */
  public SignResponseDto() {}

  /**
   * Constructor with signature.
   *
   * @param signature Signature as a Base64 encoded string
   */
  public SignResponseDto(String signature) {
    this.signature = signature;
  }

  /**
   * Constructor with all fields.
   *
   * @param signature Signature as a Base64 encoded string
   * @param algorithm Algorithm used for signing
   * @param certificate Certificate used for signing (Base64 encoded)
   */
  public SignResponseDto(String signature, String algorithm, String certificate) {
    this.signature = signature;
    this.algorithm = algorithm;
    this.certificate = certificate;
  }

  /**
   * Get the signature.
   *
   * @return Signature as a Base64 encoded string
   */
  public String getSignature() {
    return signature;
  }

  /**
   * Set the signature.
   *
   * @param signature Signature as a Base64 encoded string
   */
  public void setSignature(String signature) {
    this.signature = signature;
  }

  /**
   * Get the algorithm.
   *
   * @return Algorithm name
   */
  public String getAlgorithm() {
    return algorithm;
  }

  /**
   * Set the algorithm.
   *
   * @param algorithm Algorithm name
   */
  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  /**
   * Get the certificate.
   *
   * @return Certificate as a Base64 encoded string
   */
  public String getCertificate() {
    return certificate;
  }

  /**
   * Set the certificate.
   *
   * @param certificate Certificate as a Base64 encoded string
   */
  public void setCertificate(String certificate) {
    this.certificate = certificate;
  }
}
