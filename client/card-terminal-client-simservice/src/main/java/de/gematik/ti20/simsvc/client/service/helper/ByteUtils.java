/*-
 * #%L
 * Card Terminal Simulator
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.service.helper;

public final class ByteUtils {
  private ByteUtils() {}

  public static byte[] getByteArray(final String hexString) {
    if (hexString == null) {
      return new byte[0];
    } else {
      final String hex = HexUtils.formatHexString(hexString, false);
      byte[] result = new byte[hex.length() / 2];
      char[] enc = hex.toCharArray();

      for (int i = 0; i < enc.length; i += 2) {
        final StringBuilder curr = new StringBuilder(2);
        curr.append(enc[i]).append(enc[i + 1]);
        result[i / 2] = (byte) Integer.parseInt(curr.toString(), 16);
      }

      return result;
    }
  }

  public static int getIntValue(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Parameter 'data' cannot be null.");
    } else if (data.length != 0 && data.length <= 4) {
      int len = 4;
      byte[] tmp = new byte[len];

      for (int i = 0; i < data.length; ++i) {
        tmp[i + len - data.length] = data[i];
      }

      int intValue = tmp[0] << 24;
      intValue |= (tmp[1] & 255) << 16;
      intValue |= (tmp[2] & 255) << 8;
      intValue |= tmp[3] & 255;
      if (intValue < 0) {
        throw new IllegalArgumentException("Byte array value too big for datatype 'int'.");
      } else {
        return intValue;
      }
    } else {
      throw new IllegalArgumentException(
          "Incorrect length of parameter 'data' [Expected=1..4,Found=" + data.length + "].");
    }
  }
}
