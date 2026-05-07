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
package de.gematik.ti20.simsvc.client.service;

import de.gematik.ti20.simsvc.client.util.DpopHelper;
import de.gematik.ti20.simsvc.client.util.StorageInterceptor;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TestService {
  private final StorageInterceptor storageInterceptor;

  @Autowired
  public TestService(StorageInterceptor storageInterceptor) {
    this.storageInterceptor = storageInterceptor;
  }

  public String getAccessToken() {
    String at = "";
    final Set<String> key = storageInterceptor.getCache().keySet();
    for (final String keyValue : key) {
      if (keyValue.startsWith("at:")) {
        at = storageInterceptor.getCache().get(keyValue);
      }
    }
    return at;
  }

  public String getDpopToken(final String htm, final String htu) throws Exception {
    final String at = getAccessToken();
    MessageDigest digest = MessageDigest.getInstance("sha-256");
    digest.update(at.getBytes());
    byte[] hash = digest.digest();
    final String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

    final String dpopPublicKey = storageInterceptor.getCache().getOrDefault("dpop_public_key", "");
    final String dpopPrivateKey =
        storageInterceptor.getCache().getOrDefault("dpop_private_key", "");

    return DpopHelper.createDpop(dpopPublicKey, dpopPrivateKey, htm, htu, ath);
  }
}
