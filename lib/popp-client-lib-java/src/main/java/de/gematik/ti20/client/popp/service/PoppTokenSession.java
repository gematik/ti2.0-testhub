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
package de.gematik.ti20.client.popp.service;

import de.gematik.ti20.client.card.card.AttachedCard;
import de.gematik.ti20.client.card.card.CardConnection;
import de.gematik.ti20.client.card.card.apdu.ApduUtil;
import de.gematik.ti20.client.card.terminal.CardTerminalException;
import de.gematik.ti20.client.card.terminal.connector.ConnectorAttachedCard;
import de.gematik.ti20.client.card.terminal.pcsc.PcScAttachedCard;
import de.gematik.ti20.client.card.terminal.simsvc.SimulatorAttachedCard;
import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.message.BasePoppMessage;
import de.gematik.ti20.client.popp.message.ErrorMessage;
import de.gematik.ti20.client.popp.message.PoppMessageSerializer;
import de.gematik.ti20.client.popp.message.ScenarioResponseMessage;
import de.gematik.ti20.client.popp.message.ScenarioStep;
import de.gematik.ti20.client.popp.message.StandardScenarioMessage;
import de.gematik.ti20.client.popp.message.StartMessage;
import de.gematik.ti20.client.popp.message.TokenMessage;
import de.gematik.ti20.client.zeta.exception.ZetaHttpException;
import de.gematik.ti20.client.zeta.request.ZetaWsRequest;
import de.gematik.ti20.client.zeta.websocket.ZetaWsEventHandler;
import de.gematik.ti20.client.zeta.websocket.ZetaWsSession;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoppTokenSession {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final AttachedCard attachedCard;
  private CardConnection cardConnection;

  private final PoppClientService poppClientService;
  private final PoppTokenSessionEventHandler eventHandler;

  protected boolean serverConnected = false;

  private ZetaWsEventHandler zetaWsEventHandler;

  public PoppTokenSession(
      final AttachedCard attachedCard,
      final PoppTokenSessionEventHandler eventHandler,
      final PoppClientService poppClientService) {
    this.attachedCard = attachedCard;
    this.poppClientService = poppClientService;
    this.eventHandler = eventHandler;
  }

  public AttachedCard getAttachedCard() {
    return attachedCard;
  }

  public boolean isCardConnected() {
    return cardConnection != null && cardConnection.isConnected();
  }

  public boolean isServerConnected() {
    return serverConnected;
  }

  public boolean isPaired() {
    return isCardConnected() && isServerConnected();
  }

  public void start(final Integer smcbSlotId) throws PoppClientException {
    connectToCard();
    pairCardAndServer(smcbSlotId);
  }

  private void connectToCard() throws PoppClientException {
    log.debug(
        "Connecting to card {} in terminal {}",
        attachedCard.getId(),
        attachedCard.getTerminal().getName());
    try {
      cardConnection = attachedCard.getTerminal().connect(attachedCard);
      log.debug(
          "Connected to card {} in terminal {}",
          attachedCard.getId(),
          attachedCard.getTerminal().getName());
    } catch (de.gematik.ti20.client.card.terminal.CardTerminalException e) {
      log.error(
          "Could not connect to the card {} in terminal {}",
          attachedCard.getId(),
          attachedCard.getTerminal().getName(),
          e);
      throw new PoppClientException("Could not connect to the card", e);
    }
  }

  private void pairCardAndServer(final Integer smcbSlotId) throws PoppClientException {
    final String urlWs =
        poppClientService.getPoppClientConfig().getUrlPoppServerWs(attachedCard)
            + "/ws/popp/practitioner/api/v1/token-generation-ehc";
    final String urlHttp =
        poppClientService.getPoppClientConfig().getUrlPoppServerHttp(attachedCard);

    log.debug("Connecting to PoPP server at WS URL: {}", urlWs);
    log.debug("Connecting to PoPP server at HTTP URL: {}", urlHttp);

    final ZetaWsRequest zetaWsRequest = new ZetaWsRequest(urlWs, urlHttp);
    zetaWsRequest.setHeader("smcbSlotId", String.valueOf(smcbSlotId));

    zetaWsEventHandler =
        new ZetaWsEventHandler() {
          public void onConnected(final ZetaWsSession ws) {
            log.debug("Connected to PoPP server {}", urlWs);
            log.debug("Attached card {} paired to PoPP server {}", attachedCard.getId(), urlWs);
            serverConnected = true;
            eventHandler.onConnectedToServer(PoppTokenSession.this);
            eventHandler.onCardPairedToServer(PoppTokenSession.this);

            sendStartMessage(ws);
          }

          public void onDisconnected(final ZetaWsSession ws) {
            log.debug(
                "Disconnected from PoPP server {}",
                poppClientService.getPoppClientConfig().getUrlPoppServerWs(attachedCard));
            serverConnected = false;
            finish();
            eventHandler.onDisconnectedFromServer(PoppTokenSession.this);
          }

          @Override
          public void onMessage(final ZetaWsSession ws) {
            log.debug("Message: {}", ws.getLastMessage());

            BasePoppMessage msg = null;
            try {
              msg = PoppMessageSerializer.fromJson(ws.getLastMessage());
            } catch (Exception e) {
              log.error("Error parsing message from PoPP server: {}", ws.getLastMessage(), e);
              eventHandler.onError(
                  PoppTokenSession.this,
                  new PoppClientException("Error parsing message from PoPP server", e));
              return;
            }

            processIncomingPoppMessage(ws, msg);
          }

          @Override
          public void onException(final ZetaWsSession ws, final ZetaHttpException e) {
            log.error("Error: ", e);
            eventHandler.onError(PoppTokenSession.this, new PoppClientException(e));
          }
        };

    poppClientService
        .getZetaClientService()
        .connectToPepProxy(zetaWsRequest, zetaWsEventHandler, true);
  }

  public void finish() {
    if (cardConnection != null) {
      cardConnection.disconnect();
    }
    if (zetaWsEventHandler != null) {
      final ZetaWsSession wsSession = zetaWsEventHandler.getWsSession();
      if (wsSession != null && wsSession.isOpen()) {
        wsSession.close(1000, "Finished");
      }
    }
    eventHandler.onFinished(this);
  }

  private void sendStartMessage(final ZetaWsSession ws) {
    String cardConnectionType = "";
    if (attachedCard instanceof PcScAttachedCard) {
      cardConnectionType = "contact-standard";
    } else if (attachedCard instanceof ConnectorAttachedCard) {
      cardConnectionType = "contact-connector";
    } else if (attachedCard instanceof SimulatorAttachedCard) {
      cardConnectionType = "contact-simsvc";
    }

    sendCommand(ws, new StartMessage(cardConnectionType));
  }

  private void sendCommand(final ZetaWsSession ws, final BasePoppMessage poppMessage) {
    try {
      final String json = PoppMessageSerializer.toJson(poppMessage);
      log.debug("Sending message to PoPP server: {}", json);
      ws.send(json);
    } catch (Exception e) {
      log.error("Error sending message to PoPP server: {}", poppMessage, e);
      eventHandler.onError(
          PoppTokenSession.this,
          new PoppClientException("Error sending message to PoPP server", e));
    }
  }

  private void processIncomingPoppMessage(
      final ZetaWsSession ws, final BasePoppMessage poppMessage) {
    if (poppMessage instanceof StandardScenarioMessage) {
      processScenarioMessage(ws, (StandardScenarioMessage) poppMessage);
    } else if (poppMessage instanceof TokenMessage) {
      processTokenMessage(ws, (TokenMessage) poppMessage);
    } else if (poppMessage instanceof ErrorMessage) {
      processErrorMessage(ws, (ErrorMessage) poppMessage);
    } else {
      log.error(
          "Unsupported message type of the incoming message: {}", poppMessage.getClass().getName());
      final ErrorMessage errorMessage =
          new ErrorMessage("1235", "Unsupported message type: " + poppMessage.getClass().getName());
      sendCommand(ws, errorMessage);
    }
  }

  private void processScenarioMessage(
      final ZetaWsSession ws, final StandardScenarioMessage scenarioMessage) {
    log.debug("Processing scenario message: {}", scenarioMessage);

    final List<String> responses = new ArrayList<>();

    try {
      for (final ScenarioStep step : scenarioMessage.getSteps()) {
        final String apduCommand = step.getCommandApdu();

        try {
          final byte[] apduResponse = cardConnection.transmit(ApduUtil.hexToBytes(apduCommand));
          responses.add(ApduUtil.bytesToHex(apduResponse));
        } catch (CardTerminalException e) {
          log.error("Error sending APDU command to the card {}", apduCommand, e);
          throw new CardTerminalException("Error sending APDU command to the card", e);
        }
      }
    } catch (final Exception e) {
      log.error("Error processing scenario message: {}", scenarioMessage, e);
      ErrorMessage errorMessage =
          new ErrorMessage("1234", "Error sending APDU command: " + e.getMessage());
      sendCommand(ws, errorMessage);
      return;
    }

    sendCommand(ws, new ScenarioResponseMessage(responses));
  }

  private void processTokenMessage(final ZetaWsSession ws, final TokenMessage tokenMessage) {
    log.debug("Processing token message: {}", tokenMessage.getToken());
    eventHandler.onReceivedPoppToken(PoppTokenSession.this, tokenMessage);
  }

  private void processErrorMessage(final ZetaWsSession ws, final ErrorMessage errorMessage) {
    log.error("Processing error message: {}", errorMessage);
    eventHandler.onError(PoppTokenSession.this, new PoppClientException(errorMessage.toString()));
  }
}
