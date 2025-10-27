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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;

@Slf4j
public class AccessToken {

  // Header of the AccessToken
  public static class TokenHeader {

    private String typ = "vnd.telematik.access+jwt";
    private String alg = "ES256";
    private String kid;

    public TokenHeader(String typ, String alg, String kid) {
      this.typ = typ;
      this.alg = alg;
      this.kid = kid;
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
  }

  // Claims of the AccessToken
  public static class TokenClaims {

    private String iss; // The issuer of the token
    private List<String> aud; // Intended recipients (audiences)
    private String sub; // Subject of the token
    private String clientId; // Client identifier
    private String scope; // Permissions granted by the token
    private String jkt; // SHA-256 hash of the public key (in cnf claim)
    private String
        professionOid; // TODO: Profession OID wird temporär übertragen, weil wir aktuell keine DB

    // im Mock haben

    public TokenClaims(
        String iss,
        List<String> aud,
        String sub,
        String clientId,
        String scope,
        String jkt,
        String professionOid) {
      this.iss = iss;
      this.aud = aud;
      this.sub = sub;
      this.clientId = clientId;
      this.scope = scope;
      this.jkt = jkt;
      this.professionOid = professionOid;
    }

    public String getIss() {
      return iss;
    }

    public List<String> getAud() {
      return aud;
    }

    public String getSub() {
      return sub;
    }

    public String getClientId() {
      return clientId;
    }

    public String getScope() {
      return scope;
    }

    public String getJkt() {
      return jkt;
    }

    public String getProfessionOid() {
      return professionOid;
    }
  }

  private TokenHeader header;
  private TokenClaims claims;

  // Constructor for data-based token
  public AccessToken(TokenHeader header, TokenClaims claims) {
    this.header = header;
    this.claims = claims;
  }

  // Constructor for JWT-based token (private, only accessible via fromJwt())
  private AccessToken() {}

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
    PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, keyPassword);
    X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

    // Define JWT claims
    JwtClaims jwtClaims = new JwtClaims();
    jwtClaims.setIssuer(claims.getIss());
    jwtClaims.setExpirationTimeMinutesInTheFuture(5); // TODO: need a setting
    jwtClaims.setAudience(claims.getAud());
    jwtClaims.setSubject(claims.getSub());
    jwtClaims.setClaim("client_id", claims.getClientId());
    jwtClaims.setIssuedAtToNow();
    jwtClaims.setGeneratedJwtId();
    jwtClaims.setClaim("scope", claims.getScope());
    jwtClaims.setClaim("profession_oid", claims.getProfessionOid());

    // Create the "cnf" claim as a nested object
    Map<String, String> cnfClaim = new HashMap<>();
    cnfClaim.put("jkt", claims.getJkt()); // Add "jkt" to the "cnf" object
    jwtClaims.setClaim("cnf", cnfClaim); // Add the "cnf" object to the claims

    // Create the signature
    JsonWebSignature jws = new JsonWebSignature();
    jws.setPayload(jwtClaims.toJson());
    jws.setKey(privateKey);
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
    jws.setHeader("typ", header.getTyp());
    jws.setHeader("kid", header.getKid());

    String jwt = jws.getCompactSerialization();
    log.debug("JWT generation completed successfully.");
    return jwt;
  }

  // Parse JWT and create an AccessToken object
  public static AccessToken fromJwt(String jwt, KeyStore keyStore) throws Exception {
    log.debug("Starting JWT parsing and validation...");

    JsonWebSignature jws = new JsonWebSignature();
    jws.setCompactSerialization(jwt);

    // Extract trusted certificates from the KeyStore
    X509Certificate[] trustedCertsArray = extractTrustedCertificates(keyStore);

    // Create the resolver with the trusted certificates
    X509VerificationKeyResolver resolver = new X509VerificationKeyResolver(trustedCertsArray);
    resolver.setTryAllOnNoThumbHeader(true);

    // Create a JwtConsumer with the resolver
    JwtConsumer jwtConsumer =
        new JwtConsumerBuilder()
            .setVerificationKeyResolver(resolver)
            .setSkipDefaultAudienceValidation() // DAS WIRD IM MOCK ERSTMAL DEAKTIVIERT
            .build();

    // Verify the JWT and extract the claims
    JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);
    log.debug("JWT successfully verified.");

    @SuppressWarnings("unchecked")
    Map<String, String> cnfClaim = (Map<String, String>) jwtClaims.getClaimValue("cnf");
    String jkt = cnfClaim.get("jkt");

    // Create TokenClaims
    TokenClaims claims =
        new TokenClaims(
            jwtClaims.getIssuer(),
            jwtClaims.getStringListClaimValue("aud"),
            jwtClaims.getSubject(),
            jwtClaims.getStringClaimValue("client_id"),
            jwtClaims.getStringClaimValue("scope"),
            jkt,
            jwtClaims.getStringClaimValue("profession_oid"));

    // Create TokenHeader
    TokenHeader header =
        new TokenHeader(
            jws.getHeader("typ"), jws.getAlgorithmHeaderValue(), jws.getKeyIdHeaderValue());

    log.debug("JWT parsing and validation completed successfully.");
    return new AccessToken(header, claims);
  }

  private static X509Certificate[] extractTrustedCertificates(KeyStore keyStore) throws Exception {
    List<X509Certificate> trustedCertificates = new ArrayList<>();
    Enumeration<String> aliases = keyStore.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      Certificate cert = keyStore.getCertificate(alias);
      if (cert instanceof X509Certificate) {
        trustedCertificates.add((X509Certificate) cert);
      }
    }
    return trustedCertificates.toArray(new X509Certificate[0]);
  }
}
