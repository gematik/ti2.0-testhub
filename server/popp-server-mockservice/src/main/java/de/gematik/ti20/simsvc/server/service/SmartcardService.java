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
package de.gematik.ti20.simsvc.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.ti20.simsvc.server.model.TokenParams;
import de.gematik.ti20.simsvc.server.model.message.BasePoppMessage;
import de.gematik.ti20.simsvc.server.model.message.BasePoppMessageType;
import de.gematik.ti20.simsvc.server.model.message.ErrorMessage;
import de.gematik.ti20.simsvc.server.model.message.ScenarioResponseMessage;
import de.gematik.ti20.simsvc.server.model.message.ScenarioStep;
import de.gematik.ti20.simsvc.server.model.message.StandardScenarioMessage;
import de.gematik.ti20.simsvc.server.model.message.StartMessage;
import de.gematik.ti20.simsvc.server.model.message.TokenMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Service
public class SmartcardService {

  private final PoppTokenService tokenService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public SmartcardService(final PoppTokenService tokenService) {
    this.tokenService = tokenService;
  }

  public void onConnectionEstablished(final WebSocketSession session) {}

  public void onMessage(final WebSocketSession session, final TextMessage message) {
    final String text = message.getPayload();

    BasePoppMessage poppMessage = null;
    try {
      poppMessage = convertFromJson(text);
      processIncomingPoppMessage(session, poppMessage);
    } catch (final Exception e) {
      final ErrorMessage errorMessage =
          new ErrorMessage(
              "1234", "Failed to parse message: " + text + " Error: " + e.getMessage());
      sendCommand(session, errorMessage);

      disconnect(session, CloseStatus.BAD_DATA);
    }
  }

  public void onConnectionClosed(final WebSocketSession session, final CloseStatus status) {}

  private void sendCommand(final WebSocketSession session, final BasePoppMessage message) {
    String json = null;
    try {
      json = objectMapper.writeValueAsString(message);
      session.sendMessage(new TextMessage(json));
    } catch (final Exception e) {
      log.error("Failed to send message {} {}", json, e.getMessage());
    }
  }

  private void processIncomingPoppMessage(
      final WebSocketSession session, final BasePoppMessage poppMessage) {
    log.debug("Processing incoming PoPP message: {}", poppMessage);

    if (poppMessage instanceof StartMessage) {
      processStartMessage(session, (StartMessage) poppMessage);
    } else if (poppMessage instanceof ScenarioResponseMessage) {
      processScenarioResponseMessage(session, (ScenarioResponseMessage) poppMessage);
    } else if (poppMessage instanceof ErrorMessage) {
      processErrorMessage(session, (ErrorMessage) poppMessage);
    } else {
      final ErrorMessage errorMessage =
          new ErrorMessage("1235", "Unsupported message type: " + poppMessage.getClass().getName());
      sendCommand(session, errorMessage);
    }
  }

  private void processStartMessage(final WebSocketSession session, final StartMessage poppMessage) {
    log.debug(
        "Start message {} {} received from client",
        poppMessage.getClientSessionId(),
        poppMessage.getCardConnectionType());

    final List<ScenarioStep> steps = List.of(new ScenarioStep("F0EE000000", List.of("9000")));

    session.getAttributes().put("clientSessionId", poppMessage.getClientSessionId());
    session.getAttributes().put("cardConnectionType", poppMessage.getCardConnectionType());
    session.getAttributes().put("sequenceCounter", 0);
    session.getAttributes().put("lastSteps", steps);

    final StandardScenarioMessage scenarioMessage =
        new StandardScenarioMessage(
            poppMessage.getClientSessionId(),
            (int) session.getAttributes().get("sequenceCounter"),
            10000,
            steps);

    sendCommand(session, scenarioMessage);
  }

  private void processScenarioResponseMessage(
      final WebSocketSession session, final ScenarioResponseMessage poppMessage) {
    log.debug(
        "Scenario response message received from client for session: {}",
        session.getAttributes().get("clientSessionId"));

    @SuppressWarnings("unchecked")
    final List<ScenarioStep> stepsRequest =
        (List<ScenarioStep>) session.getAttributes().get("lastSteps");

    for (int i = 0; i < stepsRequest.size(); i++) {
      final ScenarioStep stepRequest = stepsRequest.get(i);

      if (stepRequest.getCommandApdu() == "F0EE000000") {
        final String stepResponse = poppMessage.getSteps().get(i);
        if (stepResponse != null && stepResponse.endsWith("9000")) {
          log.debug(
              "Received response for step {} {}: {}",
              i,
              stepRequest.getCommandApdu(),
              stepResponse);
          createPoppTokenAndReturn(session, stepResponse.substring(0, stepResponse.length() - 4));
          return;
        }
        log.error(
            "Received malformed response for step {} {}: {}",
            i,
            stepRequest.getCommandApdu(),
            stepResponse);
      }
    }
  }

  private void processErrorMessage(final WebSocketSession session, final ErrorMessage poppMessage) {
    log.error(
        "Error received from client: {} {}",
        poppMessage.getErrorCode(),
        poppMessage.getErrorDetail());
    disconnect(session, CloseStatus.NOT_ACCEPTABLE);
  }

  private BasePoppMessage convertFromJson(final String json) throws JsonProcessingException {
    final JsonNode jsonNode = objectMapper.readTree(json);
    final String type = jsonNode.get("type").asText();

    final Class<? extends BasePoppMessage> messageClass = BasePoppMessageType.getClassForType(type);
    return objectMapper.treeToValue(jsonNode, messageClass);
  }

  private void disconnect(final WebSocketSession session, final CloseStatus closeStatus) {
    try {
      session.close(closeStatus);
      log.debug("Disconnected from client: {}", session.getId());
    } catch (IOException e) {
      log.warn("Failed to disconnect from client: {} Error: {}", session.getId(), e.getMessage());
    }
  }

  private void createPoppTokenAndReturn(final WebSocketSession session, final String response) {
    final String clientSessionId = (String) session.getAttributes().get("clientSessionId");

    final String responseAsString = new String(hexToBytes(response), StandardCharsets.UTF_8);
    final Map<String, String> data = parseToMap(responseAsString);

    final String userInfo = session.getHandshakeHeaders().getFirst("ZETA-User-Info");
    if (!StringUtils.hasText(userInfo)) {
      log.error("No ZETA-User-Info header has been passed from ZETA PEP");
      final ErrorMessage errorMessage =
          new ErrorMessage("1237", "No ZETA-User-Info header has been passed from ZETA PEP");
      sendCommand(session, errorMessage);
      return;
    }

    final ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> userInfoMap = null;
    try {
      final byte[] decodedBytes = Base64.getDecoder().decode(userInfo);
      final String jsonString = new String(decodedBytes);
      userInfoMap = objectMapper.readValue(jsonString, Map.class);
    } catch (final Exception e) {
      log.error("ZETA-User-Info seems to be a malformed JSON: {}", userInfo);
      final ErrorMessage errorMessage =
          new ErrorMessage("1238", "ZETA-User-Info seems to be a malformed JSON: " + userInfo);
      sendCommand(session, errorMessage);
      return;
    }

    String poppToken = null;
    try {
      final TokenParams tokenParams =
          new TokenParams(
              "ehc-provider-user-x509",
              null,
              null,
              data.get("KVNR"),
              data.get("IKNR"),
              userInfoMap.get("identifier"),
              userInfoMap.get("professionOID"));
      poppToken = tokenService.createToken(tokenParams);

    } catch (final Exception e) {
      log.error("Failed to create token for client session {}", clientSessionId, e);
      ErrorMessage errorMessage =
          new ErrorMessage("1239", "Could not create a PoPP-Token: " + e.getMessage());
      sendCommand(session, errorMessage);
      return;
    }

    sendCommand(session, new TokenMessage(poppToken, "TODO: PN"));

    log.debug("Created PoPP-Token for client session {}: {}", clientSessionId, poppToken);

    try {
      session.close(CloseStatus.NORMAL);
    } catch (final IOException e) {
      log.warn("Failed to close session {}: {}", session.getId(), e.getMessage());
      // nothing to do here, we just close the session
    }
  }

  /**
   * Converts a hexadecimal string to a byte array.
   *
   * @param hexString the hexadecimal string
   * @return the byte array
   */
  private static byte[] hexToBytes(final String hexString) {
    int length = hexString.length();
    byte[] data = new byte[length / 2];
    for (int i = 0; i < length; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(hexString.charAt(i), 16) << 4)
                  + Character.digit(hexString.charAt(i + 1), 16));
    }
    return data;
  }

  private static Map<String, String> parseToMap(final String input) {
    final Map<String, String> resultMap = new HashMap<>();

    // split the input string by the pipe "|" character
    final String[] pairs = input.split("\\|");

    for (final String pair : pairs) {
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
}
