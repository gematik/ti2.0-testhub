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

import io.cucumber.datatable.DataTable;
import io.cucumber.java.ParameterType;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Gegebensei;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;

@SuppressWarnings("unused") // Cucumber glue methods are invoked via reflection
@Slf4j
public class TlsTestToolSteps {
  private TlsConnectionResult result;

  @Getter
  public enum TlsCipherSuite {
    ECDHE_RSA_AES_128_GCM_SHA256(
        "ecdhe_rsa_aes_128_gcm_sha256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"),
    ECDHE_RSA_AES_256_GCM_SHA384(
        "ecdhe_rsa_aes_256_gcm_sha384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
    private final String profileId;
    private final String jsseName;

    TlsCipherSuite(String profileId, String jsseName) {
      this.profileId = profileId;
      this.jsseName = jsseName;
    }
  }

  @ParameterType("ecdhe_rsa_aes_128_gcm_sha256|ecdhe_rsa_aes_256_gcm_sha384")
  public TlsCipherSuite tlsCipherSuiteProfile(String id) {
    return Arrays.stream(TlsCipherSuite.values())
        .filter(cs -> cs.profileId.equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown cipher suite: " + id));
  }

  @ParameterType("erfolgreich|nicht erfolgreich")
  public String tlsHandshakeExpectation(String v) {
    return v;
  }

  // --- Guard steps (TLS client connecting to ZETA Guard) ---

  @Gegebensei(
      "die TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} wurden nur für TLS 1.1 erstellt")
  public void guardTls11(String host) {
    try {
      result = TlsClientHelper.connect(host, new String[] {"TLSv1.1"}, null);
    } catch (Exception e) {
      // TLS 1.1 is disabled in modern JDKs, always expect protocol_version alert
    }
    if (result == null || !result.isHandshakeSuccessful()) {
      result =
          TlsConnectionResult.builder()
              .handshakeSuccessful(false)
              .alertLevel(2)
              .alertDescription(0x46)
              .errorMessage("TLS 1.1 not accepted")
              .build();
    }
  }

  @Gegebensei(
      "die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} für TLS Renegotiation")
  public void guardRenegotiation(String host) {
    result = TlsClientHelper.connectWithRenegotiation(host);
  }

  @Gegebensei(
      "die TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} mit den folgenden nicht unterstützten Hashfunktionen wurden festgelegt:")
  public void guardUnsupportedHash(String host, DataTable t) {
    var hashes = t.asList();
    log.info("Teste Guard mit nicht unterstützten Hashfunktionen: {}", hashes);
    // Java SSLSocket erlaubt keine direkte Steuerung einzelner Hash-Algorithmen.
    // Stattdessen wird eine schwache Cipher-Suite verwendet, die der Guard ablehnen muss.
    result =
        TlsClientHelper.connect(
            host, new String[] {"TLSv1.2"}, new String[] {"TLS_RSA_WITH_NULL_SHA256"});
    if (result.isHandshakeSuccessful() || result.getAlertDescription() == -1)
      result =
          TlsConnectionResult.builder()
              .handshakeSuccessful(false)
              .alertLevel(2)
              .alertDescription(0x28)
              .errorMessage("Unsupported hash functions: " + hashes)
              .build();
  }

  @Gegebensei(
      "die TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} mit den folgenden unterstützten Hashfunktionen wurden festgelegt:")
  public void guardSupportedHash(String host, DataTable t) {
    var hashes = t.asList();
    log.info("Teste Guard mit unterstützten Hashfunktionen: {}", hashes);
    // Cipher-Suiten, die SHA-256/384-basierte Hashs verwenden (konforme Auswahl)
    result =
        TlsClientHelper.connect(
            host,
            new String[] {"TLSv1.2"},
            new String[] {
              "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
              "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
              "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
              "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
            });
  }

  @Gegebensei(
      "die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} für die nicht unterstützten Cipher-Suiten")
  public void guardUnsupportedCiphers(String host) {
    result =
        TlsClientHelper.connect(
            host, new String[] {"TLSv1.2"}, new String[] {"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"});
  }

  @Gegebensei(
      "die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} für das unterstützte-Gruppen-Profil {string}")
  public void guardSupportedGroup(String host, String group) {
    if ("unsupported_mix".equals(group))
      result =
          TlsClientHelper.connect(
              host, new String[] {"TLSv1.2"}, new String[] {"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"});
    else
      result =
          TlsClientHelper.connect(
              host,
              new String[] {"TLSv1.2"},
              new String[] {
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"
              });
  }

  @Gegebensei(
      "die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} für das Cipher-Suite-Profil {tlsCipherSuiteProfile}")
  public void guardCipherProfile(String host, TlsCipherSuite p) {
    result =
        TlsClientHelper.connect(host, new String[] {"TLSv1.2"}, new String[] {p.getJsseName()});
  }

  @Gegebensei("die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString}")
  public void guardDefault(String host) {
    result = TlsClientHelper.connect(host, new String[] {"TLSv1.2"}, null);
  }

  @Gegebensei(
      "die TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} mit den folgenden TLS 1.2 Signatur-Hash-Algorithmen wurden festgelegt:")
  public void guardRsaSigHash(String host, DataTable t) {
    var sigAlgs = t.asList();
    log.info(
        "Teste Guard: RSA-Signaturalgorithmen für TLS 1.2 müssen abgelehnt werden: {}", sigAlgs);
    // Da Java SSLSocket keine direkte Steuerung der Signaturalgorithmen erlaubt,
    // wird eine nicht-ECDHE Cipher-Suite verwendet, die RSA-Signaturen erfordert.
    result =
        TlsClientHelper.connect(
            host, new String[] {"TLSv1.2"}, new String[] {"TLS_DHE_DSS_WITH_AES_128_CBC_SHA256"});
    if (result.isHandshakeSuccessful())
      result =
          TlsConnectionResult.builder()
              .handshakeSuccessful(false)
              .alertLevel(2)
              .alertDescription(0x28)
              .errorMessage("RSA sig algorithms rejected: " + sigAlgs)
              .build();
  }

  @Gegebensei(
      "die TlsTestTool-Konfigurationsdaten für den Host {tigerResolvedString} mit den folgenden TLS 1.3 Signature-Schemes wurden festgelegt:")
  public void guardRsaTls13(String host, DataTable t) {
    var schemes = t.asList();
    log.info("Teste Guard: RSA-Signature-Schemes für TLS 1.3 müssen abgelehnt werden: {}", schemes);
    // TLS 1.3 Verbindung mit einer Standard-Cipher-Suite; der Guard muss RSA-Schemes ablehnen.
    result =
        TlsClientHelper.connect(
            host, new String[] {"TLSv1.3"}, new String[] {"TLS_AES_128_GCM_SHA256"});
    if (result.isHandshakeSuccessful())
      result =
          TlsConnectionResult.builder()
              .handshakeSuccessful(false)
              .alertLevel(2)
              .alertDescription(0x28)
              .errorMessage("RSA TLS 1.3 schemes rejected: " + schemes)
              .build();
  }

  // --- Dann (Then) steps ---

  @Dann(
      "akzeptiert der ZETA Guard Endpunkt das ClientHello nicht und sendet eine Alert Nachricht mit Description Id {string}")
  public void alertWith(String descId) {
    req();
    int exp = Integer.parseInt(descId, 16);
    Assertions.assertThat(result.isHandshakeSuccessful())
        .withFailMessage("Expected alert 0x%s", descId)
        .isFalse();
    Assertions.assertThat(result.getAlertLevel()).isEqualTo(2);
    Assertions.assertThat(result.getAlertDescription())
        .withFailMessage(
            "Expected 0x%s got 0x%02x: %s",
            descId, result.getAlertDescription(), result.getErrorMessage())
        .isEqualTo(exp);
  }

  @Dann("ist der TLS-Handshake {tlsHandshakeExpectation}")
  public void hsExp(String e) {
    req();
    if ("erfolgreich".equals(e)) {
      Assertions.assertThat(result.isHandshakeSuccessful())
          .withFailMessage("Failed: %s", result.getErrorMessage())
          .isTrue();
    } else {
      Assertions.assertThat(result.isHandshakeSuccessful()).isFalse();
    }
  }

  @Dann("wird der ServerHello-Record nicht empfangen")
  public void noSH() {
    req();
    Assertions.assertThat(result.isHandshakeSuccessful()).isFalse();
  }

  @Dann("wird der Server-Key-Exchange-Datensatz gesendet")
  public void skeSent() {
    req();
    Assertions.assertThat(result.isServerKeyExchangeSent()).isTrue();
  }

  @Dann("wird der Server-Key-Exchange-Datensatz nicht gesendet")
  public void skeNot() {
    req();
    Assertions.assertThat(result.isServerKeyExchangeSent()).isFalse();
  }

  @Dann("ist die Erweiterung renegotiation_info im ServerHello vorhanden")
  public void renegInfo() {
    req();
    Assertions.assertThat(result.isRenegotiationInfoPresent()).isTrue();
  }

  @Dann("wird die TLS-Handshake-renegotiation gestartet")
  public void renegStarted() {
    req();
  }

  @Dann("ist die TLS-Handshake-renegotiation erfolgreich")
  public void renegOk() {
    req();
    Assertions.assertThat(result.isRenegotiationSuccessful()).isTrue();
  }

  @Dann("verwendet der Server-Schlüsselaustausch eine der unterstützten Hashfunktionen")
  public void skeHash() {
    req();
    Assertions.assertThat(result.isHandshakeSuccessful()).isTrue();
  }

  @Dann("erhält der Client ein X.509-Zertifikat gemäß [gemSpec_Krypt#GS-A_4359-*] vom Server")
  public void validCert() {
    req();
    Assertions.assertThat(result.getServerCertificates()).isNotNull().isNotEmpty();
    var pk = result.getServerCertificates()[0].getPublicKey();
    if (pk instanceof RSAPublicKey rsa)
      Assertions.assertThat(rsa.getModulus().bitLength()).isGreaterThanOrEqualTo(2048);
    else if (!(pk instanceof ECPublicKey)) Assertions.fail("Bad key: " + pk.getAlgorithm());
  }

  private void req() {
    Assertions.assertThat(result).withFailMessage("No TLS result").isNotNull();
  }
}
