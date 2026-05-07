/*-
 * #%L
 * VSDM Client Simulator Service
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
package de.gematik.ti20.simsvc.client.util;

import de.gematik.zeta.sdk.storage.SdkStorage;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import javax.annotation.Nullable;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public class StorageInterceptor implements SdkStorage {
  private HashMap<String, String> cache = new HashMap<>();

  public HashMap<String, String> getCache() {
    return cache;
  }

  @Override
  public @Nullable Object put(
      @NotNull final String key,
      @NotNull final String value,
      @NotNull final Continuation<? super Unit> continuation) {
    cache.put(key, value);
    return continuation;
  }

  @Override
  public @Nullable Object get(
      @NotNull final String key, @NotNull final Continuation<? super String> continuation) {
    return cache.get(key);
  }

  @Override
  public @Nullable Object remove(
      @NotNull final String key, @NotNull final Continuation<? super Unit> continuation) {
    return null;
  }

  @Override
  public @Nullable Object clear(@NotNull final Continuation<? super Unit> continuation) {
    return null;
  }
}
