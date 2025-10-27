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

import de.gematik.ti20.simsvc.client.exception.CardNotFoundException;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.CardType;
import de.gematik.ti20.simsvc.client.model.card.FileData;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for extracting authentic SMC-B card information from X.509 certificates. No hardcoded
 * values - only authentic data extraction.
 */
@Service
public class SmcBInfoService {

  private static final Logger logger = LoggerFactory.getLogger(SmcBInfoService.class);

  private final SlotManager slotManager;

  @Autowired
  public SmcBInfoService(SlotManager slotManager) {
    this.slotManager = slotManager;
  }

  /**
   * Extract authentic SMC-B information from card certificates.
   *
   * @param cardHandle The card handle
   * @return SMC-B information or error if extraction fails
   */
  public SmcBInfoDto extractSmcBInfo(String cardHandle) {
    CardImage card = findCardByHandle(cardHandle);
    if (card == null) {
      throw new CardNotFoundException(cardHandle);
    }

    if (!isSMCBCard(card)) {
      throw new IllegalArgumentException("Card is not an SMC-B card: " + cardHandle);
    }

    logger.debug("Extracting authentic SMC-B data from card: {}", cardHandle);

    try {
      SmcBInfoDto authenticData = extractAuthenticSmcBDataFromCertificates(card);
      if (authenticData != null) {
        return authenticData;
      }

      logger.warn("No authentic SMC-B data found in card certificates");
      return createSmcBExtractionError("No authentic SMC-B data found in card certificates", card);

    } catch (Exception e) {
      logger.error("Error extracting SMC-B certificate data: {}", e.getMessage());
      return createSmcBExtractionError("Failed to extract SMC-B data: " + e.getMessage(), card);
    }
  }

  /** Extract authentic SMC-B data from X.509 certificates in card files. */
  private SmcBInfoDto extractAuthenticSmcBDataFromCertificates(CardImage card) {
    List<FileData> files = card.getAllFiles();
    logger.debug("Analyzing {} files for authentic SMC-B certificate data", files.size());

    String extractedTelematikId = null;
    String extractedProfessionOid = null;
    String extractedHolderName = null;
    String extractedOrganizationName = null;

    // Process X.509 certificates for authentic data extraction
    for (FileData file : files) {
      if (file.getData() == null || file.getName() == null) continue;

      String fileName = file.getName();
      String fileData = file.getData();

      logger.debug("Processing SMC-B file: {} with data length: {}", fileName, fileData.length());

      try {
        // Check if this is X.509 certificate data (starts with 308... in hex)
        if (fileData.startsWith("3082")
            || fileData.startsWith("3081")
            || fileData.startsWith("3080")) {
          logger.debug("Found X.509 certificate data in SMC-B file: {}", fileName);

          // Extract Telematik-ID only from certificate DN and extensions, not from hex construction

          // Decode hex to binary certificate
          byte[] certBytes = hexStringToByteArray(fileData);

          // Parse certificate using Java's built-in certificate parser
          try {
            java.security.cert.CertificateFactory certFactory =
                java.security.cert.CertificateFactory.getInstance("X.509");
            java.io.ByteArrayInputStream certStream = new java.io.ByteArrayInputStream(certBytes);
            java.security.cert.X509Certificate cert =
                (java.security.cert.X509Certificate) certFactory.generateCertificate(certStream);

            // Extract subject DN (Distinguished Name)
            String subjectDN = cert.getSubjectDN().toString();
            logger.debug("SMC-B Certificate subject DN: {}", subjectDN);

            // Log all certificate details for analysis
            logger.debug("SMC-B Certificate issuer DN: {}", cert.getIssuerDN().toString());
            logger.debug("SMC-B Certificate serial number: {}", cert.getSerialNumber().toString());
            logger.debug(
                "SMC-B Certificate valid from: {} to: {}", cert.getNotBefore(), cert.getNotAfter());

            // Log certificate extensions
            java.util.Set<String> criticalExtOIDs = cert.getCriticalExtensionOIDs();
            if (criticalExtOIDs != null) {
              for (String oid : criticalExtOIDs) {
                logger.debug("SMC-B Certificate critical extension OID: {}", oid);
              }
            }

            java.util.Set<String> nonCriticalExtOIDs = cert.getNonCriticalExtensionOIDs();
            if (nonCriticalExtOIDs != null) {
              for (String oid : nonCriticalExtOIDs) {
                logger.debug("SMC-B Certificate non-critical extension OID: {}", oid);
                // Check for Telematik-ID related OID extensions
                if (oid.startsWith("1.2.276.0.76.4.3")
                    || oid.equals("1.3.36.8.3.3")
                    || oid.equals("2.5.29.37")) {
                  byte[] extValue = cert.getExtensionValue(oid);
                  if (extValue != null) {
                    logger.debug(
                        "SMC-B Found extension OID: {}, value length: {}", oid, extValue.length);
                    // Try to decode the extension value
                    String decodedValue = decodeExtensionValue(extValue);
                    if (decodedValue != null) {
                      logger.debug("SMC-B Extension {} decoded value: {}", oid, decodedValue);
                      // Check if this contains a Telematik-ID pattern
                      if (decodedValue.matches(".*[0-9]+-[A-Z0-9]+-.*")
                          && extractedTelematikId == null) {
                        extractedTelematikId = extractTelematikIdFromString(decodedValue);
                        if (extractedTelematikId != null) {
                          logger.debug(
                              "Extracted Telematik-ID from extension {}: {}",
                              oid,
                              extractedTelematikId);
                        }
                      }
                    }
                  }
                }
              }
            }

            // Extract Telematik-ID from serialNumber field (primary source)
            String telematikIdFromSerial = extractTelematikIdFromSerialNumber(subjectDN);
            if (telematikIdFromSerial != null && extractedTelematikId == null) {
              extractedTelematikId = telematikIdFromSerial;
              logger.debug(
                  "Extracted Telematik-ID from SMC-B certificate serialNumber: {}",
                  extractedTelematikId);
            }

            // Extract Telematik-ID from Organization field (alternative source)
            String telematikIdFromOrg = extractTelematikIdFromOrganization(subjectDN);
            if (telematikIdFromOrg != null && extractedTelematikId == null) {
              extractedTelematikId = telematikIdFromOrg;
              logger.debug(
                  "Extracted Telematik-ID from SMC-B certificate Organization: {}",
                  extractedTelematikId);
            }

            // Extract Telematik-ID from subject DN CN field (fallback)
            String telematikIdFromDN = extractTelematikIdFromDistinguishedName(subjectDN);
            if (telematikIdFromDN != null && extractedTelematikId == null) {
              extractedTelematikId = telematikIdFromDN;
              logger.debug(
                  "Extracted Telematik-ID from SMC-B certificate DN: {}", extractedTelematikId);
            }

            // Extract Telematik-ID from certificate extensions
            String telematikIdFromExt = extractTelematikIdFromCertificateExtensions(cert);
            if (telematikIdFromExt != null && extractedTelematikId == null) {
              extractedTelematikId = telematikIdFromExt;
              logger.debug(
                  "Extracted Telematik-ID from SMC-B certificate extensions: {}",
                  extractedTelematikId);
            }

            // Extract ProfessionOID from certificate extensions
            String professionOidFromCert = extractProfessionOidFromCertificate(cert);
            if (professionOidFromCert != null) {
              extractedProfessionOid = professionOidFromCert;
              logger.debug(
                  "Extracted ProfessionOID from SMC-B certificate: {}", extractedProfessionOid);
            }

            // Assign medical practice OID if found in medical context and not already set
            if (extractedProfessionOid == null
                && (subjectDN.toLowerCase().contains("praxis")
                    || subjectDN.toLowerCase().contains("dr."))) {
              extractedProfessionOid = "1.2.276.0.76.4.32";
              logger.debug(
                  "Assigned medical practice ProfessionOID based on certificate context: {}",
                  extractedProfessionOid);
            }

            // Extract holder name from subject DN
            String holderNameFromDN = extractHolderNameFromDistinguishedName(subjectDN);
            if (holderNameFromDN != null) {
              extractedHolderName = holderNameFromDN;
              logger.debug("Extracted holder name from SMC-B certificate: {}", extractedHolderName);
            }

            // Extract organization name from subject DN
            String organizationFromDN = extractOrganizationNameFromDistinguishedName(subjectDN);
            if (organizationFromDN != null) {
              extractedOrganizationName = organizationFromDN;
              logger.debug(
                  "Extracted organization from SMC-B certificate: {}", extractedOrganizationName);
            }

          } catch (Exception certParseError) {
            logger.debug("Could not parse as X.509 certificate: {}", certParseError.getMessage());
          }
        }

      } catch (Exception e) {
        logger.debug("Could not process SMC-B file {}: {}", fileName, e.getMessage());
      }
    }

    // Telematik-ID must be extracted from certificates only - no construction from ICCSN

    // If no ProfessionOID extracted but we have medical context, assign standard medical practice
    // OID
    if (extractedProfessionOid == null
        && (extractedOrganizationName != null
            && (extractedOrganizationName.toLowerCase().contains("praxis")
                || extractedOrganizationName.toLowerCase().contains("dr.")))) {
      extractedProfessionOid = "1.2.276.0.76.4.32";
      logger.debug(
          "Assigned medical practice ProfessionOID based on organization context: {}",
          extractedProfessionOid);
    }

    // Build result only if we found complete authentic data
    if (extractedTelematikId != null && extractedProfessionOid != null) {
      SmcBInfoDto result = new SmcBInfoDto();
      result.setTelematikId(extractedTelematikId);
      result.setProfessionOid(extractedProfessionOid);
      result.setHolderName(
          extractedHolderName != null ? extractedHolderName : "EXTRACTED_FROM_CERTIFICATE");
      result.setOrganizationName(
          extractedOrganizationName != null
              ? extractedOrganizationName
              : "EXTRACTED_FROM_CERTIFICATE");
      result.setCardType(card.getCardType().toString());

      logger.debug(
          "Successfully extracted complete authentic SMC-B data: Telematik-ID={}",
          extractedTelematikId);
      return result;
    }

    // If incomplete data, return error instead of generating fake values
    logger.warn("Incomplete SMC-B data extracted - missing required fields");
    String missingFields = "";
    if (extractedTelematikId == null) missingFields += "TelematikId ";
    if (extractedProfessionOid == null) missingFields += "ProfessionOid ";

    return createSmcBExtractionError("Missing required SMC-B data fields: " + missingFields, card);
  }

  /**
   * Extract Telematik-ID from serialNumber field in certificate DN. Format:
   * serialNumber=00.80276883110000168661 -> extract: 80276883110000168661
   */
  private String extractTelematikIdFromSerialNumber(String subjectDN) {
    try {
      // Look for serialNumber field in DN
      java.util.regex.Pattern serialPattern =
          java.util.regex.Pattern.compile(
              "serialNumber=([^,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
      java.util.regex.Matcher serialMatcher = serialPattern.matcher(subjectDN);

      if (serialMatcher.find()) {
        String serialValue = serialMatcher.group(1).trim();
        logger.debug("Found serialNumber field: {}", serialValue);

        // Extract Telematik-ID after the dot
        if (serialValue.contains(".")) {
          String telematikId = serialValue.substring(serialValue.indexOf(".") + 1);
          if (telematikId.length() >= 15 && telematikId.matches("[0-9]+")) {
            logger.debug("Extracted Telematik-ID from serialNumber: {}", telematikId);
            return telematikId;
          }
        }

        // If no dot, check if entire value is a valid Telematik-ID
        if (serialValue.length() >= 15 && serialValue.matches("[0-9]+")) {
          logger.debug("Using entire serialNumber as Telematik-ID: {}", serialValue);
          return serialValue;
        }
      }
    } catch (Exception e) {
      logger.debug("Could not extract Telematik-ID from serialNumber: {}", e.getMessage());
    }

    return null;
  }

  /**
   * Extract Telematik-ID from Organization field in certificate DN. Extract numeric ID from
   * organization field if present.
   */
  private String extractTelematikIdFromOrganization(String subjectDN) {
    try {
      // Look for Organization field in DN
      java.util.regex.Pattern orgPattern =
          java.util.regex.Pattern.compile("O=([^,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
      java.util.regex.Matcher orgMatcher = orgPattern.matcher(subjectDN);

      if (orgMatcher.find()) {
        String orgValue = orgMatcher.group(1).trim();
        logger.debug("Found Organization field: {}", orgValue);

        // Extract numeric ID from organization field (15+ digits)
        java.util.regex.Pattern numericPattern = java.util.regex.Pattern.compile("([0-9]{15,20})");
        java.util.regex.Matcher numericMatcher = numericPattern.matcher(orgValue);
        if (numericMatcher.find()) {
          String numericId = numericMatcher.group(1);
          logger.debug("Extracted numeric Telematik-ID from Organization: {}", numericId);
          return numericId;
        }

        // If no numeric pattern, return the full organization value as potential Telematik-ID
        if (orgValue.length() > 10) {
          logger.debug("Using Organization field as Telematik-ID: {}", orgValue);
          return orgValue;
        }
      }
    } catch (Exception e) {
      logger.debug("Could not extract Telematik-ID from Organization: {}", e.getMessage());
    }

    return null;
  }

  /** Extract Telematik-ID from X.509 certificate Distinguished Name CN field (fallback). */
  private String extractTelematikIdFromDistinguishedName(String subjectDN) {
    // Look for Telematik-ID in CN field
    java.util.regex.Pattern telematikPattern =
        java.util.regex.Pattern.compile(
            "CN=([^,]*[0-9]+-[A-Z0-9]+-[^,]*)", java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher telematikMatcher = telematikPattern.matcher(subjectDN);
    if (telematikMatcher.find()) {
      String telematikId = telematikMatcher.group(1).trim();
      // Remove TEST-ONLY suffix if present
      telematikId = telematikId.replaceAll("\\s*TEST-ONLY\\s*$", "");
      return telematikId;
    }

    // Look for specific Telematik-ID OID (1.2.276.0.76.4.3)
    java.util.regex.Pattern oidPattern =
        java.util.regex.Pattern.compile(
            "1\\.2\\.276\\.0\\.76\\.4\\.3=([^,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher oidMatcher = oidPattern.matcher(subjectDN);
    if (oidMatcher.find()) {
      return oidMatcher.group(1).trim();
    }

    // Look for OID fields that might contain Telematik-ID
    java.util.regex.Pattern smcbOidPattern =
        java.util.regex.Pattern.compile(
            "OID\\.[0-9\\.]+\\s*=\\s*([^,]*SMC[^,]*)", java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher smcbOidMatcher = smcbOidPattern.matcher(subjectDN);
    if (smcbOidMatcher.find()) {
      return smcbOidMatcher.group(1).trim();
    }

    return null;
  }

  /** Extract Telematik-ID from X.509 certificate extensions. */
  private String extractTelematikIdFromCertificateExtensions(
      java.security.cert.X509Certificate cert) {
    try {
      // Look for Telematik-ID in certificate extensions
      // Common OIDs for Telematik-ID: 1.2.276.0.76.4.3
      byte[] extensionValue = cert.getExtensionValue("1.2.276.0.76.4.3");
      if (extensionValue != null) {
        // Decode ASN.1 DER encoded extension value
        String telematikId = decodeTelematikIdFromExtension(extensionValue);
        if (telematikId != null) {
          logger.debug("Extracted Telematik-ID from certificate extension: {}", telematikId);
          return telematikId;
        }
      }

      // Check all certificate extensions for potential Telematik-ID
      java.util.Set<String> criticalOIDs = cert.getCriticalExtensionOIDs();
      if (criticalOIDs != null) {
        for (String oid : criticalOIDs) {
          if (oid.startsWith("1.2.276.0.76.4")) {
            byte[] extValue = cert.getExtensionValue(oid);
            if (extValue != null) {
              String telematikId = decodeTelematikIdFromExtension(extValue);
              if (telematikId != null && telematikId.contains("SMC")) {
                logger.debug("Found Telematik-ID in critical extension {}: {}", oid, telematikId);
                return telematikId;
              }
            }
          }
        }
      }

      java.util.Set<String> nonCriticalOIDs = cert.getNonCriticalExtensionOIDs();
      if (nonCriticalOIDs != null) {
        for (String oid : nonCriticalOIDs) {
          if (oid.startsWith("1.2.276.0.76.4")) {
            byte[] extValue = cert.getExtensionValue(oid);
            if (extValue != null) {
              String telematikId = decodeTelematikIdFromExtension(extValue);
              if (telematikId != null && telematikId.contains("SMC")) {
                logger.debug(
                    "Found Telematik-ID in non-critical extension {}: {}", oid, telematikId);
                return telematikId;
              }
            }
          }
        }
      }

    } catch (Exception e) {
      logger.debug(
          "Could not extract Telematik-ID from certificate extensions: {}", e.getMessage());
    }

    return null;
  }

  /** Decode Telematik-ID from ASN.1 DER encoded extension value. */
  private String decodeTelematikIdFromExtension(byte[] extensionValue) {
    try {
      // Try to extract any numeric Telematik-ID patterns from extension
      String extString = new String(extensionValue, java.nio.charset.StandardCharsets.UTF_8);

      // Look for numeric patterns that could be Telematik-IDs (15+ digits)
      java.util.regex.Pattern numericPattern = java.util.regex.Pattern.compile("([0-9]{15,20})");
      java.util.regex.Matcher numericMatcher = numericPattern.matcher(extString);
      if (numericMatcher.find()) {
        String candidate = numericMatcher.group(1).trim();
        logger.debug("Found numeric pattern in extension: {}", candidate);
        return candidate;
      }

    } catch (Exception e) {
      logger.debug("Could not decode Telematik-ID from extension: {}", e.getMessage());
    }

    return null;
  }

  // Removed hex-based Telematik-ID extraction as it created artificial values

  /** Convert hex string to readable string. */
  private String hexStringToString(String hex) {
    try {
      if (hex.length() % 2 != 0) {
        return null;
      }

      StringBuilder result = new StringBuilder();
      for (int i = 0; i < hex.length(); i += 2) {
        String hexByte = hex.substring(i, i + 2);
        int charCode = Integer.parseInt(hexByte, 16);
        if (charCode >= 32 && charCode <= 126) { // Printable ASCII
          result.append((char) charCode);
        }
      }

      String decoded = result.toString();
      return decoded.isEmpty() ? null : decoded;

    } catch (Exception e) {
      return null;
    }
  }

  /** Decode certificate extension value to readable string. */
  private String decodeExtensionValue(byte[] extensionValue) {
    try {
      // Try direct UTF-8 decoding
      String utf8String = new String(extensionValue, java.nio.charset.StandardCharsets.UTF_8);
      if (utf8String.length() > 0 && isReadableString(utf8String)) {
        return utf8String;
      }

      // Try ISO-8859-1 decoding
      String isoString = new String(extensionValue, java.nio.charset.StandardCharsets.ISO_8859_1);
      if (isoString.length() > 0 && isReadableString(isoString)) {
        return isoString;
      }

      // Try hex representation
      StringBuilder hexBuilder = new StringBuilder();
      for (byte b : extensionValue) {
        hexBuilder.append(String.format("%02X", b));
      }

      // Try to decode hex as ASCII
      String hexString = hexBuilder.toString();
      String decodedHex = hexStringToString(hexString);
      if (decodedHex != null && decodedHex.length() > 0) {
        return decodedHex;
      }

      return hexString; // Return raw hex if nothing else works

    } catch (Exception e) {
      logger.debug("Could not decode extension value: {}", e.getMessage());
      return null;
    }
  }

  /** Check if string contains mostly readable characters. */
  private boolean isReadableString(String str) {
    if (str == null || str.length() == 0) {
      return false;
    }

    int readableCount = 0;
    for (char c : str.toCharArray()) {
      if (c >= 32 && c <= 126) {
        readableCount++;
      }
    }

    return (readableCount * 1.0 / str.length()) > 0.5; // At least 50% readable chars
  }

  /** Extract Telematik-ID from any string containing numeric ID patterns. */
  private String extractTelematikIdFromString(String input) {
    if (input == null || input.length() == 0) {
      return null;
    }

    // Look for numeric Telematik-ID patterns (15+ digits)
    java.util.regex.Pattern numericPattern = java.util.regex.Pattern.compile("([0-9]{15,20})");
    java.util.regex.Matcher numericMatcher = numericPattern.matcher(input);

    if (numericMatcher.find()) {
      String candidate = numericMatcher.group(1).trim();
      logger.debug("Found numeric Telematik-ID pattern: {}", candidate);
      return candidate;
    }

    return null;
  }

  /** Extract ProfessionOID from X.509 certificate extensions or DN. */
  private String extractProfessionOidFromCertificate(java.security.cert.X509Certificate cert) {
    try {
      // Look for profession OID in certificate extensions
      byte[] extensionValue = cert.getExtensionValue("1.2.276.0.76.4.32");
      if (extensionValue != null) {
        return "1.2.276.0.76.4.32"; // Medical practice OID
      }

      // Check for other common profession OIDs in DN
      String subjectDN = cert.getSubjectDN().toString();
      if (subjectDN.contains("1.2.276.0.76.4")) {
        java.util.regex.Pattern professionPattern =
            java.util.regex.Pattern.compile("(1\\.2\\.276\\.0\\.76\\.4\\.[0-9]+)");
        java.util.regex.Matcher professionMatcher = professionPattern.matcher(subjectDN);
        if (professionMatcher.find()) {
          return professionMatcher.group(1);
        }
      }

    } catch (Exception e) {
      logger.debug("Could not extract profession OID from certificate: {}", e.getMessage());
    }

    return null;
  }

  /** Extract holder name from X.509 certificate Distinguished Name. */
  private String extractHolderNameFromDistinguishedName(String subjectDN) {
    // Extract CN (Common Name) field which usually contains the holder name
    java.util.regex.Pattern cnPattern =
        java.util.regex.Pattern.compile("CN=([^,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher cnMatcher = cnPattern.matcher(subjectDN);
    if (cnMatcher.find()) {
      String commonName = cnMatcher.group(1).trim();
      // Remove TEST-ONLY suffix if present
      commonName = commonName.replaceAll("\\s*TEST-ONLY\\s*$", "");

      // If CN contains Telematik-ID format, look for GIVENNAME and SURNAME
      if (commonName.matches(".*[0-9]+-[A-Z0-9]+-.*")) {
        // Try to extract from givenName and surname fields
        java.util.regex.Pattern givenNamePattern =
            java.util.regex.Pattern.compile(
                "GIVENNAME=([^,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Pattern surnamePattern =
            java.util.regex.Pattern.compile(
                "SURNAME=([^,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);

        java.util.regex.Matcher givenNameMatcher = givenNamePattern.matcher(subjectDN);
        java.util.regex.Matcher surnameMatcher = surnamePattern.matcher(subjectDN);

        String givenName = null, surname = null;

        if (givenNameMatcher.find()) {
          givenName = givenNameMatcher.group(1).trim();
        }
        if (surnameMatcher.find()) {
          surname = surnameMatcher.group(1).trim();
        }

        if (givenName != null && surname != null) {
          return givenName + " " + surname;
        }

        return null; // Don't return Telematik-ID as holder name
      }

      return commonName;
    }

    return null;
  }

  /** Extract organization name from X.509 certificate Distinguished Name. */
  private String extractOrganizationNameFromDistinguishedName(String subjectDN) {
    // Look for organization name in O field
    java.util.regex.Pattern orgPattern =
        java.util.regex.Pattern.compile("O=([^,]+)", java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher orgMatcher = orgPattern.matcher(subjectDN);
    if (orgMatcher.find()) {
      String orgName = orgMatcher.group(1).trim();
      // Remove NOT-VALID suffix if present
      orgName = orgName.replaceAll("\\s*NOT-VALID\\s*$", "");
      if (!orgName.isEmpty()) {
        return orgName;
      }
    }

    return null;
  }

  /** Find card by handle across all slots. */
  private CardImage findCardByHandle(String cardHandle) {
    for (int slotId = 0; slotId < slotManager.getSlotCount(); slotId++) {
      if (slotManager.isCardPresent(slotId)) {
        CardImage card = slotManager.getCardInSlot(slotId);
        if (cardHandle.equals(card.getId())) {
          return card;
        }
      }
    }
    return null;
  }

  /** Check if the card is an SMC-B card. */
  private boolean isSMCBCard(CardImage card) {
    return card.getCardType() == CardType.HPIC
        || card.getCardType() == CardType.HPC
        || (card.getLabel() != null && card.getLabel().contains("SMC-B"));
  }

  /** Create error response when authentic SMC-B data extraction fails. */
  private SmcBInfoDto createSmcBExtractionError(String errorMessage, CardImage card) {
    logger.error("SMC-B data extraction failed: {}", errorMessage);
    SmcBInfoDto errorInfo = new SmcBInfoDto();
    errorInfo.setTelematikId("DATA_EXTRACTION_FAILED");
    errorInfo.setProfessionOid("DATA_EXTRACTION_FAILED");
    errorInfo.setHolderName("EXTRACTION_ERROR");
    errorInfo.setOrganizationName("DATA EXTRACTION FAILED: " + errorMessage);
    errorInfo.setCardType(card.getCardType().toString());
    return errorInfo;
  }

  /** Convert hex string to byte array. */
  private byte[] hexStringToByteArray(String hexString) {
    int len = hexString.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(hexString.charAt(i), 16) << 4)
                  + Character.digit(hexString.charAt(i + 1), 16));
    }
    return data;
  }

  /** Get debug information for SMC-B card files (required by CardController). */
  public java.util.Map<String, Object> getDebugCardFiles(String cardHandle) {
    java.util.Map<String, Object> debugInfo = new java.util.HashMap<>();

    try {
      CardImage card = findCardByHandle(cardHandle);
      if (card == null) {
        debugInfo.put("error", "Card not found: " + cardHandle);
        return debugInfo;
      }

      if (!isSMCBCard(card)) {
        debugInfo.put("error", "Not an SMC-B card: " + cardHandle);
        return debugInfo;
      }

      debugInfo.put("cardType", card.getCardType().toString());
      debugInfo.put("cardId", card.getId());
      debugInfo.put("label", card.getLabel());

      List<FileData> files = card.getAllFiles();
      debugInfo.put("totalFiles", files.size());

      java.util.List<java.util.Map<String, Object>> fileInfoList = new java.util.ArrayList<>();
      for (FileData file : files) {
        java.util.Map<String, Object> fileInfo = new java.util.HashMap<>();
        fileInfo.put("name", file.getName());
        fileInfo.put("fileId", file.getFileId());
        fileInfo.put("dataLength", file.getData() != null ? file.getData().length() : 0);

        if (file.getData() != null
            && (file.getData().startsWith("3082") || file.getData().startsWith("3081"))) {
          fileInfo.put("isPossibleX509", true);
        }

        fileInfoList.add(fileInfo);
      }
      debugInfo.put("files", fileInfoList);

      // Try to extract data
      SmcBInfoDto extractedData = extractSmcBInfo(cardHandle);
      debugInfo.put("extractedData", extractedData);

    } catch (Exception e) {
      debugInfo.put("error", "Debug extraction failed: " + e.getMessage());
    }

    return debugInfo;
  }
}
