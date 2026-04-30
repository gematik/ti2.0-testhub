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
/*-
 * #%L
 * ZETA Testsuite
 * %%
 * (C) achelos GmbH, 2025, licensed for gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Factory to obtain a valid Access Token from the Docker-based ZETA-PDP (Keycloak) via OAuth 2.0
 * Token Exchange with an SMC-B signed JWT as subject_token, client-jwt authentication, and DPoP
 * proof.
 *
 * <p>The Docker PEP (ngx_pep) validates token signatures against Keycloak's JWKS endpoint. This
 * factory performs the full Keycloak token exchange flow to obtain a properly signed access token
 * that contains the {@code udat} claim required by the PEP.
 */
@Slf4j
public class ZetaPepJwtTestFactory {

  static {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  /**
   * Represents the target PDP (Keycloak) instance for token exchange. Each PDP uses the same realm
   * (zeta-guard) and client configuration, but resides behind a different ingress.
   */
  public enum PdpTarget {
    /** PoPP ZETA PDP — behind popp-zeta-ingress. */
    POPP(
        "https://popp-zeta-ingress/auth/realms/zeta-guard",
        "https://popp-zeta-ingress/auth/",
        "${zeta.server.pdp.tokenUrl}");

    final String issuerUrl;
    final String smcbTokenAud;
    final String tokenUrlPlaceholder;

    PdpTarget(String issuerUrl, String smcbTokenAud, String tokenUrlPlaceholder) {
      this.issuerUrl = issuerUrl;
      this.smcbTokenAud = smcbTokenAud;
      this.tokenUrlPlaceholder = tokenUrlPlaceholder;
    }
  }

  /** Keycloak client ID registered in the zeta-guard realm. */
  private static final String CLIENT_ID = "f00561bc-a975-47aa-b87e-5d0f97265e10";

  /** KID matching the JWKS registered for the client in the Keycloak realm config. */
  private static final String KEY_KID = "V4NrA9NjyZLpt9uIfPYQ8WCbkM5IqttRO8O3IVVuXHo";

  /** PKCS12 keystore on test classpath containing the P-256 key pair for client-jwt auth. */
  private static final String KEYSTORE_CLASSPATH = "zetakeystore.p12";

  private static final String KEYSTORE_PASSWORD = "testpassword";
  private static final String KEY_ALIAS = "zetamock";
  private static final String KEY_PASSWORD = "testpassword";

  /** SMC-B PKCS12 keystore (brainpoolP256r1) for signing the subject_token. */
  private static final String SMCB_P12_PATH =
      "doc/docker/backend/zeta/smcb-private/smcb_private.p12";

  private static final String SMCB_P12_PASSWORD = "00";
  private static final String SMCB_KEY_ALIAS = "alias";

  /** TelematikID extracted from the SMC-B certificate (registrationNumber). */
  private static final String SMCB_TELEMATIK_ID = "1-20014060625";

  /** ProfessionOID from the SMC-B certificate. */
  private static final String SMCB_PROFESSION_OID = "1.2.276.0.76.4.50";

  /** Cached ECKey for DPoP proof generation across calls. */
  private static ECKey cachedEcJwk;

  /** Cached access token for ath computation. */
  private static String cachedAccessToken;

  /** Guard: Keycloak setup runs only once per PDP per JVM. */
  private static final java.util.Set<PdpTarget> configuredPdps =
      java.util.EnumSet.noneOf(PdpTarget.class);

  /**
   * Shared nonce — fetched once per token exchange, used in SMC-B token + attestation challenge.
   */
  private static String cachedNonce;

  private ZetaPepJwtTestFactory() {
    // utility
  }

  /**
   * Obtains a valid Bearer token from the PoPP Docker Keycloak PDP (default).
   *
   * @return "Bearer &lt;access_token&gt;"
   */
  public static String createBearerToken() {
    return createBearerToken(PdpTarget.POPP);
  }

  /**
   * Obtains a valid Bearer token from the specified Docker Keycloak PDP via OAuth 2.0 Token
   * Exchange with an SMC-B signed JWT as subject_token, client-jwt authentication, and DPoP
   * binding.
   *
   * @param target the PDP instance to authenticate against
   * @return "Bearer &lt;access_token&gt;"
   */
  public static String createBearerToken(PdpTarget target) {
    try {
      String tokenEndpoint =
          TigerGlobalConfiguration.resolvePlaceholders(target.tokenUrlPlaceholder);

      // 0. One-time Keycloak setup via Admin API (update JWKS + add protocol mappers)
      if (!configuredPdps.contains(target)) {
        setupKeycloak(tokenEndpoint);
        configuredPdps.add(target);
      }

      // 1. Load P-256 key pair from classpath keystore (for client-jwt auth and DPoP)
      var keyPair = loadKeyPair();
      cachedEcJwk =
          new ECKey.Builder(Curve.P_256, keyPair.publicKey())
              .privateKey(keyPair.privateKey())
              .keyID(KEY_KID)
              .build();

      // 1b. Fetch nonce once — shared between SMC-B token and attestation challenge
      cachedNonce = fetchNonce(target);
      log.info("Fetched ZETA nonce for {}: {}", target, cachedNonce);

      // 2. Create SMC-B signed subject_token (brainpoolP256r1, using BouncyCastle)
      String subjectToken = createSmcbSubjectToken(target, tokenEndpoint);
      log.info("SMC-B subject token created for {} (length={})", target, subjectToken.length());

      // 3. Create client_assertion JWT (P-256, using Nimbus) — uses cachedNonce for attestation
      String clientAssertion = createClientAssertionJwt(cachedEcJwk, target);

      // 4. Create DPoP proof for the token endpoint (no ath for initial request)
      String dpopProof = createDpopProof(cachedEcJwk, "POST", tokenEndpoint, null);

      // 5. Token Exchange — Keycloak issues access token with udat + cdat claims
      String accessToken = doTokenExchange(tokenEndpoint, clientAssertion, dpopProof, subjectToken);
      cachedAccessToken = accessToken;
      log.info("Obtained access token from {} PDP (length={})", target, accessToken.length());
      return "Bearer " + accessToken;

    } catch (Exception e) {
      throw new AssertionError(
          "Failed to obtain token from " + target + " PDP: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a DPoP proof JWT for a PEP request. Must be called AFTER {@link #createBearerToken()}.
   */
  public static String createDpopProofForRequest(String method, String uri) {
    if (cachedEcJwk == null || cachedAccessToken == null) {
      throw new IllegalStateException("Call createBearerToken() first to initialize the DPoP key");
    }
    try {
      return createDpopProof(cachedEcJwk, method, uri, cachedAccessToken);
    } catch (Exception e) {
      throw new AssertionError("Failed to create DPoP proof: " + e.getMessage(), e);
    }
  }

  /**
   * Performs a full Keycloak token exchange via the given HTTP proxy (e.g. Tiger-Proxy). Uses the
   * PoPP PDP as default target.
   */
  public static void doTokenExchangeViaProxy(
      String tokenEndpoint, String proxyHost, int proxyPort) {
    doTokenExchangeViaProxy(PdpTarget.POPP, tokenEndpoint, proxyHost, proxyPort);
  }

  /**
   * Performs a full Keycloak token exchange via the given HTTP proxy (e.g. Tiger-Proxy). The
   * response is captured by the proxy for later validation with TGR steps. This method does NOT
   * throw on HTTP errors so that error responses can be validated by the test framework.
   *
   * @param target the PDP instance to authenticate against
   * @param tokenEndpoint the Keycloak token endpoint URL
   * @param proxyHost the proxy host
   * @param proxyPort the proxy port
   */
  public static void doTokenExchangeViaProxy(
      PdpTarget target, String tokenEndpoint, String proxyHost, int proxyPort) {
    try {
      // 0. One-time Keycloak setup
      if (!configuredPdps.contains(target)) {
        setupKeycloak(tokenEndpoint);
        configuredPdps.add(target);
      }

      // 1. Load key pair
      var keyPair = loadKeyPair();
      cachedEcJwk =
          new ECKey.Builder(Curve.P_256, keyPair.publicKey())
              .privateKey(keyPair.privateKey())
              .keyID(KEY_KID)
              .build();

      // 1b. Fetch a fresh nonce (the cached one may have expired between test scenarios)
      cachedNonce = fetchNonce(target);

      // 2. Create SMC-B signed subject_token
      String subjectToken = createSmcbSubjectToken(target, tokenEndpoint);

      // 3. Create client_assertion JWT
      String clientAssertion = createClientAssertionJwt(cachedEcJwk, target);

      // 4. Create DPoP proof for the token endpoint
      String dpopProof = createDpopProof(cachedEcJwk, "POST", tokenEndpoint, null);

      // 5. Send token exchange via proxy
      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      factory.setProxy(
          new java.net.Proxy(
              java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress(proxyHost, proxyPort)));

      RestTemplate rt = new RestTemplate(factory);
      // Don't throw on error responses - let Tiger-Proxy capture them for validation
      rt.setErrorHandler(
          new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public void handleError(
                @SuppressWarnings("NullableProblems")
                    org.springframework.http.client.ClientHttpResponse response) {
              // no-op: allow error responses to pass through
            }
          });

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      headers.set("DPoP", dpopProof);

      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
      form.add("subject_token", subjectToken);
      form.add("subject_token_type", "urn:ietf:params:oauth:token-type:jwt");
      form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
      form.add("client_assertion", clientAssertion);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
      rt.postForEntity(URI.create(tokenEndpoint), request, String.class);
      log.info("Token exchange request sent via proxy {}:{}", proxyHost, proxyPort);

    } catch (Exception e) {
      throw new AssertionError("Failed to perform token exchange via proxy: " + e.getMessage(), e);
    }
  }

  // ---- SMC-B Subject Token (brainpoolP256r1 via BouncyCastle) ----

  /**
   * Creates an SMC-B signed JWT to be used as subject_token in token exchange. The JWT is signed
   * with the brainpoolP256r1 key from the SMC-B PKCS12 file and includes the x5c certificate chain
   * in the header.
   */
  private static String createSmcbSubjectToken(
      PdpTarget target, @SuppressWarnings("unused") String tokenEndpoint) throws Exception {
    // Resolve path relative to project root
    java.io.File smcbFile =
        new java.io.File(System.getProperty("user.dir")).toPath().resolve(SMCB_P12_PATH).toFile();
    if (!smcbFile.exists()) {
      // Try from parent directories (Maven may run from test/zeta-testsuite)
      smcbFile =
          new java.io.File(System.getProperty("user.dir"))
              .toPath()
              .resolve("../../" + SMCB_P12_PATH)
              .normalize()
              .toFile();
    }

    KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
    try (InputStream is = new java.io.FileInputStream(smcbFile)) {
      ks.load(is, SMCB_P12_PASSWORD.toCharArray());
    }

    PrivateKey smcbPrivKey =
        (PrivateKey) ks.getKey(SMCB_KEY_ALIAS, SMCB_P12_PASSWORD.toCharArray());
    X509Certificate smcbCert = (X509Certificate) ks.getCertificate(SMCB_KEY_ALIAS);

    // Build x5c: base64-encoded DER certificate
    String certB64 = Base64.getEncoder().encodeToString(smcbCert.getEncoded());

    // Build JWT header
    String headerJson =
        String.format("{\"alg\":\"ES256\",\"typ\":\"JWT\",\"x5c\":[\"%s\"]}", certB64);

    // Build JWT claims — use the cached nonce (shared with attestation challenge)
    long now = System.currentTimeMillis() / 1000;
    String nonce = cachedNonce;
    String claimsJson =
        String.format(
            "{\"iss\":\"%s\",\"sub\":\"%s\",\"aud\":\"%s\",\"typ\":\"Bearer\",\"nonce\":\"%s\","
                + "\"exp\":%d,\"iat\":%d,\"jti\":\"%s\",\"professionOid\":\"%s\"}",
            CLIENT_ID,
            SMCB_TELEMATIK_ID,
            target.smcbTokenAud,
            nonce,
            now + 300,
            now,
            UUID.randomUUID(),
            SMCB_PROFESSION_OID);

    // Base64url encode
    String headerB64 =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
    String claimsB64 =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(claimsJson.getBytes(StandardCharsets.UTF_8));

    String signingInput = headerB64 + "." + claimsB64;

    // Sign with BouncyCastle (brainpoolP256r1 / SHA256withECDSA)
    Signature sig = Signature.getInstance("SHA256withECDSA", "BC");
    sig.initSign(smcbPrivKey);
    sig.update(signingInput.getBytes(StandardCharsets.UTF_8));
    byte[] derSignature = sig.sign();

    // Convert DER signature to raw R||S format (JWS requires raw concat, not DER)
    byte[] rawSignature = derToRawSignature(derSignature);
    String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(rawSignature);

    return signingInput + "." + sigB64;
  }

  /**
   * Converts a DER-encoded ECDSA signature to raw R||S format for P-256 (32-byte components). DER
   * format: 0x30 len 0x02 rLen r[rLen] 0x02 sLen s[sLen]
   */
  private static byte[] derToRawSignature(byte[] der) {
    int componentLength = 32;
    int offset = 2; // skip 0x30 and total length
    // R
    int rLen = der[offset + 1] & 0xFF;
    byte[] r = new byte[rLen];
    System.arraycopy(der, offset + 2, r, 0, rLen);
    offset += 2 + rLen;
    // S
    int sLen = der[offset + 1] & 0xFF;
    byte[] s = new byte[sLen];
    System.arraycopy(der, offset + 2, s, 0, sLen);

    // Pad/trim to componentLength
    byte[] raw = new byte[componentLength * 2];
    copyToFixed(r, raw, 0, componentLength);
    copyToFixed(s, raw, componentLength, componentLength);
    return raw;
  }

  private static void copyToFixed(byte[] src, byte[] dest, int destOffset, int len) {
    if (src.length == len) {
      System.arraycopy(src, 0, dest, destOffset, len);
    } else if (src.length > len) {
      // Trim leading zeros (when BigInteger adds a 0x00 padding byte)
      System.arraycopy(src, src.length - len, dest, destOffset, len);
    } else {
      // Pad with leading zeros
      System.arraycopy(src, 0, dest, destOffset + (len - src.length), src.length);
    }
  }

  // ---- Client Assertion & DPoP (P-256 via Nimbus) ----

  private static String createClientAssertionJwt(ECKey ecJwk, PdpTarget target) throws Exception {
    var now = Instant.now();

    // Build client-self-assessment data (required by ZETA Guard plugin)
    var platformProductId = new java.util.HashMap<String, Object>();
    platformProductId.put("platform", "linux");
    platformProductId.put("packaging_type", "jar");
    platformProductId.put("application_id", "de.gematik.test.zeta");

    var selfAssessment = new java.util.HashMap<String, Object>();
    selfAssessment.put("name", "ZeTA Test Client");
    selfAssessment.put("client_id", CLIENT_ID);
    selfAssessment.put("manufacturer_id", "gematik");
    selfAssessment.put("manufacturer_name", "gematik GmbH");
    selfAssessment.put("owner_mail", "test@gematik.de");
    selfAssessment.put("registration_timestamp", System.currentTimeMillis() / 1000);
    selfAssessment.put("platform_product_id", platformProductId);

    var claimsBuilder =
        new JWTClaimsSet.Builder()
            .issuer(CLIENT_ID)
            .subject(CLIENT_ID)
            .audience(target.issuerUrl + "/protocol/openid-connect/token")
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(60)))
            .claim("urn:telematik:client-self-assessment", selfAssessment);

    var claims = claimsBuilder.build();

    var header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(ecJwk.getKeyID()).build();

    var jwt = new SignedJWT(header, claims);
    jwt.sign(new ECDSASigner(ecJwk));
    return jwt.serialize();
  }

  private static String createDpopProof(ECKey ecJwk, String method, String uri, String accessToken)
      throws Exception {
    var now = Instant.now();
    ECKey publicJwk = new ECKey.Builder(ecJwk.toPublicJWK()).algorithm(JWSAlgorithm.ES256).build();
    var header =
        new JWSHeader.Builder(JWSAlgorithm.ES256)
            .type(new JOSEObjectType("dpop+jwt"))
            .jwk(publicJwk)
            .build();

    var claimsBuilder =
        new JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .claim("htm", method)
            .claim("htu", uri)
            .issueTime(Date.from(now));

    if (accessToken != null) {
      byte[] hash =
          MessageDigest.getInstance("SHA-256")
              .digest(accessToken.getBytes(StandardCharsets.US_ASCII));
      String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
      claimsBuilder.claim("ath", ath);
    }

    var jwt = new SignedJWT(header, claimsBuilder.build());
    jwt.sign(new ECDSASigner(ecJwk));
    return jwt.serialize();
  }

  // ---- Token Exchange ----

  private static String doTokenExchange(
      String tokenEndpoint, String clientAssertion, String dpopProof, String subjectToken) {
    RestTemplate rt = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.set("DPoP", dpopProof);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    form.add("subject_token", subjectToken);
    form.add("subject_token_type", "urn:ietf:params:oauth:token-type:jwt");
    form.add("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
    form.add("client_assertion", clientAssertion);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

    ResponseEntity<String> response =
        rt.postForEntity(URI.create(tokenEndpoint), request, String.class);

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new AssertionError(
          "Token exchange failed with status "
              + response.getStatusCode()
              + ": "
              + response.getBody());
    }

    try {
      JsonNode node = new ObjectMapper().readTree(response.getBody());
      JsonNode accessTokenNode = node.get("access_token");
      if (accessTokenNode == null || accessTokenNode.isNull()) {
        throw new AssertionError(
            "Token exchange response has no access_token: " + response.getBody());
      }
      return accessTokenNode.asText();
    } catch (Exception e) {
      throw new AssertionError("Failed to parse token exchange response: " + e.getMessage(), e);
    }
  }

  // ---- Nonce ----

  private static String fetchNonce(PdpTarget target) {
    String nonceUrl =
        TigerGlobalConfiguration.resolvePlaceholders(target.tokenUrlPlaceholder)
            .replaceAll("/protocol/openid-connect/token$", "/zeta-guard-nonce");
    RestTemplate rt = new RestTemplate();
    ResponseEntity<String> response = rt.getForEntity(URI.create(nonceUrl), String.class);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new AssertionError(
          "Failed to fetch nonce from " + nonceUrl + ": " + response.getStatusCode());
    }
    return response.getBody().trim();
  }

  // ---- Key Loading ----

  private static KeyPairHolder loadKeyPair() throws Exception {
    var ks = KeyStore.getInstance("PKCS12");
    try (var is =
        ZetaPepJwtTestFactory.class.getClassLoader().getResourceAsStream(KEYSTORE_CLASSPATH)) {
      if (is == null) {
        throw new IllegalStateException(
            "Keystore '" + KEYSTORE_CLASSPATH + "' not found on classpath");
      }
      ks.load(is, KEYSTORE_PASSWORD.toCharArray());
    }

    var key = ks.getKey(KEY_ALIAS, KEY_PASSWORD.toCharArray());
    if (!(key instanceof ECPrivateKey ecPrivateKey)) {
      throw new IllegalStateException(
          "Keystore entry '" + KEY_ALIAS + "' is not an EC private key");
    }

    var cert = (X509Certificate) ks.getCertificate(KEY_ALIAS);
    if (!(cert.getPublicKey() instanceof ECPublicKey ecPublicKey)) {
      throw new IllegalStateException(
          "Certificate for '" + KEY_ALIAS + "' does not contain an EC public key");
    }

    return new KeyPairHolder(ecPrivateKey, ecPublicKey);
  }

  // ---- One-time Keycloak Admin API setup ----

  /**
   * Configures the running Keycloak instance via Admin API so that:
   *
   * <ul>
   *   <li>The client's JWKS matches the test keystore ({@code zetakeystore.p12})
   *   <li>Hardcoded {@code udat} claim mappers are present on the client
   *   <li>The {@code zeta-guard-accesstoken-mapper} is present for the {@code cdat} claim
   * </ul>
   *
   * This avoids having to modify the {@code zeta-guard-realm.json} file.
   */
  private static void setupKeycloak(String tokenEndpoint) {
    try {
      String pdpBaseUrl = tokenEndpoint.replaceAll("/realms/.*", "");
      log.info("Configuring Keycloak via Admin API at {}", pdpBaseUrl);

      // --- Admin token ---
      String adminToken = obtainAdminToken(pdpBaseUrl);

      // --- 1. Update client JWKS / kid / public key to match zetakeystore.p12 ---
      updateClientJwks(pdpBaseUrl, adminToken);

      // --- 2. Add protocol mappers for udat + cdat directly on the client ---
      addProtocolMappersIfMissing(pdpBaseUrl, adminToken);

      log.info("Keycloak Admin API setup completed successfully");
    } catch (Exception e) {
      log.warn(
          "Keycloak Admin API setup failed — token exchange may still work "
              + "if the realm was pre-configured: {}",
          e.getMessage());
    }
  }

  private static String obtainAdminToken(String pdpBaseUrl) {
    RestTemplate rt = new RestTemplate();
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", "admin-cli");
    form.add("username", "admin");
    form.add("password", "admin");
    form.add("grant_type", "password");
    ResponseEntity<String> resp =
        rt.postForEntity(
            URI.create(pdpBaseUrl + "/realms/master/protocol/openid-connect/token"),
            new HttpEntity<>(form, h),
            String.class);
    try {
      return new ObjectMapper().readTree(resp.getBody()).get("access_token").asText();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to obtain Keycloak admin token", e);
    }
  }

  private static void updateClientJwks(String pdpBaseUrl, String adminToken) throws Exception {
    var keyPair = loadKeyPair();
    ECKey ecJwk =
        new ECKey.Builder(Curve.P_256, keyPair.publicKey())
            .keyID(KEY_KID)
            .algorithm(JWSAlgorithm.ES256)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .build();

    String jwksString = "{\"keys\":[" + ecJwk.toJSONString() + "]}";
    String publicKeyB64 = Base64.getEncoder().encodeToString(keyPair.publicKey().getEncoded());

    RestTemplate rt = new RestTemplate();
    HttpHeaders h = new HttpHeaders();
    h.set("Authorization", "Bearer " + adminToken);
    String clientUrl = pdpBaseUrl + "/admin/realms/zeta-guard/clients/" + CLIENT_ID;

    // Read-modify-write: GET the full client, update only what we need, PUT back.
    // This preserves all existing settings (scopes, flows, etc.).
    ResponseEntity<String> getResp =
        rt.exchange(URI.create(clientUrl), HttpMethod.GET, new HttpEntity<>(h), String.class);
    var om = new ObjectMapper();
    var clientNode =
        (com.fasterxml.jackson.databind.node.ObjectNode) om.readTree(getResp.getBody());

    // Merge into existing attributes (don't replace)
    var attrs =
        clientNode.has("attributes")
            ? (com.fasterxml.jackson.databind.node.ObjectNode) clientNode.get("attributes")
            : om.createObjectNode();
    attrs.put("jwks.string", jwksString);
    attrs.put("jwt.credential.kid", KEY_KID);
    attrs.put("jwt.credential.public.key", publicKeyB64);
    attrs.put("use.jwks.string", "true");
    clientNode.set("attributes", attrs);

    // Ensure serviceAccountsEnabled (needed for token exchange)
    clientNode.put("serviceAccountsEnabled", true);

    h.setContentType(MediaType.APPLICATION_JSON);
    rt.exchange(
        URI.create(clientUrl),
        HttpMethod.PUT,
        new HttpEntity<>(om.writeValueAsString(clientNode), h),
        String.class);
    log.info("Updated client JWKS and settings via Admin API (kid={})", KEY_KID);
  }

  private static void addProtocolMappersIfMissing(String pdpBaseUrl, String adminToken)
      throws Exception {
    RestTemplate rt = new RestTemplate();
    HttpHeaders authHeader = new HttpHeaders();
    authHeader.set("Authorization", "Bearer " + adminToken);

    // Fetch existing mappers
    ResponseEntity<String> resp =
        rt.exchange(
            URI.create(
                pdpBaseUrl
                    + "/admin/realms/zeta-guard/clients/"
                    + CLIENT_ID
                    + "/protocol-mappers/models"),
            HttpMethod.GET,
            new HttpEntity<>(authHeader),
            String.class);
    var existingMappers = new ObjectMapper().readTree(resp.getBody());
    var existingNames = new java.util.HashSet<String>();
    existingMappers.forEach(m -> existingNames.add(m.get("name").asText()));

    HttpHeaders postHeader = new HttpHeaders();
    postHeader.setContentType(MediaType.APPLICATION_JSON);
    postHeader.set("Authorization", "Bearer " + adminToken);
    String mappersUrl =
        pdpBaseUrl + "/admin/realms/zeta-guard/clients/" + CLIENT_ID + "/protocol-mappers/models";

    // Hardcoded udat.telid mapper
    if (!existingNames.contains("udat-telematik-id")) {
      String mapper =
          new ObjectMapper()
              .writeValueAsString(
                  java.util.Map.of(
                      "name", "udat-telematik-id",
                      "protocol", "openid-connect",
                      "protocolMapper", "oidc-hardcoded-claim-mapper",
                      "config",
                          java.util.Map.of(
                              "claim.value", SMCB_TELEMATIK_ID,
                              "claim.name", "udat.telid",
                              "jsonType.label", "String",
                              "access.token.claim", "true",
                              "id.token.claim", "true")));
      rt.postForEntity(URI.create(mappersUrl), new HttpEntity<>(mapper, postHeader), String.class);
      log.info("Added hardcoded udat.telid mapper to client");
    }

    // Hardcoded udat.prof mapper
    if (!existingNames.contains("udat-profession-oid")) {
      String mapper =
          new ObjectMapper()
              .writeValueAsString(
                  java.util.Map.of(
                      "name", "udat-profession-oid",
                      "protocol", "openid-connect",
                      "protocolMapper", "oidc-hardcoded-claim-mapper",
                      "config",
                          java.util.Map.of(
                              "claim.value", SMCB_PROFESSION_OID,
                              "claim.name", "udat.prof",
                              "jsonType.label", "String",
                              "access.token.claim", "true",
                              "id.token.claim", "true")));
      rt.postForEntity(URI.create(mappersUrl), new HttpEntity<>(mapper, postHeader), String.class);
      log.info("Added hardcoded udat.prof mapper to client");
    }

    // zeta-guard-accesstoken-mapper (for cdat claim)
    if (!existingNames.contains("zeta-guard-mapper")) {
      String mapper =
          new ObjectMapper()
              .writeValueAsString(
                  java.util.Map.of(
                      "name", "zeta-guard-mapper",
                      "protocol", "openid-connect",
                      "protocolMapper", "zeta-guard-accesstoken-mapper",
                      "config",
                          java.util.Map.of(
                              "access.token.claim", "true",
                              "id.token.claim", "true",
                              "access.tokenResponse.claim", "true")));
      rt.postForEntity(URI.create(mappersUrl), new HttpEntity<>(mapper, postHeader), String.class);
      log.info("Added zeta-guard-accesstoken-mapper to client");
    }

    // Audience mapper — required by ngx_pep which validates the 'aud' claim
    if (!existingNames.contains("audience-mapper")) {
      String mapper =
          new ObjectMapper()
              .writeValueAsString(
                  java.util.Map.of(
                      "name", "audience-mapper",
                      "protocol", "openid-connect",
                      "protocolMapper", "oidc-audience-mapper",
                      "config",
                          java.util.Map.of(
                              "included.custom.audience", "https://popp-zeta-ingress/",
                              "access.token.claim", "true",
                              "id.token.claim", "false")));
      rt.postForEntity(URI.create(mappersUrl), new HttpEntity<>(mapper, postHeader), String.class);
      log.info("Added audience mapper to client");
    }
  }

  private record KeyPairHolder(ECPrivateKey privateKey, ECPublicKey publicKey) {}
}
