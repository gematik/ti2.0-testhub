/*
 *
 * Copyright 2025-2026 gematik GmbH
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
package de.gematik.zeta.steps;

import java.security.cert.X509Certificate;
import lombok.Builder;
import lombok.Data;

/** Captures the result of a TLS connection attempt for Guard-tests (client mode). */
@Data
@Builder
public class TlsConnectionResult {

  /** Whether the TLS handshake completed successfully. */
  @Builder.Default private boolean handshakeSuccessful = false;

  /** Alert level received (2 = fatal). -1 if no alert. */
  @Builder.Default private int alertLevel = -1;

  /** Alert description code. -1 if no alert. */
  @Builder.Default private int alertDescription = -1;

  /** The negotiated cipher suite name (JSSE format, e.g. TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256). */
  private String negotiatedCipherSuite;

  /** The negotiated protocol version (e.g. "TLSv1.2"). */
  private String negotiatedProtocol;

  /** Server certificate chain received during handshake (Guard tests). */
  private X509Certificate[] serverCertificates;

  /** Whether renegotiation_info extension was present in ServerHello. */
  @Builder.Default private boolean renegotiationInfoPresent = false;

  /** Whether renegotiation was successful. */
  @Builder.Default private boolean renegotiationSuccessful = false;

  /** Whether a ServerKeyExchange message was effectively sent (curve-based handshake). */
  @Builder.Default private boolean serverKeyExchangeSent = false;

  /** The hash algorithm used in the ServerKeyExchange signature (TLS 1.2). */
  private String serverKeyExchangeHashAlgorithm;

  /** Raw exception message if handshake failed. */
  private String errorMessage;
}
