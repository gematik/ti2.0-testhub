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
package de.gematik.ti20.vsdm.test.e2e.enums;

public enum ProofMethod {
  HEALTHID("healthid"),
  EHC_PRACTITIONER_TRUSTEDCHANNEL("ehc-practitioner-trustedchannel"),
  EHC_PRACTITIONER_CVC_AUTHENTICATED("ehc-practitioner-cvc-authenticated"),
  EHC_PRACTITIONER_USER_X509("ehc-practitioner-user-x509"),
  EHC_PRACTITIONER_OWNER_X509("ehc-practitioner-owner-x509"),
  EHC_PROVIDER_TRUSTEDCHANNEL("ehc-provider-trustedchannel"),
  EHC_PROVIDER_CVC_AUTHENTICATED("ehc-provider-cvc-authenticated"),
  EHC_PROVIDER_USER_X509("ehc-provider-user-x509"),
  EHC_PROVIDER_OWNER_X509("ehc-provider-owner-x509");

  private final String value;

  ProofMethod(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ProofMethod fromValue(String value) {
    for (ProofMethod method : values()) {
      if (method.value.equals(value)) {
        return method;
      }
    }
    throw new IllegalArgumentException("Unknown proof method: " + value);
  }
}
