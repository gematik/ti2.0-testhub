/*-
 * #%L
 * VSDM 2.0 Testsuite
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DpopHelperTest {

  private static String privateKeyPem;
  private static String publicKeyPem;

  @BeforeAll
  static void loadKeys() throws Exception {
    try {
      privateKeyPem =
          Files.readString(
              Path.of(
                  Objects.requireNonNull(
                          DpopHelperTest.class.getResource("/crypto/test-ec-private.pem"))
                      .toURI()));
      publicKeyPem =
          Files.readString(
              Path.of(
                  Objects.requireNonNull(
                          DpopHelperTest.class.getResource("/crypto/test-ec-public.pem"))
                      .toURI()));
    } catch (Exception e) {
      var provider = new BouncyCastleProvider();
      KeyPairGenerator g = KeyPairGenerator.getInstance("EC", provider);
      g.initialize(256);
      KeyPair pair = g.generateKeyPair();
      String priv = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
      String pub = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
      privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + priv + "\n-----END PRIVATE KEY-----\n";
      publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + pub + "\n-----END PUBLIC KEY-----\n";
    }
  }

  @Test
  void createDpop_withInvalidPrivateKeyPem_throws() {
    String invalidPem =
        "-----BEGIN PRIVATE KEY-----\nNOT_VALID_BASE64!!!\n-----END PRIVATE KEY-----\n";
    assertThatThrownBy(
            () -> DpopHelper.createDpop(publicKeyPem, invalidPem, "GET", "https://url", null))
        .isInstanceOf(Exception.class);
  }

  @Test
  void createDpop_withPrivateKeyAsPublicKey_throws() {
    assertThatThrownBy(
            () -> DpopHelper.createDpop(privateKeyPem, privateKeyPem, "GET", "https://url", null))
        .isInstanceOf(Exception.class);
  }

  @Test
  void createDpop_header_jwk_coordinatesAreValidP256() throws Exception {
    String dpop =
        DpopHelper.createDpop(publicKeyPem, privateKeyPem, "GET", "https://example.com", null);
    JsonNode jwk = new ObjectMapper().readTree(decodeBase64Url(dpop.split("\\.")[0])).get("jwk");

    // BigIntegers.asUnsignedByteArray(32, ...) always produces exactly 32 bytes
    assertThat(Base64.getUrlDecoder().decode(jwk.get("x").asText())).hasSize(32);
    assertThat(Base64.getUrlDecoder().decode(jwk.get("y").asText())).hasSize(32);
  }

  @Test
  void createDpop_returnsTwoDotsJwt() throws Exception {
    String dpop =
        DpopHelper.createDpop(
            publicKeyPem, privateKeyPem, "GET", "https://example.com/resource", null);

    assertThat(dpop).isNotBlank();
    assertThat(dpop.split("\\.")).hasSize(3);
  }

  @Test
  void createDpop_header_hasCorrectTypAndAlg() throws Exception {
    String dpop =
        DpopHelper.createDpop(
            publicKeyPem, privateKeyPem, "GET", "https://example.com/resource", null);
    JsonNode header = new ObjectMapper().readTree(decodeBase64Url(dpop.split("\\.")[0]));

    assertThat(header.get("alg").asText()).isEqualTo("ES256");
    assertThat(header.get("typ").asText()).isEqualTo("dpop+jwt");
    assertThat(header.get("jwk")).isNotNull();
  }

  @Test
  void createDpop_header_jwk_hasCorrectKtyAndCrv() throws Exception {
    String dpop =
        DpopHelper.createDpop(
            publicKeyPem, privateKeyPem, "GET", "https://example.com/resource", null);
    JsonNode jwk = new ObjectMapper().readTree(decodeBase64Url(dpop.split("\\.")[0])).get("jwk");

    assertThat(jwk.get("kty").asText()).isEqualTo("EC");
    assertThat(jwk.get("crv").asText()).isEqualTo("P-256");
    assertThat(jwk.get("alg").asText()).isEqualTo("ES256");
    assertThat(jwk.get("x").asText()).isNotBlank();
    assertThat(jwk.get("y").asText()).isNotBlank();
  }

  @Test
  void createDpop_payload_hasRequiredClaims() throws Exception {
    String htm = "POST";
    String htu = "https://example.com/token";
    String dpop = DpopHelper.createDpop(publicKeyPem, privateKeyPem, htm, htu, null);
    JsonNode payload = new ObjectMapper().readTree(decodeBase64Url(dpop.split("\\.")[1]));

    assertThat(payload.get("jti").asText()).isNotBlank();
    assertThat(payload.get("htm").asText()).isEqualTo(htm);
    assertThat(payload.get("htu").asText()).isEqualTo(htu);
    assertThat(payload.get("iat").asLong()).isGreaterThan(0);
    assertThat(payload.has("ath")).isFalse();
  }

  @Test
  void createDpop_withAth_payloadContainsAth() throws Exception {
    String athValue = "someHashValue123";
    String dpop =
        DpopHelper.createDpop(publicKeyPem, privateKeyPem, "GET", "https://example.com", athValue);
    JsonNode payload = new ObjectMapper().readTree(decodeBase64Url(dpop.split("\\.")[1]));

    assertThat(payload.get("ath").asText()).isEqualTo(athValue);
  }

  @Test
  void createDpop_signatureIsValidES256() throws Exception {
    String dpop =
        DpopHelper.createDpop(
            publicKeyPem, privateKeyPem, "GET", "https://example.com/resource", null);
    String[] parts = dpop.split("\\.");

    String signingInput = parts[0] + "." + parts[1];
    byte[] derSig = rawToDer(Base64.getUrlDecoder().decode(parts[2]));

    byte[] keyBytes =
        Base64.getDecoder()
            .decode(publicKeyPem.replaceAll("-----[^-]+-----", "").replaceAll("\\s+", ""));
    PublicKey publicKey =
        KeyFactory.getInstance("EC", new BouncyCastleProvider())
            .generatePublic(new X509EncodedKeySpec(keyBytes));

    Signature verifier = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
    verifier.initVerify(publicKey);
    verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));

    assertThat(verifier.verify(derSig)).isTrue();
  }

  @Test
  void createDpop_eachCallProducesDifferentJti() throws Exception {
    String dpop1 =
        DpopHelper.createDpop(publicKeyPem, privateKeyPem, "GET", "https://example.com", null);
    String dpop2 =
        DpopHelper.createDpop(publicKeyPem, privateKeyPem, "GET", "https://example.com", null);

    String jti1 =
        new ObjectMapper().readTree(decodeBase64Url(dpop1.split("\\.")[1])).get("jti").asText();
    String jti2 =
        new ObjectMapper().readTree(decodeBase64Url(dpop2.split("\\.")[1])).get("jti").asText();

    assertThat(jti1).isNotEqualTo(jti2);
  }

  @Test
  void createDpop_withSpecialCharactersInHtu_encodesCorrectly() throws Exception {
    String htu = "https://example.com/path?query=val&other=@#$";
    String dpop = DpopHelper.createDpop(publicKeyPem, privateKeyPem, "GET", htu, null);
    JsonNode payload = new ObjectMapper().readTree(decodeBase64Url(dpop.split("\\.")[1]));

    assertThat(payload.get("htu").asText()).isEqualTo(htu);
  }

  @Test
  void createDpop_nullArguments_throwsException() {
    assertThatThrownBy(() -> DpopHelper.createDpop(null, privateKeyPem, "GET", "https://url", null))
        .isInstanceOf(Exception.class);
    assertThatThrownBy(() -> DpopHelper.createDpop(publicKeyPem, null, "GET", "https://url", null))
        .isInstanceOf(Exception.class);
  }

  private static String decodeBase64Url(String encoded) {
    return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
  }

  private static byte[] rawToDer(byte[] raw) throws IOException {
    BigInteger r = new BigInteger(1, Arrays.copyOfRange(raw, 0, 32));
    BigInteger s = new BigInteger(1, Arrays.copyOfRange(raw, 32, 64));
    return new DERSequence(new ASN1Integer[] {new ASN1Integer(r), new ASN1Integer(s)}).getEncoded();
  }
}
