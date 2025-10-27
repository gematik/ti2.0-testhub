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
package de.gematik.ti20.simsvc.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.ti20.simsvc.server.model.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class HttpProxyService {

  private final TokenService tokenService;

  private final RestTemplate restTemplate = new RestTemplate();

  public HttpProxyService(@Autowired TokenService tokenService) {
    this.tokenService = tokenService;

    this.restTemplate
        .getMessageConverters()
        .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
  }

  public HttpHeaders preprocess(final HttpServletRequest request) {
    return preprocess(getHeadersFromRequest(request));
  }

  public HttpHeaders preprocess(final HttpHeaders headers) throws ResponseStatusException {
    if (!StringUtils.hasText(headers.getFirst(HttpHeaders.AUTHORIZATION))) {
      log.error("No Authorization header found in request");
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No Authorization header found");
    }
    var at = tokenService.validateAccessToken(headers.getFirst(HttpHeaders.AUTHORIZATION));
    // var pt = tokenService.validatePoppToken(headers.getFirst("PoPP"), at);

    final UserInfo userInfo =
        new UserInfo(
            at.getClaims().getSub(),
            at.getClaims().getClientId(),
            at.getClaims().getProfessionOid());

    try {
      headers.add("ZETA-User-Info", tokenService.convertToBase64Json(userInfo));
    } catch (final JsonProcessingException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Could not create ZETA-User-Info", e);
    }

    headers.add("ZETA-PoPP-Token-Content", headers.getFirst("PoPP"));
    headers.add("ZETA-Client-Data", "TODO ZTA Client Data");
    headers.remove("PoPP");

    if (headers.getFirst("If-None-Match") != null) {
      headers.add("If-None-Match", headers.getFirst("If-None-Match"));
    }

    return headers;
  }

  public ResponseEntity<String> forwardRequestTo(
      final String backendUrl, final HttpServletRequest request, final HttpHeaders newHeaders) {
    log.debug("Forwarding request to backend URL: {}", backendUrl);

    final String traceId = request.getHeader("X-Trace-Id");
    final String body = getBodyFromRequest(request);
    log.debug("Request body: {}", body);

    final HttpEntity<String> entity = new HttpEntity<>(body, newHeaders);

    final String url = backendUrl + request.getRequestURI();

    final HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().toUpperCase());

    try {
      log.debug("Forwarding request {} to {} {}", traceId, httpMethod, url);
      var response = restTemplate.exchange(url, httpMethod, entity, String.class);
      log.debug(
          "Successfully forwarded request {} with status {}", traceId, response.getStatusCode());
      return response;
    } catch (final HttpStatusCodeException e) {
      log.error(
          "Error while forwarding request with status code {}: {}", traceId, e.getStatusCode());
      throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString(), e);
    } catch (final Exception e) {
      log.error("Error while forwarding request {}", traceId, e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getLocalizedMessage());
    }
  }

  private HttpHeaders getHeadersFromRequest(HttpServletRequest request) {
    HttpHeaders headers = new HttpHeaders();
    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      List<String> headerValues = Collections.list(request.getHeaders(headerName));
      headers.put(headerName, headerValues);
    }
    return headers;
  }

  private String getBodyFromRequest(HttpServletRequest request) {
    StringBuilder requestBody = new StringBuilder();
    try (BufferedReader reader = request.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        requestBody.append(line);
      }
    } catch (IOException e) {
      // could not read the body? it can be a request without body
    }

    return requestBody.toString();
  }
}
