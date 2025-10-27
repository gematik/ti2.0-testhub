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
package de.gematik.ti20.simsvc.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SecurityParams {

  private final String storeContent;
  private final String storePass;
  private final String keyAlias;
  private final String keyPass;

  @JsonCreator
  public SecurityParams(
      @JsonProperty(value = "storeContent", required = true) final String storeContent,
      @JsonProperty(value = "storePass", required = true) final String storePass,
      @JsonProperty(value = "keyAlias", required = true) final String keyAlias,
      @JsonProperty(value = "keyPass", required = true) final String keyPass) {
    this.storeContent = storeContent;
    this.storePass = storePass;
    this.keyAlias = keyAlias;
    this.keyPass = keyPass;
  }

  public String getStoreContent() {
    return storeContent;
  }

  public String getStorePass() {
    return storePass;
  }

  public String getKeyAlias() {
    return keyAlias;
  }

  public String getKeyPass() {
    return keyPass;
  }
}
