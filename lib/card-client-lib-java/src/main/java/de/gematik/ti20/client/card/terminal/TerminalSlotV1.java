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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.gematik.ti20.client.card.config.TerminalSlotConfig;
import de.gematik.ti20.client.card.exception.CardMessageException;
import de.gematik.ti20.client.card.message.CardMessage;

public abstract class TerminalSlotV1 {

  public enum Type {
    JSON,
    APDU;

    @JsonCreator
    public static TerminalSlotV1.Type fromString(String type) {
      return TerminalSlotV1.Type.valueOf(type.toUpperCase());
    }

    @JsonValue
    public String toString() {
      return this.name().toLowerCase();
    }
  }

  protected final TerminalSlotConfig config;
  protected final CardTerminalV1 terminal;

  protected boolean connected = false;
  protected boolean cardInside = false;

  public TerminalSlotV1(TerminalSlotConfig config, CardTerminalV1 terminal) {
    this.config = config;
    this.terminal = terminal;
  }

  public TerminalSlotConfig getConfig() {
    return config;
  }

  public CardTerminalV1 getTerminal() {
    return terminal;
  }

  public void init() {}

  public abstract static class TerminalSlotEventHandler {

    public abstract void onCardInserted(TerminalSlotV1 slot);

    public abstract void onCardRemoved(TerminalSlotV1 slot);
  }

  public abstract void connect();

  public abstract void disconnect();

  public abstract void send(CardMessage message) throws CardMessageException;
}
