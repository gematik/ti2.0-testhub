/*-
 * #%L
 * PoPP Testsuite
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
package de.gematik.ti20.popp;

import static de.gematik.ti20.popp.data.TestConstants.VERSION_E_CONTENT;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.*;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class EContent extends ASN1Object {

  private final ASN1Integer version = new ASN1Integer(VERSION_E_CONTENT);
  private final List<EgkInfo> egkInfos;

  @Override
  public ASN1Primitive toASN1Primitive() {
    final ASN1EncodableVector egkInfosAsAsn1 = new ASN1EncodableVector();
    egkInfos.forEach(egkInfosAsAsn1::add);
    final DERSequence egkInfosSeq = new DERSequence(egkInfosAsAsn1);
    final ASN1EncodableVector eContent = new ASN1EncodableVector();
    eContent.add(version);
    eContent.add(egkInfosSeq);
    return new DERSequence(eContent);
  }
}
