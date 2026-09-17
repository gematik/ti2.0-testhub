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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

class CardFileToolkitUtilsTest {

  @Test
  void uncompressAvdAndGvdReturnsBothDocuments() throws IOException {
    byte[] avd = gzip("allgemeine-versicherungsdaten");
    byte[] gvd = gzip("geschuetzte-versichertendaten");
    byte[] efVd = withTwoDocumentOffsets(avd, gvd);

    byte[][] result = CardFileToolkitUtils.uncompressAvdAndGvd(efVd);

    assertEquals(2, result.length);
    assertArrayEquals("allgemeine-versicherungsdaten".getBytes(StandardCharsets.UTF_8), result[0]);
    assertArrayEquals("geschuetzte-versichertendaten".getBytes(StandardCharsets.UTF_8), result[1]);
  }

  @Test
  void uncompressAvdAndGvdReturnsEmptySecondDocumentWhenOffsetIsZero() throws IOException {
    byte[] avd = gzip("allgemeine-versicherungsdaten");
    byte[] efVd = withMissingSecondDocument(avd);

    byte[][] result = CardFileToolkitUtils.uncompressAvdAndGvd(efVd);

    assertArrayEquals("allgemeine-versicherungsdaten".getBytes(StandardCharsets.UTF_8), result[0]);
    assertArrayEquals(new byte[0], result[1]);
  }

  @Test
  void uncompressAvdAndGvdRejectsTooShortInput() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> CardFileToolkitUtils.uncompressAvdAndGvd(new byte[7]));

    assertEquals("efVd is invalid", exception.getMessage());
  }

  @Test
  void uncompressGvdReturnsPayload() throws IOException {
    byte[] compressed = gzip("gvd-data");
    byte[] efGvd = withLengthPrefix(compressed);

    byte[] result = CardFileToolkitUtils.uncompressGvd(efGvd);

    assertArrayEquals("gvd-data".getBytes(StandardCharsets.UTF_8), result);
  }

  @Test
  void uncompressEfPdDelegatesToGvdLogic() throws IOException {
    byte[] compressed = gzip("pd-data");
    byte[] efPd = withLengthPrefix(compressed);

    byte[] result = CardFileToolkitUtils.uncompressEfPd(efPd);

    assertArrayEquals("pd-data".getBytes(StandardCharsets.UTF_8), result);
  }

  private static byte[] withTwoDocumentOffsets(byte[] firstDocument, byte[] secondDocument) {
    int firstStart = 8;
    int firstEnd = firstStart + firstDocument.length;
    int secondStart = firstEnd;
    int secondEnd = secondStart + secondDocument.length;
    byte[] result = new byte[secondEnd];
    writeOffset(result, 0, firstStart);
    writeOffset(result, 2, firstEnd);
    writeOffset(result, 4, secondStart);
    writeOffset(result, 6, secondEnd);
    System.arraycopy(firstDocument, 0, result, firstStart, firstDocument.length);
    System.arraycopy(secondDocument, 0, result, secondStart, secondDocument.length);
    return result;
  }

  private static byte[] withMissingSecondDocument(byte[] firstDocument) {
    int firstStart = 8;
    int firstEnd = firstStart + firstDocument.length;
    byte[] result = new byte[firstEnd];
    writeOffset(result, 0, firstStart);
    writeOffset(result, 2, firstEnd);
    writeOffset(result, 4, 0);
    writeOffset(result, 6, 0);
    System.arraycopy(firstDocument, 0, result, firstStart, firstDocument.length);
    return result;
  }

  private static byte[] withLengthPrefix(byte[] compressed) {
    byte[] result = new byte[compressed.length + 2];
    writeOffset(result, 0, compressed.length);
    System.arraycopy(compressed, 0, result, 2, compressed.length);
    return result;
  }

  private static void writeOffset(byte[] target, int index, int value) {
    target[index] = (byte) ((value >> 8) & 0xFF);
    target[index + 1] = (byte) (value & 0xFF);
  }

  private static byte[] gzip(String value) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output)) {
      gzipOutputStream.write(value.getBytes(StandardCharsets.UTF_8));
    }
    return output.toByteArray();
  }
}
