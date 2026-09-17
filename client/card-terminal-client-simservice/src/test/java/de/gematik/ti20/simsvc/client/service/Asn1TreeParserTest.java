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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import de.gematik.ti20.simsvc.client.service.helper.HexUtils;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import org.junit.jupiter.api.Test;

class Asn1TreeParserTest {

  private final Asn1TreeParser parser = new Asn1TreeParser();
  private final VirtualCardImageLoader loader = new VirtualCardImageLoader();

  @Test
  void shouldParseSmcbCertificateExtensionFromXmlCardImage() throws Exception {
    String xml = loadResourceAsString("SMC_B_80276883110000168650_gema5.xml");
    VirtualCardImageData imageData = loader.load(xml);

    String certificateHex = imageData.getCertificate("EF.C.HCI.ENC.E256");
    assertNotNull(certificateHex);

    X509Certificate certificate =
        (X509Certificate)
            CertificateFactory.getInstance("X.509")
                .generateCertificate(
                    new ByteArrayInputStream(HexUtils.decodeHexBytes(certificateHex)));

    byte[] extensionValue = certificate.getExtensionValue("1.3.36.8.3.3");
    assertNotNull(extensionValue);

    Asn1TreeParser.Asn1Node root = parser.parse(extensionValue);
    List<String> leafValues = Asn1TreeParser.collectLeafValuesAsString(root);

    assertFalse(leafValues.isEmpty());
    assertEquals("1.2.276.0.76.4.50", leafValues.get(1));
    assertEquals("1-SMC-B-Testkarte--883110000168650", leafValues.get(2));
  }

  @Test
  void shouldCollectPrimitiveLeafValuesAsString() {
    Asn1TreeParser.Asn1Node root =
        new Asn1TreeParser.Asn1Node(
            -1,
            0,
            true,
            new byte[0],
            List.of(
                new Asn1TreeParser.Asn1Node(0x01, 0, false, new byte[] {(byte) 0xFF}, List.of()),
                new Asn1TreeParser.Asn1Node(0x02, 0, false, new byte[] {0x2A}, List.of()),
                new Asn1TreeParser.Asn1Node(
                    0x0C,
                    0,
                    false,
                    "ABC".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    List.of())));

    assertEquals(List.of("true", "42", "ABC"), Asn1TreeParser.collectLeafValuesAsString(root));
  }

  @Test
  void shouldUnwrapEncapsulatedOctetStringsAtRootLevel() {
    byte[] encoded = HexUtils.decodeHexBytes("0405300302012A");

    Asn1TreeParser.Asn1Node root = parser.parse(encoded);

    assertEquals(1, root.children().size());
    Asn1TreeParser.Asn1Node sequenceNode = root.children().get(0);
    assertEquals(0x30, sequenceNode.tag());
    assertEquals(List.of("42"), Asn1TreeParser.collectLeafValuesAsString(root));
  }

  @Test
  void shouldKeepPrimitiveOctetStringWhenPayloadIsNotAsn1() {
    byte[] encoded = HexUtils.decodeHexBytes("04024869");

    Asn1TreeParser.Asn1Node root = parser.parse(encoded);

    assertEquals(1, root.children().size());
    Asn1TreeParser.Asn1Node octetStringNode = root.children().get(0);
    assertEquals(0x04, octetStringNode.tag());
    assertFalse(octetStringNode.constructed());
    assertTrue(octetStringNode.children().isEmpty());
    assertArrayEquals(HexUtils.decodeHexBytes("4869"), octetStringNode.value());
    assertEquals(List.of("4869"), Asn1TreeParser.collectLeafValuesAsString(root));
  }

  @Test
  void shouldRejectTruncatedAsn1Values() {
    byte[] encoded = HexUtils.decodeHexBytes("30030201");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> parser.parse(encoded));

    assertEquals("Invalid ASN.1: value exceeds bounds", exception.getMessage());
  }

  @Test
  void shouldCompareValueArraysByContent() {
    Asn1TreeParser.Asn1Node left =
        new Asn1TreeParser.Asn1Node(0x02, 1, false, new byte[] {0x01, 0x02}, List.of());
    Asn1TreeParser.Asn1Node sameValuesDifferentArray =
        new Asn1TreeParser.Asn1Node(0x02, 1, false, new byte[] {0x01, 0x02}, List.of());
    Asn1TreeParser.Asn1Node differentValue =
        new Asn1TreeParser.Asn1Node(0x02, 1, false, new byte[] {0x01, 0x03}, List.of());

    assertEquals(left, sameValuesDifferentArray);
    assertEquals(left.hashCode(), sameValuesDifferentArray.hashCode());
    assertNotEquals(left, differentValue);
  }

  @Test
  void shouldRenderValueArrayInToString() {
    Asn1TreeParser.Asn1Node node =
        new Asn1TreeParser.Asn1Node(0x02, 1, false, new byte[] {0x01, 0x02}, List.of());

    assertEquals(
        "Asn1Node{tag=2, offset=1, constructed=false, value=[1, 2], children=[]}", node.toString());
  }

  private String loadResourceAsString(String resourceName) throws Exception {
    return Files.readString(Path.of(ClassLoader.getSystemResource(resourceName).toURI()));
  }
}
