/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.zeta.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Factory to create {@code client_assertion} JWTs and DPoP proofs for Keycloak token exchange.
 *
 * <p>For DCR, the SMC-B certificate is included in the JWKS via {@code x5c} so Keycloak's ZeTA
 * extension can verify the client attestation. The {@code client_assertion} is signed with the
 * SMC-B key (brainpoolP256r1, alg=BP256R1). DPoP proofs use a generated EC P-256 key.
 */
@Slf4j
public final class KeycloakClientAssertionFactory {

  private static final ECKey EC_JWK;
  private static final ECPrivateKey EC_PRIVATE_KEY;
  private static final String SMCB_ALIAS = "alias";
  private static final String SMCB_PASSWORD = "00";

  static {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
    try {
      KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
      gen.initialize(256);
      KeyPair kp = gen.generateKeyPair();
      EC_PRIVATE_KEY = (ECPrivateKey) kp.getPrivate();
      ECPublicKey ecPub = (ECPublicKey) kp.getPublic();
      EC_JWK =
          new ECKey.Builder(Curve.P_256, ecPub)
              .privateKey(EC_PRIVATE_KEY)
              .keyID("zeta-e2e-" + UUID.randomUUID().toString().substring(0, 8))
              .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
              .algorithm(JWSAlgorithm.ES256)
              .build();
      log.info("Generated EC P-256 DPoP keypair, kid={}", EC_JWK.getKeyID());
    } catch (Exception e) {
      throw new ExceptionInInitializerError("Failed to generate EC keypair: " + e.getMessage());
    }
  }

  private KeycloakClientAssertionFactory() {}

  private static String getSmcbKeystorePath() {
    return TigerGlobalConfiguration.resolvePlaceholders(
        "${tiger.rootFolder|.}/doc/docker/backend/zeta/smcb-private/smcb_private.p12");
  }

  private static KeyStore loadSmcbKeyStore() {
    try {
      KeyStore ks = KeyStore.getInstance("PKCS12");
      try (var is = new FileInputStream(getSmcbKeystorePath())) {
        ks.load(is, SMCB_PASSWORD.toCharArray());
      }
      return ks;
    } catch (Exception e) {
      throw new AssertionError("Failed to load SMC-B keystore: " + e.getMessage(), e);
    }
  }

  // ───────────────────── client_assertion JWT (brainpoolP256r1) ─────────────────────

  /**
   * Creates a signed {@code client_assertion} JWT using the SMC-B key (brainpoolP256r1). The
   * signature is computed with SHA256withECDSA via BouncyCastle and serialized in JWS R||S format.
   */
  public static String createClientAssertion(String clientId, String audience) {
    try {
      KeyStore ks = loadSmcbKeyStore();
      PrivateKey privateKey = (PrivateKey) ks.getKey(SMCB_ALIAS, SMCB_PASSWORD.toCharArray());
      Certificate cert = ks.getCertificate(SMCB_ALIAS);

      var json = new com.fasterxml.jackson.databind.ObjectMapper();

      String certB64 = Base64.getEncoder().encodeToString(cert.getEncoded());
      var header = json.createObjectNode();
      header.put("alg", "BP256R1");
      header.put("typ", "JWT");
      header.putArray("x5c").add(certB64);

      Instant now = Instant.now();
      var payload = json.createObjectNode();
      payload.put("iss", clientId);
      payload.put("sub", clientId);
      payload.put("aud", audience);
      payload.put("jti", UUID.randomUUID().toString());
      payload.put("iat", now.getEpochSecond());
      payload.put("exp", now.plusSeconds(120).getEpochSecond());

      String headerB64 = b64url(json.writeValueAsBytes(header));
      String payloadB64 = b64url(json.writeValueAsBytes(payload));
      byte[] signingInput = (headerB64 + "." + payloadB64).getBytes();

      Signature sig = Signature.getInstance("SHA256withECDSA", "BC");
      sig.initSign(privateKey);
      sig.update(signingInput);
      byte[] jwsSig = derToConcat(sig.sign());

      String jwt = headerB64 + "." + payloadB64 + "." + b64url(jwsSig);
      log.info("Created SMC-B client_assertion for client_id={}", clientId);
      return jwt;
    } catch (Exception e) {
      throw new AssertionError("Failed to create client_assertion: " + e.getMessage(), e);
    }
  }

  // ───────────────────── DPoP proof JWT (EC P-256) ─────────────────────

  /** Creates a DPoP proof JWT (RFC 9449) signed with the generated EC P-256 key. */
  public static String createDpopProof(String httpMethod, String targetUri) {
    try {
      Instant now = Instant.now();
      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .jwtID(UUID.randomUUID().toString())
              .claim("htm", httpMethod)
              .claim("htu", targetUri)
              .issueTime(Date.from(now))
              .build();
      JWSHeader header =
          new JWSHeader.Builder(JWSAlgorithm.ES256)
              .type(new JOSEObjectType("dpop+jwt"))
              .jwk(EC_JWK.toPublicJWK())
              .build();
      SignedJWT jwt = new SignedJWT(header, claims);
      jwt.sign(new ECDSASigner(EC_PRIVATE_KEY));
      log.info("Created DPoP proof for {} {}", httpMethod, targetUri);
      return jwt.serialize();
    } catch (Exception e) {
      throw new AssertionError("Failed to create DPoP proof: " + e.getMessage(), e);
    }
  }

  // ───────────────────── DCR request body (x5c only) ─────────────────────

  /**
   * Builds a DCR request body with the SMC-B certificate in {@code x5c}. Keycloak extracts the
   * public key from the certificate – no explicit JWK curve parameters needed.
   */
  public static String createDcrRequestBody(String clientName) {
    try {
      KeyStore ks = loadSmcbKeyStore();
      X509Certificate cert = (X509Certificate) ks.getCertificate(SMCB_ALIAS);
      String certB64 = Base64.getEncoder().encodeToString(cert.getEncoded());
      String kid = b64url(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));

      String dcr =
          """
          {
            "client_name": "%s",
            "grant_types": ["urn:ietf:params:oauth:grant-type:token-exchange", "refresh_token"],
            "token_endpoint_auth_method": "private_key_jwt",
            "token_endpoint_auth_signing_alg": "BP256R1",
            "jwks": {
              "keys": [{
                "kty": "EC",
                "use": "sig",
                "kid": "%s",
                "x5c": ["%s"]
              }]
            }
          }
          """
              .formatted(clientName, kid, certB64);

      log.info("DCR request body created for client_name={}, kid={}", clientName, kid);
      return dcr;
    } catch (Exception e) {
      throw new AssertionError("Failed to build DCR request body: " + e.getMessage(), e);
    }
  }

  // ───────────────────── Helpers ─────────────────────

  private static String b64url(byte[] data) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
  }

  private static final int EC_COMPONENT_LEN = 32;

  static byte[] derToConcat(byte[] der) {
    int offset = 2;
    if (der[offset] != 0x02) throw new IllegalArgumentException("Expected INTEGER for R");
    offset++;
    int rLen = der[offset++] & 0xFF;
    byte[] r = new byte[rLen];
    System.arraycopy(der, offset, r, 0, rLen);
    offset += rLen;
    if (der[offset] != 0x02) throw new IllegalArgumentException("Expected INTEGER for S");
    offset++;
    int sLen = der[offset++] & 0xFF;
    byte[] s = new byte[sLen];
    System.arraycopy(der, offset, s, 0, sLen);
    byte[] result = new byte[EC_COMPONENT_LEN * 2];
    pad(r, result, 0);
    pad(s, result, EC_COMPONENT_LEN);
    return result;
  }

  private static void pad(byte[] src, byte[] dest, int off) {
    if (src.length <= EC_COMPONENT_LEN)
      System.arraycopy(src, 0, dest, off + EC_COMPONENT_LEN - src.length, src.length);
    else System.arraycopy(src, src.length - EC_COMPONENT_LEN, dest, off, EC_COMPONENT_LEN);
  }
}
