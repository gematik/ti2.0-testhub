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
package de.gematik.ti20.simsvc.client.service.helper;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public final class ByteUtils {
  private ByteUtils() {}

  public static byte[] getByteArray(String hexString) {
    if (hexString == null) {
      return null;
    } else {
      String hex = HexUtils.formatHexString(hexString, false);
      byte[] result = new byte[hex.length() / 2];
      char[] enc = hex.toCharArray();

      for (int i = 0; i < enc.length; i += 2) {
        StringBuilder curr = new StringBuilder(2);
        curr.append(enc[i]).append(enc[i + 1]);
        result[i / 2] = (byte) Integer.parseInt(curr.toString(), 16);
      }

      return result;
    }
  }

  public static byte[] getByteArray(int value) {
    if (value == 0) {
      return new byte[] {0};
    } else {
      byte[] array;
      for (array =
              new byte[] {
                (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value
              };
          array.length > 0 && array[0] == 0;
          array = subarray(array, 1)) {}

      return array;
    }
  }

  public static int getIntValue(byte[] data) {
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

  public static byte[] subarray(byte[] array, int fromIndex, int length) {
    if (array == null) {
      return null;
    } else {
      byte[] tempArray = new byte[length];
      System.arraycopy(array, fromIndex, tempArray, 0, tempArray.length);
      return tempArray;
    }
  }

  public static byte[] subarray(byte[] array, int fromIndex) {
    return array == null ? null : subarray(array, fromIndex, array.length - fromIndex);
  }

  public static byte[] unzipByteArray(byte[] input) throws IOException {
    GZIPInputStream unzippedStream = new GZIPInputStream(new ByteArrayInputStream(input));
    BufferedInputStream in = new BufferedInputStream(unzippedStream);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];

    int length;
    while ((length = in.read(buffer)) != -1) {
      out.write(buffer, 0, length);
    }

    return out.toByteArray();
  }
}
