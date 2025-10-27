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
package de.gematik.ti20.client.card.terminal.simsvc;

import de.gematik.ti20.client.card.card.AttachedCard;

/** Implementation of Card for simulated card terminals. */
public class SimulatorAttachedCard extends AttachedCard {

  private Integer slotId;
  private String label;

  public SimulatorAttachedCard(final AttachedCardInfo info, final SimulatorCardTerminal terminal) {
    super(info.getId(), info.getCardType(), terminal);
    this.slotId = info.getSlotId();
    this.label = info.getLabel();
  }

  public Integer getSlotId() {
    return slotId;
  }

  public String getLabel() {
    return label;
  }

  /** {@inheritDoc} */
  @Override
  public String getInfo() {
    return String.format("Type: %s, Simulator ID: %s", getType().getDescription(), getId());
  }
}
