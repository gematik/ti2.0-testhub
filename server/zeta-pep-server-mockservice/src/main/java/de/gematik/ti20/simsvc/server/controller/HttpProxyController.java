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

import de.gematik.ti20.simsvc.server.config.HttpConfig;
import de.gematik.ti20.simsvc.server.service.HttpProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@Order(2)
public class HttpProxyController {

  private final HttpConfig httpConfig;
  private final HttpProxyService httpProxyService;

  public HttpProxyController(
      @Autowired HttpConfig httpConfig, @Autowired HttpProxyService httpProxyService) {
    this.httpConfig = httpConfig;
    this.httpProxyService = httpProxyService;
  }

  @RequestMapping(value = "/**", headers = "!" + HttpHeaders.UPGRADE)
  public ResponseEntity<?> proxyRequest(final HttpServletRequest request) {

    if ("websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
      return null;
    }

    log.debug(
        "Received request {} {} {}",
        request.getHeader("X-Trace-Id"),
        request.getMethod(),
        request.getRequestURI());

    final HttpHeaders additionalHeaders = httpProxyService.preprocess(request);
    try {
      final ResponseEntity<String> response =
          httpProxyService.forwardRequestTo(httpConfig.getUrl(), request, additionalHeaders);

      return ResponseEntity.status(response.getStatusCode())
          .headers(response.getHeaders())
          .body(response.getBody());
    } catch (final ResponseStatusException e) {
      log.error("Error processing request: {}", e.getMessage(), e);
      return ResponseEntity.status(e.getStatusCode()).body(e.getBody().getDetail());
    } catch (final Exception e) {
      log.error("Error processing request: {}", e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body("Error processing request: " + e.getMessage());
    }
  }
}
