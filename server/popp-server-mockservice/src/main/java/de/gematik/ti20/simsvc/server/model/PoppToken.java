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
package de.gematik.ti20.simsvc.server.model;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;

@Slf4j
public class PoppToken {

  // Header of the PoPP token
  public static class TokenHeader {

    private String typ = "vnd.telematik.popp+jwt";
    private String alg = "ES256";
    private String kid;
    private List<String> x5c;

    public TokenHeader(String typ, String alg, String kid, List<String> x5c) {
      this.typ = typ;
      this.alg = alg;
      this.kid = kid;
      this.x5c = x5c;
    }

    public TokenHeader(String kid) {
      this.kid = kid;
    }

    public String getTyp() {
      return typ;
    }

    public String getAlg() {
      return alg;
    }

    public String getKid() {
      return kid;
    }

    public List<String> getX5c() {
      return x5c;
    }
  }

  // Claims of the PoPP token
  public static class TokenClaims {

    private String version = "1.0.0";
    private String iss = "https://popp.example.com";

    private String proofMethod;
    private long patientProofTime;
    private long iat;
    private String patientId;
    private String insurerId;
    private String actorId;
    private String actorProfessionOid;

    public TokenClaims(
        final String version,
        final String iss,
        final long iat,
        final String proofMethod,
        final long patientProofTime,
        final String patientId,
        final String insurerId,
        final String actorId,
        final String actorProfessionOid) {
      this.version = version;
      this.iss = iss;
      this.iat = iat;
      this.proofMethod = proofMethod;
      this.patientProofTime = patientProofTime;
      this.patientId = patientId;
      this.insurerId = insurerId;
      this.actorId = actorId;
      this.actorProfessionOid = actorProfessionOid;
    }

    public TokenClaims(
        final String proofMethod,
        final long patientProofTime,
        final long iat,
        final String patientId,
        final String insurerId,
        final String actorId,
        final String actorProfessionOid) {
      this.proofMethod = proofMethod;
      this.patientProofTime = patientProofTime;
      this.iat = iat;
      this.patientId = patientId;
      this.insurerId = insurerId;
      this.actorId = actorId;
      this.actorProfessionOid = actorProfessionOid;
    }

    public String getVersion() {
      return version;
    }

    public String getIss() {
      return iss;
    }

    public long getIat() {
      return iat;
    }

    public String getProofMethod() {
      return proofMethod;
    }

    public long getPatientProofTime() {
      return patientProofTime;
    }

    public String getPatientId() {
      return patientId;
    }

    public String getInsurerId() {
      return insurerId;
    }

    public String getActorId() {
      return actorId;
    }

    public String getActorProfessionOid() {
      return actorProfessionOid;
    }
  }

  private TokenHeader header;
  private TokenClaims claims;

  // Constructor for data-based token
  public PoppToken(TokenHeader header, TokenClaims claims) {
    this.header = header;
    this.claims = claims;
  }

  // Constructor for JWT-based token (private, only accessible via fromJwt())
  private PoppToken() {}

  // Return header and claims
  public TokenHeader getHeader() {
    return header;
  }

  public TokenClaims getClaims() {
    return claims;
  }

  // Generate JWT from the given data
  public String toJwt(KeyStore keyStore, String alias, char[] keyPassword) throws Exception {
    log.debug("Starting JWT generation for alias: {}", alias);

    // Extract private key and certificate from the KeyStore
    log.debug("Extracting private key and certificate from the KeyStore...");

    final PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPassword);
    final X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

    // Convert certificate to Base64-encoded string for the x5c header
    String x5c;
    try {
      x5c = Base64.getEncoder().encodeToString(certificate.getEncoded());
      log.debug("Certificate successfully encoded for x5c header.");
    } catch (CertificateEncodingException e) {
      log.error("Error encoding the certificate for x5c header.", e);
      throw new CertificateException(
          "Error encoding the certificate from the KeyStore for the x5c header", e);
    }

    // Define JWT claims
    log.debug("Defining JWT claims...");
    JwtClaims jwtClaims = new JwtClaims();
    jwtClaims.setIssuedAt(NumericDate.fromSeconds(claims.getIat()));

    jwtClaims.setClaim("version", claims.getVersion());
    jwtClaims.setIssuer(claims.getIss());
    jwtClaims.setClaim("proofMethod", claims.getProofMethod());
    jwtClaims.setClaim("patientProofTime", claims.getPatientProofTime());
    jwtClaims.setClaim("patientId", claims.getPatientId());
    jwtClaims.setClaim("insurerId", claims.getInsurerId());
    jwtClaims.setClaim("actorId", claims.getActorId());
    jwtClaims.setClaim("actorProfessionOid", claims.getActorProfessionOid());

    // Create the signature
    log.debug("Creating JWT signature...");
    JsonWebSignature jws = new JsonWebSignature();
    jws.setPayload(jwtClaims.toJson());
    jws.setKey(privateKey);
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
    jws.setHeader("typ", header.getTyp());
    jws.setHeader("kid", header.getKid());
    jws.setHeader("x5c", List.of(x5c));

    String jwt = jws.getCompactSerialization();
    log.debug("JWT generation completed successfully.");
    return jwt;
  }

  // Parse JWT and create a PoppToken object
  public static PoppToken fromJwt(String jwt, KeyStore keyStore) throws Exception {
    log.debug("Starting JWT parsing and validation...");

    // Extract the certificate chain from the JWT
    JsonWebSignature jws = new JsonWebSignature();
    jws.setCompactSerialization(jwt);

    // Extract the certificate chain from the x5c header
    log.debug("Extracting certificate chain from the x5c header...");
    List<X509Certificate> certs = jws.getCertificateChainHeaderValue();
    if (certs == null || certs.isEmpty()) {
      log.error("No certificates found in the JWT x5c header.");
      throw new CertificateException("No certificates found in the JWT x5c header.");
    }

    // Validate the certificate chain against the KeyStore
    log.debug("Validating certificate chain against the KeyStore...");
    validateCertificateChain(certs, keyStore);

    // Configure the resolver with the extracted certificates from x5c
    log.debug("Configuring X509VerificationKeyResolver...");
    X509VerificationKeyResolver resolver =
        new X509VerificationKeyResolver(certs.toArray(new X509Certificate[0]));
    resolver.setTryAllOnNoThumbHeader(true);

    // Create a JwtConsumer with the resolver
    log.debug("Creating JwtConsumer...");
    JwtConsumer jwtConsumer = new JwtConsumerBuilder().setVerificationKeyResolver(resolver).build();

    // Verify the JWT and extract the claims
    JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
    log.debug("JWT successfully verified.");

    // Validate required claims
    if (jwtClaims.getStringClaimValue("version") == null) {
      log.error("The claim 'version' is missing in the JWT.");
      throw new RuntimeException("The claim 'version' is missing in the JWT.");
    }
    if (jwtClaims.getIssuer() == null) {
      log.error("The claim 'iss' (Issuer) is missing in the JWT.");
      throw new RuntimeException("The claim 'iss' (Issuer) is missing in the JWT.");
    }

    // Create TokenClaims
    log.debug("Creating TokenClaims...");
    TokenClaims claims =
        new TokenClaims(
            jwtClaims.getStringClaimValue("version"),
            jwtClaims.getIssuer(),
            jwtClaims.getIssuedAt().getValueInMillis(),
            jwtClaims.getStringClaimValue("proofMethod"),
            (Long) jwtClaims.getClaimValue("patientProofTime"),
            jwtClaims.getStringClaimValue("patientId"),
            jwtClaims.getStringClaimValue("insurerId"),
            jwtClaims.getStringClaimValue("actorId"),
            jwtClaims.getStringClaimValue("actorProfessionOid"));

    // Create TokenHeader
    log.debug("Creating TokenHeader...");
    TokenHeader header =
        new TokenHeader(
            jws.getHeader("typ"),
            jws.getAlgorithmHeaderValue(),
            jws.getKeyIdHeaderValue(),
            certs.stream()
                .map(
                    cert -> {
                      try {
                        return Base64.getEncoder().encodeToString(cert.getEncoded());
                      } catch (CertificateEncodingException e) {
                        log.error(
                            "Error encoding a certificate from the JWT certificate chain.", e);
                        throw new RuntimeException(
                            "Error encoding a certificate from the JWT certificate chain", e);
                      }
                    })
                .toList());

    log.debug("JWT parsing and validation completed successfully.");
    return new PoppToken(header, claims);
  }

  // Validate the certificate chain against trusted certificates in the KeyStore
  private static void validateCertificateChain(List<X509Certificate> certs, KeyStore keyStore)
      throws Exception {
    log.debug("Extracting trusted certificates from the KeyStore...");
    List<X509Certificate> trustedCerts = new ArrayList<>();
    Enumeration<String> aliases = keyStore.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      Certificate cert = keyStore.getCertificate(alias);
      if (cert instanceof X509Certificate) {
        trustedCerts.add((X509Certificate) cert);
      }
    }

    // Check if at least one certificate in the chain is trusted
    log.debug("Checking if the certificate chain is trusted...");
    boolean isValid = certs.stream().anyMatch(trustedCerts::contains);
    if (!isValid) {
      log.error("The certificate chain in the JWT is not trusted.");
      throw new CertificateException(
          "The certificate chain in the JWT is not trusted. Ensure that the KeyStore contains the appropriate trusted certificates.");
    }
    log.debug("Certificate chain successfully validated.");
  }
}
