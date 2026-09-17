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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class XmlContainerFileHelperTest {

  @Test
  void getFirstTagValueOrNull_returnsFirstMatchingValue() throws IOException {
    String xml = "<root><tag>first</tag><tag>second</tag></root>";

    assertEquals("first", XmlContainerFileHelper.getFirstTagValueOrNull(xml, "tag"));
  }

  @Test
  void getFirstTagValueOrNull_returnsNullWhenTagIsMissing() throws IOException {
    String xml = "<root><other>value</other></root>";

    assertNull(XmlContainerFileHelper.getFirstTagValueOrNull(xml, "tag"));
  }

  @Test
  void getFirstTagValueOrNull_handlesNestedContentAsText() throws IOException {
    String xml = "<root><tag><inner>value</inner></tag></root>";

    assertEquals("value", XmlContainerFileHelper.getFirstTagValueOrNull(xml, "tag"));
  }

  @Test
  void extractCnValueOrNull_returnsValueBetweenCnAndComma() {
    String name = "O=Test, CN=Max Mustermann, OU=IT";

    assertEquals("Max Mustermann", XmlContainerFileHelper.extractCnValueOrNull(name));
  }

  @Test
  void extractCnValueOrNull_returnsNullWhenCnIsMissing() {
    assertNull(XmlContainerFileHelper.extractCnValueOrNull("O=Test, OU=IT"));
  }
}
