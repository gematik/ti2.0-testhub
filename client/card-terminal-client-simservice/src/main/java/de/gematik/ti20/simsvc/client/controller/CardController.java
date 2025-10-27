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
package de.gematik.ti20.simsvc.client.controller;

import de.gematik.ti20.simsvc.client.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.dto.CardHandleDto;
import de.gematik.ti20.simsvc.client.model.dto.ConnectionPropertiesDto;
import de.gematik.ti20.simsvc.client.model.dto.SignRequestDto;
import de.gematik.ti20.simsvc.client.model.dto.SignResponseDto;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import de.gematik.ti20.simsvc.client.model.dto.TransmitResponseDto;
import de.gematik.ti20.simsvc.client.service.CardImageParser;
import de.gematik.ti20.simsvc.client.service.CardManager;
import de.gematik.ti20.simsvc.client.service.EgkInfoService;
import de.gematik.ti20.simsvc.client.service.SignatureService;
import de.gematik.ti20.simsvc.client.service.SlotManager;
import de.gematik.ti20.simsvc.client.service.SmcBInfoService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for card operations. Provides endpoints for listing cards, establishing
 * connections, transmitting commands, signing data, and closing connections.
 */
@RestController
@RequestMapping("/cards")
public class CardController {

  private static final Logger logger = LoggerFactory.getLogger(CardController.class);

  private final CardManager cardManager;
  private final SignatureService signatureService;
  private final SmcBInfoService smcBInfoService;
  private final EgkInfoService egkInfoService;
  private final SlotManager slotManager;
  private final CardImageParser cardImageParser;

  /**
   * Constructor for CardController.
   *
   * @param cardManager Service to manage cards and connections
   * @param signatureService Service for signing operations
   * @param smcBInfoService Service for SMC-B information extraction
   */
  @Autowired
  public CardController(
      CardManager cardManager,
      SignatureService signatureService,
      SmcBInfoService smcBInfoService,
      EgkInfoService egkInfoService,
      SlotManager slotManager,
      CardImageParser cardImageParser) {
    this.cardManager = cardManager;
    this.signatureService = signatureService;
    this.smcBInfoService = smcBInfoService;
    this.egkInfoService = egkInfoService;
    this.slotManager = slotManager;
    this.cardImageParser = cardImageParser;
  }

  /**
   * List all available cards across all slots.
   *
   * @return List of card handles
   */
  @GetMapping("/")
  public ResponseEntity<List<CardHandleDto>> listCards() {
    List<CardHandleDto> cardHandles = cardManager.listAllCards();
    return ResponseEntity.ok(cardHandles);
  }

  /**
   * Establish a virtual connection to a card.
   *
   * @param cardHandle Card handle identifier
   * @return Connection properties
   */
  @GetMapping("/{cardHandle}")
  public ResponseEntity<ConnectionPropertiesDto> connect(@PathVariable String cardHandle) {
    // GlobalExceptionHandler wird die Fehlerbehandlung übernehmen
    ConnectionPropertiesDto properties = cardManager.connectToCard(cardHandle);
    return ResponseEntity.ok(properties);
  }

  /**
   * Transmit an APDU command to a connected card.
   *
   * @param cardHandle Card handle identifier
   * @param request Transmit request containing APDU command
   * @return Response containing APDU response
   */
  @PostMapping("/{cardHandle}/transmit")
  public ResponseEntity<TransmitResponseDto> transmit(
      @PathVariable String cardHandle, @RequestBody Map<String, String> requestBody) {

    try {
      String command = requestBody.get("command");
      if (command == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Missing 'command' field in request");
      }

      String normalizedCommand = command.replaceAll("\\s+", "").toUpperCase();

      // Direct handling for 0xF0EE cert-info command
      if ("F0EE000000".equals(normalizedCommand)) {
        try {
          CardImage card = cardManager.findCardByHandle(cardHandle);
          if (card == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Card not found for handle: " + cardHandle);
          }

          // Route to appropriate cert-info based on card type
          Object certInfo;
          String certData;

          if (card.getCardType() == CardType.EGK) {
            EgkInfoDto egkInfo = egkInfoService.extractEgkInfo(card);
            certData =
                String.format(
                    "cardType:%s|KVNR:%s|IKNR:%s|NAME:%s|FIRST_NAME:%s|LAST_NAME:%s",
                    egkInfo.getCardType(),
                    egkInfo.getKvnr(),
                    egkInfo.getIknr(),
                    egkInfo.getPatientName(),
                    egkInfo.getFirstName(),
                    egkInfo.getLastName());
          } else {
            // SMC-B and other card types
            SmcBInfoDto smcBInfo = smcBInfoService.extractSmcBInfo(cardHandle);
            certData =
                String.format(
                    "cardType:%s|TELEMATIK_ID:%s|PROFESSION_OID:%s|HOLDER:%s|ORG:%s",
                    smcBInfo.getCardType(),
                    smcBInfo.getTelematikId(),
                    smcBInfo.getProfessionOid(),
                    smcBInfo.getHolderName(),
                    smcBInfo.getOrganizationName());
          }

          byte[] dataBytes = certData.getBytes("UTF-8");
          String dataHex =
              org.apache.commons.codec.binary.Hex.encodeHexString(dataBytes).toUpperCase();
          String responseHex = dataHex + "9000";

          TransmitResponseDto response =
              new TransmitResponseDto(responseHex, "9000", "Success", dataHex);
          return ResponseEntity.ok(response);
        } catch (Exception certError) {
          logger.error(
              "Error processing F0EE command for card {}: {}", cardHandle, certError.getMessage());
          // Provide a fallback response with error information for debugging
          String fallbackData = "cardType:ERROR|STATUS:CERT_INFO_EXTRACTION_FAILED";
          try {
            byte[] dataBytes = fallbackData.getBytes("UTF-8");
            String dataHex =
                org.apache.commons.codec.binary.Hex.encodeHexString(dataBytes).toUpperCase();
            String responseHex = dataHex + "9000";

            TransmitResponseDto response =
                new TransmitResponseDto(
                    responseHex,
                    "9000",
                    "Error in cert-info processing: " + certError.getMessage(),
                    dataHex);
            return ResponseEntity.ok(response);
          } catch (Exception fallbackError) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Complete cert-info processing failure: " + certError.getMessage());
          }
        }
      }

      // Direct handling for 0x80EE EGK-Info command (legacy support)
      if ("80EE000000".equals(normalizedCommand)) {
        try {
          CardImage card = cardManager.findCardByHandle(cardHandle);
          if (card == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Card not found for handle: " + cardHandle);
          }

          EgkInfoDto egkInfo = egkInfoService.extractEgkInfo(card);
          String egkData =
              String.format(
                  "KVNR:%s|IKNR:%s|NAME:%s",
                  egkInfo.getKvnr(), egkInfo.getIknr(), egkInfo.getPatientName());

          byte[] dataBytes = egkData.getBytes("UTF-8");
          String dataHex =
              org.apache.commons.codec.binary.Hex.encodeHexString(dataBytes).toUpperCase();
          String responseHex = dataHex + "9000";

          TransmitResponseDto response =
              new TransmitResponseDto(responseHex, "9000", "Success", dataHex);
          return ResponseEntity.ok(response);
        } catch (Exception egkError) {
          logger.error(
              "Error processing 80EE command for card {}: {}", cardHandle, egkError.getMessage());
          // Provide a fallback response with error information for debugging
          String fallbackData = "KVNR:ERROR_PROCESSING|IKNR:ERROR|NAME:EGK Info Extraction Failed";
          try {
            byte[] dataBytes = fallbackData.getBytes("UTF-8");
            String dataHex =
                org.apache.commons.codec.binary.Hex.encodeHexString(dataBytes).toUpperCase();
            String responseHex = dataHex + "9000";

            TransmitResponseDto response =
                new TransmitResponseDto(
                    responseHex,
                    "9000",
                    "Error in EGK processing: " + egkError.getMessage(),
                    dataHex);
            return ResponseEntity.ok(response);
          } catch (Exception fallbackError) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Complete EGK processing failure: " + egkError.getMessage());
          }
        }
      }

      // Normal APDU processing for other commands
      TransmitResponseDto response = cardManager.transmitCommand(cardHandle, command);
      return ResponseEntity.ok(response);

    } catch (final ResponseStatusException e) {
      throw e;
    } catch (final Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "APDU transmission failed: " + e.getMessage());
    }
  }

  /**
   * Sign data with the card's certificate.
   *
   * @param cardHandle Card handle identifier
   * @param request Sign request containing data to sign and options
   * @return Response containing signature
   */
  @PostMapping("/{cardHandle}/sign")
  public ResponseEntity<SignResponseDto> sign(
      @PathVariable String cardHandle, @RequestBody SignRequestDto request) {
    try {
      SignResponseDto response = signatureService.signData(cardHandle, request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains("SHA1") || e.getMessage().contains("deprecated")) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
      }
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Card not found or not connected: " + cardHandle);
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Signing failed: " + e.getMessage());
    }
  }

  /**
   * Close a virtual connection to a card.
   *
   * @param cardHandle Card handle identifier
   * @return Empty response
   */
  @DeleteMapping("/{cardHandle}")
  public ResponseEntity<Void> disconnect(@PathVariable String cardHandle) {
    // GlobalExceptionHandler wird die Fehlerbehandlung übernehmen
    cardManager.disconnectCard(cardHandle);
    return ResponseEntity.noContent().build();
  }

  /**
   * Get SMC-B card information including Telematik-ID and ProfessionOID.
   *
   * @param cardHandle Card handle
   * @return SMC-B information
   */
  @GetMapping("/{cardHandle}/smc-b-info")
  public ResponseEntity<SmcBInfoDto> getSmcBInfo(@PathVariable String cardHandle) {
    SmcBInfoDto smcBInfo = smcBInfoService.extractSmcBInfo(cardHandle);
    return ResponseEntity.ok(smcBInfo);
  }

  /**
   * Get certificate from card.
   *
   * @param cardHandle Card handle
   * @param request Certificate request containing key type
   * @return Certificate data
   */
  @PostMapping("/{cardHandle}/certificate")
  public ResponseEntity<Map<String, String>> getCertificate(
      @PathVariable String cardHandle, @RequestBody Map<String, String> request) {
    try {
      String keyType = request.getOrDefault("keyType", "AUT");
      Map<String, String> response = signatureService.getCertificate(cardHandle, keyType);
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Certificate retrieval failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
  }

  /**
   * Get certificate information for any card type. Returns EGK info for EGK cards and SMC-B info
   * for SMC-B cards.
   *
   * @param cardHandle Card handle
   * @return Certificate information based on card type
   */
  @GetMapping("/{cardHandle}/cert-info")
  public ResponseEntity<Object> getCertificateInfo(@PathVariable String cardHandle) {
    try {
      // Find the card to determine its type
      CardImage card = cardManager.findCardByHandle(cardHandle);
      if (card == null) {
        throw new IllegalArgumentException("Card not found: " + cardHandle);
      }

      // Route to appropriate service based on card type
      if (card.getCardType() == CardType.EGK) {
        EgkInfoDto egkInfo = egkInfoService.extractEgkInfo(card);
        return ResponseEntity.ok(egkInfo);
      } else if (card.getCardType() == CardType.HPIC || card.getCardType() == CardType.SMCB) {
        SmcBInfoDto smcBInfo = smcBInfoService.extractSmcBInfo(cardHandle);
        return ResponseEntity.ok(smcBInfo);
      } else {
        // For other card types, return basic card information
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("cardType", card.getCardType().toString());
        basicInfo.put("cardId", card.getId());
        basicInfo.put(
            "message",
            "Certificate information extraction not supported for card type: "
                + card.getCardType());
        return ResponseEntity.ok(basicInfo);
      }
    } catch (Exception e) {
      Map<String, String> errorResponse = new HashMap<>();
      errorResponse.put("error", "Certificate information retrieval failed: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
  }

  /**
   * Get debug information for a card.
   *
   * @param cardHandle Card handle
   * @return Debug information as JSON
   */
  @GetMapping("/{cardHandle}/debug-info")
  public ResponseEntity<Map<String, Object>> getDebugInfo(@PathVariable String cardHandle) {
    Map<String, Object> debugInfo = new HashMap<>();
    try {
      debugInfo = signatureService.getCardDebugInfo(cardHandle);
      return ResponseEntity.ok(debugInfo);
    } catch (Exception e) {
      debugInfo.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(debugInfo);
    }
  }

  /**
   * Extract EGK information from the card containing authentic KVNR, IKNR and patient data.
   *
   * @param cardHandle The card handle identifier
   * @return EGK information with real patient data from certificate
   */
  @GetMapping("/{cardHandle}/egk-info")
  public ResponseEntity<?> getEgkInfo(@PathVariable String cardHandle) {
    try {
      // Find the card image for the given handle
      CardImage card = cardManager.findCardByHandle(cardHandle);
      if (card == null) {
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("error", "Card not found");
        errorInfo.put("message", "No card found for handle: " + cardHandle);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorInfo);
      }

      EgkInfoDto egkInfo = egkInfoService.extractEgkInfo(card);
      return ResponseEntity.ok(egkInfo);

    } catch (Exception e) {
      Map<String, Object> errorInfo = new HashMap<>();
      errorInfo.put("error", "Internal Server Error");
      errorInfo.put("message", "An unexpected error occurred");
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorInfo);
    }
  }

  /**
   * Get detailed SMC-B debug information.
   *
   * @param cardHandle Card handle
   * @return SMC-B debug information as JSON
   */
  @GetMapping("/{cardHandle}/smc-b-debug")
  public ResponseEntity<Map<String, Object>> getSmcBDebugInfo(@PathVariable String cardHandle) {
    try {
      Map<String, Object> debugInfo = smcBInfoService.getDebugCardFiles(cardHandle);
      return ResponseEntity.ok(debugInfo);
    } catch (Exception e) {
      Map<String, Object> errorInfo = new HashMap<>();
      errorInfo.put("error", e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorInfo);
    }
  }

  /**
   * Load a card from XML file into a slot.
   *
   * @param request Map containing cardType and xmlFile parameters
   * @return Response with card handle and slot information
   */
  @PostMapping("/load")
  public ResponseEntity<?> loadCard(@RequestBody Map<String, String> request) {
    try {
      String cardType = request.get("cardType");
      String xmlFile = request.get("xmlFile");

      if (cardType == null || xmlFile == null) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Missing required fields");
        error.put("message", "Both 'cardType' and 'xmlFile' are required");
        return ResponseEntity.badRequest().body(error);
      }

      logger.debug("Loading card type {} from file {}", cardType, xmlFile);

      // Parse the card image from the XML file
      CardImage cardImage = cardImageParser.parseCardImageFromFile(xmlFile);
      if (cardImage == null) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Card parsing failed");
        error.put("message", "Failed to parse card from XML file: " + xmlFile);
        return ResponseEntity.badRequest().body(error);
      }

      // Find an available slot
      int availableSlot = -1;
      for (int i = 0; i < slotManager.getSlotCount(); i++) {
        if (!slotManager.isCardPresent(i)) {
          availableSlot = i;
          break;
        }
      }

      if (availableSlot == -1) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "No available slots");
        error.put("message", "All slots are occupied");
        return ResponseEntity.badRequest().body(error);
      }

      // Insert the card into the slot
      boolean inserted = slotManager.insertCard(availableSlot, cardImage);
      if (!inserted) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Card insertion failed");
        error.put("message", "Failed to insert card into slot " + availableSlot);
        return ResponseEntity.internalServerError().body(error);
      }

      logger.debug("Successfully loaded card {} into slot {}", cardImage.getId(), availableSlot);

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("cardHandle", cardImage.getId());
      response.put("cardType", cardImage.getCardType().toString());
      response.put("slot", availableSlot);
      response.put("message", "Card loaded successfully");

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      logger.error("Error loading card: {}", e.getMessage(), e);
      Map<String, String> error = new HashMap<>();
      error.put("error", "Internal Server Error");
      error.put("message", "An unexpected error occurred");
      return ResponseEntity.internalServerError().body(error);
    }
  }
}
