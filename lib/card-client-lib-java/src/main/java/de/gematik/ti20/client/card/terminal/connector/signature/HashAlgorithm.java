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
package de.gematik.ti20.client.card.terminal.connector.signature;

/** Enum for hash algorithms supported by the Connector signature service. */
public enum HashAlgorithm {
  /** SHA-256 hash algorithm. */
  SHA256("http://www.w3.org/2001/04/xmlenc#sha256", "SHA256withRSA"),

  /** SHA-384 hash algorithm. */
  SHA384("http://www.w3.org/2001/04/xmldsig-more#sha384", "SHA384withRSA"),

  /** SHA-512 hash algorithm. */
  SHA512("http://www.w3.org/2001/04/xmlenc#sha512", "SHA512withRSA");

  private final String uri;
  private final String signatureAlgorithm;

  HashAlgorithm(String uri, String signatureAlgorithm) {
    this.uri = uri;
    this.signatureAlgorithm = signatureAlgorithm;
  }

  /**
   * Returns the URI for this hash algorithm.
   *
   * @return the URI
   */
  public String getUri() {
    return uri;
  }

  /**
   * Returns the signature algorithm associated with this hash algorithm.
   *
   * @return the signature algorithm
   */
  public String getSignatureAlgorithm() {
    return signatureAlgorithm;
  }
}
