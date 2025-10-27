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
package de.gematik.ti20.client.card.card;

/**
 * Class for configuring signature operations. Provides options for specifying hash algorithm,
 * signature type, and key references.
 */
public class SignOptions {

  /** Enumeration of supported hash algorithms. */
  public enum HashAlgorithm {
    SHA256("SHA256"),
    SHA384("SHA384"),
    SHA512("SHA512");

    private final String algorithmName;

    HashAlgorithm(String algorithmName) {
      this.algorithmName = algorithmName;
    }

    public String getAlgorithmName() {
      return algorithmName;
    }
  }

  /** Enumeration of supported signature types. */
  public enum SignatureType {
    RSA("RSA"),
    ECDSA("ECDSA");

    private final String typeName;

    SignatureType(String typeName) {
      this.typeName = typeName;
    }

    public String getTypeName() {
      return typeName;
    }
  }

  private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA256;
  private SignatureType signatureType = SignatureType.ECDSA;
  private String keyReference =
      null; // Default key reference is null, which means the card will use its default key

  /**
   * Creates default sign options. Default values: - Hash algorithm: SHA-256 - Signature type: RSA -
   * Key reference: 03 (typically used for C.QES on health cards)
   */
  public SignOptions() {
    // Use default values
  }

  /**
   * Creates sign options with the specified parameters.
   *
   * @param hashAlgorithm the hash algorithm
   * @param signatureType the signature type
   * @param keyReference the key reference
   */
  public SignOptions(
      HashAlgorithm hashAlgorithm, SignatureType signatureType, String keyReference) {
    this.hashAlgorithm = hashAlgorithm;
    this.signatureType = signatureType;
    this.keyReference = keyReference;
  }

  /**
   * Creates sign options with the specified parameters.
   *
   * @param hashAlgorithm the hash algorithm
   * @param signatureType the signature type
   */
  public SignOptions(HashAlgorithm hashAlgorithm, SignatureType signatureType) {
    this.hashAlgorithm = hashAlgorithm;
    this.signatureType = signatureType;
  }

  /**
   * Creates sign options with the specified parameters.
   *
   * @param hashAlgorithm the hash algorithm
   */
  public SignOptions(HashAlgorithm hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
  }

  /**
   * Gets the hash algorithm.
   *
   * @return the hash algorithm
   */
  public HashAlgorithm getHashAlgorithm() {
    return hashAlgorithm;
  }

  /**
   * Sets the hash algorithm.
   *
   * @param hashAlgorithm the hash algorithm
   * @return this object for method chaining
   */
  public SignOptions setHashAlgorithm(HashAlgorithm hashAlgorithm) {
    this.hashAlgorithm = hashAlgorithm;
    return this;
  }

  /**
   * Gets the signature type.
   *
   * @return the signature type
   */
  public SignatureType getSignatureType() {
    return signatureType;
  }

  /**
   * Sets the signature type.
   *
   * @param signatureType the signature type
   * @return this object for method chaining
   */
  public SignOptions setSignatureType(SignatureType signatureType) {
    this.signatureType = signatureType;
    return this;
  }

  /**
   * Gets the key reference.
   *
   * @return the key reference
   */
  public String getKeyReference() {
    return keyReference;
  }

  /**
   * Sets the key reference.
   *
   * @param keyReference the key reference
   * @return this object for method chaining
   */
  public SignOptions setKeyReference(String keyReference) {
    this.keyReference = keyReference;
    return this;
  }
}
