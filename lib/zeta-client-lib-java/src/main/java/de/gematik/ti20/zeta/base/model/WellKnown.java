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
package de.gematik.ti20.zeta.base.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

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

  public WellKnown() {}

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getAuthorization_endpoint() {
    return authorization_endpoint;
  }

  public void setAuthorization_endpoint(String authorization_endpoint) {
    this.authorization_endpoint = authorization_endpoint;
  }

  public String getToken_endpoint() {
    return token_endpoint;
  }

  public void setToken_endpoint(String token_endpoint) {
    this.token_endpoint = token_endpoint;
  }

  public String getJwks_uri() {
    return jwks_uri;
  }

  public void setJwks_uri(String jwks_uri) {
    this.jwks_uri = jwks_uri;
  }

  public String getNonce_endpoint() {
    return nonce_endpoint;
  }

  public void setNonce_endpoint(String nonce_endpoint) {
    this.nonce_endpoint = nonce_endpoint;
  }

  public String getOpenid_providers_endpoint() {
    return openid_providers_endpoint;
  }

  public void setOpenid_providers_endpoint(String openid_providers_endpoint) {
    this.openid_providers_endpoint = openid_providers_endpoint;
  }

  public List<String> getScopes_supported() {
    return scopes_supported;
  }

  public void setScopes_supported(List<String> scopes_supported) {
    this.scopes_supported = scopes_supported;
  }

  public List<String> getResponse_types_supported() {
    return response_types_supported;
  }

  public void setResponse_types_supported(List<String> response_types_supported) {
    this.response_types_supported = response_types_supported;
  }

  public List<String> getResponse_modes_supported() {
    return response_modes_supported;
  }

  public void setResponse_modes_supported(List<String> response_modes_supported) {
    this.response_modes_supported = response_modes_supported;
  }

  public List<String> getGrant_types_supported() {
    return grant_types_supported;
  }

  public void setGrant_types_supported(List<String> grant_types_supported) {
    this.grant_types_supported = grant_types_supported;
  }

  public List<String> getToken_endpoint_auth_methods_supported() {
    return token_endpoint_auth_methods_supported;
  }

  public void setToken_endpoint_auth_methods_supported(
      List<String> token_endpoint_auth_methods_supported) {
    this.token_endpoint_auth_methods_supported = token_endpoint_auth_methods_supported;
  }

  public List<String> getToken_endpoint_auth_signing_alg_values_supported() {
    return token_endpoint_auth_signing_alg_values_supported;
  }

  public void setToken_endpoint_auth_signing_alg_values_supported(
      List<String> token_endpoint_auth_signing_alg_values_supported) {
    this.token_endpoint_auth_signing_alg_values_supported =
        token_endpoint_auth_signing_alg_values_supported;
  }

  public String getService_documentation() {
    return service_documentation;
  }

  public void setService_documentation(String service_documentation) {
    this.service_documentation = service_documentation;
  }

  public List<String> getUi_locales_supported() {
    return ui_locales_supported;
  }

  public void setUi_locales_supported(List<String> ui_locales_supported) {
    this.ui_locales_supported = ui_locales_supported;
  }

  public List<String> getCode_challenge_methods_supported() {
    return code_challenge_methods_supported;
  }

  public void setCode_challenge_methods_supported(List<String> code_challenge_methods_supported) {
    this.code_challenge_methods_supported = code_challenge_methods_supported;
  }
}
