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
package de.gematik.ti20.simsvc.client.repository;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class PoppTokenRepository {

  private final Map<Integer, String> cache = new ConcurrentHashMap<>();
  private final PoppTokenInspector poppTokenInspector = new PoppTokenInspector();

  public void put(
      final String terminalId, final Integer slotId, final String cardId, final String poppToken) {
    cache.put(Objects.hash(terminalId, slotId, cardId), poppToken);
  }

  public String get(final String terminalId, final Integer slotId, final String cardId) {
    final String poppToken = cache.get(Objects.hash(terminalId, slotId, cardId));
    if (poppToken != null && isExpired(poppToken)) {
      cache.remove(Objects.hash(terminalId, slotId, cardId));
      return null; // Token is expired, remove it from cache
    }

    return poppToken;
  }

  private boolean isExpired(String poppToken) {
    final Long patientProofTime = poppTokenInspector.getPatientProofTime(poppToken);
    return patientProofTime != null
        && System.currentTimeMillis() / 1000 - patientProofTime > (3 * 30 * 24 * 60 * 60);
  }
}
