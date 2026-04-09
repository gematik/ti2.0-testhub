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

import static de.gematik.ti20.popp.StepsHashdb.signEntryForHashDb;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.security.Security;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;

class StepsHashdbTest {

  static {
    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    Security.insertProviderAt(new BouncyCastleProvider(), 1);
  }

  @Test
  @SneakyThrows
  void signEntryForHashDbTest() {
    final byte[] dataForHashDb =
        signEntryForHashDb(
            "4D516D65BA306BC9AF747E03BE24D57E905A70D0CF5BBD97783E6CA77AF201EE",
            "19AAF229FDDA443B6B39AA349E621E007A851FDBE92F8235DC4C6A80F68237C8",
            "import");
    assertThat(dataForHashDb).isNotEmpty();
    final ASN1Primitive eContentAsAsn1 = new ASN1InputStream(dataForHashDb).readObject();
    assertThat(eContentAsAsn1).isInstanceOf(ASN1Sequence.class);
    assertThat(((ASN1Sequence) eContentAsAsn1).getObjectAt(0))
        .isInstanceOf(ASN1ObjectIdentifier.class);
    assertThat(((ASN1Sequence) eContentAsAsn1).getObjectAt(0))
        .isEqualTo(new ASN1ObjectIdentifier("1.2.840.113549.1.7.2"));
  }
}
