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
package de.gematik.ti20.client.card.carddata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CardDataSmcb extends CardData {

  private String institutionName;
  private String bsnr;
  private SecurityData securityData;

  @JsonCreator
  public CardDataSmcb(
      @JsonProperty("number") String cardNumber,
      @JsonProperty("expires") String cardExpires,
      @JsonProperty("institutionName") String institutionName,
      @JsonProperty("bsnr") String bsnr) {
    super(Type.SMCB, cardNumber, cardExpires);
    this.institutionName = institutionName;
    this.bsnr = bsnr;
    this.securityData = new SecurityData();
  }

  public static class SecurityData {

    private String privateKey;
    private String authCertificate;

    public SecurityData() {}

    @JsonCreator
    public SecurityData(
        @JsonProperty("privateKey") String privateKey,
        @JsonProperty("authCertificate") String authCertificate) {
      this.privateKey = privateKey;
      this.authCertificate = authCertificate;
    }

    public String getPrivateKey() {
      return privateKey;
    }

    public void setPrivateKey(String privateKey) {
      this.privateKey = privateKey;
    }

    public String getAuthCertificate() {
      return authCertificate;
    }

    public void setAuthCertificate(String authCertificate) {
      this.authCertificate = authCertificate;
    }
  }

  public String getInstitutionName() {
    return institutionName;
  }

  public String getBsnr() {
    return bsnr;
  }

  public SecurityData getSecurityData() {
    return securityData;
  }
}
