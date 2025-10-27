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
package de.gematik.ti20.simsvc.client.service.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.IOUtils;

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
}
