/*-
 * #%L
 * VSDM 2.0 Testsuite
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
package de.gematik.ti20.vsdm.test.load;

import de.gematik.zeta.sdk.BuildConfig;
import de.gematik.zeta.sdk.SdkStatus;
import de.gematik.zeta.sdk.TpmConfig;
import de.gematik.zeta.sdk.ZetaSdk;
import de.gematik.zeta.sdk.ZetaSdkClient;
import de.gematik.zeta.sdk.ZetaSdkClientExtension;
import de.gematik.zeta.sdk.attestation.model.AttestationConfig;
import de.gematik.zeta.sdk.attestation.model.PlatformProductId;
import de.gematik.zeta.sdk.authentication.AuthConfig;
import de.gematik.zeta.sdk.authentication.SubjectTokenProvider;
import de.gematik.zeta.sdk.authentication.smb.SmbTokenProvider;
import de.gematik.zeta.sdk.network.http.client.ZetaHttpClientBuilder;
import de.gematik.zeta.sdk.storage.SdkStorage;
import de.gematik.zeta.sdk.storage.StorageConfig;
import io.ktor.client.plugins.logging.LogLevel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Slf4j
final class ZetaClientPool {

  private static final String SCOPE = "vsdservice";

  private final String resourceBase;
  private final int capacity;
  private final Map<String, ActorPool> poolsByActorId;

  ZetaClientPool(
      final String resourceBase,
      final int capacity,
      final List<SimulationConfigBean.SmcbData> smcbs) {
    this.resourceBase = Objects.requireNonNull(resourceBase, "resourceBase");
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be greater than 0");
    }
    this.capacity = capacity;
    final List<SimulationConfigBean.SmcbData> smcbList =
        List.copyOf(Objects.requireNonNull(smcbs, "smcbs"));
    if (smcbList.isEmpty()) {
      throw new IllegalArgumentException("smcbs must not be empty");
    }
    final Map<String, ActorPool> pools = new HashMap<>();
    final HashSet<String> actorIds = new HashSet<>();
    for (final SimulationConfigBean.SmcbData smcbData : smcbList) {
      final String actorId = requireActorId(smcbData.getActorId());
      if (!actorIds.add(actorId)) {
        throw new IllegalArgumentException("duplicate actorId in smcbs: " + actorId);
      }
      pools.put(actorId, new ActorPool(smcbData));
    }
    this.poolsByActorId = Map.copyOf(pools);
    initializePools();
    final List<String> configuredActorIds =
        smcbList.stream().map(SimulationConfigBean.SmcbData::getActorId).toList();
    log.info(
        "ZetaClientPool created for '{}' with capacity {} and actorIds {}",
        resourceBase,
        capacity,
        configuredActorIds);
  }

  ZetaSdkClient acquire(final String actorId) throws InterruptedException {
    final ActorPool pool = poolForActorId(actorId);
    return pool.available.take();
  }

  void release(final String actorId, final ZetaSdkClient client) {
    poolForActorId(actorId).available.offer(client);
  }

  void cleanup() {
    log.info("Clear ZetaClientPool for '{}'", resourceBase);
    for (Map.Entry<String, ActorPool> entry : poolsByActorId.entrySet()) {
      for (ZetaSdkClient client : entry.getValue().available) {
        SdkStatus status = ZetaSdkClientExtension.status(client);
        log.info("Status for actorId '{}': {}", entry.getKey(), status);
      }
    }
  }

  private ActorPool poolForActorId(final String actorId) {
    final String normalizedActorId = requireActorId(actorId);
    final ActorPool pool = poolsByActorId.get(normalizedActorId);
    if (pool == null) {
      throw new IllegalArgumentException("unknown actorId: " + normalizedActorId);
    }
    return pool;
  }

  private void initializePools() {
    for (Map.Entry<String, ActorPool> entry : poolsByActorId.entrySet()) {
      final String actorId = entry.getKey();
      final ActorPool actorPool = entry.getValue();
      for (int i = 0; i < capacity; i++) {
        final ZetaSdkClient client = createClient(resourceBase, actorPool.smcbData);
        log.info(
            "Created ZetaSdkClient for actorId '{}' status {}",
            actorId,
            ZetaSdkClientExtension.status(client));

        // TODO pre authentication or registration of client

        //        final boolean authenticated = ZetaSdkClientExtension.authenticate(client);
        //        if (!authenticated) {
        //          throw new IllegalStateException(
        //              "Unable to authenticate ZetaSdkClient for actorId '" + actorId + "'");
        //        }

        actorPool.available.offer(client);
      }
      actorPool.created.set(capacity);
      log.debug(
          "Initialized {} ZetaSdkClients for resource '{}' and actorId '{}'",
          capacity,
          resourceBase,
          actorId);
    }
  }

  private static ZetaSdkClient createClient(
      final String resourceBase, final SimulationConfigBean.SmcbData smcbData) {
    return ZetaSdk.INSTANCE.build(
        resourceBase,
        new BuildConfig(
            "demo-client",
            "0.2.0",
            "sdk-client",
            new StorageConfig.Custom(
                new SdkStorage() {
                  private HashMap<String, String> cache = new HashMap<>();

                  @Override
                  public @Nullable Object put(
                      @NonNull String s,
                      @NonNull String s1,
                      @NonNull Continuation<? super Unit> continuation) {
                    cache.put(s, s1);
                    return continuation;
                  }

                  @Override
                  public @Nullable Object get(
                      @NonNull String s, @NonNull Continuation<? super String> continuation) {
                    return cache.get(s);
                  }

                  @Override
                  public @Nullable Object remove(
                      @NonNull String s, @NonNull Continuation<? super Unit> continuation) {
                    return cache.remove(s);
                  }

                  @Override
                  public @Nullable Object clear(@NonNull Continuation<? super Unit> continuation) {
                    cache.clear();
                    return continuation;
                  }
                }),
            new TpmConfig() {},
            new AuthConfig(
                List.of(SCOPE),
                30L,
                false,
                tokenProvider(smcbData),
                AttestationConfig.software(),
                ""),
            new PlatformProductId.LinuxProductId(
                PlatformProductId.PLATFORM_LINUX, "jar", "testhub", "latest"),
            new ZetaHttpClientBuilder().disableServerValidation(true).logging(LogLevel.ALL),
            null,
            null,
            null));
  }

  private static SubjectTokenProvider tokenProvider(final SimulationConfigBean.SmcbData smcbData) {
    final String keyPath = Objects.requireNonNull(smcbData.getKeypath(), "smcb keypath");

    final Path privateKeyPath = Path.of(keyPath);
    if (!Files.exists(privateKeyPath) || !Files.isRegularFile(privateKeyPath)) {
      throw new IllegalStateException("SMCB private key file does not exist: " + keyPath);
    }
    if (!Files.isReadable(privateKeyPath)) {
      throw new IllegalStateException("SMCB private key file is not readable: " + keyPath);
    }

    return new SmbTokenProvider(new SmbTokenProvider.Credentials(keyPath, "alias", "00", ""));
  }

  private static String requireActorId(final String actorId) {
    if (actorId == null || actorId.isBlank()) {
      throw new IllegalArgumentException("actorId must not be blank");
    }
    return actorId;
  }

  private static final class ActorPool {
    private final SimulationConfigBean.SmcbData smcbData;
    private final BlockingQueue<ZetaSdkClient> available = new LinkedBlockingQueue<>();
    private final AtomicInteger created = new AtomicInteger(0);

    private ActorPool(final SimulationConfigBean.SmcbData smcbData) {
      this.smcbData = smcbData;
    }
  }
}
