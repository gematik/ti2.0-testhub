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
package de.gematik.zeta.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.zeta.config.PoPpConfig;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Keycloak Admin API helpers for seeding / cleaning SMC-B user attributes used by maxClients tests.
 */
@Slf4j
public final class KeycloakAdminHelper {

  private static final String REALM = "zeta-guard";
  private static final String ATTR_CLIENT_IDS = "zetaguard.smcbuser.client_ids";
  private static final String ATTR_TELEMATIK_ID = "zetaguard.smcbuser.telematik_id";

  private static final String KEYCLOAK_CLIENT_ID = "admin-cli";
  private static final String KEYCLOAK_GRANT_TYPE = "password";

  /** Matches TestHub compose {@code SMCB_HASHING_PEPPER}. */
  private static final String DEFAULT_HASHING_PEPPER = "-085c1245-dc0d-4d39-95b4-97496bec6182";

  /** Matches TestHub compose {@code KC_BOOTSTRAP_ADMIN_USERNAME} / {@code KEYCLOAK_ADMIN}. */
  private static final String DEFAULT_ADMIN_USERNAME = "admin";

  /**
   * Matches TestHub compose {@code KC_BOOTSTRAP_ADMIN_PASSWORD} / {@code KEYCLOAK_ADMIN_PASSWORD}.
   */
  private static final String DEFAULT_ADMIN_PASSWORD = "admin";

  private static final ObjectMapper JSON = new ObjectMapper();

  private KeycloakAdminHelper() {}

  private static String resolvePdpBaseUrl() {
    String tokenUrl = PoPpConfig.tokenUrl();
    return tokenUrl.replaceAll("/realms/.*", "");
  }

  /** Resolves the pepper used to compute the SMC-B username hash. */
  private static String getHashingPepper() {
    String fromTiger = TigerGlobalConfiguration.readString("zeta.server.pdp.smcbHashingPepper", "");
    if (fromTiger != null && !fromTiger.isBlank()) {
      return fromTiger;
    }
    return DEFAULT_HASHING_PEPPER;
  }

  /** Resolves the Keycloak {@code master} realm admin username used to obtain admin tokens. */
  private static String getAdminUsername() {
    String fromTiger = TigerGlobalConfiguration.readString("zeta.server.pdp.adminUsername", "");
    if (fromTiger != null && !fromTiger.isBlank()) {
      return fromTiger;
    }
    return DEFAULT_ADMIN_USERNAME;
  }

  /** Resolves the Keycloak {@code master} realm admin password used to obtain admin tokens. */
  private static String getAdminPassword() {
    String fromTiger = TigerGlobalConfiguration.readString("zeta.server.pdp.adminPassword", "");
    if (fromTiger != null && !fromTiger.isBlank()) {
      return fromTiger;
    }
    return DEFAULT_ADMIN_PASSWORD;
  }

  /**
   * Builds the {@code Authorization: Bearer ...} header used for all Keycloak Admin REST API calls
   * in this class.
   */
  private static HttpHeaders authHeaders(String token) {
    HttpHeaders h = new HttpHeaders();
    h.setBearerAuth(token);
    return h;
  }

  /**
   * Builds an authenticated, ready-to-send JSON request: sets the bearer token, {@code
   * Content-Type: application/json}, and serializes {@code body} to a string.
   */
  private static HttpEntity<String> buildAuthenticatedJsonRequest(String token, JsonNode body) {
    try {
      HttpHeaders h = authHeaders(token);
      h.setContentType(MediaType.APPLICATION_JSON);
      return new HttpEntity<>(JSON.writeValueAsString(body), h);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize JSON body", e);
    }
  }

  /**
   * Computes the Keycloak username zeta-guard derives for an SMC-B user, i.e. {@code
   * Base64Url(SHA-256(telematikId + pepper))} — the same algorithm as zeta-guard's {@code
   * toSpicyHash()}.
   */
  private static String computeSpicyHashUsername(String telematikId, String pepper) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest((telematikId + pepper).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to compute spicy-hash username", e);
    }
  }

  /**
   * Authenticates against the Keycloak {@code master} realm as the built-in {@code admin} user
   * (password grant via the {@code admin-cli} client) and returns a bearer token usable for
   * subsequent Admin REST API calls.
   */
  private static String obtainAdminToken() {
    RestTemplate rt = new RestTemplate();
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", KEYCLOAK_CLIENT_ID);
    form.add("username", getAdminUsername());
    form.add("password", getAdminPassword());
    form.add("grant_type", KEYCLOAK_GRANT_TYPE);
    ResponseEntity<String> resp =
        rt.postForEntity(
            URI.create(resolvePdpBaseUrl() + "/realms/master/protocol/openid-connect/token"),
            new HttpEntity<>(form, h),
            String.class);
    try {
      return JSON.readTree(resp.getBody()).get("access_token").asText();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to obtain Keycloak admin token", e);
    }
  }

  /**
   * Sets the {@code zeta-guard} realm's user-profile {@code unmanagedAttributePolicy} to {@code
   * ADMIN_EDIT}, so that custom (unmanaged) attributes such as {@code
   * zetaguard.smcbuser.client_ids} can be written via the Admin REST API. Idempotent: safe to call
   * repeatedly.
   *
   * <p>Takes no parameters (operates on the fixed {@link #REALM}) and returns nothing; success is
   * signaled by the absence of a thrown exception.
   *
   * @throws AssertionError if reading or updating the realm's user-profile configuration fails
   *     (non-2xx response from Keycloak).
   */
  public static void enableUnmanagedAttributesAdminEdit() {
    String token = obtainAdminToken();
    String url = resolvePdpBaseUrl() + "/admin/realms/" + REALM + "/users/profile";
    RestTemplate rt = new RestTemplate();
    HttpHeaders h = authHeaders(token);

    ResponseEntity<String> get =
        rt.exchange(URI.create(url), HttpMethod.GET, new HttpEntity<>(h), String.class);
    if (!get.getStatusCode().is2xxSuccessful() || get.getBody() == null) {
      throw new AssertionError("GET users/profile failed: " + get.getStatusCode());
    }
    try {
      ObjectNode profile = (ObjectNode) JSON.readTree(get.getBody());
      profile.put("unmanagedAttributePolicy", "ADMIN_EDIT");
      if (!profile.has("attributes") || profile.get("attributes").isNull()) {
        profile.putArray("attributes");
      }
      // Send as String: RestTemplate bean-serializes JsonNode (emits bogus "array" field).
      ResponseEntity<String> put =
          rt.exchange(
              URI.create(url),
              HttpMethod.PUT,
              buildAuthenticatedJsonRequest(token, profile),
              String.class);
      if (!put.getStatusCode().is2xxSuccessful()) {
        throw new AssertionError(
            "PUT users/profile failed: " + put.getStatusCode() + " " + put.getBody());
      }
      log.info("Set unmanagedAttributePolicy=ADMIN_EDIT on realm {}", REALM);
    } catch (AssertionError e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError("Failed to enable unmanaged attributes: " + e.getMessage(), e);
    }
  }

  /**
   * Looks up a single Keycloak user in {@link #REALM} by exact username match via the Admin REST
   * API ({@code GET /users?username=...&exact=true}).
   *
   * @param token a valid Keycloak admin bearer token, e.g. from {@link #obtainAdminToken()}.
   * @param username the exact username to search for. Note that Keycloak's username matching may be
   *     case-insensitive depending on server config, but this method does not itself vary case —
   *     callers dealing with the spicy-hash username (see {@link #computeSpicyHashUsername(String,
   *     String)}) should try both the original and lower-cased form if the first lookup misses.
   * @return the matching user's JSON representation (as returned by Keycloak, containing at least
   *     {@code id}), or {@link Optional#empty()} if no user matches or the request itself failed
   *     (non-2xx response, e.g. because {@code token} is invalid or expired).
   * @throws AssertionError if the response body could not be parsed as JSON.
   */
  public static Optional<JsonNode> findUserByUsername(String token, String username) {
    RestTemplate rt = new RestTemplate();
    URI uri =
        UriComponentsBuilder.fromUriString(
                resolvePdpBaseUrl() + "/admin/realms/" + REALM + "/users")
            .queryParam("username", username)
            .queryParam("exact", "true")
            .build(true)
            .toUri();
    ResponseEntity<String> resp =
        rt.exchange(uri, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), String.class);
    if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
      return Optional.empty();
    }
    try {
      JsonNode arr = JSON.readTree(resp.getBody());
      if (arr.isArray() && !arr.isEmpty()) {
        return Optional.of(arr.get(0));
      }
      return Optional.empty();
    } catch (Exception e) {
      throw new AssertionError("Failed to parse user search response: " + e.getMessage(), e);
    }
  }

  /**
   * Deletes the SMC-B user identified by the spicy-hash of {@code telematikId}, if present. A no-op
   * (logged, not an error) when no matching user exists.
   *
   * <p><strong>Prefer {@link #resetClientIdsIfPresent(String)} for inter-scenario cleanup.</strong>
   * Hard-deleting the broker-linked user and then immediately re-provisioning the same external
   * SMC-B identity (via a fresh token exchange in the next scenario) was observed to put the PDP's
   * identity broker into a bad, unrecoverable state: even with zero users left in the realm, the
   * next bootstrap token exchange for the same {@code telematikId} failed with {@code 409
   * conflict / "Duplicate resource error"}. A real SMC-B card's federated identity is never deleted
   * in production, so this delete+recreate churn is purely a test artifact. This method remains
   * available for full teardown / one-off use, but is no longer called by the {@code
   * @smcb_max_clients} scenario cleanup hook.
   *
   * @param telematikId the SMC-B holder's plaintext Telematik-ID whose corresponding Keycloak user
   *     (see {@link #computeSpicyHashUsername(String, String)}) should be removed.
   * @throws AssertionError if the matching user is found, but the Admin REST API delete call fails
   *     (non-2xx response).
   */
  public static void deleteSmcbUser(String telematikId) {
    String token = obtainAdminToken();
    String username = computeSpicyHashUsername(telematikId, getHashingPepper());
    Optional<JsonNode> existing = findUserByUsername(token, username.toLowerCase());
    if (existing.isEmpty()) {
      existing = findUserByUsername(token, username);
    }
    if (existing.isEmpty()) {
      log.info("No SMC-B user to delete for telematikId={}", telematikId);
      return;
    }
    String userId = existing.get().get("id").asText();
    RestTemplate rt = new RestTemplate();
    ResponseEntity<String> resp =
        rt.exchange(
            URI.create(resolvePdpBaseUrl() + "/admin/realms/" + REALM + "/users/" + userId),
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders(token)),
            String.class);
    if (!resp.getStatusCode().is2xxSuccessful()) {
      throw new AssertionError(
          "DELETE user " + userId + " failed: " + resp.getStatusCode() + " " + resp.getBody());
    }
    log.info("Deleted SMC-B user {} (telematikId={})", userId, telematikId);
  }

  /**
   * Builds the Keycloak user {@code attributes} JSON object used to fully replace a user's
   * attribute set
   */
  private static ObjectNode buildReplacementAttributes(String telematikId, List<String> clientIds) {
    ObjectNode attrs = JSON.createObjectNode();
    ArrayNode ids = attrs.putArray(ATTR_CLIENT_IDS);
    for (String id : clientIds) {
      ids.add(id);
    }
    attrs.putArray(ATTR_TELEMATIK_ID).add(telematikId);
    return attrs;
  }

  /** Manually update user attributes via Keycloak API */
  private static void updateUserAttributesViaKeycloakAPI(
      String token, String userId, String telematikId, List<String> clientIds) {
    RestTemplate rt = new RestTemplate();
    HttpHeaders h = authHeaders(token);
    String userUrl = resolvePdpBaseUrl() + "/admin/realms/" + REALM + "/users/" + userId;

    ResponseEntity<String> get =
        rt.exchange(URI.create(userUrl), HttpMethod.GET, new HttpEntity<>(h), String.class);
    if (!get.getStatusCode().is2xxSuccessful() || get.getBody() == null) {
      throw new AssertionError("GET user failed: " + get.getStatusCode());
    }
    try {
      ObjectNode user = (ObjectNode) JSON.readTree(get.getBody());
      user.set("attributes", buildReplacementAttributes(telematikId, clientIds));
      ResponseEntity<String> put =
          rt.exchange(
              URI.create(userUrl),
              HttpMethod.PUT,
              buildAuthenticatedJsonRequest(token, user),
              String.class);
      if (!put.getStatusCode().is2xxSuccessful()) {
        throw new AssertionError(
            "PUT user attributes failed: " + put.getStatusCode() + " " + put.getBody());
      }
    } catch (AssertionError e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError("Failed to update user attributes: " + e.getMessage(), e);
    }
  }

  /**
   * Counts the current number of values in the SMC-B user's {@code zetaguard.smcbuser.client_ids}
   * attribute. Used to verify PDP-side LRU eviction (see A_25748-02: exceeding {@code
   * SMCB_USER_MAX_CLIENTS} evicts the least-recently-used client instead of rejecting the request)
   * keeps the list bounded, without relying on backend log assertions.
   *
   * @param telematikId the SMC-B holder's plaintext Telematik-ID.
   * @return the number of client-id values currently stored, or {@code 0} if no matching user
   *     exists or the attribute is absent.
   */
  public static int countClientIds(String telematikId) {
    String token = obtainAdminToken();
    String username = computeSpicyHashUsername(telematikId, getHashingPepper());
    Optional<JsonNode> existing = findUserByUsername(token, username.toLowerCase());
    if (existing.isEmpty()) {
      existing = findUserByUsername(token, username);
    }
    if (existing.isEmpty()) {
      return 0;
    }
    JsonNode clientIds = existing.get().path("attributes").path(ATTR_CLIENT_IDS);
    return clientIds.isArray() ? clientIds.size() : 0;
  }

  /**
   * Resets {@code zetaguard.smcbuser.client_ids} to an empty list on the SMC-B user identified by
   * {@code telematikId}, if one exists — without deleting the user or its federated-identity link.
   * A no-op (logged, not an error) when no matching user exists yet (e.g. the very first scenario
   * run in a fresh environment, before any token exchange has bootstrapped the user).
   *
   * <p>Intended for inter-scenario cleanup (see {@code
   * SmcbMaxClientsSteps#cleanupSmcbUserAfterScenario()}): unlike {@link #deleteSmcbUser(String)},
   * this keeps the same persistent broker-linked user across scenario runs, avoiding a
   * delete+recreate cycle that was observed to leave the PDP's identity broker in a bad state (a
   * subsequent bootstrap token exchange for the same {@code telematikId} failing with {@code 409
   * conflict / "Duplicate resource error"} even though zero users remained in the realm).
   *
   * @param telematikId the SMC-B holder's plaintext Telematik-ID.
   * @throws AssertionError if a matching user exists, but reading/updating it via the Admin REST
   *     API fails.
   */
  public static void resetClientIdsIfPresent(String telematikId) {
    String token = obtainAdminToken();
    String username = computeSpicyHashUsername(telematikId, getHashingPepper());
    Optional<JsonNode> existing = findUserByUsername(token, username.toLowerCase());
    if (existing.isEmpty()) {
      existing = findUserByUsername(token, username);
    }
    if (existing.isEmpty()) {
      log.info("No SMC-B user to reset for telematikId={}", telematikId);
      return;
    }
    String userId = existing.get().get("id").asText();
    updateUserAttributesViaKeycloakAPI(token, userId, telematikId, new ArrayList<>());
    log.info("Reset client_ids on SMC-B user {} (telematikId={})", userId, telematikId);
  }

  /**
   * Overwrites {@code zetaguard.smcbuser.client_ids} on an <em>existing</em> SMC-B user (created
   * via a prior successful token exchange so the federated IdP link is present) with {@code count}
   * freshly (i.e. dummy/fake) generated random UUIDs.
   *
   * @param telematikId the SMC-B holder's plaintext Telematik-ID; used both to locate the existing
   *     Keycloak user (via {@link #computeSpicyHashUsername(String, String)}) and to (re-)set the
   *     user's {@code zetaguard.smcbuser.telematik_id} attribute.
   * @param count the number of random client-id values to seed into {@code
   *     zetaguard.smcbuser.client_ids}; must be {@code >= 0}. A value of {@code 0} clears the
   *     attribute (empty list).
   * @return the Keycloak internal user id ({@code UUID} as string) of the updated SMC-B user.
   * @throws IllegalArgumentException if {@code count < 0}.
   * @throws AssertionError if no matching SMC-B user exists for {@code telematikId} (bootstrap it
   *     with a successful token exchange first), or if reading/updating the user via the Admin REST
   *     API fails.
   */
  public static String overwriteUserAttributes(String telematikId, int count) {
    if (count < 0) {
      throw new IllegalArgumentException("count must be >= 0");
    }
    String token = obtainAdminToken();
    String pepper = getHashingPepper();
    String username = computeSpicyHashUsername(telematikId, pepper);
    String usernameLower = username.toLowerCase();

    List<String> clientIds = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      clientIds.add(UUID.randomUUID().toString());
    }

    Optional<JsonNode> existing = findUserByUsername(token, usernameLower);
    if (existing.isEmpty()) {
      existing = findUserByUsername(token, username);
    }

    if (existing.isEmpty()) {
      throw new AssertionError(
          "SMC-B user not found for telematikId="
              + telematikId
              + " (username="
              + usernameLower
              + "). Bootstrap with a successful token exchange before seeding client_ids.");
    }

    String userId = existing.get().get("id").asText();
    updateUserAttributesViaKeycloakAPI(token, userId, telematikId, clientIds);
    return userId;
  }
}
