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
package de.gematik.ti20.simsvc.server.model.message;

public enum BasePoppMessageType {
  ERROR("Error", ErrorMessage.class),
  START("StartMessage", StartMessage.class),
  TOKEN("Token", TokenMessage.class),
  STANDARD_SCENARIO("StandardScenario", StandardScenarioMessage.class),
  SCENARIO_RESPONSE("ScenarioResponse", ScenarioResponseMessage.class);

  private final String value;
  private final Class<? extends BasePoppMessage> clazz;

  BasePoppMessageType(String value, Class<? extends BasePoppMessage> clazz) {
    this.value = value;
    this.clazz = clazz;
  }

  public String getValue() {
    return value;
  }

  public Class<? extends BasePoppMessage> getClazz() {
    return clazz;
  }

  public static Class<? extends BasePoppMessage> getClassForType(String type) {
    for (BasePoppMessageType messageType : values()) {
      if (messageType.getValue().equals(type)) {
        return messageType.getClazz();
      }
    }
    throw new IllegalArgumentException("Unknown BasePoppMessageType: " + type);
  }
}
