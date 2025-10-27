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

public class VsdDataParser {

  public static VdResult parseVd(String vd) throws IOException {
    byte[] data = ByteUtils.getByteArray(vd);
    byte[][] bytes = CardFileToolkitUtils.uncompressAvdAndGvd(data);
    return new VdResult(new String(bytes[0], "ISO-8859-15"), new String(bytes[1], "ISO-8859-15"));
  }

  public static String parsePd(String pd) throws IOException {
    byte[] data = ByteUtils.getByteArray(pd);
    byte[] pdbytes = CardFileToolkitUtils.uncompressEfPd(data);
    return new String(pdbytes, "ISO-8859-15");
  }

  public static String parseGvd(String gvd) throws IOException {
    byte[] data = ByteUtils.unzipByteArray(ByteUtils.getByteArray(gvd.substring(4)));
    return new String(data, "ISO-8859-15");
  }
}
