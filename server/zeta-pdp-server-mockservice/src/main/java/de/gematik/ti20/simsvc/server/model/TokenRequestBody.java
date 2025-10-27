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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TokenRequestBody {

  // Required parameters
  private String
      grant_type; // grant_type (e.g., "urn:ietf:params:oauth:grant-type:token-exchange" or
  // "refresh_token")
  private String requested_token_type; // requested_token_type (e.g.,
  // "urn:ietf:params:oauth:token-type:access_token")
  //  private String clientAssertionType; // client_assertion_type (e.g.,
  // "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
  //  private String clientAssertion;     // client_assertion (JWT signed by the client)
  private String subject_token; // subject_token (SMC-B Access Token or Refresh Token)
  private String subject_token_type; // subject_token_type (e.g.,
  // "urn:ietf:params:oauth:token-type:access_token")

  // Optional parameters
  //  private String scope;               // Requested scopes (optional)

}
