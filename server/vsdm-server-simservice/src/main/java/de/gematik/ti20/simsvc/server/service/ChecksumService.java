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

import java.security.MessageDigest;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChecksumService {

  public static final String HEADER_NAME = "VSDM-Pz";

  public void addChecksumHeader(final String encodedResponse, final HttpHeaders responseHeaders) {
    if (encodedResponse != null && !encodedResponse.isEmpty()) {
      final String checksum = calculateChecksum(encodedResponse);
      if (checksum != null) {
        responseHeaders.add(HEADER_NAME, checksum);
      }
    }
  }

  private String calculateChecksum(final String encodedResponse) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-512");
      md.update(encodedResponse.getBytes());
      final byte[] digest = md.digest();
      return Base64.getUrlEncoder().encodeToString(digest);
    } catch (final Exception e) {
      log.error("Cannot generate checksum", e);
      return null;
    }
  }
}
