/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.zeta.steps;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.gematik.rbellogger.data.RbelElement;
import de.gematik.rbellogger.facets.pki.RbelX509CertificateFacet;
import de.gematik.test.tiger.common.config.ConfigurationValuePrecedence;
import de.gematik.test.tiger.common.config.TigerGlobalConfiguration;
import de.gematik.test.tiger.lib.rbel.RbelMessageRetriever;
import io.cucumber.java.de.Dann;
import io.cucumber.java.en.And;
import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DLSequence;

/**
 * Extracts Telematik-ID, professionOID, commonName and organizationName from an SMC-B certificate
 * that has already been parsed by the RBel engine (requires {@code x509} in {@code
 * tigerProxy.activateRbelParsingFor}).
 *
 * <p>Subject-DN fields (CN, O) are read directly from the parsed RBel tree. TelematikID and
 * professionOID are extracted from the Admission extension (OID 1.3.36.8.3.3) via the {@link
 * X509Certificate} object held by the {@link RbelX509CertificateFacet}, because the RBel ASN.1
 * converter does not recursively parse extension values.
 */
public class SmcbSteps {

  private static final ASN1ObjectIdentifier ADMISSION_OID =
      new ASN1ObjectIdentifier("1.3.36.8.3.3");

  /**
   * Extracts SMC-B certificate data from the RBel tree at the given path and stores it as a Tiger
   * variable.
   *
   * @param rbelPath RBel path to the certificate node, e.g. {@code
   *     "$.body.subject_token.header.x5c.0"}
   * @param varName Tiger variable name (e.g. {@code "SMCB-INFO"})
   */
  @Dann(
      "extrahiere SMC-B Daten aus dem Knoten {string} der aktuellen Anfrage in die Variable"
          + " {string}")
  @And("extract SMC-B data from node {string} of the current request into the variable {string}")
  public void extractSmcbDataFromTree(String rbelPath, String varName) {
    var retriever = RbelMessageRetriever.getInstance();

    // Subject-DN fields from the parsed RBel tree (RbelX500Converter)
    String commonName = getElementText(retriever, rbelPath + ".content.subject.CN");
    String orgName = getElementText(retriever, rbelPath + ".content.subject.O");

    // X509Certificate object from the RBel facet for Admission extension parsing
    RbelElement certElement = retriever.findElementInCurrentRequest(rbelPath + ".content");
    X509Certificate x509 =
        certElement.getFacetOrFail(RbelX509CertificateFacet.class).getCertificate();

    CertificateInfo info = new CertificateInfo();
    info.commonName = commonName;
    info.organizationName = orgName;
    extractAdmissionValues(x509, info);

    TigerGlobalConfiguration.putValue(varName, info, ConfigurationValuePrecedence.TEST_CONTEXT);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class CertificateInfo {
    @JsonProperty("telematikId")
    String telematikId;

    @JsonProperty("commonName")
    String commonName;

    @JsonProperty("organizationName")
    String organizationName;

    @JsonProperty("professionId")
    String professionId;
  }

  private static String getElementText(RbelMessageRetriever retriever, String rbelPath) {
    var elements = retriever.findElementsInCurrentRequestOrEmpty(rbelPath);
    if (elements.isEmpty()) {
      return "";
    }
    return elements
        .getFirst()
        .printValue()
        .orElseGet(() -> elements.getFirst().getRawStringContent());
  }

  private static void extractAdmissionValues(X509Certificate cert, CertificateInfo info) {
    try {
      byte[] extValue = cert.getExtensionValue(ADMISSION_OID.getId());
      if (extValue == null) {
        return;
      }
      try (var asn1In = new ASN1InputStream(new ByteArrayInputStream(extValue))) {
        var obj = asn1In.readObject();
        if (obj instanceof ASN1OctetString octetString) {
          try (var asn1In2 =
              new ASN1InputStream(new ByteArrayInputStream(octetString.getOctets()))) {
            var seqObj = asn1In2.readObject();
            if (seqObj instanceof ASN1Sequence admissionData) {
              // Unwrap single-element sequences to reach ProfessionInfo (3 elements)
              while (admissionData.size() == 1) {
                admissionData = (ASN1Sequence) admissionData.getObjectAt(0);
              }
              info.professionId = extractFromSequence(admissionData, 1);
              info.telematikId = extractFromSequence(admissionData, 2);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new AssertionError("Could not extract Admission data from SMC-B certificate.", e);
    }
  }

  private static String extractFromSequence(ASN1Sequence seq, int index) {
    if (seq.size() >= 3) {
      ASN1Encodable enc = seq.getObjectAt(index);
      while (enc instanceof DLSequence dlSeq) {
        enc = dlSeq.getObjectAt(0);
      }
      return enc.toASN1Primitive().toString();
    }
    return "";
  }
}
