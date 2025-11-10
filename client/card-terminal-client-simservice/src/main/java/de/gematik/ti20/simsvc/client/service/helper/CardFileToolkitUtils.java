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

import java.io.IOException;
import java.util.Arrays;

public class CardFileToolkitUtils {
  static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  public static byte[][] uncompressAvdAndGvd(final byte[] efVd) throws IOException {
    int startOffsetOfFirstDocument = ByteUtils.getIntValue(Arrays.copyOfRange(efVd, 0, 2));
    int endOffsetOfFirstDocument = ByteUtils.getIntValue(Arrays.copyOfRange(efVd, 2, 4));
    int startOffsetOfSecondDocument = -1;
    int endOffsetOfSecondDocument = -1;
    if (efVd.length > 7) {
      startOffsetOfSecondDocument = ByteUtils.getIntValue(Arrays.copyOfRange(efVd, 4, 6));
      endOffsetOfSecondDocument = ByteUtils.getIntValue(Arrays.copyOfRange(efVd, 6, 8));
    } else {
      throw new RuntimeException("efVd is invalid");
    }
    byte[] allgemeineVersicherungsdaten =
        XmlContainerFileHelper.uncompressDocumentWithStartAndEndOffset(
            efVd, startOffsetOfFirstDocument, endOffsetOfFirstDocument);
    byte[] geschuetzteVersichertendaten = EMPTY_BYTE_ARRAY;
    if (startOffsetOfSecondDocument > 0) {
      geschuetzteVersichertendaten =
          XmlContainerFileHelper.uncompressDocumentWithStartAndEndOffset(
              efVd, startOffsetOfSecondDocument, endOffsetOfSecondDocument);
    }
    return new byte[][] {allgemeineVersicherungsdaten, geschuetzteVersichertendaten};
  }

  public static byte[] uncompressEfPd(final byte[] efPd) throws IOException {
    return uncompressGvd(efPd);
  }

  public static byte[] uncompressGvd(final byte[] efGvd) throws IOException {
    int lengthOfData = ByteUtils.getIntValue(Arrays.copyOfRange(efGvd, 0, 2));
    return XmlContainerFileHelper.uncompressDocumentWithStartAndEndOffset(
        efGvd, 2, lengthOfData + 2);
  }
}
