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
import de.gematik.ti20.simsvc.client.model.card.CardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Implementation of digital signature protocol for smart cards. This handles signature operations
 * like MSE:SET and PSO:CDS (Compute Digital Signature).
 */
@Service
public class SignatureProtocolService extends AbstractCardProtocol {

  private Map<String, byte[]> securityEnvironment = new HashMap<>();
  private boolean signatureReady = false;

  @Override
  public boolean canHandle(ApduCommand command) {
    byte cla = command.getCla();
    byte ins = command.getIns();
    byte p1 = command.getP1();
    byte p2 = command.getP2();

    // MSE:SET for digital signature
    if (cla == 0x00 && ins == 0x22 && p1 == (byte) 0x41 && p2 == (byte) 0xA6) {
      return true;
    }

    // PSO:COMPUTE DIGITAL SIGNATURE
    if (cla == 0x00 && ins == (byte) 0x2A && p1 == (byte) 0x9E && p2 == (byte) 0x9A) {
      return true;
    }

    return false;
  }

  @Override
  public ApduResponse processCommand(CardImage card, ApduCommand command) {
    byte ins = command.getIns();
    byte p1 = command.getP1();
    byte p2 = command.getP2();

    if (ins == 0x22) { // MSE:SET
      return handleMseSetSignature(command);
    } else if (ins == (byte) 0x2A) { // PSO
      if (p1 == (byte) 0x9E && p2 == (byte) 0x9A) { // COMPUTE DIGITAL SIGNATURE
        return handleComputeDigitalSignature(card, command);
      }
    }

    return createErrorResponse(0x6D00); // Instruction not supported
  }

  /**
   * Handle MSE:SET for digital signature preparation.
   *
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleMseSetSignature(ApduCommand command) {
    byte[] data = command.getData();

    if (data == null || data.length < 3) {
      return createErrorResponse(0x6A80); // Incorrect parameters in the data field
    }

    // Parse and store key parameters
    try {
      securityEnvironment.clear();

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
        }
      }

      // Set flag indicating signature preparation is complete
      signatureReady = true;

      return createSuccessResponse();
    } catch (Exception e) {
      logger.error("Error parsing MSE data: {}", e.getMessage());
      return createErrorResponse(0x6A80); // Incorrect parameters in the data field
    }
  }

  /**
   * Handle PSO:CDS (Compute Digital Signature) command.
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleComputeDigitalSignature(CardImage card, ApduCommand command) {
    // Check if the card supports digital signatures
    CardType cardType = card.getCardType();
    if (cardType != CardType.SMCB && cardType != CardType.HBA) {
      logger.warn("Card type {} does not support digital signatures", cardType);
      return createErrorResponse(0x6A81); // Function not supported
    }

    // Check if signature was prepared with MSE:SET
    if (!signatureReady) {
      logger.warn("Signature not prepared with MSE:SET");
      return createErrorResponse(0x6985); // Conditions of use not satisfied
    }

    byte[] data = command.getData();
    if (data == null || data.length == 0) {
      return createErrorResponse(0x6A80); // Incorrect parameters in the data field
    }

    try {
      // Find the private key for signing from the card
      byte[] signature = computeRealSignature(card, data);
      if (signature == null) {
        logger.error("No suitable private key found for signing");
        return createErrorResponse(0x6A88); // Referenced data not found
      }

      logger.debug("Generated real digital signature of {} bytes", signature.length);

      // Reset signature preparation state after successful signature
      signatureReady = false;

      return ApduResponse.createSuccessResponse(signature);
    } catch (Exception e) {
      logger.error("Error computing digital signature: {}", e.getMessage());
      return createErrorResponse(0x6F00); // Technical error
    }
  }

  /**
   * Compute a real digital signature using the card's private key.
   *
   * @param card The card image containing keys
   * @param data The data to sign
   * @return The signature bytes, or null if no suitable key found
   */
  private byte[] computeRealSignature(CardImage card, byte[] data) {
    try {
      // Find a suitable private key from the card
      List<de.gematik.ti20.simsvc.client.model.card.Key> allKeys = card.getAllKeys();
      for (de.gematik.ti20.simsvc.client.model.card.Key key : allKeys) {
        if (key.getPrivateKey() != null && key.getName() != null) {
          // Try to use this key for signing
          try {
            return signWithCardKey(key, data);
          } catch (Exception e) {
            logger.debug("Could not sign with key {}: {}", key.getName(), e.getMessage());
            continue;
          }
        }
      }
      return null;
    } catch (Exception e) {
      logger.error("Error finding private key for signing: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Sign data with a specific card key.
   *
   * @param key The key to use for signing
   * @param data The data to sign
   * @return The signature bytes
   * @throws Exception If signing fails
   */
  private byte[] signWithCardKey(de.gematik.ti20.simsvc.client.model.card.Key key, byte[] data)
      throws Exception {
    // Decode the private key from Base64
    byte[] keyBytes = java.util.Base64.getDecoder().decode(key.getPrivateKey());

    // Determine key type and create appropriate signature
    if (key.getName().contains("_E256") || key.getName().contains("_CVC_E")) {
      // EC key
      return signWithEcKey(keyBytes, data);
    } else {
      // RSA key
      return signWithRsaKey(keyBytes, data);
    }
  }

  /** Sign with RSA key. */
  private byte[] signWithRsaKey(byte[] keyBytes, byte[] data) throws Exception {
    java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
    java.security.spec.PKCS8EncodedKeySpec keySpec =
        new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
    java.security.PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

    java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
    signature.initSign(privateKey);
    signature.update(data);
    return signature.sign();
  }

  /** Sign with EC key. */
  private byte[] signWithEcKey(byte[] keyBytes, byte[] data) throws Exception {
    java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("EC");
    java.security.spec.PKCS8EncodedKeySpec keySpec =
        new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
    java.security.PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

    java.security.Signature signature = java.security.Signature.getInstance("SHA256withECDSA");
    signature.initSign(privateKey);
    signature.update(data);
    return signature.sign();
  }

  @Override
  public void reset() {
    securityEnvironment.clear();
    signatureReady = false;
  }

  @Override
  public String getProtocolName() {
    return "SIGNATURE";
  }
}
