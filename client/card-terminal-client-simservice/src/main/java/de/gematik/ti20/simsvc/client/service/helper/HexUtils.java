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

  private HexUtils() {}

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
}
