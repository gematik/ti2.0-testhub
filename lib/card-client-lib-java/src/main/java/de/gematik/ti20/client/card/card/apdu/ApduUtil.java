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
package de.gematik.ti20.client.card.card.apdu;

import java.security.MessageDigest;

/** Utility class for APDU operations. */
public class ApduUtil {

  // Status codes
  private static final int STATUS_SUCCESS = 0x9000;

  // Common AIDs
  public static final byte[] AID_SIGNATURE_APPLICATION =
      new byte[] {
        (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00,
        (byte) 0x85, (byte) 0x01, (byte) 0x00, (byte) 0x00
      };

  /**
   * Creates a SELECT command APDU for selecting an application by AID.
   *
   * @param aid the application identifier
   * @return the SELECT command as byte array
   */
  public static byte[] createSelectApdu(byte[] aid) {
    byte[] command = new byte[6 + aid.length];
    command[0] = 0x00; // CLA
    command[1] = (byte) 0xA4; // INS - SELECT
    command[2] = 0x04; // P1 - Select by AID
    command[3] = 0x0C; // P2 - No FCI
    command[4] = (byte) aid.length; // Lc

    System.arraycopy(aid, 0, command, 5, aid.length);
    command[5 + aid.length] = 0x00; // Le

    return command;
  }

  /**
   * Alias for compatibility with old code. Creates a SELECT command APDU for selecting an
   * application by AID string.
   *
   * @param aidString the application identifier as hex string
   * @return the SELECT command as byte array
   */
  public static byte[] buildSelectApplicationApdu(String aidString) {
    byte[] aid;
    try {
      aid = javax.xml.bind.DatatypeConverter.parseHexBinary(aidString);
    } catch (Exception e) {
      // Default to signature AID if parsing fails
      aid = AID_SIGNATURE_APPLICATION;
    }
    return createSelectApdu(aid);
  }

  /**
   * Creates a MANAGE SECURITY ENVIRONMENT command APDU for setting up a signature operation with
   * default key reference "1".
   *
   * @return the MSE command APDU
   */
  public static byte[] createMSESetCommand() {
    return createMSESetCommand("1");
  }

  /**
   * Creates a MANAGE SECURITY ENVIRONMENT command APDU for setting up a signature operation with a
   * specific key reference.
   *
   * @param keyReference the key reference to use for signing (decimal or hex string with 0x prefix)
   * @return the MSE command APDU
   */
  public static byte[] createMSESetCommand(String keyReference) {
    // Parse key reference appropriately
    int keyRef = 1; // Default key reference

    try {
      if (keyReference.startsWith("0x")) {
        // Hex string with prefix
        keyRef = Integer.parseInt(keyReference.substring(2), 16);
      } else if (keyReference.matches("[0-9A-Fa-f]{1,2}")) {
        // Looks like a hex string without prefix
        keyRef = Integer.parseInt(keyReference, 16);
      } else {
        // Treat as decimal
        keyRef = Integer.parseInt(keyReference);
      }
    } catch (NumberFormatException e) {
      // Use default key reference 1
    }

    // Example MSE:SET for signature with algorithm SHA-256/RSA
    byte[] command =
        new byte[] {
          0x00, // CLA
          0x22, // INS - MANAGE SECURITY ENVIRONMENT
          0x41, // P1 - SET
          (byte) 0xB6, // P2 - For signature
          0x06, // Lc
          (byte) 0x80,
          0x01,
          0x10, // Algorithm SHA-256 + PKCS#1-V1.5
          (byte) 0x83,
          0x01,
          (byte) keyRef // Key reference
        };

    return command;
  }

  /**
   * Creates a VERIFY command APDU for PIN verification.
   *
   * @param pin the PIN
   * @return the VERIFY command APDU
   */
  public static byte[] createVerifyPINCommand(byte[] pin) {
    byte[] command = new byte[5 + pin.length];
    command[0] = 0x00; // CLA
    command[1] = 0x20; // INS - VERIFY
    command[2] = 0x00; // P1
    command[3] = 0x01; // P2 - PIN reference 1
    command[4] = (byte) pin.length; // Lc

    System.arraycopy(pin, 0, command, 5, pin.length);

    return command;
  }

  /**
   * Creates a PERFORM SECURITY OPERATION command APDU for signing a hash.
   *
   * @param hash the hash to sign
   * @return the PSO command as byte array
   */
  public static byte[] createSignCommand(byte[] hash) {
    // Create PSO:COMPUTE DIGITAL SIGNATURE command
    byte[] command = new byte[5 + hash.length];
    command[0] = 0x00; // CLA
    command[1] = 0x2A; // INS - PERFORM SECURITY OPERATION
    command[2] = (byte) 0x9E; // P1 - Compute Digital Signature
    command[3] = (byte) 0x9A; // P2 - Input is a hash
    command[4] = (byte) hash.length; // Lc

    System.arraycopy(hash, 0, command, 5, hash.length);

    return command;
  }

  /**
   * Alias for createSignCommand for backward compatibility.
   *
   * @param hash the hash to sign
   * @return the PSO command as byte array
   */
  public static byte[] buildSignCommandApdu(byte[] hash) {
    return createSignCommand(hash);
  }

  /**
   * Creates a SHA-256 hash from data.
   *
   * @param data the data to hash
   * @return the hash
   * @throws Exception if hashing fails
   */
  public static byte[] createHashFromData(byte[] data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return digest.digest(data);
  }

  /** Alias for createHashFromData to maintain backward compatibility. */
  public static byte[] hashData(byte[] data) throws Exception {
    return createHashFromData(data);
  }

  /**
   * Checks if a response APDU indicates success.
   *
   * @param response the response APDU
   * @return true if the response indicates success, false otherwise
   */
  public static boolean isSuccess(byte[] response) {
    if (response.length < 2) {
      return false;
    }

    int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
    return sw == STATUS_SUCCESS;
  }

  /** Alias for isSuccess for backward compatibility. */
  public static boolean isSuccessResponse(byte[] response) {
    return isSuccess(response);
  }

  /**
   * Returns a string representation of the response status.
   *
   * @param response the response APDU
   * @return the status as a string
   */
  public static String getResponseStatusString(byte[] response) {
    if (response.length < 2) {
      return "Invalid response";
    }

    int sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
    return String.format("0x%04X", sw);
  }

  /** Alias for getResponseStatusString for backward compatibility. */
  public static String getStatusString(byte[] response) {
    return getResponseStatusString(response);
  }

  /**
   * Extracts the data from a response APDU (without status bytes).
   *
   * @param response the response APDU
   * @return the data
   */
  public static byte[] extractDataFromResponse(byte[] response) {
    if (response.length <= 2) {
      return new byte[0];
    }

    byte[] data = new byte[response.length - 2];
    System.arraycopy(response, 0, data, 0, data.length);
    return data;
  }

  /** Alias for extractDataFromResponse for backward compatibility. */
  public static byte[] extractSignature(byte[] response) {
    return extractDataFromResponse(response);
  }

  /**
   * Converts a byte array to a hexadecimal string.
   *
   * @param bytes the byte array
   * @return the hexadecimal string
   */
  public static String bytesToHex(byte[] bytes) {
    StringBuilder hex = new StringBuilder();
    for (byte b : bytes) {
      hex.append(String.format("%02X", b));
    }
    return hex.toString();
  }

  /**
   * Converts a hexadecimal string to a byte array.
   *
   * @param hexString the hexadecimal string
   * @return the byte array
   */
  public static byte[] hexToBytes(String hexString) {
    int length = hexString.length();
    byte[] data = new byte[length / 2];
    for (int i = 0; i < length; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(hexString.charAt(i), 16) << 4)
                  + Character.digit(hexString.charAt(i + 1), 16));
    }
    return data;
  }
}
