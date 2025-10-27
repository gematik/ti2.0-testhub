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

import org.apache.commons.codec.binary.Hex;

/**
 * Represents an APDU (Application Protocol Data Unit) response from a smart card. An APDU response
 * consists of optional data and a status word.
 */
public class ApduResponse {

  private final byte[] data; // Response data
  private final byte sw1; // Status byte 1
  private final byte sw2; // Status byte 2
  private final String statusMessage; // Human-readable status message

  /**
   * Constructor with data and status word components.
   *
   * @param data Response data (may be null)
   * @param sw1 Status byte 1
   * @param sw2 Status byte 2
   */
  public ApduResponse(byte[] data, byte sw1, byte sw2) {
    this.data = data != null ? data.clone() : null;
    this.sw1 = sw1;
    this.sw2 = sw2;
    this.statusMessage = getStatusMessageForSW(sw1, sw2);
  }

  /**
   * Constructor with status word only (no data).
   *
   * @param sw1 Status byte 1
   * @param sw2 Status byte 2
   */
  public ApduResponse(byte sw1, byte sw2) {
    this(null, sw1, sw2);
  }

  /**
   * Constructor with status word as a combined integer.
   *
   * @param statusWord Status word as a 16-bit integer
   */
  public ApduResponse(int statusWord) {
    this(null, (byte) ((statusWord >> 8) & 0xFF), (byte) (statusWord & 0xFF));
  }

  /**
   * Create a successful response (SW=9000) with data.
   *
   * @param data Response data
   * @return ApduResponse with success status
   */
  public static ApduResponse createSuccessResponse(byte[] data) {
    return new ApduResponse(data, (byte) 0x90, (byte) 0x00);
  }

  /**
   * Create a successful response (SW=9000) with no data.
   *
   * @return ApduResponse with success status
   */
  public static ApduResponse createSuccessResponse() {
    return new ApduResponse((byte) 0x90, (byte) 0x00);
  }

  /**
   * Create an error response with the given status word.
   *
   * @param statusWord Status word as a 16-bit integer
   * @return ApduResponse with error status
   */
  public static ApduResponse createErrorResponse(int statusWord) {
    return new ApduResponse(statusWord);
  }

  /**
   * Get the response data.
   *
   * @return Response data (may be null)
   */
  public byte[] getData() {
    return data != null ? data.clone() : null;
  }

  /**
   * Get status byte 1.
   *
   * @return Status byte 1
   */
  public byte getSw1() {
    return sw1;
  }

  /**
   * Get status byte 2.
   *
   * @return Status byte 2
   */
  public byte getSw2() {
    return sw2;
  }

  /**
   * Get the complete status word as a 16-bit integer.
   *
   * @return Status word
   */
  public int getStatusWord() {
    return ((sw1 & 0xFF) << 8) | (sw2 & 0xFF);
  }

  /**
   * Get the status word as a hex string.
   *
   * @return Status word as a hex string
   */
  public String getStatusWordHex() {
    return String.format("%02X%02X", sw1, sw2);
  }

  /**
   * Get a human-readable message for the status word.
   *
   * @return Status message
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Check if the response indicates success (SW=9000).
   *
   * @return true if successful, false otherwise
   */
  public boolean isSuccess() {
    return sw1 == (byte) 0x90 && sw2 == (byte) 0x00;
  }

  /**
   * Convert the APDU response to a byte array.
   *
   * @return Byte array representation of the response
   */
  public byte[] toBytes() {
    int length = 2; // SW is always 2 bytes

    if (data != null && data.length > 0) {
      length += data.length;
    }

    byte[] bytes = new byte[length];

    if (data != null && data.length > 0) {
      System.arraycopy(data, 0, bytes, 0, data.length);
    }

    bytes[length - 2] = sw1;
    bytes[length - 1] = sw2;

    return bytes;
  }

  /**
   * Convert the APDU response to a hex string.
   *
   * @return Hex string representation of the response
   */
  public String toHex() {
    return Hex.encodeHexString(toBytes()).toUpperCase();
  }

  /**
   * Get a human-readable message for a status word.
   *
   * @param sw1 Status byte 1
   * @param sw2 Status byte 2
   * @return Human-readable status message
   */
  private String getStatusMessageForSW(byte sw1, byte sw2) {
    int sw = ((sw1 & 0xFF) << 8) | (sw2 & 0xFF);

    switch (sw) {
      case 0x9000:
        return "Success";
      case 0x6100:
        return "More data available";
      case 0x6200:
        return "Warning: State of non-volatile memory unchanged";
      case 0x6281:
        return "Warning: Part of returned data may be corrupted";
      case 0x6282:
        return "Warning: End of file reached before reading Le bytes";
      case 0x6283:
        return "Warning: Selected file invalidated";
      case 0x6284:
        return "Warning: FCI not formatted according to specification";
      case 0x6300:
        return "Warning: State of non-volatile memory changed";
      case 0x6381:
        return "Warning: File filled up by the last write";
      case 0x6400:
        return "Error: State of non-volatile memory unchanged";
      case 0x6500:
        return "Error: State of non-volatile memory changed";
      case 0x6700:
        return "Error: Wrong length";
      case 0x6800:
        return "Error: Functions in CLA not supported";
      case 0x6881:
        return "Error: Logical channel not supported";
      case 0x6882:
        return "Error: Secure messaging not supported";
      case 0x6900:
        return "Error: Command not allowed";
      case 0x6981:
        return "Error: Command incompatible with file structure";
      case 0x6982:
        return "Error: Security status not satisfied";
      case 0x6983:
        return "Error: Authentication method blocked";
      case 0x6984:
        return "Error: Referenced data invalidated";
      case 0x6985:
        return "Error: Conditions of use not satisfied";
      case 0x6986:
        return "Error: Command not allowed (no current EF)";
      case 0x6987:
        return "Error: Expected SM data objects missing";
      case 0x6988:
        return "Error: SM data objects incorrect";
      case 0x6A00:
        return "Error: Wrong parameters P1-P2";
      case 0x6A80:
        return "Error: Incorrect parameters in the data field";
      case 0x6A81:
        return "Error: Function not supported";
      case 0x6A82:
        return "Error: File not found";
      case 0x6A83:
        return "Error: Record not found";
      case 0x6A84:
        return "Error: Not enough memory space in the file";
      case 0x6A85:
        return "Error: Lc inconsistent with TLV structure";
      case 0x6A86:
        return "Error: Incorrect parameters P1-P2";
      case 0x6A87:
        return "Error: Lc inconsistent with P1-P2";
      case 0x6A88:
        return "Error: Referenced data not found";
      case 0x6B00:
        return "Error: Wrong parameters P1-P2";
      case 0x6C00:
        return "Error: Wrong Le field; exact length is in SW2";
      case 0x6D00:
        return "Error: Instruction code not supported or invalid";
      case 0x6E00:
        return "Error: Class not supported";
      case 0x6F00:
        return "Error: Unknown error";
      default:
        return "Unknown status word: " + String.format("%04X", sw);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ApduResponse: ");

    if (data != null && data.length > 0) {
      sb.append("[");
      sb.append(Hex.encodeHexString(data).toUpperCase());
      sb.append("] ");
    }

    sb.append(String.format("%02X%02X", sw1, sw2));
    sb.append(" (").append(statusMessage).append(")");

    return sb.toString();
  }
}
