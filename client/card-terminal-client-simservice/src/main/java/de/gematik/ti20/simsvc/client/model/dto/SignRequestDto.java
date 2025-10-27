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
package de.gematik.ti20.simsvc.client.model.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object (DTO) for sign requests. Contains the data to be signed and signing options.
 */
public class SignRequestDto {

  private String data;
  private Map<String, String> options;

  /** Default constructor. */
  public SignRequestDto() {
    this.options = new HashMap<>();
  }

  /**
   * Constructor with data.
   *
   * @param data Data to be signed (Base64 encoded)
   */
  public SignRequestDto(String data) {
    this.data = data;
    this.options = new HashMap<>();
  }

  /**
   * Constructor with data and options.
   *
   * @param data Data to be signed (Base64 encoded)
   * @param options Options for the signing operation
   */
  public SignRequestDto(String data, Map<String, String> options) {
    this.data = data;
    this.options = options != null ? new HashMap<>(options) : new HashMap<>();
  }

  /**
   * Get the data to be signed.
   *
   * @return Data as a Base64 encoded string
   */
  public String getData() {
    return data;
  }

  /**
   * Set the data to be signed.
   *
   * @param data Data as a Base64 encoded string
   */
  public void setData(String data) {
    this.data = data;
  }

  /**
   * Get the signing options.
   *
   * @return Map of option names to values
   */
  public Map<String, String> getOptions() {
    return new HashMap<>(options);
  }

  /**
   * Set the signing options.
   *
   * @param options Map of option names to values
   */
  public void setOptions(Map<String, String> options) {
    this.options = options != null ? new HashMap<>(options) : new HashMap<>();
  }

  /**
   * Add an option.
   *
   * @param key Option name
   * @param value Option value
   */
  public void addOption(String key, String value) {
    this.options.put(key, value);
  }

  /**
   * Get an option value.
   *
   * @param key Option name
   * @return Option value or null if not present
   */
  public String getOption(String key) {
    return this.options.get(key);
  }

  /**
   * Get an option value with a default.
   *
   * @param key Option name
   * @param defaultValue Default value if option is not present
   * @return Option value or default value if not present
   */
  public String getOption(String key, String defaultValue) {
    return this.options.getOrDefault(key, defaultValue);
  }
}
