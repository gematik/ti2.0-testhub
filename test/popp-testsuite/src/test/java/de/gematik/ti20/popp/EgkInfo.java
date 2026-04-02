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
package de.gematik.ti20.popp;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.*;

@RequiredArgsConstructor
public class EgkInfo extends ASN1Object {

  private final ASN1Integer status;
  private final DERBitString hashAut;
  private final DEROctetString hashCvc;
  private final ASN1UTCTime notAfter;

  public EgkInfo(
      final String status,
      final byte[] autHashAsHexString,
      final byte[] cvcHashAsHexString,
      final String notAfter) {

    this.status =
        switch (status) {
          case "import" -> new ASN1Integer(0);
          case "remove" -> new ASN1Integer(1);
          default -> throw new IllegalArgumentException("Invalid status: " + status);
        };

    hashAut = new DERBitString(autHashAsHexString);
    hashCvc = new DEROctetString(cvcHashAsHexString);
    this.notAfter = new ASN1UTCTime(notAfter);
  }

  @Override
  public ASN1Primitive toASN1Primitive() {
    final ASN1EncodableVector egkInfo = new ASN1EncodableVector();
    egkInfo.add(status);
    egkInfo.add(hashAut);
    egkInfo.add(hashCvc);
    egkInfo.add(notAfter);
    return new DERSet(egkInfo);
  }
}
