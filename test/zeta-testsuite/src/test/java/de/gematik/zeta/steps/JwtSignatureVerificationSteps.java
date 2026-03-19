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
package de.gematik.zeta.steps;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import io.cucumber.java.de.Und;
import io.cucumber.java.en.And;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** Step definitions for verifying JWT signatures (ES256). */
@Slf4j
public class JwtSignatureVerificationSteps {

  @Und("verifiziere die ES256 Signatur des JWT {tigerResolvedString}")
  @And("verify the ES256 signature of JWT {tigerResolvedString}")
  public void verifyEs256Signature(String jwt) {
    verifyEs256SignatureInternal(jwt);
  }

  @Und("verifiziere die ES256 Signatur des JWT Tokens {string}")
  @And("verify the ES256 signature of JWT token {string}")
  public void verifyEs256SignatureFromString(String jwt) {
    String resolved = TigerGlobalConfiguration.resolvePlaceholders(jwt);
    verifyEs256SignatureInternal(resolved);
  }

  private void verifyEs256SignatureInternal(String jwtString) {
    try {
      SignedJWT signedJwt = SignedJWT.parse(jwtString);

      // Try cryptographic verification via x5c header (PoPP tokens include the certificate)
      List<com.nimbusds.jose.util.Base64> x5c = signedJwt.getHeader().getX509CertChain();
      if (x5c != null && !x5c.isEmpty()) {
        verifyCryptographically(signedJwt, x5c);
      } else {
        // Fallback: structural validation only (no public key available)
        verifyStructurally(jwtString);
      }

    } catch (Exception e) {
      throw new AssertionError("Failed to verify ES256 signature", e);
    }
  }

  /**
   * Cryptographic verification: extracts the EC public key from the x5c certificate chain and
   * verifies the JWT signature mathematically.
   */
  private void verifyCryptographically(SignedJWT signedJwt, List<com.nimbusds.jose.util.Base64> x5c)
      throws Exception {

    byte[] certBytes = x5c.getFirst().decode();
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    X509Certificate cert =
        (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
    ECPublicKey ecPublicKey = (ECPublicKey) cert.getPublicKey();

    JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);
    boolean valid = signedJwt.verify(verifier);

    log.info(
        "ES256 signature cryptographically verified against x5c certificate (valid: {})", valid);
    assertThat(valid).as("ES256 signature must be cryptographically valid").isTrue();
  }

  /**
   * Structural verification only: checks that the raw signature is 64 bytes (R‖S, each 32 bytes).
   * Used as fallback when no x5c header is present and thus no public key is available.
   */
  private void verifyStructurally(String jwtString) {
    String[] parts = jwtString.split("\\.");
    assertThat(parts).as("JWT should have 3 parts (header.payload.signature)").hasSize(3);

    byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
    log.info(
        "No x5c header present – falling back to structural check (signature length: {} bytes)",
        signatureBytes.length);
    assertThat(signatureBytes).as("ES256 signature should be 64 bytes (R=32 + S=32)").hasSize(64);
  }
}
