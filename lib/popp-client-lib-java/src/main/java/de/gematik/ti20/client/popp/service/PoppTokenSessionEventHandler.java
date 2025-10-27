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

import de.gematik.ti20.client.popp.exception.PoppClientException;
import de.gematik.ti20.client.popp.message.TokenMessage;

public interface PoppTokenSessionEventHandler {

  void onConnectedToTerminalSlot(PoppTokenSession pts);

  void onDisconnectedFromTerminalSlot(PoppTokenSession pts);

  void onCardInserted(PoppTokenSession pts);

  void onCardRemoved(PoppTokenSession pts);

  void onCardPairedToServer(PoppTokenSession pts);

  void onConnectedToServer(PoppTokenSession pts);

  void onDisconnectedFromServer(PoppTokenSession pts);

  void onError(PoppTokenSession pts, PoppClientException exception);

  void onReceivedPoppToken(PoppTokenSession pts, TokenMessage tokenMessage);

  void onFinished(PoppTokenSession pts);
}
