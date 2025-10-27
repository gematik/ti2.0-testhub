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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SignOptionsTest {

  @Test
  void testDefaultConstructor() {
    SignOptions options = new SignOptions();
    assertEquals(SignOptions.HashAlgorithm.SHA256, options.getHashAlgorithm());
    assertEquals(SignOptions.SignatureType.ECDSA, options.getSignatureType());
    assertNull(options.getKeyReference());
  }

  @Test
  void testConstructorWithAllParams() {
    SignOptions options =
        new SignOptions(SignOptions.HashAlgorithm.SHA512, SignOptions.SignatureType.RSA, "03");
    assertEquals(SignOptions.HashAlgorithm.SHA512, options.getHashAlgorithm());
    assertEquals(SignOptions.SignatureType.RSA, options.getSignatureType());
    assertEquals("03", options.getKeyReference());
  }

  @Test
  void testConstructorWithHashAndSignatureType() {
    SignOptions options =
        new SignOptions(SignOptions.HashAlgorithm.SHA384, SignOptions.SignatureType.ECDSA);
    assertEquals(SignOptions.HashAlgorithm.SHA384, options.getHashAlgorithm());
    assertEquals(SignOptions.SignatureType.ECDSA, options.getSignatureType());
    assertNull(options.getKeyReference());
  }

  @Test
  void testConstructorWithHashOnly() {
    SignOptions options = new SignOptions(SignOptions.HashAlgorithm.SHA512);
    assertEquals(SignOptions.HashAlgorithm.SHA512, options.getHashAlgorithm());
    assertEquals(SignOptions.SignatureType.ECDSA, options.getSignatureType());
    assertNull(options.getKeyReference());
  }

  @Test
  void testSettersAndChaining() {
    SignOptions options =
        new SignOptions()
            .setHashAlgorithm(SignOptions.HashAlgorithm.SHA384)
            .setSignatureType(SignOptions.SignatureType.RSA)
            .setKeyReference("42");
    assertEquals(SignOptions.HashAlgorithm.SHA384, options.getHashAlgorithm());
    assertEquals(SignOptions.SignatureType.RSA, options.getSignatureType());
    assertEquals("42", options.getKeyReference());
  }

  @Test
  void testEnumGetters() {
    assertEquals("SHA256", SignOptions.HashAlgorithm.SHA256.getAlgorithmName());
    assertEquals("RSA", SignOptions.SignatureType.RSA.getTypeName());
  }
}
