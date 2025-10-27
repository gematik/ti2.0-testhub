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

import de.gematik.ti20.simsvc.server.service.WellKnownService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class WellKnownController {

  private final WellKnownService wellKnownService;

  public WellKnownController(@Autowired WellKnownService wellKnownService) {
    this.wellKnownService = wellKnownService;
  }

  @GetMapping(value = "/.well-known/oauth-protected-resource", produces = "application/json")
  @ResponseBody
  public ResponseEntity<String> getWellKnown(HttpServletRequest request) throws IOException {
    return ResponseEntity.ok(wellKnownService.getWellKnown(request));
  }
}
