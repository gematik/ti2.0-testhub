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
import org.springframework.stereotype.Repository;

@Repository
public class VsdmDataRepository {

  private final Map<Integer, VsdmCachedValue> cache = new ConcurrentHashMap<>();

  public void put(
      final String terminalId,
      final Integer slotId,
      final String cardId,
      final VsdmCachedValue value) {
    if (value == null) {
      cache.remove(Objects.hash(terminalId, slotId, cardId));
    } else {
      cache.put(Objects.hash(terminalId, slotId, cardId), value);
    }
  }

  public VsdmCachedValue get(final String terminalId, final Integer slotId, final String cardId) {
    return cache.get(Objects.hash(terminalId, slotId, cardId));
  }

  /** Remove all cached data. */
  public void clear() {
    cache.clear();
  }
}
