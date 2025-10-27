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
package de.gematik.ti20.client.card.terminal.simsvc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardCertInfo;
import de.gematik.ti20.client.card.card.CardCertInfoEgk;
import de.gematik.ti20.client.card.card.CardCertInfoSmcb;
import de.gematik.ti20.client.card.card.SignOptions;
import de.gematik.ti20.client.card.card.apdu.ApduUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for communicating with the CardSimulator REST API. This client handles the communication
 * with the external CardSimulator service.
 */
public class SimulatorClient {

  private static final Logger log = LoggerFactory.getLogger(SimulatorClient.class);

  private final String baseUrl;
  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  /**
   * Constructs a new CardSimulatorClient.
   *
   * @param baseUrl the base URL of the CardSimulator REST API
   */
  public SimulatorClient(final String baseUrl) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    this.httpClient = new OkHttpClient();
    this.objectMapper = new ObjectMapper();

    log.debug("CardSimulatorClient initialized with baseUrl: {}", this.baseUrl);
  }

  /**
   * Gets the list of available cards from the CardSimulator.
   *
   * @return a list of card information
   * @throws IOException if communication with the API fails
   */
  public List<AttachedCardInfo> getAvailableCards() throws IOException {
    Request request = new Request.Builder().url(baseUrl + "cards/").get().build();

    log.debug("Getting available cards from CardSimulator at {}", baseUrl);
    log.debug("Sending request to get available cards");

    Call call = httpClient.newCall(request);
    try (Response response = call.execute()) {
      if (!response.isSuccessful()) {
        log.error("Failed to get available cards. Status code: {}", response.code());
        throw new IOException("Failed to get available cards. Status code: " + response.code());
      }

      final String responseBody = response.body().string();
      log.debug("Received response for available cards: {}", responseBody);

      final AttachedCardInfo[] cards =
          objectMapper.readValue(responseBody, AttachedCardInfo[].class);
      return Arrays.asList(cards);
    }
  }

  public EgkInfo getEgkInfo(final SimulatorAttachedCard card) throws IOException {
    Request request =
        new Request.Builder().url(baseUrl + "cards/" + card.getId() + "/egk-info").get().build();

    log.debug(
        "Getting EGK info for card: {} on terminal: {}",
        card.getId(),
        card.getTerminal().getName());
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error(
            "Failed to get EGK info for card {}. Status code: {}", card.getId(), response.code());
        throw new IOException("Failed to get EGK info. Status code: " + response.code());
      }

      String responseBody = response.body().string();
      log.debug("Received EGK info: {}", responseBody);

      return objectMapper.readValue(responseBody, EgkInfo.class);
    }
  }

  /**
   * Opens a connection to the attached card
   *
   * @return a list of card information
   * @throws IOException if communication with the API fails
   */
  public CardConnectionInfo connectToCard(AttachedCard card) throws IOException {
    final Request request =
        new Request.Builder().url(baseUrl + "cards/" + card.getId()).get().build();

    log.debug(
        "Sending connecting request to the card: {} on terminal: {}",
        card.getId(),
        card.getTerminal().getName());
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("Failed to connect the card {}. Status code: {}", card.getId(), response.code());
        throw new IOException("Failed to connect to card. Status code: " + response.code());
      }

      String responseBody = response.body().string();
      log.debug("Connection established with the response: {}", responseBody);

      return objectMapper.readValue(responseBody, CardConnectionInfo.class);
    }
  }

  /**
   * Closes the connection to the attached card
   *
   * @throws IOException if communication with the API fails
   */
  public void disconnectFromCard(final AttachedCard card) throws IOException {
    final Request request =
        new Request.Builder().url(baseUrl + "cards/" + card.getId()).delete().build();

    log.debug(
        "Sending disconnecting request to the card: {} on terminal: {}",
        card.getId(),
        card.getTerminal().getName());
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error(
            "Failed to disconnect the card {}. Status code: {}", card.getId(), response.code());
        throw new IOException("Failed to disconnect the card. Status code: " + response.code());
      }

      final String responseBody = response.body().string();
      log.debug("Connection closed with the response: {}", responseBody);
    }
  }

  /**
   * Transmits an APDU command to a specific card.
   *
   * @param cardId the ID of the card
   * @param apduRequest the APDU request containing the command to transmit
   * @return the response APDU
   * @throws IOException if communication with the API fails
   */
  public ApduResponse transmitApdu(String cardId, ApduRequest apduRequest) throws IOException {

    String requestBody = objectMapper.writeValueAsString(apduRequest);

    Request request =
        new Request.Builder()
            .url(baseUrl + "cards/" + cardId + "/transmit")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build();

    log.debug("Sending APDU to card {}: {}", cardId, apduRequest.getCommand());
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("Failed to transmit APDU. Status code: {}", response.code());
        throw new IOException("Failed to transmit APDU. Status code: " + response.code());
      }

      String responseBody = response.body().string();
      log.debug("Received APDU response: {}", responseBody);

      ApduResponse apduResponse = objectMapper.readValue(responseBody, ApduResponse.class);
      if (apduResponse.isSuccessful()) {
        return apduResponse;
      }
      log.error(
          "Received failure in the APDU Response. Status word: {} Message: {}",
          apduResponse.getStatusWord(),
          apduResponse.getStatusMessage());
      throw new IOException(
          "Received failure in the APDU Response. Status: "
              + apduResponse.getStatusWord()
              + " "
              + apduResponse.getStatusMessage());
    }
  }

  public CardCertInfo getCertInfo(String cardId) throws IOException {

    ApduResponse response = transmitApdu(cardId, new ApduRequest("F0EE000000"));
    if (response.getStatusWord() == null || !response.getStatusWord().equals("9000")) {
      log.error(
          "Failed to get the cert info via transmit of F0EE000000. Status: {}",
          response.getStatusMessage());
      throw new IOException(
          "Failed to get the cert info via transmit of F0EE000000. Status: "
              + response.getStatusMessage());
    }

    String dataAsString = new String(response.getData(), StandardCharsets.UTF_8);
    Map<String, String> data = parseToMap(dataAsString);

    if (data.isEmpty()) {
      log.error("No certificate information found in the response.");
      throw new IOException("No certificate data found in the response.");
    }

    switch (data.get("cardType")) {
      case "HPIC":
      case "SMC-B":
        return new CardCertInfoSmcb(
            data.get("TELEMATIK_ID"),
            data.get("PROFESSION_OID"),
            data.get("HOLDER"),
            data.get("ORG"));
      case "EGK":
        return new CardCertInfoEgk(
            data.get("KVNR"),
            data.get("IKNR"),
            data.get("NAME"),
            data.get("FIRST_NAME"),
            data.get("LAST_NAME"));
    }

    return null;

    //    // Select Master File
    //    ApduResponse response1 = transmitApdu(cardId, new ApduRequest("00 A4 04 0C 07
    // D2760001448000"));
    //    if (response1.getStatusWord() == null || !response1.getStatusWord().equals("9000")) {
    //      log.error("Failed to select Master File. Status: {}", response1.getStatusMessage());
    //      throw new IOException("Failed to select Master File. Status: " +
    // response1.getStatusMessage());
    //    }
    //
    //    ApduResponse response2 = transmitApdu(cardId, new ApduRequest("00 B0 84 00 00"));
    //    if (response2.getStatusWord() == null || !response2.getStatusWord().equals("9000")) {
    //      log.error("Failed to read certificate. Status: {}", response2.getStatusMessage());
    //      throw new IOException("Failed to read certificate. Status: " +
    // response2.getStatusMessage());
    //    }
    //    return bytesToHex(response2.getData());
  }

  /**
   * Signs data with a specific card.
   *
   * @param cardId the ID of the card
   * @param data the data to sign as a byte array
   * @return the signature
   * @throws IOException if communication with the API fails
   */
  public byte[] signData(String cardId, byte[] data, SignOptions options) throws IOException {
    SignRequest signRequest = new SignRequest(data, options);
    String requestBody = objectMapper.writeValueAsString(signRequest);

    Request request =
        new Request.Builder()
            .url(baseUrl + "cards/" + cardId + "/sign")
            .post(RequestBody.create(requestBody, MediaType.get("application/json")))
            .build();

    log.debug("Sending sign request to card {}", cardId);
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        log.error("Failed to sign data. Status code: {}", response.code());
        throw new IOException("Failed to sign data. Status code: " + response.code());
      }

      String responseBody = response.body().string();
      log.debug("Received signature response");

      SignResponse signResponse = objectMapper.readValue(responseBody, SignResponse.class);
      return signResponse.getSignatureBytes();
    }
  }

  public static Map<String, String> parseToMap(String input) {
    Map<String, String> resultMap = new HashMap<>();

    // split the input string by the pipe "|" character
    String[] pairs = input.split("\\|");

    for (String pair : pairs) {
      // split each pair by the first colon ":" character
      String[] keyValue = pair.split(":", 2);
      if (keyValue.length == 2) {
        String key = keyValue[0].trim();
        String value = keyValue[1].trim();
        resultMap.put(key, value);
      }
    }

    return resultMap;
  }

  /** Request class for APDU transmission. */
  public static class ApduRequest {

    private final String command;

    public ApduRequest(String command) {
      this.command = command;
    }

    public ApduRequest(byte[] command) {
      this.command = ApduUtil.bytesToHex(command);
    }

    public String getCommand() {
      return command;
    }
  }

  /** Response class for APDU transmission. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ApduResponse {

    private String statusWord;
    private String statusMessage;
    private String data;

    public String getStatusWord() {
      return statusWord;
    }

    public String getStatusMessage() {
      return statusMessage;
    }

    public byte[] getData() {
      return ApduUtil.hexToBytes(data);
    }

    public byte[] getResponse() {
      return ApduUtil.hexToBytes(data + statusWord);
    }

    public boolean isSuccessful() {
      return statusWord.equals("9000");
    }
  }

  /** Request class for signing data. */
  private static class SignRequest {

    private final byte[] data;
    private SignRequestOptions options;

    public SignRequest(String content, SignOptions options) {
      if (content == null) {
        throw new IllegalArgumentException("Content is null");
      }
      this.data = content.getBytes(StandardCharsets.UTF_8);

      this.options = new SignRequestOptions(options);
    }

    public SignRequest(byte[] data, SignOptions options) {
      if (data == null) {
        throw new IllegalArgumentException("data is null");
      }
      this.data = data;

      this.options = new SignRequestOptions(options);
    }

    @JsonProperty("data")
    public String getData() {
      return Base64.getEncoder().encodeToString(data);
    }

    @JsonProperty("options")
    public SignRequestOptions getOptions() {
      return options;
    }
  }

  private static class SignRequestOptions {

    private String algorithm;
    private String keyType = "AUT"; // or QES, or ENC
    private String keyReference = null; // Optional, can be null

    public SignRequestOptions(SignOptions options) {
      if (options == null) {
        throw new IllegalArgumentException("SignOptions cannot be null");
      }
      this.algorithm =
          options.getHashAlgorithm().getAlgorithmName()
              + "with"
              + options.getSignatureType().getTypeName();
      this.keyReference = options.getKeyReference();
      if (keyReference != null && !keyReference.isBlank()) {
        keyType = null;
      }
    }

    public String getAlgorithm() {
      return algorithm;
    }

    public String getKeyType() {
      return keyType;
    }

    public String getKeyReference() {
      return keyReference;
    }
  }

  /** Response class for signing data. */
  private static class SignResponse {

    private String signature;
    private String algorithm;
    private String certificate;

    public String getSignature() {
      return signature;
    }

    public byte[] getSignatureBytes() {
      return Base64.getDecoder().decode(signature);
    }

    public void setSignature(String signature) {
      this.signature = signature;
    }

    public String getAlgorithm() {
      return algorithm;
    }

    public void setAlgorithm(String algorithm) {
      this.algorithm = algorithm;
    }

    public String getCertificate() {
      return certificate;
    }

    public void setCertificate(String certificate) {
      this.certificate = certificate;
    }
  }
}
