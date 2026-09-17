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
package de.gematik.ti20.simsvc.client.service.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.NodeList;

public final class XmlContainerFileHelper {

  private XmlContainerFileHelper() {}

  public static byte[] uncompressDocumentWithStartAndEndOffset(
      byte[] givenData, int startOffset, int endOffset) throws IOException {
    byte[] rawData = Arrays.copyOfRange(givenData, startOffset, endOffset + 1);
    GZIPInputStream zipIn = new GZIPInputStream(new ByteArrayInputStream(rawData));
    return readDataFromStreamWithRawSizeAndThrowExceptionIfFails(zipIn);
  }

  public static byte[] readDataFromStreamWithRawSizeAndThrowExceptionIfFails(
      InputStream inputStream) throws IOException {
    return IOUtils.toByteArray(inputStream);
  }

  public static String getFirstTagValueOrNull(String xml, String tagName) throws IOException {
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setExpandEntityReferences(false);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      var builder = factory.newDocumentBuilder();
      var document =
          builder.parse(
              new ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
      NodeList nodes = document.getElementsByTagName(tagName);
      if (nodes.getLength() == 0) {
        return null;
      }
      return nodes.item(0).getTextContent();
    } catch (Exception e) {
      throw new IOException("Failed to extract XML tag value for tag: " + tagName, e);
    }
  }

  public static String extractCnValueOrNull(String name) {
    if (name == null) {
      return null;
    }
    int startIndex = name.indexOf("CN=");
    if (startIndex < 0) {
      return null;
    }
    startIndex += 3;
    int endIndex = name.indexOf(',', startIndex);
    if (endIndex < 0) {
      endIndex = name.length();
    }
    return name.substring(startIndex, endIndex).trim();
  }
}
