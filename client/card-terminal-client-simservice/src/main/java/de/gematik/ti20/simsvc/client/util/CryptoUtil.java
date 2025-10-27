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

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/** Utility class for cryptographic operations. */
public class CryptoUtil {

  static {
    // Initialize Bouncy Castle provider
    Security.addProvider(new BouncyCastleProvider());
  }

  /** Private constructor to prevent instantiation. */
  private CryptoUtil() {
    // Utility class should not be instantiated
  }

  /**
   * Create a digest implementation based on algorithm name. Only supports secure, modern algorithms
   * (no SHA1).
   *
   * @param algorithm Algorithm name (e.g., "SHA-256")
   * @return Digest implementation
   */
  public static Digest createDigest(String algorithm) {
    switch (algorithm.replace("-", "").toUpperCase()) {
      case "SHA256":
        return new SHA256Digest();
      case "SHA384":
        return new SHA384Digest();
      case "SHA512":
        return new SHA512Digest();
      case "SHA1":
        throw new IllegalArgumentException(
            "SHA1 is deprecated and insecure. Use SHA256, SHA384, or SHA512 instead.");
      default:
        throw new IllegalArgumentException("Unsupported digest algorithm: " + algorithm);
    }
  }

  /**
   * Generate a random challenge of the specified length.
   *
   * @param length Length of the challenge in bytes
   * @return Random challenge bytes
   */
  public static byte[] generateRandomChallenge(int length) {
    SecureRandom random = new SecureRandom();
    byte[] challenge = new byte[length];
    random.nextBytes(challenge);
    return challenge;
  }

  /**
   * Generate an RSA key pair.
   *
   * @param keySize Key size in bits (e.g., 2048)
   * @return RSA key pair
   * @throws NoSuchAlgorithmException If the RSA algorithm is not available
   */
  public static KeyPair generateRsaKeyPair(int keySize) throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(keySize);
    return keyGen.generateKeyPair();
  }

  /**
   * Generate an EC key pair.
   *
   * @param curve Curve name (e.g., "secp256r1")
   * @return EC key pair
   * @throws NoSuchAlgorithmException If the EC algorithm is not available
   */
  public static KeyPair generateEcKeyPair(String curve) throws NoSuchAlgorithmException {
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(256); // For secp256r1
    return keyGen.generateKeyPair();
  }

  /**
   * Compute a SHA-256 hash of the input data.
   *
   * @param data Input data
   * @return SHA-256 hash
   * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available
   */
  public static byte[] sha256(byte[] data) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return digest.digest(data);
  }

  /**
   * Sign data with a private key.
   *
   * @param privateKey Private key
   * @param data Data to sign
   * @param algorithm Signature algorithm (e.g., "SHA256withRSA")
   * @return Signature bytes
   * @throws Exception If signing fails
   */
  public static byte[] sign(PrivateKey privateKey, byte[] data, String algorithm) throws Exception {
    Signature signature = Signature.getInstance(algorithm);
    signature.initSign(privateKey);
    signature.update(data);
    return signature.sign();
  }

  /**
   * Verify a signature with a public key.
   *
   * @param publicKey Public key
   * @param data Original data
   * @param signatureBytes Signature to verify
   * @param algorithm Signature algorithm (e.g., "SHA256withRSA")
   * @return true if the signature is valid, false otherwise
   * @throws Exception If verification fails
   */
  public static boolean verify(
      PublicKey publicKey, byte[] data, byte[] signatureBytes, String algorithm) throws Exception {
    Signature signature = Signature.getInstance(algorithm);
    signature.initVerify(publicKey);
    signature.update(data);
    return signature.verify(signatureBytes);
  }

  /**
   * Create a private key from PKCS#8 encoded bytes.
   *
   * @param keyBytes Private key bytes in PKCS#8 format
   * @param algorithm Key algorithm (e.g., "RSA")
   * @return Private key
   * @throws Exception If key creation fails
   */
  public static PrivateKey createPrivateKey(byte[] keyBytes, String algorithm) throws Exception {
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
    return keyFactory.generatePrivate(keySpec);
  }

  /**
   * Create a public key from X.509 encoded bytes.
   *
   * @param keyBytes Public key bytes in X.509 format
   * @param algorithm Key algorithm (e.g., "RSA")
   * @return Public key
   * @throws Exception If key creation fails
   */
  public static PublicKey createPublicKey(byte[] keyBytes, String algorithm) throws Exception {
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
    return keyFactory.generatePublic(keySpec);
  }

  /**
   * Extract the public key from an X.509 certificate.
   *
   * @param certificate X.509 certificate
   * @return Public key
   */
  public static PublicKey extractPublicKey(X509Certificate certificate) {
    return certificate.getPublicKey();
  }

  /**
   * Create a DER-encoded signature algorithm identifier.
   *
   * @param algorithm Algorithm OID
   * @return DER-encoded algorithm identifier
   * @throws Exception If encoding fails
   */
  public static byte[] createSignatureAlgorithmIdentifier(String algorithm) throws Exception {
    AlgorithmIdentifier algId;

    if (algorithm.equals("SHA256withRSA")) {
      algId =
          new AlgorithmIdentifier(
              new ASN1ObjectIdentifier("1.2.840.113549.1.1.11"), DERNull.INSTANCE);
    } else if (algorithm.equals("SHA384withRSA")) {
      algId =
          new AlgorithmIdentifier(
              new ASN1ObjectIdentifier("1.2.840.113549.1.1.12"), DERNull.INSTANCE);
    } else if (algorithm.equals("SHA512withRSA")) {
      algId =
          new AlgorithmIdentifier(
              new ASN1ObjectIdentifier("1.2.840.113549.1.1.13"), DERNull.INSTANCE);
    } else if (algorithm.equals("SHA256withECDSA")) {
      algId = new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.10045.4.3.2"));
    } else if (algorithm.equals("SHA384withECDSA")) {
      algId = new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.10045.4.3.3"));
    } else if (algorithm.equals("SHA512withECDSA")) {
      algId = new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.10045.4.3.4"));
    } else {
      throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
    }

    return algId.getEncoded(ASN1Encoding.DER);
  }
}
