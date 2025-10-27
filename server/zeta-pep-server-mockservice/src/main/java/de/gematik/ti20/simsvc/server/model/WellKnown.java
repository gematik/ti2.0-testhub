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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WellKnown {

  private String issuer;
  private String authorization_endpoint;
  private String token_endpoint;
  private String jwks_uri;
  private String nonce_endpoint;
  private String openid_providers_endpoint;
  private List<String> scopes_supported;
  private List<String> response_types_supported;
  private List<String> response_modes_supported;
  private List<String> grant_types_supported;
  private List<String> token_endpoint_auth_methods_supported;
  private List<String> token_endpoint_auth_signing_alg_values_supported;
  private String service_documentation;
  private List<String> ui_locales_supported;
  private List<String> code_challenge_methods_supported;
}
