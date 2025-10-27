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
package de.gematik.ti20.client.card.card;

import javax.smartcardio.ATR;

/** Utility class for detecting card types. */
public class CardTypeDetector {

  // Example ATR patterns for different German health card types
  // These are simplified examples and would need to be updated with real ATR patterns
  private static final byte[] EGK_ATR_PREFIX =
      new byte[] {(byte) 0x3B, (byte) 0xDD, (byte) 0x00, (byte) 0xFF, (byte) 0x81, (byte) 0x31};
  private static final byte[] HBA_ATR_PREFIX =
      new byte[] {(byte) 0x3B, (byte) 0xDF, (byte) 0x18, (byte) 0xFF, (byte) 0x81, (byte) 0x31};
  private static final byte[] SMC_B_ATR_PREFIX =
      new byte[] {(byte) 0x3B, (byte) 0xDF, (byte) 0x96, (byte) 0xFF, (byte) 0x81, (byte) 0x31};

  /**
   * Detects the card type based on its ATR.
   *
   * @param atr the Answer to Reset
   * @return the detected card type
   */
  public static CardType detectTypeFromATR(ATR atr) {
    byte[] atrBytes = atr.getBytes();

    if (matchesPrefix(atrBytes, EGK_ATR_PREFIX)) {
      return CardType.EGK;
    } else if (matchesPrefix(atrBytes, HBA_ATR_PREFIX)) {
      return CardType.HBA;
    } else if (matchesPrefix(atrBytes, SMC_B_ATR_PREFIX)) {
      return CardType.SMC_B;
    } else {
      return CardType.UNKNOWN;
    }
  }

  /**
   * Checks if a byte array starts with the specified prefix.
   *
   * @param bytes the byte array
   * @param prefix the prefix
   * @return true if the byte array starts with the prefix, false otherwise
   */
  private static boolean matchesPrefix(byte[] bytes, byte[] prefix) {
    if (bytes.length < prefix.length) {
      return false;
    }

    for (int i = 0; i < prefix.length; i++) {
      if (bytes[i] != prefix[i]) {
        return false;
      }
    }

    return true;
  }
}
