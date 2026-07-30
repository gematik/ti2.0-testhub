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
package de.gematik.ti20.simsvc.client.service.popp;

import de.gematik.ti20.simsvc.client.config.VsdmClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PoppClientWarmup {

  private static final int MAX_WARMUP_ATTEMPTS = 5;
  private static final long RETRY_DELAY_MILLIS = 2_000L;

  private final VsdmClientConfig vsdmClientConfig;
  private final PoppClientAdapter poppClientAdapter;

  @EventListener(ApplicationReadyEvent.class)
  void warmupOnStartup() {
    if (vsdmClientConfig.isUseMockPoppToken()) {
      return;
    }

    Thread warmupThread = new Thread(this::warmupPoppClient, "popp-client-warmup");
    warmupThread.setDaemon(true);
    warmupThread.start();
  }

  private void warmupPoppClient() {
    for (int attempt = 1; attempt <= MAX_WARMUP_ATTEMPTS; attempt++) {
      try {
        poppClientAdapter.getPoppToken(null);
        log.info("PoPP client warmup completed on attempt {}/{}.", attempt, MAX_WARMUP_ATTEMPTS);
        return;
      } catch (Exception e) {
        if (attempt == MAX_WARMUP_ATTEMPTS) {
          log.warn("PoPP client warmup failed after {} attempts.", MAX_WARMUP_ATTEMPTS, e);
          return;
        }

        log.info(
            "PoPP client warmup attempt {}/{} failed. Retrying in {} ms.",
            attempt,
            MAX_WARMUP_ATTEMPTS,
            RETRY_DELAY_MILLIS,
            e);
        try {
          Thread.sleep(RETRY_DELAY_MILLIS);
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
          log.warn("PoPP client warmup was interrupted.", interruptedException);
          return;
        }
      }
    }
  }
}
