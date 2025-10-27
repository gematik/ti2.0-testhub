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
package de.gematik.ti20.client.popp.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class StartMessage extends BasePoppMessage {

  @JsonProperty("version")
  private String version = "1.0.0";

  @JsonProperty("cardConnectionType")
  private String cardConnectionType;

  @JsonProperty("clientSessionId")
  private String clientSessionId;

  public StartMessage() {
    super(BasePoppMessageType.START);
  }

  public StartMessage(String cardConnectionType, String clientSessionId) {
    super(BasePoppMessageType.START);
    this.cardConnectionType = cardConnectionType;
    this.clientSessionId = clientSessionId;
  }

  public StartMessage(String cardConnectionType) {
    super(BasePoppMessageType.START);
    this.cardConnectionType = cardConnectionType;
    this.clientSessionId = UUID.randomUUID().toString();
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getCardConnectionType() {
    return cardConnectionType;
  }

  public void setCardConnectionType(String cardConnectionType) {
    this.cardConnectionType = cardConnectionType;
  }

  public String getClientSessionId() {
    return clientSessionId;
  }

  public void setClientSessionId(String clientSessionId) {
    this.clientSessionId = clientSessionId;
  }
}
