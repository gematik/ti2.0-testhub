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
package de.gematik.ti20.simsvc.client.service.protocol;

import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base implementation of the CardProtocol interface. Provides common functionality for
 * protocol implementations.
 */
public abstract class AbstractCardProtocol implements CardProtocol {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Extract TLV (Tag-Length-Value) data from an APDU command.
   *
   * @param data The raw data containing TLV structures
   * @param tag The tag to extract
   * @return The value associated with the tag, or null if not found
   */
  protected byte[] extractTlvValue(byte[] data, byte tag) {
    try {
      if (data == null || data.length < 3) {
        return null;
      }

      logger.debug(
          "TLV extraction: Looking for tag 0x{} in data: {}",
          String.format("%02X", tag),
          hexEncode(data));

      // Skip the outer tag if present (like 7C for PACE)
      int startIdx = 0;
      if (data[0] == (byte) 0x7C) {
        // Skip tag and length
        if ((data[1] & 0x80) != 0) {
          int lengthBytes = data[1] & 0x7F;
          startIdx = 2 + lengthBytes;
        } else {
          startIdx = 2;
        }
      }

      // Find the specific tag
      for (int i = startIdx; i < data.length; ) {
        int currentTag = data[i++] & 0xFF;

        // If found the tag
        if (currentTag == (tag & 0xFF)) {
          // Get length
          int len = data[i++] & 0xFF;

          // Check if we have enough data
          if (i + len <= data.length) {
            byte[] value = new byte[len];
            System.arraycopy(data, i, value, 0, len);
            return value;
          }
        } else {
          // Skip this tag's value
          byte len = data[i++];
          if (i + len <= data.length) {
            i += len;
          } else {
            break; // Avoid out of bounds error
          }
        }
      }

      return null;
    } catch (Exception e) {
      logger.error("Error extracting TLV: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Create an empty success response.
   *
   * @return A success response with no data
   */
  protected ApduResponse createSuccessResponse() {
    return ApduResponse.createSuccessResponse();
  }

  /**
   * Create an error response with the given status word.
   *
   * @param statusWord The status word indicating the error
   * @return An error response
   */
  protected ApduResponse createErrorResponse(int statusWord) {
    return new ApduResponse(statusWord);
  }

  /**
   * Convert a byte array to a hex string.
   *
   * @param data The byte array to convert
   * @return A hex string representation
   */
  protected String hexEncode(byte[] data) {
    if (data == null) {
      return "null";
    }

    StringBuilder sb = new StringBuilder();
    for (byte b : data) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
