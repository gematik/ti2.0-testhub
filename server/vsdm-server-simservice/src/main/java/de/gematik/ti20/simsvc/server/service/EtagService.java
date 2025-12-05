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

import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class EtagService {

  public static final String HEADER_NAME = "Etag";

  private final String ALGORITHM = "HmacSHA256";
  private final String KEY =
      "gematik-simsvc-etag-key"; // This should be securely stored and managed

  // Store etags per kvnr
  private final Map<String, String> etagStore = new HashMap<>();

  private String calculateEtag(final String kvnr, final String encodedResponse) {
    if (kvnr == null || kvnr.isEmpty() || encodedResponse == null || encodedResponse.isEmpty()) {
      return null;
    }
    if (etagStore.containsKey(kvnr)) {
      return etagStore.get(kvnr);
    }

    try {
      final String data = encodedResponse + System.currentTimeMillis();

      final SecretKeySpec secretKeySpec = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(secretKeySpec);
      byte[] digest = mac.doFinal(data.getBytes());

      final String etag = HexFormat.of().formatHex(digest);
      etagStore.put(kvnr, etag);

      return etag;
    } catch (final Exception e) {
      log.error("Cannot generate etag", e);
      return null;
    }
  }

  public void addEtagHeader(
      final String kvnr, final String encodedResponse, final HttpHeaders responseHeaders) {
    if (encodedResponse != null && !encodedResponse.isEmpty()) {
      final String etag = calculateEtag(kvnr, encodedResponse);
      if (etag != null) {
        responseHeaders.add(HEADER_NAME, addEtagPadding(etag));
      }
    }
  }

  public boolean checkEtag(final String kvnr, final String requestEtag) {
    log.debug("Request Etag: {}", requestEtag);
    if (kvnr == null || kvnr.isEmpty()) {
      return false;
    }

    final String etag = etagStore.get(kvnr);
    if (etag == null) {
      return false;
    }
    log.debug("Etag found: {}", etag);

    if (requestEtag == null) {
      return false;
    }

    return etag.equals(removeEtagPadding(requestEtag));
  }

  // etag response headers must be padded with quotes
  private String addEtagPadding(String etag) {
    if (StringUtils.hasLength(etag)
        && (!(etag.startsWith("\"") || etag.startsWith("W/\"")) || !etag.endsWith("\""))) {
      etag = "\"" + etag + "\"";
    }
    return etag;
  }

  private String removeEtagPadding(String etag) {
    if (StringUtils.hasLength(etag)) {
      if (etag.startsWith("\"") && etag.endsWith("\"")) {
        etag = etag.substring(1, etag.length() - 1);
      }
    }
    return etag;
  }
}
