/*-
 * #%L
 * VSDM Client Simulator Service
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */

package de.gematik.ti20.simsvc.client.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;

/**
 * Helper for creating DPoP JWTs (RFC 9449) using ES256 / P-256.
 *
 * <p>Uses BouncyCastle throughout: PEM parsing via {@link PEMParser}, key decoding via {@link
 * PrivateKeyFactory}/{@link PublicKeyFactory}, hashing via {@link SHA256Digest}, and signing via
 * {@link ECDSASigner}. No JCA bridge is required.
 */
public class DpopHelper {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private DpopHelper() {}

  /**
   * Create a DPoP JWT (RFC 9449) signed with ES256.
   *
   * @param publicKeyPem X.509 PEM public key ({@code -----BEGIN PUBLIC KEY-----})
   * @param privateKeyPem PKCS#8 PEM private key ({@code -----BEGIN PRIVATE KEY-----})
   * @param htm HTTP method (e.g. {@code "GET"})
   * @param htu HTTP URI (e.g. {@code "https://example.com/resource"})
   * @param athVal optional {@code ath} claim value (access-token hash); {@code null} to omit
   * @return compact serialised DPoP JWT ({@code header.payload.signature})
   * @throws DpopException on crypto or encoding errors
   */
  public static String createDpop(
      String publicKeyPem, String privateKeyPem, String htm, String htu, String athVal)
      throws DpopException {

    ECPublicKeyParameters ecPub = parseEcPublicKey(publicKeyPem);
    ECPrivateKeyParameters ecPriv = parseEcPrivateKey(privateKeyPem);

    try {
      ObjectNode jwk =
          MAPPER
              .createObjectNode()
              .put("kty", "EC")
              .put("crv", "P-256")
              .put("alg", "ES256")
              .put(
                  "x",
                  b64url(
                      BigIntegers.asUnsignedByteArray(
                          32, ecPub.getQ().getAffineXCoord().toBigInteger())))
              .put(
                  "y",
                  b64url(
                      BigIntegers.asUnsignedByteArray(
                          32, ecPub.getQ().getAffineYCoord().toBigInteger())));

      ObjectNode header =
          MAPPER.createObjectNode().put("alg", "ES256").put("typ", "dpop+jwt").set("jwk", jwk);

      ObjectNode payload =
          MAPPER
              .createObjectNode()
              .put("jti", UUID.randomUUID().toString())
              .put("htm", htm)
              .put("htu", htu)
              .put("iat", epoch());
      if (athVal != null) payload.put("ath", athVal);

      String signingInput =
          b64url(MAPPER.writeValueAsString(header))
              + "."
              + b64url(MAPPER.writeValueAsString(payload));
      return signingInput + "." + signES256(signingInput, ecPriv);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new DpopException("Failed to serialize JWT parts", e);
    }
  }

  private static ECPrivateKeyParameters parseEcPrivateKey(String pem) throws DpopException {
    try (PEMParser parser = new PEMParser(new StringReader(pem))) {
      Object obj = parser.readObject();
      if (obj instanceof PrivateKeyInfo info) {
        return (ECPrivateKeyParameters) PrivateKeyFactory.createKey(info);
      }
      throw new DpopException(
          "Expected PKCS#8 PrivateKeyInfo, got: " + (obj == null ? "null" : obj.getClass()));
    } catch (IOException e) {
      throw new DpopException("Failed to parse PKCS#8 private key PEM", e);
    }
  }

  private static ECPublicKeyParameters parseEcPublicKey(String pem) throws DpopException {
    try (PEMParser parser = new PEMParser(new StringReader(pem))) {
      Object obj = parser.readObject();
      if (obj instanceof SubjectPublicKeyInfo info) {
        return (ECPublicKeyParameters) PublicKeyFactory.createKey(info);
      }
      throw new DpopException(
          "Expected SubjectPublicKeyInfo, got: " + (obj == null ? "null" : obj.getClass()));
    } catch (IOException e) {
      throw new DpopException("Failed to parse public key PEM", e);
    }
  }

  /**
   * Sign the UTF-8 bytes of {@code input} with ES256. Uses BC's {@link SHA256Digest} for hashing
   * and {@link ECDSASigner} for signing. Returns the raw (r‖s) signature, each component
   * zero-padded to 32 bytes, base64url-encoded.
   */
  private static String signES256(String input, ECPrivateKeyParameters ecPriv)
      throws DpopException {
    try {
      SHA256Digest digest = new SHA256Digest();
      byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
      digest.update(inputBytes, 0, inputBytes.length);
      byte[] hash = new byte[digest.getDigestSize()];
      digest.doFinal(hash, 0);

      ECDSASigner signer = new ECDSASigner();
      signer.init(true, ecPriv);
      BigInteger[] rs = signer.generateSignature(hash);

      return b64url(
          Arrays.concatenate(
              BigIntegers.asUnsignedByteArray(32, rs[0]),
              BigIntegers.asUnsignedByteArray(32, rs[1])));
    } catch (RuntimeException e) {
      throw new DpopException("Failed to produce ES256 signature", e);
    }
  }

  private static String b64url(byte[] data) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }

  private static String b64url(String s) {
    return b64url(s.getBytes(StandardCharsets.UTF_8));
  }

  private static long epoch() {
    return System.currentTimeMillis() / 1000;
  }

  /** Checked exception for DPoP helper failures. Wraps underlying IO / crypto errors. */
  public static class DpopException extends Exception {
    public DpopException(String message) {
      super(message);
    }

    public DpopException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
