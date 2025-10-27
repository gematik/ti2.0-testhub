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
package de.gematik.ti20.simsvc.client.util;

import static org.junit.jupiter.api.Assertions.*;

import java.security.*;
import org.bouncycastle.crypto.Digest;
import org.junit.jupiter.api.Test;

class CryptoUtilTest {

  @Test
  void testCreateDigest() {
    Digest d256 = CryptoUtil.createDigest("SHA-256");
    assertNotNull(d256);
    Digest d384 = CryptoUtil.createDigest("SHA-384");
    assertNotNull(d384);
    Digest d512 = CryptoUtil.createDigest("SHA-512");
    assertNotNull(d512);
    assertThrows(IllegalArgumentException.class, () -> CryptoUtil.createDigest("SHA-1"));
    assertThrows(IllegalArgumentException.class, () -> CryptoUtil.createDigest("MD5"));
  }

  @Test
  void testGenerateRandomChallenge() {
    byte[] challenge = CryptoUtil.generateRandomChallenge(16);
    assertNotNull(challenge);
    assertEquals(16, challenge.length);
  }

  @Test
  void testGenerateRsaKeyPair() throws Exception {
    KeyPair kp = CryptoUtil.generateRsaKeyPair(2048);
    assertNotNull(kp.getPrivate());
    assertNotNull(kp.getPublic());
  }

  @Test
  void testGenerateEcKeyPair() throws Exception {
    KeyPair kp = CryptoUtil.generateEcKeyPair("secp256r1");
    assertNotNull(kp.getPrivate());
    assertNotNull(kp.getPublic());
  }

  @Test
  void testSha256() throws Exception {
    byte[] hash = CryptoUtil.sha256("test".getBytes());
    assertEquals(32, hash.length);
  }

  @Test
  void testSignAndVerify() throws Exception {
    KeyPair kp = CryptoUtil.generateRsaKeyPair(2048);
    byte[] data = "sign me".getBytes();
    byte[] sig = CryptoUtil.sign(kp.getPrivate(), data, "SHA256withRSA");
    assertTrue(CryptoUtil.verify(kp.getPublic(), data, sig, "SHA256withRSA"));
    assertFalse(CryptoUtil.verify(kp.getPublic(), "other".getBytes(), sig, "SHA256withRSA"));
  }

  @Test
  void testCreatePrivateAndPublicKey() throws Exception {
    KeyPair kp = CryptoUtil.generateRsaKeyPair(2048);
    byte[] privBytes = kp.getPrivate().getEncoded();
    byte[] pubBytes = kp.getPublic().getEncoded();

    PrivateKey priv = CryptoUtil.createPrivateKey(privBytes, "RSA");
    PublicKey pub = CryptoUtil.createPublicKey(pubBytes, "RSA");

    assertNotNull(priv);
    assertNotNull(pub);
  }

  @Test
  void testCreateSignatureAlgorithmIdentifier() throws Exception {
    byte[] der = CryptoUtil.createSignatureAlgorithmIdentifier("SHA256withRSA");
    assertNotNull(der);
    assertThrows(
        IllegalArgumentException.class,
        () -> CryptoUtil.createSignatureAlgorithmIdentifier("MD5withRSA"));
  }
}
