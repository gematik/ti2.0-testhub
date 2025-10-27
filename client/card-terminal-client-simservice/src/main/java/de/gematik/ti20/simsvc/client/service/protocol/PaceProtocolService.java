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

import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementation of the PACE (Password Authenticated Connection Establishment) protocol. This
 * service handles all APDU commands related to PACE protocol for establishing a secure channel.
 */
@Service
public class PaceProtocolService implements CardProtocol {

  private static final Logger logger = LoggerFactory.getLogger(PaceProtocolService.class);

  // PACE state
  private boolean paceAuthenticated = false;
  private boolean trustedChannelEstablished = false;
  private Map<String, byte[]> securityEnvironment = new HashMap<>();
  private Map<String, Object> paceContext = new HashMap<>();

  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  public boolean canHandle(ApduCommand command) {
    byte cla = command.getCla();
    byte ins = command.getIns();
    byte p1 = command.getP1();
    byte p2 = command.getP2();

    // PACE protocol initialization (MSE:SET AT)
    if (cla == 0x00 && ins == 0x22 && p1 == (byte) 0xC1 && p2 == (byte) 0xA4) {
      return true;
    }

    // PACE protocol execution (General Authenticate)
    if (cla == 0x00 && ins == (byte) 0x86) {
      return true;
    }

    return false;
  }

  @Override
  public ApduResponse processCommand(CardImage card, ApduCommand command) {
    byte ins = command.getIns();

    switch (ins) {
      case 0x22: // MSE:SET
        return handleMseSetAt(command);
      case (byte) 0x86: // GENERAL AUTHENTICATE
        return handleGeneralAuthenticate(card, command);
      default:
        logger.warn(
            "Unsupported instruction in PACE protocol: 0x{}",
            Hex.encodeHexString(new byte[] {ins}).toUpperCase());
        return new ApduResponse(0x6D00); // Instruction not supported
    }
  }

  /**
   * Handle MSE:SET AT command for PACE protocol initialization.
   *
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleMseSetAt(ApduCommand command) {
    byte[] data = command.getData();

    if (data == null || data.length < 3) {
      return new ApduResponse(0x6A80); // Incorrect parameters in the data field
    }

    try {
      // Parse and store PACE parameters (Tags 80, 83, etc.)
      int i = 0;
      while (i < data.length) {
        int tag = data[i++] & 0xFF;
        int len = data[i++] & 0xFF;

        if (i + len <= data.length) {
          byte[] value = new byte[len];
          System.arraycopy(data, i, value, 0, len);
          i += len;

          // Store parameter for later use
          securityEnvironment.put(String.format("%02X", tag), value);

          // Protocol ID (83 tag) for PACE (e.g., 03 for PACE with ECDH)
          if (tag == 0x83) {
            int protocolID = value[0] & 0xFF;
            logger.debug("PACE Protocol ID: {}", String.format("%02X", protocolID));
          }
        }
      }

      // Reset PACE context
      paceContext.clear();
      paceAuthenticated = false;
      trustedChannelEstablished = false;

      return ApduResponse.createSuccessResponse();
    } catch (Exception e) {
      logger.error("Error parsing MSE data: {}", e.getMessage());
      return new ApduResponse(0x6A80); // Incorrect parameters in the data field
    }
  }

  /**
   * Handle GENERAL AUTHENTICATE command for PACE protocol execution.
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleGeneralAuthenticate(CardImage card, ApduCommand command) {
    byte[] data = command.getData();

    logger.debug(
        "General Authenticate: P1={}, P2={}",
        String.format("%02X", command.getP1()),
        String.format("%02X", command.getP2()));

    if (data == null || data.length == 0) {
      logger.debug("PACE/Trusted Channel-Befehl im Dynamic Authentication Data Format erkannt");
      logger.debug("Allgemeiner Authenticate-Befehl im TLV-Format");

      // Initialisierungsbefehl, generiere zufälligen Nonce
      byte[] nonce = new byte[16]; // 128 Bit
      secureRandom.nextBytes(nonce);

      // Speichere Nonce im Kontext
      paceContext.put("nonce", nonce);

      // Start timing measurement for protocol execution
      paceContext.put("startTime", System.currentTimeMillis());

      // Empty response for initial authenticate command
      return ApduResponse.createSuccessResponse();
    } else {
      // PACE protocol steps processing with Dynamic Authentication Data Format
      if (data[0] == (byte) 0x7C) {
        logger.debug("PACE/Trusted Channel-Befehl im Dynamic Authentication Data Format erkannt");
        return processPaceStep(card, data);
      }
    }

    return new ApduResponse(0x6A80); // Incorrect parameters in the data field
  }

  /**
   * Process a PACE protocol step based on the TLV data.
   *
   * @param card The card image
   * @param data The TLV data
   * @return The APDU response
   */
  private ApduResponse processPaceStep(CardImage card, byte[] data) {
    try {
      // Check for mapping step (tag 0x85)
      logger.debug("Prüfe auf PACE Schritt 2 (Mapping)");
      byte[] mappingData = extractTlvValue(data, (byte) 0x85);
      if (mappingData != null) {
        logger.debug("PACE Schritt 2: Mapping mit Tag 0x85 erkannt");
        // Process mapping step
        logger.debug("PACE Schritt 2 (Mapping) erfolgreich, gehe zu Schritt 3");

        // Generate a simulated response based on the client's ephemeral public key
        // In a real implementation, this would involve cryptographic operations
        byte[] responseData = data; // Echo back the same data for simulation

        return ApduResponse.createSuccessResponse(responseData);
      }

      // Check for ECDH key exchange (tag 0x81)
      byte[] keyExchangeData = extractTlvValue(data, (byte) 0x81);
      if (keyExchangeData != null) {
        logger.debug("PACE Schritt 1: ECDH Key Exchange mit Tag 0x81 erkannt");
        logger.debug("Empfangener Public Key: {}", Hex.encodeHexString(keyExchangeData));

        logger.debug("PACE-Protokoll gestartet, Zeitmessung begonnen");
        logger.debug("PACE Schritt 1 erfolgreich, generiere Antwort");

        // Generate a simulated response with the server's ephemeral public key
        // This would normally be a cryptographic operation generating a valid ECDH key
        byte[] responseData = data; // Echo back the same data for simulation

        return ApduResponse.createSuccessResponse(responseData);
      }

      // Check for authentication token (tag 0x87)
      byte[] authTokenData = extractTlvValue(data, (byte) 0x87);
      if (authTokenData != null) {
        // Process authentication token
        // This would normally verify the client's authentication token

        // Send empty response for simulation
        return ApduResponse.createSuccessResponse();
      }

      // Check for mutual authentication (tag 0x8E)
      byte[] mutualAuthData = extractTlvValue(data, (byte) 0x8E);
      if (mutualAuthData != null) {
        // Process mutual authentication
        // This would normally verify the client's mutual authentication token

        // PACE protocol successful
        paceAuthenticated = true;
        trustedChannelEstablished = true;

        // Calculate protocol execution time
        if (paceContext.containsKey("startTime")) {
          long startTime = (Long) paceContext.get("startTime");
          long endTime = System.currentTimeMillis();
          logger.debug("PACE-Protokoll erfolgreich abgeschlossen in {} ms", (endTime - startTime));
        }

        // Send empty response for simulation
        return ApduResponse.createSuccessResponse();
      }

      logger.debug("Allgemeiner Authenticate-Befehl im TLV-Format");
      return ApduResponse.createSuccessResponse();

    } catch (Exception e) {
      logger.error("Fehler bei der Verarbeitung des PACE-Protokollschritts: {}", e.getMessage());
      return new ApduResponse(0x6F00); // Technical error
    }
  }

  /**
   * Extract the value for a specific tag from TLV data.
   *
   * @param data The TLV data
   * @param tag The tag to extract
   * @return The value or null if the tag is not found
   */
  private byte[] extractTlvValue(byte[] data, byte tag) {
    try {
      if (data == null || data.length < 3) {
        return null;
      }

      logger.debug(
          "TLV-Extraktion: Suche nach Tag 0x{} in Daten: {}",
          String.format("%02X", tag),
          Hex.encodeHexString(data));

      // Skip the outer 7C tag if present
      int startIdx = 0;
      if (data[0] == (byte) 0x7C) {
        // Skip 7C tag and its length
        startIdx = 2;
        // If it's a complex length
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
          int len = data[i++] & 0xFF;
          if (i + len <= data.length) {
            i += len;
          } else {
            break;
          }
        }
      }

      return null;
    } catch (Exception e) {
      logger.error("Fehler bei der TLV-Extraktion: {}", e.getMessage());
      return null;
    }
  }

  @Override
  public void reset() {
    paceAuthenticated = false;
    trustedChannelEstablished = false;
    securityEnvironment.clear();
    paceContext.clear();
  }

  @Override
  public String getProtocolName() {
    return "PACE";
  }

  /**
   * Check if the PACE protocol has been successfully completed.
   *
   * @return true if PACE is authenticated, false otherwise
   */
  public boolean isPaceAuthenticated() {
    return paceAuthenticated;
  }

  /**
   * Check if a trusted channel has been established.
   *
   * @return true if a trusted channel is established, false otherwise
   */
  public boolean isTrustedChannelEstablished() {
    return trustedChannelEstablished;
  }
}
