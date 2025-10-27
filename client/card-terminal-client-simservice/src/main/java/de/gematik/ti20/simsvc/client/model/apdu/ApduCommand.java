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
package de.gematik.ti20.simsvc.client.model.apdu;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents an APDU (Application Protocol Data Unit) command sent to a smart card. An APDU command
 * consists of a header and optional data.
 */
public class ApduCommand {

  private final byte cla; // Class byte
  private final byte ins; // Instruction byte
  private final byte p1; // Parameter 1
  private final byte p2; // Parameter 2
  private final byte[] data; // Command data
  private final Integer le; // Expected length of response

  /**
   * Constructor with individual components.
   *
   * @param cla Class byte
   * @param ins Instruction byte
   * @param p1 Parameter 1
   * @param p2 Parameter 2
   * @param data Command data (may be null)
   * @param le Expected length (may be null)
   */
  public ApduCommand(byte cla, byte ins, byte p1, byte p2, byte[] data, Integer le) {
    this.cla = cla;
    this.ins = ins;
    this.p1 = p1;
    this.p2 = p2;
    this.data = data != null ? data.clone() : null;
    this.le = le;
  }

  /**
   * Parse an APDU command from a hex string. Format: "CLA INS P1 P2 [Lc DATA] [Le]"
   *
   * @param hexCommand Hex string representation of the command
   * @return Parsed ApduCommand
   * @throws IllegalArgumentException If the hex string is invalid
   */
  public static ApduCommand fromHex(String hexCommand) {
    if (StringUtils.isBlank(hexCommand)) {
      throw new IllegalArgumentException("APDU command cannot be empty");
    }

    // Remove spaces and convert to lowercase
    String normalized = hexCommand.replaceAll("\\s+", "").toLowerCase();

    try {
      byte[] bytes = Hex.decodeHex(normalized);

      if (bytes.length < 4) {
        throw new IllegalArgumentException("APDU command must have at least 4 bytes (header)");
      }

      byte cla = bytes[0];
      byte ins = bytes[1];
      byte p1 = bytes[2];
      byte p2 = bytes[3];

      byte[] data = null;
      Integer le = null;

      if (bytes.length > 4) {
        // Case 2: CLA INS P1 P2 Le
        if (bytes.length == 5) {
          le = bytes[4] & 0xFF;
          if (le == 0) {
            le = 256; // Le=0 means 256 bytes expected
          }
        }
        // Case 3: CLA INS P1 P2 Lc DATA
        else if (bytes.length > 5) {
          int lc = bytes[4] & 0xFF;
          if (lc > 0) {
            // Sonderfall: Wenn die restliche Datenlänge kleiner als Lc ist,
            // nehmen wir an, dass die verbleibenden Bytes die Daten darstellen
            // (dies unterstützt TLV-Strukturen, die über mehrere Befehle verteilt sind)
            if (bytes.length < 5 + lc) {
              data = new byte[bytes.length - 5];
              System.arraycopy(bytes, 5, data, 0, bytes.length - 5);
            }
            // Normaler Fall: Genug Bytes für die angegebene Datenlänge
            else if (bytes.length >= 5 + lc) {
              data = new byte[lc];
              System.arraycopy(bytes, 5, data, 0, lc);

              // Case 4: CLA INS P1 P2 Lc DATA Le
              if (bytes.length > 5 + lc) {
                le = bytes[5 + lc] & 0xFF;
                if (le == 0) {
                  le = 256; // Le=0 means 256 bytes expected
                }
              }
            }
          }
        }
      }

      return new ApduCommand(cla, ins, p1, p2, data, le);

    } catch (DecoderException e) {
      throw new IllegalArgumentException("Invalid hex string: " + e.getMessage(), e);
    }
  }

  /**
   * Convert the APDU command to a byte array.
   *
   * @return Byte array representation of the command
   */
  public byte[] toBytes() {
    int length = 4; // Header is always 4 bytes

    if (data != null && data.length > 0) {
      length += 1 + data.length; // Lc + DATA
    }

    if (le != null) {
      length += 1; // Le
    }

    byte[] bytes = new byte[length];
    bytes[0] = cla;
    bytes[1] = ins;
    bytes[2] = p1;
    bytes[3] = p2;

    int offset = 4;

    if (data != null && data.length > 0) {
      bytes[offset++] = (byte) data.length;
      System.arraycopy(data, 0, bytes, offset, data.length);
      offset += data.length;
    }

    if (le != null) {
      bytes[offset] = le == 256 ? 0 : le.byteValue();
    }

    return bytes;
  }

  /**
   * Convert the APDU command to a hex string.
   *
   * @return Hex string representation of the command
   */
  public String toHexString() {
    return Hex.encodeHexString(toBytes()).toUpperCase();
  }

  /**
   * Get the class byte.
   *
   * @return Class byte
   */
  public byte getCla() {
    return cla;
  }

  /**
   * Get the instruction byte.
   *
   * @return Instruction byte
   */
  public byte getIns() {
    return ins;
  }

  /**
   * Get parameter 1.
   *
   * @return Parameter 1
   */
  public byte getP1() {
    return p1;
  }

  /**
   * Get parameter 2.
   *
   * @return Parameter 2
   */
  public byte getP2() {
    return p2;
  }

  /**
   * Get the command data.
   *
   * @return Command data (may be null)
   */
  public byte[] getData() {
    return data != null ? data.clone() : null;
  }

  /**
   * Get the expected length.
   *
   * @return Expected length (may be null)
   */
  public Integer getLe() {
    return le;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ApduCommand: ");
    sb.append(String.format("%02X %02X %02X %02X", cla, ins, p1, p2));

    if (data != null && data.length > 0) {
      sb.append(String.format(" %02X", data.length));
      sb.append(" [");
      for (byte b : data) {
        sb.append(String.format("%02X", b));
      }
      sb.append("]");
    }

    if (le != null) {
      sb.append(String.format(" %02X", le == 256 ? 0 : le));
    }

    return sb.toString();
  }
}
