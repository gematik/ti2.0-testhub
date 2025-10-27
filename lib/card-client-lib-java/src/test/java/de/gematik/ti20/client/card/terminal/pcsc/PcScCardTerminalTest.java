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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.CardType;
import de.gematik.ti20.client.card.card.CardTypeDetector;
import de.gematik.ti20.client.card.config.PcScConnectionConfig;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import java.util.List;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class PcScCardTerminalTest {

  private PcScConnectionConfig config;
  private CardTerminal terminal;
  private PcScCardTerminal pcscTerminal;

  @BeforeEach
  void setUp() {
    config = mock(PcScConnectionConfig.class);
    when(config.getName()).thenReturn("Reader1");
    terminal = mock(CardTerminal.class);
    pcscTerminal = new PcScCardTerminal(config, terminal);
  }

  @Test
  void testGetAttachedCards_cardPresent() throws Exception {
    when(terminal.isCardPresent()).thenReturn(true);

    Card card = mock(Card.class);
    ATR atr = new ATR(new byte[] {0x3B, 0x13, 0x00});
    when(terminal.connect("*")).thenReturn(card);
    when(card.getATR()).thenReturn(atr);

    try (MockedStatic<CardTypeDetector> detector = mockStatic(CardTypeDetector.class)) {
      detector.when(() -> CardTypeDetector.detectTypeFromATR(atr)).thenReturn(CardType.EGK);

      List<? extends AttachedCard> cards = pcscTerminal.getAttachedCards();
      assertEquals(1, cards.size());
      AttachedCard attached = cards.get(0);
      assertTrue(attached instanceof PcScAttachedCard);
      assertEquals("Reader1-3B1300", attached.getId());
      assertEquals(CardType.EGK, attached.getType());
    }

    verify(card, times(1)).disconnect(false);
  }

  @Test
  void testGetAttachedCards_noCardPresent() throws Exception {
    when(terminal.isCardPresent()).thenReturn(false);
    List<? extends AttachedCard> cards = pcscTerminal.getAttachedCards();
    assertTrue(cards.isEmpty());
  }

  @Test
  void testGetAttachedCards_cardException() throws Exception {
    when(terminal.isCardPresent()).thenThrow(new CardException("fail"));
    assertThrows(CardTerminalException.class, () -> pcscTerminal.getAttachedCards());
  }

  @Test
  void testConnect_success() throws Exception {
    PcScAttachedCard card = mock(PcScAttachedCard.class);
    Card physicalCard = mock(Card.class);
    CardChannel channel = mock(CardChannel.class);

    when(terminal.connect("*")).thenReturn(physicalCard);
    when(physicalCard.getBasicChannel()).thenReturn(channel);

    CardConnection connection = pcscTerminal.connect(card);
    assertNotNull(connection);
    assertTrue(connection instanceof PcScCardConnection);
  }

  @Test
  void testConnect_wrongType() {
    AttachedCard card = mock(AttachedCard.class);
    assertThrows(CardTerminalException.class, () -> pcscTerminal.connect(card));
  }

  @Test
  void testConnect_cardException() throws Exception {
    PcScAttachedCard card = mock(PcScAttachedCard.class);
    when(terminal.connect("*")).thenThrow(new CardException("fail"));
    assertThrows(CardTerminalException.class, () -> pcscTerminal.connect(card));
  }
}
