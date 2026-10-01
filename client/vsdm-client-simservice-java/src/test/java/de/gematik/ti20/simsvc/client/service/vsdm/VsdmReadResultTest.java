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
package de.gematik.ti20.simsvc.client.service.vsdm;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.ti20.simsvc.client.repository.VsdmCachedValue;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class VsdmReadResultTest {

  @Test
  void shouldCreateReadResultFromCachedValue() {
    final VsdmCachedValue cachedValue =
        new VsdmCachedValue("etag-123", "pruefziffer-456", "vsdm-data");

    final VsdmReadResult result = VsdmReadResult.from(cachedValue);

    assertThat(result.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.etag()).isEqualTo(cachedValue.etag());
    assertThat(result.pruefziffer()).isEqualTo(cachedValue.pruefziffer());
    assertThat(result.value()).isEqualTo(cachedValue.vsdmData());
    assertThat(result.contentType()).isNull();
  }
}
