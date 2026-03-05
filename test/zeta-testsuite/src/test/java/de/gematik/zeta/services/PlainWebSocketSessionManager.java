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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * Minimal plain WebSocket (text frames) session manager for tests.
 *
 * <p>We use it for PoPP endpoints which are not STOMP-based.
 */
@Slf4j
public class PlainWebSocketSessionManager {

  @Setter private static int connectionTimeoutSeconds = 5;
  @Setter private static int messageTimeoutSeconds = 5;
  @Setter @Getter private static String proxyUri = null;

  @Getter private WebSocketSession session;

  private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

  /** Connects to the given WebSocket URL (ws/wss). */
  public void connect(String url) {
    connect(url, null);
  }

  /** Connects to the given WebSocket URL (ws/wss) using custom handshake headers. */
  public void connect(String url, WebSocketHttpHeaders handshakeHeaders) {
    var resolvedUrl = TigerGlobalConfiguration.resolvePlaceholders(url).trim();
    validateTargetUrl(resolvedUrl);

    try {
      var clientManager = ClientManager.createClient();

      // IMPORTANT:
      // Only configure TLS when we really use a TLS endpoint (wss://).
      // If we configure an SSL engine for a plain ws:// endpoint, Tyrus will try to speak TLS and
      // fail with: "Unrecognized SSL message, plaintext connection?".
      if (resolvedUrl.startsWith("wss://")) {
        SslConfigurationService.configureForTesting();
        var sslContext = SslConfigurationService.getTrustAllSslContext();
        var sslEngineConfigurator = new SslEngineConfigurator(sslContext);
        sslEngineConfigurator.setHostnameVerifier((hostname, session) -> true);
        clientManager
            .getProperties()
            .put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
      }

      // Configure HTTP Proxy if set (for Tiger Proxy traffic recording)
      if (proxyUri != null && !proxyUri.isBlank()) {
        var resolvedProxyUri = TigerGlobalConfiguration.resolvePlaceholders(proxyUri).trim();
        log.info("Using HTTP proxy for WebSocket: {}", resolvedProxyUri);
        clientManager.getProperties().put(ClientProperties.PROXY_URI, resolvedProxyUri);
      }

      var webSocketClient = new StandardWebSocketClient(clientManager);

      var latch = new CountDownLatch(1);
      var errorRef = new AtomicReference<Throwable>();

      var handler =
          new AbstractWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(@NotNull WebSocketSession session) {
              PlainWebSocketSessionManager.this.session = session;
              latch.countDown();
            }

            @Override
            protected void handleTextMessage(
                @NotNull WebSocketSession session, @NotNull TextMessage message) {
              var payload = message.getPayload();
              log.info("RECEIVED WS: {}", payload);
              var ignored = messageQueue.offer(payload);
            }

            @Override
            public void handleTransportError(
                @NotNull WebSocketSession session, @NotNull Throwable exception) {
              errorRef.compareAndSet(null, exception);
              latch.countDown();
            }
          };

      var headers = handshakeHeaders != null ? handshakeHeaders : new WebSocketHttpHeaders();

      // Spring 6: use CompletableFuture-based handshake API (avoids deprecated doHandshake
      // overload)
      webSocketClient
          .execute(handler, headers, URI.create(resolvedUrl))
          .thenAccept(
              s -> {
                // Some implementations call afterConnectionEstablished synchronously, but we keep
                // this
                // as a safe fallback.
                if (PlainWebSocketSessionManager.this.session == null) {
                  PlainWebSocketSessionManager.this.session = s;
                }
                latch.countDown();
              })
          .exceptionally(
              ex -> {
                errorRef.compareAndSet(null, ex);
                latch.countDown();
                return null;
              });

      boolean connected = latch.await(connectionTimeoutSeconds, TimeUnit.SECONDS);
      var error = errorRef.get();
      if (error != null) {
        throw new AssertionError("WebSocket connection failed: " + error.getMessage(), error);
      }

      assertThat(connected)
          .withFailMessage(
              "WebSocket connection to '%s' not established within %d seconds",
              resolvedUrl, connectionTimeoutSeconds)
          .isTrue();

      assertThat(session).as("WebSocket session created").isNotNull();
      assertThat(session.isOpen()).as("WebSocket session open").isTrue();

    } catch (Exception e) {
      throw new AssertionError("Failed to open WebSocket connection to '" + resolvedUrl + "'", e);
    }
  }

  /** Sends a text frame. */
  public void sendText(String text) {
    assertThat(session).as("WebSocket session must be connected").isNotNull();
    assertThat(session.isOpen()).as("WebSocket session must be open").isTrue();

    var resolved = TigerGlobalConfiguration.resolvePlaceholders(Objects.toString(text, ""));

    try {
      session.sendMessage(new TextMessage(resolved));
      log.info("SENT WS: {}", resolved);
    } catch (Exception e) {
      throw new AssertionError("Failed to send WebSocket message: " + e.getMessage(), e);
    }
  }

  /** Awaits the next message (text frame) and stores it as last received. */
  public String awaitMessage() {
    try {
      var message = messageQueue.poll(messageTimeoutSeconds, TimeUnit.SECONDS);
      assertThat(message)
          .withFailMessage("No WebSocket message received within %d seconds", messageTimeoutSeconds)
          .isNotNull();
      return message;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for WebSocket message", e);
    }
  }

  /** Closes the session. */
  public void close() {
    if (session == null) {
      return;
    }
    try {
      session.close();
    } catch (Exception e) {
      log.warn("Failed to close WebSocket session: {}", e.getMessage());
    }
  }

  private void validateTargetUrl(String url) {
    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException ex) {
      throw new AssertionError("Invalid WebSocket URL '" + url + "': " + ex.getMessage(), ex);
    }

    var host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new AssertionError("WebSocket URL '" + url + "' does not contain a valid host");
    }

    try {
      // Fail fast when DNS cannot resolve the provided hostname.
      var ignored = InetAddress.getByName(host);
    } catch (Exception ex) {
      throw new AssertionError(
          String.format("WebSocket host '%s' cannot be resolved (URL: %s)", host, url), ex);
    }

    var port = uri.getPort();
    if (port < 0) {
      port =
          switch (uri.getScheme()) {
            case "wss", "https" -> 443;
            case "ws", "http" -> 80;
            default -> -1;
          };
    }

    if (port > 0) {
      var timeoutMillis =
          (int) Math.min(Duration.ofSeconds(connectionTimeoutSeconds).toMillis(), 2000);
      try (var socket = new Socket()) {
        socket.connect(new InetSocketAddress(host, port), timeoutMillis);
      } catch (Exception ex) {
        throw new AssertionError(
            String.format("Cannot reach WebSocket endpoint %s:%d (%s)", host, port, url), ex);
      }
    }
  }
}
