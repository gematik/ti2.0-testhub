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
package de.gematik.ti20.simsvc.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.client.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.exception.CardException;
import de.gematik.ti20.simsvc.client.model.apdu.ApduCommand;
import de.gematik.ti20.simsvc.client.model.apdu.ApduResponse;
import de.gematik.ti20.simsvc.client.model.apdu.ApduScenario;
import de.gematik.ti20.simsvc.client.model.apdu.ApduStep;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.card.FileData;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Service for processing APDU commands for smart cards. */
@Service
public class ApduProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ApduProcessor.class);

  // Constants for file selection
  private static final String MF_ID = "3F00";
  private static final String DF_ESIGN_AID = "A000000167455349474E";

  // Selected application and file state
  private String selectedAid = null;
  private String selectedFileId = null;
  private byte[] selectedFileContent = null;

  // Authentication state
  private boolean authenticated = false;
  private boolean paceAuthenticated = false;
  private boolean trustedChannelEstablished = false;
  private Map<String, byte[]> securityEnvironment = new HashMap<>();
  private Map<String, Object> paceContext = new HashMap<>();

  // APDU scenarios
  private final Map<String, ApduScenario> scenarios;
  private final EgkInfoService egkInfoService;
  private final ObjectMapper objectMapper;

  /**
   * Constructor with APDU scenarios and EGK info service.
   *
   * @param scenarios Map of APDU scenarios
   * @param egkInfoService Service for extracting EGK patient data
   */
  @Autowired
  public ApduProcessor(Map<String, ApduScenario> scenarios, EgkInfoService egkInfoService) {
    this.scenarios = scenarios;
    this.egkInfoService = egkInfoService;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Process an APDU command for a specific card.
   *
   * @param card The card image to process the command against
   * @param command The APDU command to process
   * @return The APDU response
   */
  public ApduResponse processCommand(CardImage card, ApduCommand command) {
    logger.debug("Processing command: {}", command);

    try {
      // First check if this command is part of a scenario
      ApduResponse scenarioResponse = checkScenarioResponse(command);
      if (scenarioResponse != null) {
        return scenarioResponse;
      }

      // Otherwise process normally
      // Determine command type and delegate to the appropriate handler
      byte cla = command.getCla();
      byte ins = command.getIns();

      // Standard ISO/IEC 7816 commands
      if (cla == 0x00) {
        switch (ins) {
          case (byte) 0xA4: // SELECT
            return handleSelect(card, command);
          case (byte) 0xB0: // READ BINARY
            return handleReadBinary(card, command);
          case (byte) 0x22: // MANAGE SECURITY ENVIRONMENT
            return handleManageSecurityEnvironment(card, command);
          case (byte) 0x2A: // PERFORM SECURITY OPERATION (PSO)
            return handlePerformSecurityOperation(card, command);
          case (byte) 0x86: // GENERAL AUTHENTICATE
            return handleGeneralAuthenticate(card, command);
          default:
            logger.warn(
                "Unsupported instruction: 0x{}",
                Hex.encodeHexString(new byte[] {ins}).toUpperCase());
            return new ApduResponse(0x6D00); // Instruction code not supported
        }
      }
      // Card-specific commands (CLA 0x80)
      else if (cla == (byte) 0x80) {
        switch (ins) {
          case (byte) 0xCA: // GET DATA
            return handleGetData(card, command);
          case (byte) 0xEE: // GET EGK INFO - Custom APDU for EGK patient data
            logger.debug("Processing 0x80EE command - GET EGK INFO");
            return handleGetEgkInfoSimplified(card, command);
          default:
            logger.warn(
                "Unsupported instruction for CLA 0x80: 0x{}",
                Hex.encodeHexString(new byte[] {ins}).toUpperCase());
            return new ApduResponse(0x6D00); // Instruction code not supported
        }
      } else {
        logger.warn("Unsupported class: 0x{}", Hex.encodeHexString(new byte[] {cla}).toUpperCase());
        return new ApduResponse(0x6E00); // Class not supported
      }
    } catch (Exception e) {
      logger.error("Unexpected error while processing command", e);

      // Check if this is a CardException to get the specific status word
      if (e instanceof CardException) {
        CardException cardException = (CardException) e;
        logger.error(
            "Card error: {}, status word: {}", e.getMessage(), cardException.getStatusWordHex());
        return new ApduResponse(cardException.getStatusWord());
      }

      return new ApduResponse(0x6F00); // Unknown error
    }
  }

  /**
   * Check if the command matches any scenario step and return the appropriate response.
   *
   * @param command The APDU command to check
   * @return The scenario response or null if no matching scenario found
   */
  private ApduResponse checkScenarioResponse(ApduCommand command) {
    String commandHex = command.toHexString().replaceAll("\\s+", "").toUpperCase();

    // Search through all scenarios and steps for a matching command
    for (ApduScenario scenario : scenarios.values()) {
      for (ApduStep step : scenario.getSteps()) {
        // Normalize the step command by removing spaces
        String stepCommand = step.getCommandApdu().replaceAll("\\s+", "").toUpperCase();

        if (commandHex.startsWith(stepCommand)) {
          logger.debug(
              "Found matching scenario step: {} in scenario {}",
              step.getName(),
              scenario.getName());

          // Create a response based on the status word and predefined response data
          String statusWord =
              step.getExpectedStatusWords().get(0); // Use first expected status word

          try {
            // Convert status word to integer
            int sw = Integer.parseInt(statusWord, 16);

            // Generate sample response data based on the command type
            byte[] responseData = generateResponseDataForCommand(command, step);

            if (responseData != null && responseData.length > 0) {
              return new ApduResponse(responseData, (byte) ((sw >> 8) & 0xFF), (byte) (sw & 0xFF));
            } else {
              return new ApduResponse(sw);
            }
          } catch (NumberFormatException e) {
            logger.error("Invalid status word format: {}", statusWord);
          }
        }
      }
    }

    return null; // No matching scenario found
  }

  /**
   * Generate appropriate response data for a command based on its type.
   *
   * @param command The APDU command
   * @param step The matching scenario step
   * @return Response data or null if no data needed
   */
  private byte[] generateResponseDataForCommand(ApduCommand command, ApduStep step) {
    byte cla = command.getCla();
    byte ins = command.getIns();

    try {
      // SELECT command
      if (cla == 0x00 && ins == (byte) 0xA4) {
        // For MF or DF.ESIGN selection, return FCI template
        if ("select-master-file".equals(step.getName())) {
          return Hex.decodeHex("6F1084082D3760001447000A504530201001");
        } else if ("select-df-esign".equals(step.getName())) {
          return Hex.decodeHex("6F128410A000000167455349474E5A504530201001");
        }
      }

      // READ BINARY command
      else if (cla == 0x00 && ins == (byte) 0xB0) {
        // Different file contents based on step name
        if ("read-version".equals(step.getName())) {
          return Hex.decodeHex("47656D617469632047323120412E312E3020362E31352E30");
        } else if ("read-sub-ca-cv-certificate".equals(step.getName())) {
          return Hex.decodeHex(
              "7F2181BD5F378187468269756E676572206E6F63682C2064696520536368756C"
                  + "7465726E206E617373207A752073616C62656E20696E2064656E20536368757074"
                  + "7065726E2C20676568C3B67274206A65747A74207A75206D65696E656E204C6965"
                  + "626C696E6773626573636872656962756E67656E2C206469652069636820616E67"
                  + "6562656E206B616E6E2C2077656E6E2065696E65204B6172746520696D2054726F"
                  + "636B656E6E76657266616872656E2069737421");
        } else if ("read-end-entity-cv-certificate".equals(step.getName())) {
          return Hex.decodeHex(
              "7F2181E57F4E81B54E6F6368206D6568722042797465732C2064696520"
                  + "6963682066C3BC72206469652044656D6F2D5A7765636B6520766572776572"
                  + "74656E206B616E6E2C207765696C20696368206A65747A7420676572616465"
                  + "20446174656E2064617A75206572737465726C652C20756E6420646965736520"
                  + "736F6C6C656E20617566206A6564656E2046616C6C2073696E6E766F6C6C20"
                  + "73656974656E2C207A756D696E64657374206D656872206F64657220776569"
                  + "6E69676572206DC3B67A656E20736965206465722053696E6E2073656974656E2C"
                  + "20617562657220736965206BC3B6C3B6C3B6C3B66E6E657420617563682047"
                  + "65726179747320736569742E");
        } else if ("read-ef-c-ch-aut-e256".equals(step.getName())) {
          return Hex.decodeHex(
              "3082059A30820482A00302010202083C3BC49623A64B46300D06092A864886"
                  + "F70D01010B0500305B310B300906035504061302444531203018060355040A0C11"
                  + "676561746B68204279746573204769626874310C300A060355040B0C0354657374"
                  + "311C301A06035504030C13544553542044722E205465737464617465");
        }

        // Handle special read-ef-c-ch-aut-e256 step
        if ("read-ef-c-ch-aut-e256".equals(step.getName())) {
          return Hex.decodeHex(
              "3082059A30820482A00302010202083C3BC49623A64B46300D06092A864886"
                  + "F70D01010B0500305B310B300906035504061302444531203018060355040A0C11"
                  + "676561746B68204279746573204769626874310C300A060355040B0C0354657374"
                  + "311C301A06035504030C13544553542044722E205465737464617465");
        }
      }

      // GET DATA command
      else if (cla == (byte) 0x80 && ins == (byte) 0xCA) {
        if ("retrieve-public-key-identifiers".equals(step.getName())) {
          return Hex.decodeHex("010484010982010A83010B");
        }
      }

      // GENERAL AUTHENTICATE
      else if (cla == 0x00 && ins == (byte) 0x86) {
        if ("mutual-authentication-step-2".equals(step.getName())) {
          return Hex.decodeHex(
              "7C41860539AABBCCDDEEFF0102030405060708090A0B0C0D0E0F1011121314"
                  + "1516171819202122232425262728293031323334353637383940414243444546"
                  + "4748495051525354555657585960616263");
        }
      }
    } catch (DecoderException e) {
      logger.error("Error decoding hex string: {}", e.getMessage());
    }

    return null;
  }

  /**
   * Handle SELECT command.
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleSelect(CardImage card, ApduCommand command) {
    byte p1 = command.getP1();
    byte p2 = command.getP2();
    byte[] data = command.getData();

    // Check select by file ID (P1=04)
    if (p1 == 0x04) {
      // Select by AID (application identifier)
      if (p2 == 0x0C && data != null) {
        String aid = Hex.encodeHexString(data).toUpperCase();
        logger.debug("SELECT by AID: {}", aid);

        // Check for DF.ESIGN selection
        if (DF_ESIGN_AID.equals(aid)) {
          selectedAid = aid;
          return ApduResponse.createSuccessResponse();
        }

        // Check for MF selection - eGK master file
        if ("D2760001448000".equals(aid)) {
          selectedAid = null;
          selectedFileId = MF_ID;
          return ApduResponse.createSuccessResponse();
        }

        // EGK specific AIDs
        if ("D27600000101".equals(aid)) { // esign application
          selectedAid = aid;
          return ApduResponse.createSuccessResponse();
        }

        if ("D27600000102".equals(aid)) { // hca application
          selectedAid = aid;
          return ApduResponse.createSuccessResponse();
        }

        if ("D27600000103".equals(aid)) { // qualifizierte signatur application
          selectedAid = aid;
          return ApduResponse.createSuccessResponse();
        }

        // Application not found
        return new ApduResponse(0x6A82); // File not found
      }
    }

    // Other select options not supported
    return new ApduResponse(0x6A86); // Incorrect parameters P1-P2
  }

  /**
   * Handle READ BINARY command.
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleReadBinary(CardImage card, ApduCommand command) {
    byte p1 = command.getP1();
    byte p2 = command.getP2();

    // Handle EGK-specific file accesses by file identifiers
    int fileIdentifier = ((p1 & 0xFF) << 8) | (p2 & 0xFF);

    // EGK Global Data Objects (GDO) file
    if (fileIdentifier == 0x8100) {
      // Find real GDO data from card
      FileData gdoFile = findFileByIdentifier(card, "2F02");
      if (gdoFile != null && gdoFile.getData() != null) {
        try {
          byte[] responseData = Hex.decodeHex(gdoFile.getData());
          return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
        } catch (DecoderException e) {
          logger.error("Error decoding real GDO data: {}", e.getMessage());
        }
      }
      logger.warn("No real GDO data found in card, returning file not found");
      return new ApduResponse(0x6A82); // File not found
    }

    // EGK version file
    if (fileIdentifier == 0x9100) {
      // Find real version data from card
      FileData versionFile = findFileByIdentifier(card, "2F11");
      if (versionFile != null && versionFile.getData() != null) {
        try {
          byte[] responseData = Hex.decodeHex(versionFile.getData());
          return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
        } catch (DecoderException e) {
          logger.error("Error decoding real version data: {}", e.getMessage());
        }
      }
      logger.warn("No real version data found in card, returning file not found");
      return new ApduResponse(0x6A82); // File not found
    }

    // EGK authentication certificate (contains KVNR and potentially IK)
    if (fileIdentifier == 0x8400) {
      // Find real certificate data from card
      FileData certFile = findFileByIdentifier(card, "C500");
      if (certFile != null && certFile.getData() != null) {
        try {
          byte[] responseData = Hex.decodeHex(certFile.getData());
          return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
        } catch (DecoderException e) {
          logger.error("Error decoding real certificate data: {}", e.getMessage());
        }
      }
      logger.warn("No real certificate data found in card, returning file not found");
      return new ApduResponse(0x6A82); // File not found
    }

    // Special handling for 8400 read (read-ef-c-ch-aut-e256)
    if (p1 == (byte) 0x84 && p2 == (byte) 0x00) {
      // Find real certificate data from card
      FileData certFile = findFileByIdentifier(card, "C500");
      if (certFile != null && certFile.getData() != null) {
        try {
          byte[] responseData = Hex.decodeHex(certFile.getData());
          return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
        } catch (DecoderException e) {
          logger.error("Error decoding real certificate data: {}", e.getMessage());
        }
      }
      logger.warn("No real certificate data found for 8400 read, returning file not found");
      return new ApduResponse(0x6A82); // File not found
    }

    // For other file IDs, look in the card's file system
    String fileId = String.format("%02X%02X", p1, p2);

    // Find the file in the card structure
    FileData file = findFileInCard(card, fileId);
    if (file != null && file.getData() != null) {
      try {
        byte[] fileData = Base64.getDecoder().decode(file.getData());
        selectedFileContent = fileData; // Store for subsequent reads

        // Return the file contents
        return new ApduResponse(fileData, (byte) 0x90, (byte) 0x00);
      } catch (IllegalArgumentException e) {
        logger.error("Error decoding file data: {}", e.getMessage());
        return new ApduResponse(0x6F00); // Technical error
      }
    }

    // File not found
    return new ApduResponse(0x6A82); // File not found
  }

  /**
   * Find file by identifier in card.
   *
   * @param card The card image
   * @param fileId The file identifier to search for
   * @return FileData if found, null otherwise
   */
  private FileData findFileByIdentifier(CardImage card, String fileId) {
    List<FileData> allFiles = card.getAllFiles();
    for (FileData file : allFiles) {
      if (fileId.equalsIgnoreCase(file.getFileId())) {
        return file;
      }
    }
    return null;
  }

  /**
   * Handle MANAGE SECURITY ENVIRONMENT command.
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleManageSecurityEnvironment(CardImage card, ApduCommand command) {
    byte p1 = command.getP1();
    byte p2 = command.getP2();
    byte[] data = command.getData();

    logger.debug(
        "Manage Security Environment: P1={}, P2={}",
        String.format("%02X", p1),
        String.format("%02X", p2));

    if (p1 == (byte) 0xC1 && p2 == (byte) 0xA4) {
      // MSE: Set for mutual authentication (PACE)

      if (data == null || data.length < 3) {
        return new ApduResponse(0x6A80); // Incorrect parameters in the data field
      }

      // Parse und speichere PACE-Parameter (Tags 80, 83, etc.)
      try {
        int i = 0;
        while (i < data.length) {
          byte tag = data[i++];
          byte len = data[i++];

          if (i + len <= data.length) {
            byte[] value = new byte[len];
            System.arraycopy(data, i, value, 0, len);
            i += len;

            // Speichere Parameter für spätere Verwendung
            securityEnvironment.put(String.format("%02X", tag), value);

            // Protocoll-ID (83-Tag) für PACE (z.B. 03 für PACE mit ECDH)
            if (tag == (byte) 0x83) {
              byte protocolID = value[0];
              logger.debug("PACE Protocol ID: {}", String.format("%02X", protocolID));
            }
          }
        }

        // Setze PACE-Kontext zurück
        paceContext.clear();

        return ApduResponse.createSuccessResponse();
      } catch (Exception e) {
        logger.error("Error parsing MSE data: {}", e.getMessage());
        return new ApduResponse(0x6A80); // Incorrect parameters in the data field
      }
    } else if (p1 == (byte) 0x41 && p2 == (byte) 0xA6) {
      // MSE: Set for digital signature

      // Parse und speichere Schlüsselparameter
      try {
        securityEnvironment.clear();

        int i = 0;
        while (i < data.length) {
          byte tag = data[i++];
          byte len = data[i++];

          if (i + len <= data.length) {
            byte[] value = new byte[len];
            System.arraycopy(data, i, value, 0, len);
            i += len;

            // Speichere Parameter für spätere Verwendung
            securityEnvironment.put(String.format("%02X", tag), value);
          }
        }

        return ApduResponse.createSuccessResponse();
      } catch (Exception e) {
        logger.error("Error parsing MSE data: {}", e.getMessage());
        return new ApduResponse(0x6A80); // Incorrect parameters in the data field
      }
    }

    logger.warn(
        "Unsupported MSE parameters: P1={}, P2={}",
        String.format("%02X", p1),
        String.format("%02X", p2));
    return new ApduResponse(0x6A86); // Incorrect parameters P1-P2
  }

  /**
   * Handle PERFORM SECURITY OPERATION (PSO) command (INS=2A). Used for digital signature
   * operations.
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handlePerformSecurityOperation(CardImage card, ApduCommand command) {
    byte p1 = command.getP1();
    byte p2 = command.getP2();
    byte[] data = command.getData();

    logger.debug(
        "Perform Security Operation: P1={}, P2={}",
        String.format("%02X", p1),
        String.format("%02X", p2));

    // Handle PSO: COMPUTE DIGITAL SIGNATURE (P1=9E, P2=9A)
    if (p1 == (byte) 0x9E && p2 == (byte) 0x9A) {
      logger.debug("PSO: Compute Digital Signature erkannt");

      try {
        // Prüfen, ob zuvor ein MSE:SET für digitale Signatur ausgeführt wurde
        if (securityEnvironment.isEmpty()) {
          logger.warn("PSO:CDS ohne vorheriges MSE:SET ausgeführt");
          return new ApduResponse(0x6985); // Conditions of use not satisfied
        }

        // In data sollte ein Hash oder Daten zu signieren enthalten sein
        if (data == null || data.length < 2) {
          return new ApduResponse(0x6A80); // Incorrect parameters in data field
        }

        // Prüfen auf spezielles TLV-Format (7F4E)
        byte[] hashToSign = null;
        if (data.length > 4 && data[0] == (byte) 0x7F && data[1] == (byte) 0x4E) {
          // TLV-Format mit Hashwert in Tag 0x86 extrahieren
          hashToSign = extractTagValue(data, (byte) 0x86);
          logger.debug(
              "Hash aus 7F4E/86 extrahiert: {}",
              hashToSign != null ? Hex.encodeHexString(hashToSign) : "null");
        } else {
          // Direkter Hash oder Daten
          hashToSign = data;
        }

        if (hashToSign == null) {
          return new ApduResponse(0x6A80); // Incorrect parameters in data field
        }

        // Simulierte digitale Signatur erstellen
        // Im echten Fall würde hier der private Schlüssel der Karte verwendet
        byte[] signature = createSignature(hashToSign);

        logger.debug("Digitale Signatur erstellt: {}", Hex.encodeHexString(signature));
        return new ApduResponse(signature, (byte) 0x90, (byte) 0x00);
      } catch (Exception e) {
        logger.error("Fehler bei der Signaturerstellung: {}", e.getMessage());
        return new ApduResponse(0x6F00); // Technical error
      }
    }

    // PSO: HASH (P1=90, P2=A0) - Used in some signature workflows
    if (p1 == (byte) 0x90 && p2 == (byte) 0xA0) {
      // Store hash for later use
      if (data != null && data.length > 0) {
        securityEnvironment.put("HASH", data);
        return ApduResponse.createSuccessResponse();
      } else {
        return new ApduResponse(0x6A80); // Incorrect parameters in data field
      }
    }

    // PSO: VERIFY CERTIFICATE (P1=00, P2=BE)
    if (p1 == (byte) 0x00 && p2 == (byte) 0xBE) {
      // Simulate certificate verification
      return ApduResponse.createSuccessResponse();
    }

    logger.warn(
        "Unsupported PSO parameters: P1={}, P2={}",
        String.format("%02X", p1),
        String.format("%02X", p2));
    return new ApduResponse(0x6A86); // Incorrect parameters P1-P2
  }

  /**
   * Create a simulated digital signature.
   *
   * @param data The data to sign
   * @return The signature
   */
  private byte[] createSignature(byte[] data) {
    // Create a fixed-length signature with some randomness
    byte[] signature = new byte[128]; // 1024-bit signature

    // Copy input data at the beginning of the signature
    if (data.length > 0) {
      System.arraycopy(data, 0, signature, 0, Math.min(data.length, 20));
    }

    // Fill the rest with pseudorandom data based on the hash
    int seed = 0;
    for (byte b : data) {
      seed = (seed * 31) + (b & 0xFF);
    }
    java.util.Random random = new java.util.Random(seed);
    for (int i = 20; i < signature.length; i++) {
      signature[i] = (byte) random.nextInt(256);
    }

    return signature;
  }

  /**
   * Handle GENERAL AUTHENTICATE command.
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleGeneralAuthenticate(CardImage card, ApduCommand command) {
    byte p1 = command.getP1();
    byte p2 = command.getP2();
    byte[] data = command.getData();

    logger.debug(
        "General Authenticate: P1={}, P2={}", String.format("%02X", p1), String.format("%02X", p2));

    // Basic validation
    if (data == null || data.length < 2) {
      return new ApduResponse(0x6A80); // Incorrect parameters in the data field
    }

    try {
      // Überprüfe, ob wir ein TLV-Format mit 0x7C als äußerem Tag haben (Dynamic Authentication
      // Data)
      if (data[0] == (byte) 0x7C) {
        logger.debug("PACE/Trusted Channel-Befehl im Dynamic Authentication Data Format erkannt");

        // PACE SCHRITT 1: ECDH Key Exchange (Tag 0x81)
        if (containsTag(data, (byte) 0x81)) {
          logger.debug("PACE Schritt 1: ECDH Key Exchange mit Tag 0x81 erkannt");

          // Den Wert des 0x81 Tags (Ephemeral Public Key) extrahieren
          byte[] publicKeyData = extractTagValue(data, (byte) 0x81);
          if (publicKeyData != null) {
            logger.debug("Empfangener Public Key: {}", Hex.encodeHexString(publicKeyData));
          }

          // PACE-Kontext initialisieren oder zurücksetzen
          paceContext.clear();
          paceContext.put("step", 1);

          // Startzeit für Performance-Messung speichern
          paceContext.put("startTime", System.currentTimeMillis());
          logger.debug("PACE-Protokoll gestartet, Zeitmessung begonnen");

          // Challenge für den Kartenleser generieren
          byte[] challenge = new byte[8];
          new SecureRandom().nextBytes(challenge);
          paceContext.put("challenge", challenge);

          try {
            // Simulierte Antwort für ersten PACE-Schritt (Karten-Ephemeral-Punkt)
            // Format: 7C LEN (82 LEN [Ephemeral Point])
            byte[] responseData =
                Hex.decodeHex(
                    "7C3C820140041E5AE49B8D5BD8D62A0F349B5FD1D56F6F8FD10DD69F5BD4DD6DC69C8C9FCE3B041E08E3A4F5D28F133D1DFCE9709C59BF1FBBEC3569671A2D33DFC18F108E");
            logger.debug("PACE Schritt 1 erfolgreich, generiere Antwort");
            paceContext.put("step", 2);
            return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
          } catch (DecoderException e) {
            logger.error("Fehler bei PACE Schritt 1: {}", e.getMessage());
            return new ApduResponse(0x6F00);
          }
        }

        // PACE SCHRITT 2: Mapping (Tag 0x85)
        else if (data.length > 10 && data[0] == (byte) 0x7C) {
          // Versuche Tag 0x85 im TLV-Format zu finden
          logger.debug("Prüfe auf PACE Schritt 2 (Mapping)");

          // Da das Tag manchmal tief verschachtelt sein kann, prüfen wir direkt den Inhalt
          boolean hasTag85 = false;
          for (int i = 0; i < data.length - 2; i++) {
            if (data[i] == (byte) 0x85 && i + 1 < data.length && (data[i + 1] & 0xFF) > 0) {
              hasTag85 = true;
              break;
            }
          }

          if (hasTag85 || containsTag(data, (byte) 0x85)) {
            logger.debug("PACE Schritt 2: Mapping mit Tag 0x85 erkannt");

            // Prüfen, ob wir in der richtigen Sequenz sind
            if (paceContext.containsKey("step") && (int) paceContext.get("step") == 2) {
              try {
                // Simulierte Antwort für zweiten PACE-Schritt (Karten-Mapping)
                byte[] responseData =
                    Hex.decodeHex(
                        "7C3C820140041E5AE49B8D5BD8D62A0F349B5FD1D56F6F8FD10DD69F5BD4DD6DC69C8C9FCE3B041E08E3A4F5D28F133D1DFCE9709C59BF1FBBEC3569671A2D33DFC18F108E");
                logger.debug("PACE Schritt 2 (Mapping) erfolgreich, gehe zu Schritt 3");
                paceContext.put("step", 3);
                return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
              } catch (DecoderException e) {
                logger.error("Fehler bei PACE Schritt 2: {}", e.getMessage());
                return new ApduResponse(0x6F00);
              }
            } else {
              logger.warn(
                  "PACE Schritt 2 (Mapping) außerhalb der Sequenz aufgerufen (aktueller Schritt: {})",
                  paceContext.containsKey("step")
                      ? paceContext.get("step")
                      : "nicht initialisiert");
              return new ApduResponse(0x6A80); // Incorrect parameters
            }
          }
        }

        // PACE SCHRITT 3: Authentication Token (Tag 0x87)
        else if (data.length > 10
            && paceContext.containsKey("step")
            && (int) paceContext.get("step") == 3) {
          logger.debug("PACE Schritt 3: Authentication Token vermutlich mit Tag 0x87");

          // Manuelle Debug-Ausgabe für Tag-Erkennung
          boolean tag87Found = false;
          for (int i = 0; i < data.length - 1; i++) {
            if (data[i] == (byte) 0x87) {
              logger.debug("Tag 0x87 gefunden an Position {}", i);
              tag87Found = true;
            }
          }

          if (!tag87Found) {
            // Wenn das Tag nicht direkt gefunden wurde, prüfen wir die Hex-Repräsentation
            logger.debug(
                "Versuche Tag 0x87 in Hex-String zu finden: {}", Hex.encodeHexString(data));
          }

          byte[] authTokenData = extractTagValue(data, (byte) 0x87);
          if (authTokenData != null) {
            logger.debug(
                "Empfangener Authentication Token: {}", Hex.encodeHexString(authTokenData));
          }

          // Prüfen, ob wir in der richtigen Sequenz sind (oder unabhängig fortsetzen, wenn wir in
          // Schritt 2 sind)
          if (paceContext.containsKey("step")
              && ((int) paceContext.get("step") == 3 || (int) paceContext.get("step") == 2)) {
            // Setzt den Kontext auf Schritt 3, falls dieser noch auf 2 ist
            if ((int) paceContext.get("step") == 2) {
              logger.debug("Setze PACE-Kontext von Schritt 2 auf 3 für Authentication-Token");
              paceContext.put("step", 3);
            }

            try {
              // Simulierte Antwort für dritten PACE-Schritt (Karten-Authentication-Token)
              // Stellen Sie sicher, dass der Hex-String eine gerade Anzahl von Zeichen hat
              byte[] responseData = Hex.decodeHex("7C148C12E214C8520F511B94B483235B2540E0712E6A");
              logger.debug("PACE Schritt 3 (Authentication-Token) erfolgreich, gehe zu Schritt 4");
              paceContext.put("step", 4);
              return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
            } catch (DecoderException e) {
              logger.error("Fehler bei PACE Schritt 3: {}", e.getMessage());
              // Fallback für Hex-Decodierung
              paceContext.put("step", 4);
              try {
                // Alternative Antwort für PACE Schritt 3 mit Fixed bytes
                byte[] fallbackResponse =
                    new byte[] {
                      (byte) 0x7C, (byte) 0x14, (byte) 0x8C, (byte) 0x12,
                      (byte) 0xE2, (byte) 0x14, (byte) 0xC8, (byte) 0x52,
                      (byte) 0x0F, (byte) 0x51, (byte) 0x1B, (byte) 0x94,
                      (byte) 0xB4, (byte) 0x83, (byte) 0x23, (byte) 0x5B,
                      (byte) 0x25, (byte) 0x40, (byte) 0xE0, (byte) 0x71,
                      (byte) 0x2E, (byte) 0x6A
                    };
                logger.debug("Verwende Fallback-Antwort für PACE Schritt 3");
                return new ApduResponse(fallbackResponse, (byte) 0x90, (byte) 0x00);
              } catch (Exception e2) {
                logger.error("Auch Fallback-Antwort fehlgeschlagen: {}", e2.getMessage());
                return new ApduResponse(0x6F00);
              }
            }
          } else {
            logger.warn(
                "PACE Schritt 3 (Authentication Token) außerhalb der Sequenz aufgerufen (aktueller Schritt: {})",
                paceContext.containsKey("step") ? paceContext.get("step") : "nicht initialisiert");
            return new ApduResponse(0x6A80); // Incorrect parameters
          }
        }

        // PACE SCHRITT 4: Mutual Authentication (Tags 0x8A, 0x8E oder 0x9A)
        else if (containsTag(data, (byte) 0x8A)
            || containsTag(data, (byte) 0x8E)
            || containsTag(data, (byte) 0x9A)
            ||
            // Verbesserte Erkennung für Mutual Authentication über den Tag 0x7C im Dynamic
            // Authentication Data Format
            (data.length > 4
                && data[0] == (byte) 0x7C
                && paceContext.containsKey("step")
                && (int) paceContext.get("step") >= 3)) {

          logger.debug(
              "PACE Schritt 4: Mutual Authentication erkannt - Data: {}",
              Hex.encodeHexString(data));
          logger.debug(
              "PACE Mutual Authentication: Verarbeite finalen Schritt des PACE-Protokolls");

          // Detaillierte Tag-Analyse für Debugging
          boolean tag8AFound = containsTag(data, (byte) 0x8A);
          boolean tag8EFound = containsTag(data, (byte) 0x8E);
          boolean tag9AFound = containsTag(data, (byte) 0x9A);

          logger.debug(
              "Mutual Authentication Tags gefunden: 8A={}, 8E={}, 9A={}",
              tag8AFound,
              tag8EFound,
              tag9AFound);

          // Protokolliere detaillierte Informationen zum PACE-Kontext für bessere
          // Nachvollziehbarkeit
          logger.debug("PACE-Kontext für Mutual Authentication (Schritt 4): {}", paceContext);
          logger.debug(
              "PACE Authentifizierungsstatus: Authentifiziert={}, Trusted Channel={}",
              paceAuthenticated,
              trustedChannelEstablished);

          // Prüfen, ob wir in der richtigen Sequenz sind (Schritt 3 oder 4 sind beide zulässig)
          if (paceContext.containsKey("step")
              && ((int) paceContext.get("step") == 4 || (int) paceContext.get("step") == 3)) {

            // Wenn wir noch in Schritt 3 sind, aktualisieren wir den Kontext
            if ((int) paceContext.get("step") == 3) {
              logger.debug("Kontext-Update von Schritt 3 zu 4 für Mutual Authentication");
              paceContext.put("step", 4);
            }

            try {
              // Verbesserte Antwort für vierten PACE-Schritt mit TLV-Format
              byte[] responseData;

              // Je nachdem, welches Tag gefunden wurde, geben wir eine passende Antwort
              if (tag8EFound) {
                // 0x8E-Tag Antwort (Mutual Authentication Token)
                responseData = Hex.decodeHex("7C148C12E214C8520F511B94B483235B2540E0712E6A");
                logger.debug("Antworte mit Mutual Authentication Token für Tag 8E");
              } else {
                // Standard-Antwort für allgemeine Mutual Authentication
                responseData = Hex.decodeHex("9A029000");
                logger.debug("Antworte mit Standard-Token für Mutual Authentication");
              }

              logger.debug(
                  "PACE Schritt 4 (Mutual Authentication) erfolgreich, PACE-Kanal wird etabliert");

              // PACE-Kanal ist jetzt etabliert
              paceAuthenticated = true;
              trustedChannelEstablished = true;

              // Detaillierte Protokollierung des erfolgreichen Abschlusses
              logger.debug(
                  "Trusted Channel Status: PACE Authentifiziert={}, Trusted Channel Etabliert={}",
                  paceAuthenticated,
                  trustedChannelEstablished);

              // Erweiterte Protokollierung für bessere Diagnose
              String tokenType =
                  tag8EFound
                      ? "Mutual Authentication Token (8E)"
                      : tag8AFound
                          ? "Authentication Token (8A)"
                          : tag9AFound ? "Abschluss-Token (9A)" : "Dynamisches Token";

              logger.debug("PACE abgeschlossen mit: {}", tokenType);

              // Protokollieren von performance-relevanten Metriken
              long startTime = System.currentTimeMillis();
              if (paceContext.containsKey("startTime")) {
                startTime = (long) paceContext.get("startTime");
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("PACE-Protokoll abgeschlossen in {} ms", duration);
              }

              // Protokolliere kryptographische Informationen für Debugging (ohne sensitive Daten)
              logger.debug(
                  "PACE erfolgreich mit {} Schlüsselaustauschschritten abgeschlossen",
                  paceContext.containsKey("step") ? paceContext.get("step") : "unbekannt");

              // Zurück zur Ausgangssituation für nächste PACE-Session
              paceContext.clear();
              return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
            } catch (DecoderException e) {
              logger.error("Fehler bei PACE Schritt 4: {}", e.getMessage());
              return new ApduResponse(0x6F00);
            }
          } else {
            logger.warn(
                "PACE Schritt 4 (Mutual Authentication) außerhalb der Sequenz aufgerufen (aktueller Schritt: {})",
                paceContext.containsKey("step") ? paceContext.get("step") : "nicht initialisiert");
            return new ApduResponse(0x6A80); // Incorrect parameters
          }
        }

        // TRUSTED CHANNEL ETABLIERUNG nach PACE
        else if (paceAuthenticated) {
          logger.debug("Trusted Channel nach PACE-Authentifizierung");

          // Trusted Channel Schritt 1: Terminal Authentication
          if (p1 == 0x00
              && p2 == 0x00
              && command.getCla() == 0x00
              && command.getIns() == (byte) 0x88) {
            logger.debug("Trusted Channel - Schritt 1: Erste Mutual Authentication");

            try {
              // Simulierte Antwort für TC Schritt 1 mit Terminal Authentication
              byte[] responseData =
                  Hex.decodeHex(
                      "7C2A8228818081010A83011A8407B51AAB9AF1E18E8B10AC0E78FAE62E407666466E9F88F0264F4E76E38BF05A75D5");
              logger.debug("Trusted Channel Schritt 1 erfolgreich, generiere Antwort");
              return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
            } catch (DecoderException e) {
              logger.error("Fehler bei TC Schritt 1: {}", e.getMessage());
              return new ApduResponse(0x6F00);
            }
          }

          // Trusted Channel Schritt 2: Card Authentication
          else if (p1 == 0x00
              && p2 == 0x00
              && command.getCla() == 0x10
              && command.getIns() == (byte) 0x86) {
            logger.debug("Trusted Channel - Schritt 2: Card Authentication");

            try {
              // Simulierte Antwort für TC Schritt 2 mit Card Authentication
              byte[] responseData = Hex.decodeHex("9701099000");
              logger.debug("Trusted Channel Schritt 2 erfolgreich, generiere Antwort");

              // Trusted Channel etabliert
              trustedChannelEstablished = true;
              authenticated = true;
              logger.debug(
                  "Trusted Channel erfolgreich etabliert, sichere Kommunikation jetzt verfügbar");

              return new ApduResponse(responseData, (byte) 0x90, (byte) 0x00);
            } catch (DecoderException e) {
              logger.error("Fehler bei TC Schritt 2: {}", e.getMessage());
              return new ApduResponse(0x6F00);
            }
          }

          // Andere Befehle nach PACE
          else if (p1 == 0x00 && p2 == 0x00 && containsTag(data, (byte) 0x85)) {
            logger.debug("PACE: Weiterer Schritt mit Tag 0x85");
            return ApduResponse.createSuccessResponse();
          }
        }

        // WEITERLEITUNG NACH TRUSTED CHANNEL ETABLIERUNG
        else if (trustedChannelEstablished) {
          logger.debug("Authenticate-Befehl nach Trusted Channel-Etablierung");

          // Authentifizierte Befehle weiterleiten
          if (card.getCardType() == CardType.EGK
              || card.getCardType() == CardType.HBA
              || card.getCardType() == CardType.SMCB) {
            logger.debug("Authentifizierten Befehl an Karte weiterleiten");
            return ApduResponse.createSuccessResponse(new byte[] {(byte) 0x90, (byte) 0x00});
          }
        }

        // Fallback für andere TLV-strukturierte Authenticate-Befehle
        logger.debug("Allgemeiner Authenticate-Befehl im TLV-Format");
        return ApduResponse.createSuccessResponse(new byte[0]);
      }

      logger.warn("Unbekanntes Format für General Authenticate");
      return new ApduResponse(0x6A80); // Incorrect parameters in the data field
    } catch (Exception e) {
      logger.error("Fehler bei der Authentifizierung: {}", e.getMessage(), e);
      return new ApduResponse(0x6982); // Security status not satisfied
    }
  }

  /**
   * Hilfsmethode zum Prüfen, ob ein Byte-Array einen bestimmten Wert enthält
   *
   * @param data Das zu durchsuchende Byte-Array
   * @param value Der zu suchende Wert
   * @return true, wenn der Wert gefunden wurde
   */
  private boolean contains(byte[] data, byte value) {
    for (byte b : data) {
      if (b == value) {
        return true;
      }
    }
    return false;
  }

  /**
   * Hilfsmethode zum Prüfen, ob ein TLV-strukturiertes Byte-Array einen bestimmten Tag enthält
   *
   * @param data Das zu durchsuchende Byte-Array im TLV-Format
   * @param tag Der zu suchende Tag
   * @return true, wenn der Tag gefunden wurde
   */
  private boolean containsTag(byte[] data, byte tag) {
    if (data == null || data.length < 2) {
      return false;
    }

    int i = 0;
    // TLV-Parsing für BER-TLV Strukturen mit rekursiver Suche
    while (i < data.length - 1) {
      // Wenn wir am Ende des Arrays sind oder nicht genug Bytes für ein TLV haben
      if (i + 1 >= data.length) {
        break;
      }

      byte currentTag = data[i];

      // Prüfen, ob wir den gesuchten Tag gefunden haben
      if (currentTag == tag) {
        return true;
      }

      // Wenn wir einen constructed Tag haben (Bit 6 gesetzt)
      if ((currentTag & 0x20) != 0) {
        // Länge abrufen
        int len = data[i + 1] & 0xFF;
        i += 2;

        // Wenn genug Bytes für ein constructed TLV vorhanden sind
        if (i + len <= data.length) {
          // Extrahiere die Daten des constructed Tags
          byte[] constructedData = new byte[len];
          System.arraycopy(data, i, constructedData, 0, len);

          // Rekursive Suche im constructed Tag
          if (containsTag(constructedData, tag)) {
            return true;
          }

          i += len;
        } else {
          // Ungültige Länge, weiter zum nächsten Tag
          i++;
        }
      } else {
        // Primitive Tag, Länge abrufen und überspringen
        int len = data[i + 1] & 0xFF;
        i += 2 + len;
      }
    }

    return false;
  }

  /**
   * Hilfsmethode zum Extrahieren des Werts eines Tags aus einem TLV-strukturierten Byte-Array
   *
   * @param data Das zu durchsuchende Byte-Array im TLV-Format
   * @param tag Der zu suchende Tag
   * @return Der Wert des Tags oder null, wenn nicht gefunden
   */
  /**
   * Extrahiert den Wert eines bestimmten Tags aus einer TLV-Struktur. Implementiert erweiterte
   * Protokollierung für bessere Fehleranalyse.
   *
   * @param data Die TLV-Daten, in denen gesucht werden soll
   * @param tag Der zu suchende Tag
   * @return Die extrahierten Daten oder null, wenn der Tag nicht gefunden wurde
   */
  private byte[] extractTagValue(byte[] data, byte tag) {
    if (data == null || data.length < 2) {
      logger.debug("TLV-Extraktion: Ungültige Daten (null oder zu kurz)");
      return null;
    }

    // Debug-Ausgabe für die zu durchsuchenden Daten
    if (logger.isDebugEnabled()) {
      logger.debug(
          "TLV-Extraktion: Suche nach Tag 0x{} in Daten: {}",
          String.format("%02X", tag),
          Hex.encodeHexString(data));
    }

    int i = 0;
    int recursionLevel = 0; // Für bessere Protokollierung der TLV-Struktur

    // TLV-Parsing für BER-TLV Strukturen mit rekursiver Suche
    while (i < data.length - 1) {
      // Wenn wir am Ende des Arrays sind oder nicht genug Bytes für ein TLV haben
      if (i + 1 >= data.length) {
        logger.debug(
            "TLV-Extraktion: Ende der Daten ohne ausreichende Bytes für TLV-Struktur bei Index {}",
            i);
        break;
      }

      byte currentTag = data[i];

      // Prüfen, ob wir den gesuchten Tag gefunden haben
      if (currentTag == tag) {
        // Länge abrufen
        int len = data[i + 1] & 0xFF;
        i += 2;

        // Wenn genug Bytes für den Wert vorhanden sind
        if (i + len <= data.length) {
          // Extrahiere den Wert des Tags
          byte[] value = new byte[len];
          System.arraycopy(data, i, value, 0, len);
          return value;
        } else {
          // Ungültige Länge
          return null;
        }
      }

      // Wenn wir einen constructed Tag haben (Bit 6 gesetzt)
      if ((currentTag & 0x20) != 0) {
        // Länge abrufen
        int len = data[i + 1] & 0xFF;
        i += 2;

        // Wenn genug Bytes für ein constructed TLV vorhanden sind
        if (i + len <= data.length) {
          // Extrahiere die Daten des constructed Tags
          byte[] constructedData = new byte[len];
          System.arraycopy(data, i, constructedData, 0, len);

          // Rekursive Suche im constructed Tag
          byte[] result = extractTagValue(constructedData, tag);
          if (result != null) {
            return result;
          }

          i += len;
        } else {
          // Ungültige Länge, weiter zum nächsten Tag
          i++;
        }
      } else {
        // Primitive Tag, Länge abrufen und überspringen
        int len = data[i + 1] & 0xFF;
        i += 2 + len;
      }
    }

    return null;
  }

  /**
   * Hilfsmethode zum Finden einer Datei in der Kartenstruktur.
   *
   * @param card Die Kartenstruktur
   * @param fileId Die Datei-ID
   * @return Die Datei oder null, wenn nicht gefunden
   */
  private FileData findFileInCard(CardImage card, String fileId) {
    if (card == null) {
      return null;
    }

    // Die getAllFiles-Methode nutzen, um alle Dateien der Karte zu bekommen
    List<FileData> allFiles = card.getAllFiles();
    if (allFiles != null) {
      for (FileData file : allFiles) {
        // Vergleiche die ID der Datei mit der gesuchten ID
        if (fileId != null && fileId.equals(file.getFileId())) {
          return file;
        }
      }
    }

    return null;
  }

  /**
   * Handle GET DATA command (CLA=80, INS=CA).
   *
   * @param card The card image
   * @param command The APDU command
   * @return The APDU response
   */
  private ApduResponse handleGetData(CardImage card, ApduCommand command) {
    byte p1 = command.getP1();
    byte p2 = command.getP2();

    logger.debug("GET DATA: P1={}, P2={}", String.format("%02X", p1), String.format("%02X", p2));

    // Check for "Get public key identifiers" (P1P2=0100)
    if (p1 == 0x01 && p2 == 0x00) {
      // Simulate a response with key identifiers
      try {
        // Sample response with key identifiers
        byte[] response = Hex.decodeHex("010484010982010A83010B");
        return ApduResponse.createSuccessResponse(response);
      } catch (DecoderException e) {
        logger.error("Error creating GET DATA response: {}", e.getMessage());
        return new ApduResponse(0x6F00); // Unknown error
      }
    }

    // Sonstige GET DATA Befehle
    return new ApduResponse(0x6A88); // Referenced data not found
  }

  /**
   * Handler for GET EGK INFO command (0x80EE). Returns authentic EGK patient data extracted from
   * card certificates.
   *
   * @param card The card image containing authentic EGK data
   * @param command The APDU command
   * @return APDU response containing authentic EGK patient data
   */
  private ApduResponse handleGetEgkInfoSimplified(CardImage card, ApduCommand command) {
    logger.debug("Processing GET EGK INFO command (0x80EE) for card: {}", card.getId());

    try {
      // Extract authentic EGK patient data using the card image
      EgkInfoDto egkInfo = egkInfoService.extractEgkInfo(card);

      // Create structured response with authentic data
      StringBuilder response = new StringBuilder();
      response.append("KVNR:").append(egkInfo.getKvnr()).append("|");
      response.append("IKNR:").append(egkInfo.getIknr()).append("|");
      response.append("NAME:").append(egkInfo.getPatientName());

      byte[] responseBytes = response.toString().getBytes("UTF-8");
      logger.debug(
          "Successfully created EGK info response with {} bytes containing authentic data: KVNR={}, Patient={}",
          responseBytes.length,
          egkInfo.getKvnr(),
          egkInfo.getPatientName());

      return new ApduResponse(responseBytes, (byte) 0x90, (byte) 0x00);

    } catch (Exception e) {
      logger.error(
          "Error processing GET EGK INFO command for card {}: {}", card.getId(), e.getMessage());
      // Create fallback response with basic data for debugging
      try {
        String fallbackResponse = "KVNR:DEBUG_ERROR|IKNR:ERROR|NAME:Processing Error";
        byte[] responseBytes = fallbackResponse.getBytes("UTF-8");
        return new ApduResponse(responseBytes, (byte) 0x90, (byte) 0x00);
      } catch (Exception fallbackException) {
        logger.error("Failed to create fallback response: {}", fallbackException.getMessage());
        return new ApduResponse(0x6F00); // Unknown error
      }
    }
  }

  /**
   * Create TLV (Tag-Length-Value) encoded response for APDU data.
   *
   * @param tagByte1 First byte of the tag
   * @param tagByte2 Second byte of the tag
   * @param data The data to encode
   * @return TLV-encoded byte array
   */
  private byte[] createTlvResponse(byte tagByte1, byte tagByte2, byte[] data) {
    int dataLength = data.length;

    // Calculate length field size
    byte[] lengthBytes;
    if (dataLength < 0x80) {
      // Short form: length in one byte
      lengthBytes = new byte[] {(byte) dataLength};
    } else if (dataLength < 0x100) {
      // Long form: 0x81 + 1 byte length
      lengthBytes = new byte[] {(byte) 0x81, (byte) dataLength};
    } else if (dataLength < 0x10000) {
      // Long form: 0x82 + 2 bytes length
      lengthBytes = new byte[] {(byte) 0x82, (byte) (dataLength >> 8), (byte) (dataLength & 0xFF)};
    } else {
      // Long form: 0x83 + 3 bytes length
      lengthBytes =
          new byte[] {
            (byte) 0x83,
            (byte) (dataLength >> 16),
            (byte) ((dataLength >> 8) & 0xFF),
            (byte) (dataLength & 0xFF)
          };
    }

    // Construct TLV response: Tag + Length + Value
    byte[] response = new byte[2 + lengthBytes.length + data.length];
    int offset = 0;

    // Tag (2 bytes)
    response[offset++] = tagByte1;
    response[offset++] = tagByte2;

    // Length
    System.arraycopy(lengthBytes, 0, response, offset, lengthBytes.length);
    offset += lengthBytes.length;

    // Value (JSON data)
    System.arraycopy(data, 0, response, offset, data.length);

    return response;
  }
}
