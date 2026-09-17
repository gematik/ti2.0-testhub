/*-
 * #%L
 * Card Terminal Simulator
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.ti20.simsvc.client.service;

import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.model.dto.SmcBInfoDto;
import de.gematik.ti20.simsvc.client.service.helper.HexUtils;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for extracting authentic SMC-B card information from X.509 certificates. No hardcoded
 * values - only authentic data extraction.
 */
@Service
public class SmcBInfoService {

  private static final Logger logger = LoggerFactory.getLogger(SmcBInfoService.class);
  private static final Asn1TreeParser ASN1_TREE_PARSER = new Asn1TreeParser();

  public SmcBInfoDto extractSmcBInfo(final VirtualCardImageData imageData) {
    if (imageData == null || imageData.getCertificate("EF.C.HCI.ENC.E256") == null) {
      // not an SmcB
      return null;
    }

    final SmcBInfoDto smcBInfoDto = new SmcBInfoDto();
    smcBInfoDto.setCardType("SMCB");
    final String encData = imageData.getCertificate("EF.C.HCI.ENC.E256");
    parseEncCertificate(encData, smcBInfoDto);

    final String autData = imageData.getCertificate("EF.C.HCI.AUT.E256");
    parseAutCertificate(autData, smcBInfoDto);

    return smcBInfoDto;
  }

  private void parseEncCertificate(final String autData, final SmcBInfoDto smcBInfoDto) {
    if (autData == null || autData.isBlank()) {
      logger.debug("autData is empty");
      return;
    }

    try {
      final java.security.cert.CertificateFactory certFactory =
          java.security.cert.CertificateFactory.getInstance("X.509");
      final byte[] buf = HexUtils.decodeHexBytes(autData);
      final java.security.cert.X509Certificate cert =
          (java.security.cert.X509Certificate)
              certFactory.generateCertificate(new ByteArrayInputStream(buf));

      final byte[] extensionValue = cert.getExtensionValue("1.3.36.8.3.3");
      final Asn1TreeParser.Asn1Node extensionAsn1Tree =
          extensionValue != null ? ASN1_TREE_PARSER.parse(extensionValue) : null;
      if (extensionAsn1Tree != null) {
        final List<String> leafValues = Asn1TreeParser.collectLeafValuesAsString(extensionAsn1Tree);

        smcBInfoDto.setProfessionOid(leafValues.get(1));
        smcBInfoDto.setTelematikId(leafValues.get(2));
      }

    } catch (final Exception certParseError) {
      logger.debug(
          "Could not initialize X.509 certificate factory: {}", certParseError.getMessage());
    }
  }

  private void parseAutCertificate(final String autData, final SmcBInfoDto smcBInfoDto) {
    if (autData == null || autData.isBlank()) {
      logger.debug("autData is empty");
      return;
    }

    try {
      final java.security.cert.CertificateFactory certFactory =
          java.security.cert.CertificateFactory.getInstance("X.509");
      final byte[] buf = HexUtils.decodeHexBytes(autData);
      final java.security.cert.X509Certificate cert =
          (java.security.cert.X509Certificate)
              certFactory.generateCertificate(new ByteArrayInputStream(buf));

      final byte[] extensionValue = cert.getExtensionValue("1.3.36.8.3.3");
      final Asn1TreeParser.Asn1Node extensionAsn1Tree =
          extensionValue != null ? ASN1_TREE_PARSER.parse(extensionValue) : null;
      if (extensionAsn1Tree != null) {
        logger.debug(
            "Parsed ASN.1 extension tree for 1.3.36.8.3.3 with {} top-level node(s)",
            extensionAsn1Tree.children().size());
      }

      final String name = extractValueOrNull(cert.getSubjectX500Principal().getName(), "CN=");
      smcBInfoDto.setHolderName(name);

      final String organizationName =
          extractValueOrNull(cert.getSubjectX500Principal().getName(), "O=");
      smcBInfoDto.setOrganizationName(organizationName);
    } catch (final Exception certParseError) {
      logger.debug(
          "Could not initialize X.509 certificate factory: {}", certParseError.getMessage());
    }
  }

  public static String extractValueOrNull(final String name, final String key) {
    if (name == null) {
      return null;
    }
    int startIndex = name.indexOf(key);
    if (startIndex < 0) {
      return null;
    }
    startIndex += key.length();
    int endIndex = name.indexOf(',', startIndex);
    if (endIndex < 0) {
      endIndex = name.length();
    }
    return name.substring(startIndex, endIndex).trim();
  }
}
