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
package de.gematik.ti20.client.zeta.websocket;

import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.exception.ZetaHttpResponseException;
import de.gematik.ti20.client.zeta.request.ZetaHttpRequest;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZetaWsEventHandler {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private ZetaWsSession wsSession = null;
  private final CompletableFuture<ZetaWsSession> resultFuture;

  public ZetaWsEventHandler() {
    this.resultFuture = null;
  }

  public ZetaWsEventHandler(final CompletableFuture<ZetaWsSession> resultFuture) {
    this.resultFuture = resultFuture;
  }

  public ZetaHttpRequest getRequest() {
    if (this.hasSession()) {
      return this.wsSession.getRequest();
    }
    throw new IllegalStateException(
        "ZetaWsSession is not yet initialized in this eventHandler, you need to call connect first");
  }

  public boolean hasSession() {
    return this.wsSession != null;
  }

  public ZetaWsSession getWsSession() {
    return this.wsSession;
  }

  public void setWsSession(ZetaWsSession wsSession) {
    this.wsSession = wsSession;
  }

  public void onConnected(ZetaWsSession ws) {
    // override it if you need some custom actions here
  }

  public void onDisconnected(ZetaWsSession ws) {
    // override it if you need some custom actions here
    if (resultFuture != null) {
      resultFuture.complete(ws);
    }
  }

  public void onException(ZetaWsSession ws, ZetaHttpException exception) {
    // override it if you need some custom actions here
    if (resultFuture != null) {
      ws.close(1000, "Exception on connection");
      resultFuture.completeExceptionally(exception);
    }
  }

  /**
   * This implementation just waits for the first message and closes the websocket connection. If
   * you need real asynchronous communication, override it and process the messages with additional
   * communication
   *
   * @param ws
   */
  public void onMessage(ZetaWsSession ws) {
    if (resultFuture != null) {
      // TODO: add code and message
      ws.close(1, "");
      resultFuture.complete(ws);
    }
  }

  public WebSocketListener getListener() {
    return listener;
  }

  private final WebSocketListener listener =
      new WebSocketListener() {
        @Override
        public void onOpen(final okhttp3.WebSocket webSocket, final okhttp3.Response response) {
          super.onOpen(webSocket, response);
          log.debug(
              "Opened connection with traceId {} to {}",
              getRequest().getTraceId(),
              getRequest().getUrl());
          ZetaWsEventHandler.this.onConnected(wsSession);
        }

        @Override
        public void onClosed(
            final okhttp3.WebSocket webSocket, final int code, final String reason) {
          super.onClosed(webSocket, code, reason);
          wsSession.close(code, reason);
          log.debug("Closed connection with traceId {}", getRequest().getTraceId());
          ZetaWsEventHandler.this.onDisconnected(wsSession);
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable t, okhttp3.Response response) {
          super.onFailure(webSocket, t, response);

          ZetaHttpResponseException customException;
          if (response != null && response.code() >= 400) {
            customException =
                new ZetaHttpResponseException(response.code(), response.message(), t, getRequest());
          } else {
            customException =
                new ZetaHttpResponseException(
                    HttpURLConnection.HTTP_INTERNAL_ERROR, t.getMessage(), t, getRequest());
          }

          if (customException.getCode() != 401) {
            log.warn(
                "Received exception {} for {} {}",
                customException.getCode(),
                getRequest().getTraceId(),
                customException.getMessage());
          }

          ZetaWsEventHandler.this.onException(wsSession, customException);
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, String text) {
          super.onMessage(webSocket, text);

          log.debug(
              "Received text message {} with length {}", getRequest().getTraceId(), text.length());

          wsSession.setLastMessage(text);

          ZetaWsEventHandler.this.onMessage(wsSession);
        }
      };
}
