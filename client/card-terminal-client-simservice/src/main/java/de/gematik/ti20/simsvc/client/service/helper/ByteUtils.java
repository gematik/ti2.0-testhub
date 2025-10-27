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
import java.util.Date;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ByteUtils {
  private ByteUtils() {}

  public static byte[] concatenate(byte[]... data) {
    int resultLength = 0;

    for (byte[] array : data) {
      if (array != null) {
        resultLength += array.length;
      }
    }

    byte[] result = new byte[resultLength];
    int offset = 0;

    for (byte[] array : data) {
      if (array != null) {
        System.arraycopy(array, 0, result, offset, array.length);
        offset += array.length;
      }
    }

    return result;
  }

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

  public static byte[] getByteArrayAdapted(int value, int arrayLength) {
    byte[] array = getByteArray(value);
    if (array.length > arrayLength) {
      throw new IllegalArgumentException(
          "Invalid arrayLength parameter. Array of length="
              + array.length
              + " cannot be adapted to length="
              + arrayLength
              + ".");
    } else if (array.length < arrayLength) {
      byte[] newArray = new byte[arrayLength];
      System.arraycopy(array, 0, newArray, arrayLength - array.length, array.length);
      return newArray;
    } else {
      return array;
    }
  }

  public static byte[] getByteArray(long value) {
    if (value == 0L) {
      return new byte[] {0};
    } else {
      byte[] array;
      for (array =
              new byte[] {
                (byte) ((int) (value >>> 56)),
                (byte) ((int) (value >>> 48)),
                (byte) ((int) (value >>> 40)),
                (byte) ((int) (value >>> 32)),
                (byte) ((int) (value >>> 24)),
                (byte) ((int) (value >>> 16)),
                (byte) ((int) (value >>> 8)),
                (byte) ((int) value)
              };
          array.length > 0 && array[0] == 0;
          array = subarray(array, 1)) {}

      return array;
    }
  }

  public static int getIntValue(byte b) {
    return b & 255;
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

  public static int getIntValue(String hexValue) {
    return Integer.valueOf(hexValue, 16);
  }

  public static long getLongValue(String hexValue) {
    return Long.valueOf(hexValue, 16);
  }

  public static long getLongValue(byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Parameter 'data' cannot be null.");
    } else if (data.length != 0 && data.length <= 8) {
      int len = 8;
      byte[] tmp = new byte[len];

      for (int i = 0; i < data.length; ++i) {
        tmp[i + len - data.length] = data[i];
      }

      long longValue = (long) tmp[0] << 56;
      longValue |= ((long) tmp[1] & 255L) << 48;
      longValue |= ((long) tmp[2] & 255L) << 40;
      longValue |= ((long) tmp[3] & 255L) << 32;
      longValue |= ((long) tmp[4] & 255L) << 24;
      longValue |= ((long) tmp[5] & 255L) << 16;
      longValue |= ((long) tmp[6] & 255L) << 8;
      longValue |= (long) tmp[7] & 255L;
      if (longValue < 0L) {
        throw new IllegalArgumentException("Byte array value too big for datatype 'long'.");
      } else {
        return longValue;
      }
    } else {
      throw new IllegalArgumentException(
          "Incorrect length of parameter 'data' [Expected=1..8,Found=" + data.length + "].");
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

  public static byte[] adaptByteArrayToLength(byte[] bytes, int length, byte fillByte) {
    byte[] result = new byte[length];

    for (int i = 0; i < result.length; ++i) {
      result[i] = fillByte;
    }

    if (bytes != null) {
      int arrLength = bytes.length;
      if (arrLength > result.length) {
        arrLength = result.length;
      }

      System.arraycopy(bytes, 0, result, 0, arrLength);
    }

    return result;
  }

  public static byte[] gzipByteArray(byte[] input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GZIPOutputStream gzipOut = new GZIPOutputStream(out);
    gzipOut.write(input);
    gzipOut.close();
    return out.toByteArray();
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

  public static byte[] generateRandom(int length) {
    Random random = new Random((new Date()).getTime());
    byte[] randomBytes = new byte[length];
    random.nextBytes(randomBytes);
    return randomBytes;
  }
}
