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
package de.gematik.ti20.client.card.exception;

import de.gematik.ti20.client.card.terminal.TerminalSlotV1;
import java.io.Serial;

public class CardTerminalSlotException extends CardTerminalException {

  @Serial private static final long serialVersionUID = -8205168705218953858L;

  private final TerminalSlotV1 terminalSlot;

  public CardTerminalSlotException(String message, TerminalSlotV1 terminalSlot) {
    super(message, (terminalSlot == null) ? null : terminalSlot.getTerminal());
    this.terminalSlot = terminalSlot;
  }

  public CardTerminalSlotException(String message, TerminalSlotV1 terminalSlot, Throwable cause) {
    super(message, (terminalSlot == null) ? null : terminalSlot.getTerminal(), cause);
    this.terminalSlot = terminalSlot;
  }
}
