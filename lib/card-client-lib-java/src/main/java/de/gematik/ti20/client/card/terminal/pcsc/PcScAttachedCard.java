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
package de.gematik.ti20.client.card.terminal.pcsc;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardType;
import javax.smartcardio.ATR;

/** Implementation of Card for USB card terminals. */
public class PcScAttachedCard extends AttachedCard {

  private final ATR atr;

  /**
   * Constructs a new USB card.
   *
   * @param id the card identifier
   * @param type the card type
   * @param atr the Answer to Reset of the card
   */
  public PcScAttachedCard(String id, CardType type, PcScCardTerminal terminal, ATR atr) {
    super(id, type, terminal);
    this.atr = atr;
  }

  /** {@inheritDoc} */
  @Override
  public String getInfo() {
    byte[] atrBytes = atr.getBytes();
    StringBuilder hexATR = new StringBuilder();
    for (byte b : atrBytes) {
      hexATR.append(String.format("%02X", b));
    }

    return String.format("Type: %s, ATR: %s", getType().getDescription(), hexATR);
  }

  /**
   * Returns the Answer to Reset of this card.
   *
   * @return the ATR
   */
  public ATR getATR() {
    return atr;
  }
}
