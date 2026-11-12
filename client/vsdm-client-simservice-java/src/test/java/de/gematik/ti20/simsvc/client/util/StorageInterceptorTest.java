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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.junit.jupiter.api.Test;

class StorageInterceptorTest {

  private final StorageInterceptor storageInterceptor = new StorageInterceptor();

  @Test
  void put_storesValue() {
    final Continuation<Unit> continuation = mock(Continuation.class);

    final Object result = storageInterceptor.put("key", "value", continuation);

    assertThat(result).isNotNull();
    assertThat(storageInterceptor.getCache()).containsEntry("key", "value");
  }

  @Test
  void get_returnsStoredValue() {
    final Continuation<? super String> continuation = mock(Continuation.class);
    storageInterceptor.getCache().put("key", "value");

    assertThat(storageInterceptor.get("key", continuation)).isEqualTo("value");
    assertThat(storageInterceptor.get("unknown", continuation)).isNull();
  }

  @Test
  void remove_deletesValue() {
    final Continuation<? super Unit> continuation = mock(Continuation.class);
    storageInterceptor.getCache().put("key", "value");

    final Object result = storageInterceptor.remove("key", continuation);

    assertThat(result).isNotNull();
    assertThat(storageInterceptor.getCache()).doesNotContainKey("key");
  }

  @Test
  void clear_deletesAllValues() {
    final Continuation<? super Unit> continuation = mock(Continuation.class);
    storageInterceptor.getCache().put("first", "one");
    storageInterceptor.getCache().put("second", "two");

    final Object result = storageInterceptor.clear(continuation);

    assertThat(result).isNotNull();
    assertThat(storageInterceptor.getCache()).isEmpty();
  }
}
