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
package de.gematik.ti20.simsvc.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mock controller for OAuth 2.0 Dynamic Client Registration (RFC 7591). Accepts a DCR request and
 * returns a mock 201 response with a generated client_id.
 */
@Slf4j
@RestController
public class RegisterController {

  private final String registrationBaseUrl;

  public RegisterController(
      @Value("${zeta.pdp.registration-base-url:}") String registrationBaseUrl) {
    this.registrationBaseUrl = registrationBaseUrl;
  }

  @PostMapping(
      path = "/register",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> registerClient(
      HttpServletRequest httpServletRequest, @RequestBody DcrRequest request) {
    log.info(
        "Dynamic Client Registration request received for client_name: {}",
        request.getClientName());

    List<String> validationErrors = validateRequest(request);
    if (!validationErrors.isEmpty()) {
      String errorDescription = String.join("; ", validationErrors);
      log.warn("DCR validation failed: {}", errorDescription);
      return ResponseEntity.badRequest()
          .body(new DcrErrorResponse("invalid_client_metadata", errorDescription));
    }

    String clientId = UUID.randomUUID().toString();
    long clientIdIssuedAt = Instant.now().getEpochSecond();
    String registrationAccessToken = UUID.randomUUID().toString();
    String registrationClientUri = buildRegistrationClientUri(httpServletRequest, clientId);

    List<String> redirectUris =
        request.getRedirectUris() != null ? request.getRedirectUris() : List.of();

    DcrResponse response =
        new DcrResponse(
            clientId,
            clientIdIssuedAt,
            request.getClientName(),
            request.getGrantTypes(),
            request.getTokenEndpointAuthMethod(),
            request.getJwks(),
            redirectUris,
            registrationClientUri,
            registrationAccessToken);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  private String buildRegistrationClientUri(
      HttpServletRequest httpServletRequest, String clientId) {
    String baseUrl =
        registrationBaseUrl != null && !registrationBaseUrl.isBlank()
            ? registrationBaseUrl.trim().replaceAll("/+$", "")
            : deriveBaseUrlFromRequest(httpServletRequest);
    return baseUrl + "/register/" + clientId;
  }

  private String deriveBaseUrlFromRequest(HttpServletRequest httpServletRequest) {
    String forwardedProto = firstForwardedValue(httpServletRequest.getHeader("X-Forwarded-Proto"));
    String forwardedHost = firstForwardedValue(httpServletRequest.getHeader("X-Forwarded-Host"));
    String forwardedPort = firstForwardedValue(httpServletRequest.getHeader("X-Forwarded-Port"));

    String scheme =
        forwardedProto != null && !forwardedProto.isBlank()
            ? forwardedProto
            : httpServletRequest.getScheme();

    String requestHost =
        forwardedHost != null && !forwardedHost.isBlank()
            ? forwardedHost
            : httpServletRequest.getServerName();

    String host = stripPortFromHost(requestHost);
    Integer portFromForwardedHeader = parsePort(forwardedPort);
    Integer portFromHost = extractPortFromHost(requestHost);
    int requestPort =
        httpServletRequest.getServerPort() > 0
            ? httpServletRequest.getServerPort()
            : defaultPortForScheme(scheme);
    int port =
        portFromForwardedHeader != null
            ? portFromForwardedHeader
            : portFromHost != null ? portFromHost : requestPort;

    String contextPath =
        httpServletRequest.getContextPath() != null ? httpServletRequest.getContextPath() : "";
    boolean defaultPort =
        ("http".equalsIgnoreCase(scheme) && port == 80)
            || ("https".equalsIgnoreCase(scheme) && port == 443);

    return URI.create(scheme + "://" + host + (defaultPort ? "" : ":" + port) + contextPath)
        .toString()
        .replaceAll("/$", "");
  }

  private String firstForwardedValue(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) {
      return null;
    }
    return headerValue.split(",")[0].trim();
  }

  private String stripPortFromHost(String hostValue) {
    if (hostValue == null || hostValue.isBlank()) {
      return hostValue;
    }
    if (hostValue.startsWith("[") && hostValue.contains("]")) {
      return hostValue.substring(0, hostValue.indexOf(']') + 1);
    }
    int colonIndex = hostValue.lastIndexOf(':');
    return colonIndex > -1 ? hostValue.substring(0, colonIndex) : hostValue;
  }

  private Integer extractPortFromHost(String hostValue) {
    if (hostValue == null || hostValue.isBlank()) {
      return null;
    }
    if (hostValue.startsWith("[") && hostValue.contains("]")) {
      int suffixSeparator = hostValue.indexOf("]:");
      if (suffixSeparator > -1) {
        return parsePort(hostValue.substring(suffixSeparator + 2));
      }
      return null;
    }
    int colonIndex = hostValue.lastIndexOf(':');
    if (colonIndex > -1) {
      return parsePort(hostValue.substring(colonIndex + 1));
    }
    return null;
  }

  private Integer parsePort(String rawPort) {
    if (rawPort == null || rawPort.isBlank()) {
      return null;
    }
    try {
      int parsedPort = Integer.parseInt(rawPort.trim());
      return parsedPort >= 1 && parsedPort <= 65535 ? parsedPort : null;
    } catch (NumberFormatException e) {
      log.debug("Ignoring invalid forwarded port value: {}", rawPort);
      return null;
    }
  }

  private int defaultPortForScheme(String scheme) {
    return "https".equalsIgnoreCase(scheme) ? 443 : 80;
  }

  private List<String> validateRequest(DcrRequest request) {
    List<String> errors = new ArrayList<>();
    if (request.getClientName() == null || request.getClientName().isBlank()) {
      errors.add("client_name is required and must not be blank");
    }
    if (request.getGrantTypes() == null || request.getGrantTypes().isEmpty()) {
      errors.add("grant_types is required and must not be empty");
    }
    if (request.getTokenEndpointAuthMethod() == null
        || request.getTokenEndpointAuthMethod().isBlank()) {
      errors.add("token_endpoint_auth_method is required");
    }
    if (request.getJwks() == null || request.getJwks().isEmpty()) {
      errors.add("jwks is required and must contain at least one key");
    }
    return errors;
  }

  @Getter
  @Setter
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DcrRequest {

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("grant_types")
    private List<String> grantTypes;

    @JsonProperty("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    @JsonProperty("jwks")
    private JsonNode jwks;

    @JsonProperty("redirect_uris")
    private List<String> redirectUris;
  }

  /** RFC 7591 error response with {@code error} and {@code error_description}. */
  @Getter
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DcrErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;
  }

  @Getter
  @Setter
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class DcrResponse {

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_id_issued_at")
    private long clientIdIssuedAt;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("grant_types")
    private List<String> grantTypes;

    @JsonProperty("token_endpoint_auth_method")
    private String tokenEndpointAuthMethod;

    @JsonProperty("jwks")
    private JsonNode jwks;

    @JsonProperty("redirect_uris")
    private List<String> redirectUris;

    @JsonProperty("registration_client_uri")
    private String registrationClientUri;

    @JsonProperty("registration_access_token")
    private String registrationAccessToken;
  }
}
