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

/**
 * Data Transfer Object (DTO) for transmit responses. Contains the APDU response received from a
 * card.
 */
public class TransmitResponseDto {

  private String response;
  private String statusWord;
  private String statusMessage;
  private String data;

  /** Default constructor. */
  public TransmitResponseDto() {}

  /**
   * Constructor with response and status word.
   *
   * @param response Complete APDU response as a hex string
   * @param statusWord Status word as a hex string
   */
  public TransmitResponseDto(String response, String statusWord) {
    this.response = response;
    this.statusWord = statusWord;
  }

  /**
   * Constructor with all fields.
   *
   * @param response Complete APDU response as a hex string
   * @param statusWord Status word as a hex string
   * @param statusMessage Human-readable status message
   * @param data Response data as a hex string (without status word)
   */
  public TransmitResponseDto(
      String response, String statusWord, String statusMessage, String data) {
    this.response = response;
    this.statusWord = statusWord;
    this.statusMessage = statusMessage;
    this.data = data;
  }

  /**
   * Get the complete APDU response.
   *
   * @return APDU response as a hex string
   */
  public String getResponse() {
    return response;
  }

  /**
   * Set the complete APDU response.
   *
   * @param response APDU response as a hex string
   */
  public void setResponse(String response) {
    this.response = response;
  }

  /**
   * Get the status word.
   *
   * @return Status word as a hex string
   */
  public String getStatusWord() {
    return statusWord;
  }

  /**
   * Set the status word.
   *
   * @param statusWord Status word as a hex string
   */
  public void setStatusWord(String statusWord) {
    this.statusWord = statusWord;
  }

  /**
   * Get the status message.
   *
   * @return Human-readable status message
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Set the status message.
   *
   * @param statusMessage Human-readable status message
   */
  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  /**
   * Get the response data.
   *
   * @return Response data as a hex string (without status word)
   */
  public String getData() {
    return data;
  }

  /**
   * Set the response data.
   *
   * @param data Response data as a hex string (without status word)
   */
  public void setData(String data) {
    this.data = data;
  }
}
