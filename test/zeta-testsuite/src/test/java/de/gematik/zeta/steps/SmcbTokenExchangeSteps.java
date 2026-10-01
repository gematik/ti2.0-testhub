/*-
 * #%L
 * ZeTA Testsuite
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
package de.gematik.zeta.steps;

import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import io.cucumber.java.de.Dann;
import java.security.Security;
import java.security.cert.X509Certificate;
import lombok.extern.slf4j.Slf4j;

/** Cucumber step that extracts SMC-B certificate data (TelematikID, professionOID). */
@Slf4j
public class SmcbTokenExchangeSteps {

  static {
    // Remove BouncyCastle JSSE so SunJSSE handles TLS with standard root CAs (e.g. DigiCert).
    Security.removeProvider("BCJSSE");
  }

  /** Extracts the TelematikID from the Admission extension of an SMC-B certificate. */
  private String extractTelematikIdFromCert(X509Certificate cert) {
    try {
      byte[] extValue = cert.getExtensionValue("1.3.36.8.3.3"); // Admission OID
      if (extValue == null) {
        throw new IllegalStateException("No Admission extension in SMC-B certificate");
      }
      try (var asn1In = new org.bouncycastle.asn1.ASN1InputStream(extValue)) {
        var octetString = (org.bouncycastle.asn1.ASN1OctetString) asn1In.readObject();
        try (var asn1In2 = new org.bouncycastle.asn1.ASN1InputStream(octetString.getOctets())) {
          var seq = (org.bouncycastle.asn1.ASN1Sequence) asn1In2.readObject();
          // Navigate to the registration number (TelematikID)
          while (seq.size() == 1) {
            seq = (org.bouncycastle.asn1.ASN1Sequence) seq.getObjectAt(0);
          }
          if (seq.size() >= 3) {
            var regNum = seq.getObjectAt(2);
            while (regNum instanceof org.bouncycastle.asn1.DLSequence dlseq) {
              regNum = dlseq.getObjectAt(0);
            }
            return regNum.toASN1Primitive().toString();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Could not extract TelematikID from certificate: {}", e.getMessage());
    }
    // Fallback
    return "3-2-TestTelematikId";
  }

  /** Extracts the profession OID from the Admission extension of an SMC-B certificate. */
  private String extractProfessionOidFromCert(X509Certificate cert) {
    try {
      byte[] extValue = cert.getExtensionValue("1.3.36.8.3.3"); // Admission OID
      if (extValue == null) {
        throw new IllegalStateException("No Admission extension in SMC-B certificate");
      }
      try (var asn1In = new org.bouncycastle.asn1.ASN1InputStream(extValue)) {
        var octetString = (org.bouncycastle.asn1.ASN1OctetString) asn1In.readObject();
        try (var asn1In2 = new org.bouncycastle.asn1.ASN1InputStream(octetString.getOctets())) {
          var seq = (org.bouncycastle.asn1.ASN1Sequence) asn1In2.readObject();
          // Navigate admission data (unwrap single-element sequences)
          while (seq.size() == 1) {
            seq = (org.bouncycastle.asn1.ASN1Sequence) seq.getObjectAt(0);
          }
          // Index 1 = professionOid, Index 2 = telematikId (ProfessionInfo structure)
          if (seq.size() >= 3) {
            var profOid = seq.getObjectAt(1);
            while (profOid instanceof org.bouncycastle.asn1.DLSequence dlseq) {
              profOid = dlseq.getObjectAt(0);
            }
            return profOid.toASN1Primitive().toString();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Could not extract profession OID from certificate: {}", e.getMessage());
    }
    return "1.2.276.0.76.4.50"; // Fallback: Betriebsstätte Arzt
  }

  @Dann("schreibe Daten aus dem SMC-B Zertifikat {string} in die Variable {string}")
  public void extractSmcbCertificateData(String certBase64Var, String variableName) {
    String resolvedCert = TigerGlobalConfiguration.resolvePlaceholders(certBase64Var);
    try {
      byte[] certBytes = java.util.Base64.getDecoder().decode(resolvedCert);
      java.security.cert.CertificateFactory cf =
          java.security.cert.CertificateFactory.getInstance("X.509");
      java.security.cert.X509Certificate cert =
          (java.security.cert.X509Certificate)
              cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));

      // Extract TelematikID and professionOID from Admission extension (OID 1.3.36.8.3.3)
      String telematikId = extractTelematikIdFromCert(cert);
      String professionId = extractProfessionOidFromCert(cert);

      TigerGlobalConfiguration.putValue(variableName + ".telematikId", telematikId);
      TigerGlobalConfiguration.putValue(variableName + ".professionId", professionId);
      log.info("Extracted SMC-B data: telematikId={}, professionId={}", telematikId, professionId);
    } catch (Exception e) {
      throw new AssertionError("Failed to extract SMC-B certificate data: " + e.getMessage(), e);
    }
  }
}
