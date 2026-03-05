/*-
 * #%L
 * ZETA Testsuite
 * %%
 * (C) achelos GmbH, 2025, licensed for gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

package de.gematik.zeta.services;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for SSL/TLS configuration in test environments. Provides a TrustAll SSLContext for
 * testing purposes. Thread-safe singleton pattern.
 */
@Slf4j
public class SslConfigurationService {

  private static final AtomicBoolean configured = new AtomicBoolean(false);
  private static SSLContext trustAllSslContext = null;

  /**
   * Returns a cached TrustAll SSLContext for testing purposes. Creates the context once and reuses
   * it for all connections.
   *
   * @return TrustAll SSLContext
   */
  public static synchronized SSLContext getTrustAllSslContext() throws Exception {
    if (trustAllSslContext == null) {
      log.info("Creating TrustAll SSLContext (once)");

      TrustManager[] trustAllCerts =
          new TrustManager[] {
            new X509TrustManager() {
              public X509Certificate[] getAcceptedIssuers() {
                return null;
              }

              public void checkClientTrusted(X509Certificate[] certs, String authType) {}

              public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
          };

      trustAllSslContext = SSLContext.getInstance("TLS");
      trustAllSslContext.init(null, trustAllCerts, new SecureRandom());

      log.info("TrustAll SSLContext created and cached");
    }
    return trustAllSslContext;
  }

  /**
   * Configures SSL for testing purposes - sets default SSL context and hostname verifier. Executed
   * only once (thread-safe).
   */
  public static synchronized void configureForTesting() {
    // Use compareAndSet for atomic check-and-set
    if (!configured.compareAndSet(false, true)) {
      // Already configured by another thread
      return;
    }

    try {
      log.info("Configuring SSL for testing purposes...");

      // Restrict TLS Named Groups
      System.setProperty("jdk.tls.namedGroups", "secp256r1,secp384r1,secp521r1");

      // Get or create cached SSL Context
      SSLContext sslContext = getTrustAllSslContext();

      // Set as global default
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
      SSLContext.setDefault(sslContext);

      log.info("SSL configuration completed");

    } catch (Exception e) {
      // Reset flag on failure so it can be retried
      configured.set(false);
      log.warn("SSL configuration failed: {}", e.getMessage());
      throw new RuntimeException("SSL configuration for tests failed", e);
    }
  }
}
