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
package de.gematik.ti20.client.card.terminal;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.gematik.ti20.client.card.config.TerminalSlotConfig;
import de.gematik.ti20.client.card.exception.CardMessageException;
import de.gematik.ti20.client.card.exception.CardTerminalSlotException;
import de.gematik.ti20.client.card.message.CardMessage;
import de.gematik.ti20.client.card.message.CardMessageParser;
import de.gematik.ti20.client.card.message.CardResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminalSlotJsonV1 extends TerminalSlotV1 {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final OkHttpClient client = new OkHttpClient();
  private WebSocket ws;

  public TerminalSlotJsonV1(TerminalSlotConfig config, CardTerminalV1 terminal) {
    super(config, terminal);
  }

  private final WebSocketListener listener =
      new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
          super.onOpen(webSocket, response);
          TerminalSlotJsonV1.this.connected = true;
          terminal.eventHandler.onConnected(TerminalSlotJsonV1.this);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
          super.onClosed(webSocket, code, reason);
          TerminalSlotJsonV1.this.connected = false;
          terminal.eventHandler.onDisconnected(TerminalSlotJsonV1.this);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
          super.onMessage(webSocket, text);

          var terminal = TerminalSlotJsonV1.this.getTerminal();

          CardResult result;
          try {
            result = CardMessageParser.parseResultFromJson(text);
          } catch (CardMessageException e) {
            log.error("Failed to parse message from JSON", e);
            terminal.eventHandler.onError(
                new CardTerminalSlotException(e.getMessage(), TerminalSlotJsonV1.this, e));
            return;
          }

          if (result.getType() == CardResult.Type.EVENT) {
            switch (result.getEvent()) {
              case CARD_INSERTED:
                TerminalSlotJsonV1.this.cardInside = true;
                terminal.eventHandler.onCardInserted(TerminalSlotJsonV1.this);
                return;
              case CARD_REMOVED:
                TerminalSlotJsonV1.this.cardInside = false;
                terminal.eventHandler.onCardRemoved(TerminalSlotJsonV1.this);
                return;
            }
          }

          if (result.getType() == CardResult.Type.ERROR) {
            terminal.eventHandler.onError(
                new CardTerminalSlotException(
                    result.getError().getMessage(), TerminalSlotJsonV1.this));
            return;
          }

          terminal.eventHandler.onMessage(TerminalSlotJsonV1.this, result);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
          super.onClosing(webSocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
          super.onFailure(webSocket, t, response);

          var terminal = TerminalSlotJsonV1.this.getTerminal();
          terminal.eventHandler.onError(
              new CardTerminalSlotException(t.getMessage(), TerminalSlotJsonV1.this, t));
        }
      };

  public void connect() {
    var connectionInfo = this.terminal.getConfig().getConnection();

    var url = connectionInfo.get("url") + "/cardreader/slot/" + this.config.getSlotId();

    Request request = new Request.Builder().url(url).build();

    ws = client.newWebSocket(request, listener);
  }

  public void disconnect() {
    try {
      if (ws != null) {
        ws.close(1000, "Normal closure");
      }
    } catch (Exception e) {
      log.warn("Error on disconnecting from terminal slot", e);
    }
  }

  public void send(CardMessage message) throws CardTerminalSlotException {
    try {
      var json = message.getJson();
      ws.send(json);
    } catch (JsonProcessingException e) {
      throw new CardTerminalSlotException("Failed to serialize message to JSON", this, e);
    } catch (Exception e) {
      throw new CardTerminalSlotException("Failed to send a message to carddata", this, e);
    }
  }
}
