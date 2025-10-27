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
import de.gematik.ti20.client.zeta.request.ZetaWsRequest;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZetaWsSession {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final ZetaWsEventHandler eventHandler;
  private final ZetaWsRequest request;

  private okhttp3.WebSocket webSocket = null;

  private String lastMessage = null;
  private ZetaHttpException lastError = null;

  public ZetaWsSession(final ZetaWsRequest request, final ZetaWsEventHandler eventHandler) {
    this.request = request;
    this.eventHandler = eventHandler;
    eventHandler.setWsSession(this);
  }

  public void open(OkHttpClient client) {
    this.webSocket = client.newWebSocket(request.build(), eventHandler.getListener());
  }

  public void close(final int code, final String reason) {
    if (this.webSocket != null) {
      try {
        this.webSocket.close(code, reason);
      } catch (final Exception e) {
        log.warn(
            "Failed to close WebSocket connection with code {} and reason '{}': {}",
            code,
            reason,
            e.getMessage());
        // nothing to do
      }
    }
  }

  public void send(final String message) {
    if (this.webSocket == null) {
      throw new IllegalStateException("WebSocket is not open. Cannot send message: " + message);
    }

    final Boolean result = this.webSocket.send(message);

    if (!result) {
      throw new IllegalStateException(
          "Failed to send message:  " + message + "; trace Id " + request.getTraceId());
    }
  }

  public ZetaWsEventHandler getEventHandler() {
    return eventHandler;
  }

  public ZetaWsRequest getRequest() {
    return request;
  }

  public void setLastMessage(String message) {
    this.lastMessage = message;
    this.lastError = null;
  }

  public String getLastMessage() {
    return lastMessage;
  }

  public void setLastError(ZetaHttpException error) {
    this.lastError = error;
    this.lastMessage = null;
  }

  public ZetaHttpException getLastError() {
    return lastError;
  }

  public boolean isOpen() {
    return this.webSocket != null;
  }

  public boolean hasError() {
    return lastError != null;
  }

  public boolean hasMessage() {
    return lastMessage != null;
  }
}
