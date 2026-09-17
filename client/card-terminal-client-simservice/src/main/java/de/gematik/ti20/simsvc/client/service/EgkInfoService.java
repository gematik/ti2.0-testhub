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
import de.gematik.ti20.simsvc.client.model.dto.EgkInfoDto;
import de.gematik.ti20.simsvc.client.service.helper.ByteUtils;
import de.gematik.ti20.simsvc.client.service.helper.CardFileToolkitUtils;
import de.gematik.ti20.simsvc.client.service.helper.HexUtils;
import de.gematik.ti20.simsvc.client.service.helper.XmlContainerFileHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EgkInfoService {

  private static final Logger logger = LoggerFactory.getLogger(EgkInfoService.class);
  public static final String ISO_8859_15 = "ISO-8859-15";

  public EgkInfoDto extractEgkInfo(final VirtualCardImageData imageData) {
    logger.debug("Extracting EGK info from VirtualCardImageData");

    if (imageData == null || imageData.getCertificate("EF.PD") == null) {
      // not an eGK
      return null;
    }

    final EgkInfoDto egkInfoDto = new EgkInfoDto();
    egkInfoDto.setCardType("EGK");

    final String pdData = imageData.getCertificate("EF.PD");
    parsePersonalData(pdData, egkInfoDto);

    final String vdData = imageData.getCertificate("EF.VD");
    parseInsuranceData(vdData, egkInfoDto);

    final String autData = imageData.getCertificate("EF.C.CH.AUT.E256");
    parseCertificate(autData, egkInfoDto);

    return egkInfoDto;
  }

  private void parsePersonalData(final String pdData, final EgkInfoDto egkInfoDto) {
    if (pdData != null) {
      try {
        final String personalData = parsePd(pdData);
        egkInfoDto.setKvnr(
            XmlContainerFileHelper.getFirstTagValueOrNull(personalData, "Versicherten_ID"));
        egkInfoDto.setFirstName(
            XmlContainerFileHelper.getFirstTagValueOrNull(personalData, "Vorname"));
        egkInfoDto.setLastName(
            XmlContainerFileHelper.getFirstTagValueOrNull(personalData, "Nachname"));
        egkInfoDto.setDateOfBirth(
            XmlContainerFileHelper.getFirstTagValueOrNull(personalData, "Geburtsdatum"));

      } catch (final IOException e) {
        logger.error("Error parsing PD data", e);
      }
    }
  }

  private void parseInsuranceData(final String vdData, final EgkInfoDto egkInfoDto) {
    if (vdData != null) {
      try {
        final String[] vData = parseVd(vdData);
        egkInfoDto.setIknr(
            XmlContainerFileHelper.getFirstTagValueOrNull(vData[0], "Kostentraegerkennung"));
        egkInfoDto.setInsuranceName(
            XmlContainerFileHelper.getFirstTagValueOrNull(vData[0], "Name"));
      } catch (IOException e) {
        logger.error("Error parsing VD data", e);
      }
    }
  }

  private void parseCertificate(final String autData, final EgkInfoDto egkInfoDto) {
    if (autData == null || autData.isBlank()) {
      logger.debug("autData is empty");
      return;
    }

    try {
      java.security.cert.CertificateFactory certFactory =
          java.security.cert.CertificateFactory.getInstance("X.509");

      byte[] buf = HexUtils.decodeHexBytes(autData);
      java.security.cert.X509Certificate cert =
          (java.security.cert.X509Certificate)
              certFactory.generateCertificate(new ByteArrayInputStream(buf));
      cert.checkValidity();
      egkInfoDto.setValid(true);
      return;
    } catch (final Exception certParseError) {
      logger.debug(
          "Could not initialize X.509 certificate factory: {}", certParseError.getMessage());
    }

    egkInfoDto.setValid(false);
  }

  public static String parsePd(final String pd) throws IOException {
    byte[] data = ByteUtils.getByteArray(pd);
    byte[] pdbytes = CardFileToolkitUtils.uncompressEfPd(data);
    return new String(pdbytes, ISO_8859_15);
  }

  public static String[] parseVd(final String vd) throws IOException {
    byte[] data = ByteUtils.getByteArray(vd);
    byte[][] vdbytes = CardFileToolkitUtils.uncompressAvdAndGvd(data);
    return new String[] {new String(vdbytes[0], ISO_8859_15), new String(vdbytes[1], ISO_8859_15)};
  }
}
