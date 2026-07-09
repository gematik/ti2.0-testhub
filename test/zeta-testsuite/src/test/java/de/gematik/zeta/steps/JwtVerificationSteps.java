/*-
 * #%L
 * ZeTA Testsuite
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
package de.gematik.zeta.steps;

import static de.gematik.ti20.rbel.fluent.RbelFluentApi.assertCurrentRequest;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.data.RbelElement;
import io.cucumber.java.de.Und;
import io.cucumber.java.en.And;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Cucumber Steps for JWT ES256 signature verification.
 *
 * <h3>Warum eigene Verification statt RBel?</h3>
 *
 * <p>Tiger/RBel bietet über {@code $.signature.isValid} eine eingebaute JWT-Signaturprüfung an. In
 * der Theorie genügt es, die Keystores über {@code keyFolders} in tiger.yaml bereitzustellen. In
 * der Praxis liefert RBel (Tiger 4.2.4) für unsere JWTs jedoch {@code isValid=null}:
 *
 * <ol>
 *   <li><b>brainpoolP256r1 (subject_token):</b> RBel nutzt intern jose4j, das brainpool-Kurven
 *       nicht unterstützt. Auch der x5c-Fallback in {@code RbelJwtConverter} scheitert daran, weil
 *       {@code JsonWebSignature.verifySignature()} eine {@code JoseException} wirft.
 *   <li><b>P-256 mit JWK im Header (client_assertion):</b> RBel prüft nur Keys aus {@code
 *       keyFolders} und x5c – <em>nicht</em> das eingebettete JWK aus dem JWT-Header. Die Keys aus
 *       zetakeystore.p12 werden zwar geladen, der Abgleich über jose4j schlägt aber fehl
 *       (vermutlich weil jose4j PrivateKeys nicht zur Verifikation nutzen kann und der zugehörige
 *       PublicKey nicht separat im {@code RbelKeyManager} landet).
 * </ol>
 *
 * <p><b>Strategie:</b> Zuerst wird RBels {@code $.signature.isValid} geprüft. Nur wenn dort {@code
 * null} steht (= RBel konnte nicht verifizieren), wird auf JWK/x5c aus dem JWT-Header
 * zurückgegriffen. Sobald Tiger/jose4j brainpool unterstützt und der KeyFolder-Abgleich
 * funktioniert, können diese Steps durch eingebaute TGR-Steps ersetzt werden:
 *
 * <pre>{@code
 * Und TGR prüfe aktueller Request stimmt im Knoten
 *     "$.body.client_assertion.signature.isValid" überein mit "true"
 * }</pre>
 */
@Slf4j
public class JwtVerificationSteps {

  /**
   * Verifies the signature of a JWT extracted from the current request at the given RBel path.
   * Prefers RBel's built-in verification, falls back to JWK/x5c header verification.
   */
  @Und("verifiziere die ES256 Signatur des JWT aus dem aktuellen Request Knoten {string}")
  @And("verify the ES256 signature of the JWT from current request node {string}")
  public void verifyJwtSignatureFromCurrentRequest(String rbelPath) {
    RbelElement jwtElement = assertCurrentRequest().elementAtPath(rbelPath);

    // Try RBel built-in verification first
    if (tryRbelVerification(jwtElement, rbelPath)) {
      return;
    }

    // Fallback: verify using JWK/x5c from the JWT header
    String jwt = jwtElement.getRawStringContent();
    assertThat(jwt).as("JWT string must not be empty").isNotBlank();
    verifyJwtWithHeaderKey(jwt);
  }

  /**
   * Verifies the ES256 signature of a JWT passed as a variable string. Looks it up in the current
   * request's known paths for RBel verification, falls back to JWK/x5c.
   */
  @Und("verifiziere die ES256 Signatur des JWT Tokens {string}")
  @And("verify the ES256 signature of JWT token {string}")
  public void verifyJwtSignatureFromVariable(String jwt) {
    String resolvedJwt =
        de.gematik.test.tiger.common.config.TigerGlobalConfiguration.resolvePlaceholders(jwt);

    var request = assertCurrentRequest();
    for (String path :
        List.of(
            "$.header.PoPP",
            "$.header.Authorization",
            "$.body.client_assertion",
            "$.body.subject_token",
            "$.body.access_token")) {
      String rawContent;
      RbelElement element;
      try {
        element = request.elementAtPath(path);
        rawContent = element.getRawStringContent();
      } catch (AssertionError e) {
        log.debug("Path {} not found in current request, skipping: {}", path, e.getMessage());
        continue;
      }
      String comparableRaw =
          rawContent != null && rawContent.startsWith("Bearer ")
              ? rawContent.substring(7)
              : rawContent;
      if (resolvedJwt.equals(comparableRaw) || resolvedJwt.equals(rawContent)) {
        if (tryRbelVerification(element, path)) {
          return;
        }
      }
    }

    // Fallback: verify the JWT directly
    verifyJwtWithHeaderKey(resolvedJwt);
  }

  /** Tries to verify using RBel's built-in $.signature.isValid. Returns true if verified. */
  private boolean tryRbelVerification(RbelElement jwtElement, String context) {
    List<RbelElement> isValidNodes = jwtElement.findRbelPathMembers("$.signature.isValid");
    if (!isValidNodes.isEmpty()) {
      String isValid = isValidNodes.getFirst().getRawStringContent();
      if ("true".equalsIgnoreCase(isValid)) {
        List<RbelElement> verifiedByNodes =
            jwtElement.findRbelPathMembers("$.signature.verifiedUsing");
        String verifiedBy =
            verifiedByNodes.isEmpty()
                ? "unknown"
                : verifiedByNodes.getFirst().getRawStringContent();
        log.info("JWT at {} verified by RbelParser (key: {})", context, verifiedBy);
        return true;
      }
    }
    log.info(
        "RBel could not verify JWT at {} (isValid={}), falling back to header key verification",
        context,
        isValidNodes.isEmpty() ? "absent" : isValidNodes.getFirst().getRawStringContent());
    return false;
  }

  /**
   * Verifies a JWT signature using the key material from its own header (JWK or x5c). This is the
   * fallback when RBel's keyFolders don't contain the matching key.
   */
  private void verifyJwtWithHeaderKey(String jwt) {
    try {
      com.nimbusds.jwt.SignedJWT signedJwt = com.nimbusds.jwt.SignedJWT.parse(jwt.trim());
      assertThat(signedJwt.getHeader().getAlgorithm())
          .as("JWT must use ES256")
          .isEqualTo(com.nimbusds.jose.JWSAlgorithm.ES256);

      // Try JWK first
      com.nimbusds.jose.jwk.JWK jwk = signedJwt.getHeader().getJWK();
      if (jwk != null) {
        var ecKey = com.nimbusds.jose.jwk.ECKey.parse(jwk.toJSONObject());
        boolean valid = signedJwt.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(ecKey));
        assertThat(valid).as("ES256 signature verification with JWK must succeed").isTrue();
        log.info("ES256 signature verified via JWK (curve: {})", ecKey.getCurve());
        return;
      }

      // Try x5c
      var x5c = signedJwt.getHeader().getX509CertChain();
      if (x5c != null && !x5c.isEmpty()) {
        verifyWithX5c(signedJwt, x5c);
        return;
      }

      // kid-only token (e.g. a PoPP token: header has only kid/typ/alg). Resolve the public key
      // via OpenID-Federation discovery of the token issuer:
      //   <iss>/.well-known/openid-federation  → metadata.oauth_resource.signed_jwks_uri
      //   → signed-jwks (JWS of type jwk-set+jwt) → keys[] → pick by kid → verify ES256.
      String kid = signedJwt.getHeader().getKeyID();
      String iss = signedJwt.getJWTClaimsSet().getStringClaim("iss");
      if (kid != null && iss != null) {
        verifyViaFederationDiscovery(signedJwt, iss, kid);
        return;
      }

      throw new AssertionError(
          "JWT has neither jwk, x5c nor a resolvable kid/iss in the header for verification");
    } catch (Exception e) {
      throw new AssertionError("JWT signature verification failed: " + e.getMessage(), e);
    }
  }

  /**
   * Resolves the signing key of a {@code kid}-only JWT (e.g. a PoPP token) via OpenID-Federation
   * discovery of its issuer and verifies the ES256 signature:
   *
   * <ol>
   *   <li>GET {@code <iss>/.well-known/openid-federation} (Entity Statement, a JWS).
   *   <li>Read {@code metadata.oauth_resource.signed_jwks_uri}.
   *   <li>GET the signed JWKS (a JWS of type {@code jwk-set+jwt}) and extract its {@code keys}.
   *   <li>Pick the EC key whose {@code kid} matches the token header and verify the signature.
   * </ol>
   */
  private void verifyViaFederationDiscovery(
      com.nimbusds.jwt.SignedJWT signedJwt, String issuer, String kid) throws Exception {
    String entityStatementJwt = httpGet(issuer + "/.well-known/openid-federation");
    var esClaims = com.nimbusds.jwt.SignedJWT.parse(entityStatementJwt).getJWTClaimsSet();

    @SuppressWarnings("unchecked")
    var metadata = (java.util.Map<String, Object>) esClaims.getClaim("metadata");
    assertThat(metadata).as("entity statement must contain a metadata block").isNotNull();
    @SuppressWarnings("unchecked")
    var oauthResource = (java.util.Map<String, Object>) metadata.get("oauth_resource");
    assertThat(oauthResource)
        .as("entity statement metadata must contain oauth_resource")
        .isNotNull();
    String signedJwksUri = (String) oauthResource.get("signed_jwks_uri");
    assertThat(signedJwksUri)
        .as("oauth_resource must contain a signed_jwks_uri")
        .isNotBlank()
        .startsWith("https://");

    String signedJwksJwt = httpGet(signedJwksUri);
    var jwksClaims = com.nimbusds.jwt.SignedJWT.parse(signedJwksJwt).getJWTClaimsSet();
    var jwkSet = com.nimbusds.jose.jwk.JWKSet.parse(jwksClaims.toJSONObject());

    com.nimbusds.jose.jwk.JWK jwk = jwkSet.getKeyByKeyId(kid);
    assertThat(jwk)
        .as("signed JWKS must contain a key with kid=%s (federation discovery)", kid)
        .isNotNull();

    var ecKey = jwk.toECKey();
    boolean valid = signedJwt.verify(new com.nimbusds.jose.crypto.ECDSAVerifier(ecKey));
    assertThat(valid)
        .as("ES256 signature verification via federation-discovered key (kid=%s) must succeed", kid)
        .isTrue();
    log.info(
        "ES256 signature verified via OpenID-Federation discovery (iss={}, kid={}, jwksUri={})",
        issuer,
        kid,
        signedJwksUri);
  }

  /** Simple direct HTTPS GET (public discovery endpoints, valid public TLS certs). */
  private String httpGet(String url) throws Exception {
    var client =
        java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(20))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();
    var request =
        java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(20))
            .GET()
            .build();
    var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as("GET %s must return 200", url).isEqualTo(200);
    return response.body().trim();
  }

  /** Verifies using x5c certificate (supports brainpoolP256r1 via BouncyCastle). */
  private void verifyWithX5c(
      com.nimbusds.jwt.SignedJWT signedJwt, List<com.nimbusds.jose.util.Base64> x5c) {
    if (java.security.Security.getProvider("BC") == null) {
      java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    try {
      byte[] certBytes = x5c.getFirst().decode();
      var cf = java.security.cert.CertificateFactory.getInstance("X.509");
      var cert =
          (java.security.cert.X509Certificate)
              cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
      var publicKey = (java.security.interfaces.ECPublicKey) cert.getPublicKey();

      // Sign using BouncyCastle (handles both P-256 and brainpoolP256r1)
      String[] parts = signedJwt.serialize().split("\\.");
      byte[] signingInput =
          (parts[0] + "." + parts[1]).getBytes(java.nio.charset.StandardCharsets.UTF_8);
      byte[] rawSig = com.nimbusds.jose.util.Base64URL.from(parts[2]).decode();
      byte[] derSig = convertRawToDer(rawSig);

      java.security.Signature sig = java.security.Signature.getInstance("SHA256withECDSA", "BC");
      sig.initVerify(publicKey);
      sig.update(signingInput);
      boolean valid = sig.verify(derSig);
      assertThat(valid).as("ES256 signature verification with x5c must succeed").isTrue();
      log.info("ES256 signature verified via x5c (BouncyCastle)");
    } catch (Exception e) {
      throw new AssertionError("x5c signature verification failed: " + e.getMessage(), e);
    }
  }

  private byte[] convertRawToDer(byte[] rawSignature) {
    int halfLen = rawSignature.length / 2;
    var r = new java.math.BigInteger(1, java.util.Arrays.copyOfRange(rawSignature, 0, halfLen));
    var s =
        new java.math.BigInteger(
            1, java.util.Arrays.copyOfRange(rawSignature, halfLen, rawSignature.length));
    var v = new org.bouncycastle.asn1.ASN1EncodableVector();
    v.add(new org.bouncycastle.asn1.ASN1Integer(r));
    v.add(new org.bouncycastle.asn1.ASN1Integer(s));
    try {
      return new org.bouncycastle.asn1.DERSequence(v).getEncoded();
    } catch (java.io.IOException e) {
      throw new AssertionError("Failed to encode DER signature: " + e.getMessage(), e);
    }
  }
}
