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
/*
 * Copyright (Date see Readme), gematik GmbH
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

import de.gematik.ti20.simsvc.client.model.VirtualCardImageData;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Component
public final class VirtualCardImageLoader {

  public VirtualCardImageData load(final String xmlString)
      throws IOException, SAXException, ParserConfigurationException {

    final List<String> certificateIds =
        List.of(
            // eGK
            "EF.PD",
            "EF.VD",
            "EF.C.CH.AUT.E256",
            // SmcB
            "EF.C.HCI.ENC.E256",
            "EF.C.HCI.AUT.E256");

    final Map<String, String> certificates = new java.util.HashMap<>();
    for (final String certificateId : certificateIds) {
      final String certData = getCertificateData(xmlString, certificateId);
      if (certData != null) {
        certificates.put(certificateId, certData);
      }
    }

    return new VirtualCardImageData(certificates);
  }

  private Element getDOMRootElement(final String xmlDoc)
      throws IOException, SAXException, ParserConfigurationException {

    final var dbFactory = DocumentBuilderFactory.newInstance();

    dbFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

    dbFactory.setXIncludeAware(false);
    dbFactory.setExpandEntityReferences(false);

    dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    final var dBuilder = dbFactory.newDocumentBuilder();
    final var doc = dBuilder.parse(new InputSource(new StringReader(xmlDoc)));

    doc.getDocumentElement().normalize();
    return doc.getDocumentElement();
  }

  private String getCertificateData(final String xmlDoc, final String certName)
      throws IOException, SAXException, ParserConfigurationException {
    final var rootElement = getDOMRootElement(xmlDoc);
    final var children = rootElement.getElementsByTagName("child");
    for (int i = 0; i < children.getLength(); i++) {
      final var child = (Element) children.item(i);
      if (certName.equals(child.getAttribute("id"))) {
        final var attributes = child.getElementsByTagName("attribute");
        for (int j = 0; j < attributes.getLength(); j++) {
          final var attribute = (Element) attributes.item(j);
          if ("body".equals(attribute.getAttribute("id"))) {
            return attribute.getTextContent().trim();
          }
        }
      }
    }
    return null;
  }
}
