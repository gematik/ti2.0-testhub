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
package de.gematik.ti20.client.zeta.auth;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.ECGenParameterSpec;

public class DpopToken {

  //  static {
  //    Security.addProvider(new BouncyCastleProvider());
  //  }

  public static KeyPair createKeyPair()
      throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
    // TODO: Probleme mit Bouncycastle gehabt, müsste später angegangen werden, wenn es beim
    // Brainpool bleiben soll
    //    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
    //    keyPairGenerator.initialize(new ECGenParameterSpec("brainpoolP256r1"));
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
    keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
    return keyPairGenerator.generateKeyPair();
  }

  public static DpopToken create() {
    return new DpopToken();
  }
}
