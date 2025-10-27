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

public final class HexUtils {
  private static final byte[] HEX_CHAR_TABLE =
      new byte[] {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70};
  private static final String SPACE = " ";

  private HexUtils() {}

  public static String getHexString(int value) {
    String hexString = Integer.toHexString(value);
    return formatHexString(hexString, true);
  }

  public static String formatHexString(String hexString, boolean insertSpaces) {
    if (hexString == null) {
      return null;
    } else {
      String hex = removeSpaces(hexString);
      hex = hex.toUpperCase();
      if (hex.length() % 2 != 0) {
        hex = "0" + hex;
      }

      if (insertSpaces) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < hex.length(); ++i) {
          sb.append(hex.charAt(i));
          if (i % 2 == 1 && i != hex.length() - 1) {
            sb.append(" ");
          }
        }

        hex = sb.toString();
      }

      return hex;
    }
  }

  private static String removeSpaces(String hexString) {
    return !hexString.contains(" ") ? hexString : hexString.replace(" ", "");
  }

  public static String getHexStringNoSpaces(int value) {
    String hexString = Integer.toHexString(value);
    return formatHexString(hexString, false);
  }

  public static String getHexString(byte[] raw) {
    return formatHexString(convertToString(raw), true);
  }

  public static String getHexStringNoSpaces(byte[] raw) {
    return formatHexString(convertToString(raw), false);
  }

  private static String convertToString(byte[] raw) {
    if (raw == null) {
      return null;
    } else if (raw.length == 0) {
      return "";
    } else {
      byte[] hex = null;
      hex = new byte[2 * raw.length];
      int index = 0;

      for (byte b : raw) {
        int v = b & 255;
        hex[index++] = HEX_CHAR_TABLE[v >>> 4];
        hex[index++] = HEX_CHAR_TABLE[v & 15];
      }

      return new String(hex);
    }
  }

  public static int getOctettCount(String hexString) {
    String hexStringWithoutSpaces = removeSpaces(hexString);
    return (int) Math.ceil((double) hexStringWithoutSpaces.length() / (double) 2.0F);
  }
}
