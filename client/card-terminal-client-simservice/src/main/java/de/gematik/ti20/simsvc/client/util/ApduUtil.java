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
package de.gematik.ti20.simsvc.client.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/** Utility class for APDU-related operations. */
public class ApduUtil {

  /** Private constructor to prevent instantiation. */
  private ApduUtil() {
    // Utility class should not be instantiated
  }

  /**
   * Convert a hex string to a byte array.
   *
   * @param hexString Hex string (e.g., "00A4040C")
   * @return Byte array
   * @throws IllegalArgumentException If the hex string is invalid
   */
  public static byte[] hexToBytes(String hexString) {
    if (hexString == null || hexString.isEmpty()) {
      return new byte[0];
    }

    // Remove spaces and non-hex characters
    String cleanHex = hexString.replaceAll("[^0-9A-Fa-f]", "");

    try {
      return Hex.decodeHex(cleanHex);
    } catch (DecoderException e) {
      throw new IllegalArgumentException("Invalid hex string: " + hexString, e);
    }
  }

  /**
   * Convert a byte array to a hex string.
   *
   * @param bytes Byte array
   * @return Hex string (e.g., "00A4040C")
   */
  public static String bytesToHex(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return "";
    }

    return Hex.encodeHexString(bytes).toUpperCase();
  }

  /**
   * Extract the status word from an APDU response.
   *
   * @param response APDU response as a byte array
   * @return Status word as an integer
   * @throws IllegalArgumentException If the response is too short
   */
  public static int extractStatusWord(byte[] response) {
    if (response == null || response.length < 2) {
      throw new IllegalArgumentException("Invalid response: too short");
    }

    int sw1 = response[response.length - 2] & 0xFF;
    int sw2 = response[response.length - 1] & 0xFF;

    return (sw1 << 8) | sw2;
  }

  /**
   * Extract the data portion from an APDU response.
   *
   * @param response APDU response as a byte array
   * @return Data portion (excluding status word)
   * @throws IllegalArgumentException If the response is too short
   */
  public static byte[] extractResponseData(byte[] response) {
    if (response == null || response.length < 2) {
      throw new IllegalArgumentException("Invalid response: too short");
    }

    if (response.length == 2) {
      return new byte[0]; // No data, only status word
    }

    byte[] data = new byte[response.length - 2];
    System.arraycopy(response, 0, data, 0, data.length);

    return data;
  }

  /**
   * Format an APDU command for logging.
   *
   * @param command APDU command as a byte array
   * @return Formatted string representation
   */
  public static String formatApduCommand(byte[] command) {
    if (command == null || command.length < 4) {
      return "Invalid APDU command";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("CLA: ").append(String.format("%02X", command[0]));
    sb.append(", INS: ").append(String.format("%02X", command[1]));
    sb.append(", P1: ").append(String.format("%02X", command[2]));
    sb.append(", P2: ").append(String.format("%02X", command[3]));

    if (command.length > 4) {
      if (command.length == 5) {
        sb.append(", Le: ").append(String.format("%02X", command[4]));
      } else {
        int lc = command[4] & 0xFF;
        sb.append(", Lc: ").append(String.format("%02X", lc));

        if (command.length >= 5 + lc) {
          sb.append(", Data: ");
          for (int i = 5; i < 5 + lc && i < command.length; i++) {
            sb.append(String.format("%02X", command[i]));
          }

          if (command.length > 5 + lc) {
            sb.append(", Le: ").append(String.format("%02X", command[5 + lc]));
          }
        }
      }
    }

    return sb.toString();
  }
}
