/*-
 * #%L
 * ZeTA Testsuite
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
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
 * For additional notes and disclaimer from gematik and in case of changes
 * by gematik, find details in the "Readme" file.
 * #L%
 */
package de.gematik.zeta.services;

import java.util.Base64;

/**
 * Factory zum Erzeugen eines gültigen subject_token für den ZETA-PDP-Mock.
 *
 * <p>Der PDP-Mock (`AccessTokenService`) prüft die Signatur NICHT (`setSkipSignatureVerification`),
 * sondern liest nur `sub` und `professionOid` aus dem Payload. Daher reicht ein syntaktisch
 * gültiges JWT (header.payload.signature) mit passenden Claims.
 */
public final class ZetaPdpSubjectTokenFactory {

  private ZetaPdpSubjectTokenFactory() {
    // utility class
  }

  /**
   * Erzeugt ein minimales, syntaktisch gültiges JWT für den PDP-Mock.
   *
   * <p>Format: base64(header).base64(payload).base64(dummy-signature)
   *
   * @param subject der `sub`-Claim (z.B. "zeta-client")
   * @param professionOid der `professionOid`-Claim (z.B. "1.2.276.0.76.4.49")
   * @return kompakt serialisiertes JWT
   */
  public static String createSubjectToken(String subject, String professionOid) {
    // Header: {"alg":"HS256","typ":"JWT"}
    String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    // Payload: {"sub":"...","professionOid":"..."}
    String payload =
        String.format("{\"sub\":\"%s\",\"professionOid\":\"%s\"}", subject, professionOid);

    // Dummy-Signatur (wird vom PDP-Mock nicht geprüft)
    String signature = "dummy-signature";

    return base64Url(header) + "." + base64Url(payload) + "." + base64Url(signature);
  }

  /**
   * Erzeugt ein Standard-Test-Token mit Default-Werten.
   *
   * @return kompakt serialisiertes JWT
   */
  public static String createDefaultSubjectToken() {
    return createSubjectToken("zeta-client", "1.2.276.0.76.4.49");
  }

  private static String base64Url(String input) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes());
  }
}
