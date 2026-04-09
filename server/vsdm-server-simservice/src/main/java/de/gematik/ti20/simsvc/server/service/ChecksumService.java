/*-
 * #%L
 * VSDM Server Simservice
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
package de.gematik.ti20.simsvc.server.service;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ChecksumService {

  public static final String HEADER_NAME = "VSDM-Pz";

  public void addChecksumHeader(final String kvnr, final HttpHeaders responseHeaders) {
    if (kvnr != null && !kvnr.isEmpty()) {
      final String checksum = calculateChecksum(kvnr);
      if (checksum != null) {
        responseHeaders.add(HEADER_NAME, checksum);
      }
    }
  }

  public String calculateChecksum(final String kvnr) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-512");
      md.update(kvnr.getBytes());
      final byte[] digest = md.digest();
      // we have to take the 48-byte prefix, bc the base64 presentation must be exactly 64 bytes
      // long (512 bits = 64 bytes) and base64 encoding increases the size by 4/3
      final byte[] prefix = Arrays.copyOf(digest, 48);
      return Base64.getUrlEncoder().encodeToString(prefix);
    } catch (final Exception e) {
      log.error("Cannot generate checksum", e);
      return null;
    }
  }
}
