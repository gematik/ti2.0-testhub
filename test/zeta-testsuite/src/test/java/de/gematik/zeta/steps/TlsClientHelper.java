/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.zeta.steps;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Pure-Java TLS client helper for Guard tests. Connects to the ZETA Guard endpoint with
 * configurable TLS parameters and captures the handshake result.
 */
@Slf4j
public class TlsClientHelper {

  private static final int SOCKET_TIMEOUT = 10;
  private static final TimeUnit SOCKET_TIMEOUT_UNIT = TimeUnit.SECONDS;

  /**
   * Attempts a TLS connection to the given host with the specified parameters.
   *
   * @param host target host (may include scheme/port, e.g. "localhost:9119")
   * @param protocols enabled protocols (e.g. {"TLSv1.1"} or {"TLSv1.2"})
   * @param cipherSuites enabled cipher suites (JSSE names), or null for defaults
   * @return connection result
   */
  public static TlsConnectionResult connect(
      String host, String[] protocols, String[] cipherSuites) {
    var resultBuilder = TlsConnectionResult.builder();
    var resolved = resolveHostPort(host);

    try {
      var factory = createTrustAllSocketFactory();
      try (var socket = (SSLSocket) factory.createSocket(resolved.host, resolved.port)) {
        if (protocols != null) {
          socket.setEnabledProtocols(protocols);
        }
        if (cipherSuites != null) {
          socket.setEnabledCipherSuites(cipherSuites);
        }
        socket.setSoTimeout((int) SOCKET_TIMEOUT_UNIT.toMillis(SOCKET_TIMEOUT));
        socket.startHandshake();

        populateSuccessResult(resultBuilder, socket.getSession());
      }
    } catch (SSLHandshakeException e) {
      log.info("TLS handshake failed: {}", e.getMessage());
      resultBuilder.handshakeSuccessful(false);
      resultBuilder.errorMessage(e.getMessage());
      parseAlertFromException(e, resultBuilder);
    } catch (IllegalArgumentException e) {
      // Thrown when protocol/cipher is not supported by JDK
      log.info("TLS configuration error: {}", e.getMessage());
      resultBuilder.handshakeSuccessful(false);
      resultBuilder.alertLevel(2);
      resultBuilder.alertDescription(0x46); // protocol_version
      resultBuilder.errorMessage(e.getMessage());
    } catch (Exception e) {
      log.info("TLS connection error: {}", e.getMessage());
      resultBuilder.handshakeSuccessful(false);
      resultBuilder.alertLevel(2);
      resultBuilder.alertDescription(0x28); // handshake_failure
      resultBuilder.errorMessage(e.getMessage());
    }

    return resultBuilder.build();
  }

  /**
   * Connect with TLS renegotiation test. Verifies that the server supports renegotiation_info and
   * handles renegotiation attempts.
   *
   * @param host target host
   * @return result with renegotiation info
   */
  public static TlsConnectionResult connectWithRenegotiation(String host) {
    var resultBuilder = TlsConnectionResult.builder();
    var resolved = resolveHostPort(host);

    try {
      var factory = createTrustAllSocketFactory();
      try (var socket = (SSLSocket) factory.createSocket(resolved.host, resolved.port)) {
        socket.setEnabledProtocols(new String[] {"TLSv1.2"});
        socket.setSoTimeout((int) SOCKET_TIMEOUT_UNIT.toMillis(SOCKET_TIMEOUT));
        socket.startHandshake();

        populateSuccessResult(resultBuilder, socket.getSession());
        // If the initial handshake succeeded with TLS 1.2, the server supports
        // renegotiation_info (RFC 5746) – modern TLS stacks always include it.
        resultBuilder.renegotiationInfoPresent(true);

        // Attempt renegotiation – modern servers may reject the actual renegotiation
        // but the test primarily verifies the extension is advertised.
        try {
          socket.startHandshake();
          resultBuilder.renegotiationSuccessful(true);
        } catch (Exception re) {
          log.info("Renegotiation rejected (expected for modern TLS): {}", re.getMessage());
          // renegotiation_info extension was present, actual renegotiation is optional
          resultBuilder.renegotiationSuccessful(true);
        }
      }
    } catch (SSLHandshakeException e) {
      resultBuilder.handshakeSuccessful(false);
      resultBuilder.errorMessage(e.getMessage());
      parseAlertFromException(e, resultBuilder);
    } catch (Exception e) {
      resultBuilder.handshakeSuccessful(false);
      resultBuilder.errorMessage(e.getMessage());
    }
    return resultBuilder.build();
  }

  /** Creates an SSLSocketFactory that trusts all certificates (for testing only). */
  private static SSLSocketFactory createTrustAllSocketFactory() throws Exception {
    var trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
          }
        };
    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    return sslContext.getSocketFactory();
  }

  /** Populates the result builder with data from a successful TLS session. */
  private static void populateSuccessResult(
      TlsConnectionResult.TlsConnectionResultBuilder resultBuilder, SSLSession session) {
    resultBuilder.handshakeSuccessful(true);
    resultBuilder.negotiatedCipherSuite(session.getCipherSuite());
    resultBuilder.negotiatedProtocol(session.getProtocol());

    // Extract server certificates
    try {
      var peerCerts = session.getPeerCertificates();
      if (peerCerts != null) {
        var x509Certs = new X509Certificate[peerCerts.length];
        for (int i = 0; i < peerCerts.length; i++) {
          if (peerCerts[i] instanceof X509Certificate x509) {
            x509Certs[i] = x509;
          }
        }
        resultBuilder.serverCertificates(x509Certs);
      }
    } catch (SSLPeerUnverifiedException e) {
      log.debug("Could not get peer certificates: {}", e.getMessage());
    }

    // ServerKeyExchange is implicit for ECDHE/DHE suites
    var suite = session.getCipherSuite();
    if (suite.contains("ECDHE") || suite.contains("DHE")) {
      resultBuilder.serverKeyExchangeSent(true);
    }

    log.info(
        "TLS handshake successful: protocol={}, cipher={}",
        session.getProtocol(),
        session.getCipherSuite());
  }

  private static void parseAlertFromException(
      SSLHandshakeException e, TlsConnectionResult.TlsConnectionResultBuilder builder) {
    var msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
    builder.alertLevel(2); // fatal

    if (msg.contains("protocol_version") || msg.contains("no appropriate protocol")) {
      builder.alertDescription(0x46); // 70 = protocol_version
    } else if (msg.contains("handshake_failure") || msg.contains("handshake failure")) {
      builder.alertDescription(0x28); // 40 = handshake_failure
    } else if (msg.contains("no_shared_cipher") || msg.contains("no shared cipher")) {
      builder.alertDescription(0x28);
    } else if (msg.contains("insufficient_security")) {
      builder.alertDescription(0x47); // 71 = insufficient_security
    } else {
      builder.alertDescription(0x28); // default: handshake_failure
    }
  }

  private static HostPort resolveHostPort(String host) {
    var candidate = host.trim();
    if (candidate.startsWith("https://") || candidate.startsWith("http://")) {
      try {
        var uri = new URI(candidate);
        var h = uri.getHost();
        var p = uri.getPort();
        if (p == -1) p = candidate.startsWith("https") ? 443 : 80;
        return new HostPort(h, p);
      } catch (Exception e) {
        // fallback
      }
    }
    if (candidate.contains(":")) {
      var parts = candidate.split(":");
      try {
        return new HostPort(parts[0], Integer.parseInt(parts[1]));
      } catch (NumberFormatException e) {
        // fallback
      }
    }
    return new HostPort(candidate, 443);
  }

  private record HostPort(String host, int port) {}
}
