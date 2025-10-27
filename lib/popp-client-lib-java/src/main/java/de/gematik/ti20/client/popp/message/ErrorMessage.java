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

public class ErrorMessage extends BasePoppMessage {

  @JsonProperty("errorCode")
  private String errorCode;

  @JsonProperty("errorDetail")
  private String errorDetail;

  public ErrorMessage() {
    super(BasePoppMessageType.ERROR);
  }

  public ErrorMessage(String errorCode, String errorDetail) {
    super(BasePoppMessageType.ERROR);
    this.errorCode = errorCode;
    this.errorDetail = errorDetail;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorDetail() {
    return errorDetail;
  }

  public void setErrorDetail(String errorDetail) {
    this.errorDetail = errorDetail;
  }

  public String toString() {
    return errorCode + " " + errorDetail;
  }
}
