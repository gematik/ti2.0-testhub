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
import de.gematik.ti20.client.card.config.CardTerminalConfig;
import de.gematik.ti20.client.card.exception.CardTerminalException;
import de.gematik.ti20.client.card.message.CardResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import okhttp3.OkHttpClient;

public class CardTerminalV1 extends TerminalSlotV1.TerminalSlotEventHandler {

  public enum Type {
    SIMULATOR,
    SIMSVC;

    // TODO: add more terminal types

    @JsonCreator
    public static Type fromString(String type) {
      return Type.valueOf(type.toUpperCase());
    }

    @JsonValue
    public String toString() {
      return this.name().toUpperCase();
    }
  }

  private final CardTerminalConfig config;
  protected final List<TerminalSlotV1> slots = Collections.synchronizedList(new ArrayList<>());
  protected CardTerminalEventHandler eventHandler;

  public interface CardTerminalEventHandler {

    void onCardInserted(TerminalSlotV1 slot);

    void onCardRemoved(TerminalSlotV1 slot);

    void onConnected(TerminalSlotV1 terminal);

    void onDisconnected(TerminalSlotV1 terminal);

    void onMessage(TerminalSlotV1 slot, CardResult cardResult);

    void onError(CardTerminalException exception);
  }

  public CardTerminalV1(CardTerminalConfig config, OkHttpClient okHttpClient) {
    this.config = config;
    config
        .getSlots()
        .forEach(
            slotConfig ->
                slots.add(TerminalSlotFactoryV1.createFrom(slotConfig, this, okHttpClient)));
  }

  public CardTerminalConfig getConfig() {
    return config;
  }

  public TerminalSlotV1 getSlot(String slotId) {
    return slots.stream()
        .filter(slot -> slot.getConfig().getSlotId().equals(slotId))
        .findFirst()
        .orElse(null);
  }

  public void init() {
    slots.forEach(TerminalSlotV1::init);
  }

  public void connect(String slotId, CardTerminalEventHandler eventHandler) {
    this.eventHandler = eventHandler;
    TerminalSlotV1 slot = getSlot(slotId);
    if (slot == null) {
      throw new IllegalArgumentException("Slot not found: " + slotId);
    }
    slot.connect();
  }

  public void disconnect(String slotId) {
    TerminalSlotV1 slot = getSlot(slotId);
    if (slot == null) {
      throw new IllegalArgumentException("Slot not found: " + slotId);
    }
    slot.disconnect();
  }

  @Override
  public void onCardInserted(TerminalSlotV1 slot) {
    this.eventHandler.onCardInserted(slot);
  }

  @Override
  public void onCardRemoved(TerminalSlotV1 slot) {
    this.eventHandler.onCardRemoved(slot);
  }
}
