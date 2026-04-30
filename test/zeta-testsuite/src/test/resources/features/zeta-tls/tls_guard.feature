#language:de
# Befehl zum Ausführen des Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@TLS_Guard'
@PRODUKT:ZETA @TLS_Guard

Funktionalität: TLS-Konformität ZETA Guard

  @Protokollversion
  Szenariogrundriss: TLS 1.1 darf nicht unterstützt werden.
    Gegeben sei die TlsTestTool-Konfigurationsdaten für den Host "<host>" wurden nur für TLS 1.1 erstellt
    # 46 in hexadezimal entspricht 70 in dezimal und ist der Alert Code für TLS Protocol Version Failure.
    Dann akzeptiert der ZETA Guard Endpunkt das ClientHello nicht und sendet eine Alert Nachricht mit Description Id "46"
    Beispiele:
      | host             |
      | ${zeta_base_url} |

  @Renegotiation
  Szenariogrundriss: TLS-Renegotiation-Indication-Extension - ZETA Guard - TLS-Renegotiation-Indication-Extension.
    Gegeben sei die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host "<host>" für TLS Renegotiation
    Dann ist der TLS-Handshake erfolgreich
    Und ist die Erweiterung renegotiation_info im ServerHello vorhanden
    Und wird die TLS-Handshake-renegotiation gestartet
    Und ist die TLS-Handshake-renegotiation erfolgreich
    Beispiele:
      | host             |
      | ${zeta_base_url} |

  @Hashfunktionen_ungueltig
  Szenariogrundriss: TLS-Verbindungen, zulässige Hashfunktionen bei Signaturen im TLS-Handshake - ZETA Guard - mindestens SHA-256 unterstützen
    Gegeben sei die TlsTestTool-Konfigurationsdaten für den Host "<host>" mit den folgenden nicht unterstützten Hashfunktionen wurden festgelegt:
      | MD5    |
      | SHA1   |
      | SHA224 |
    # 28 in hexadezimal entspricht 40 in dezimal und ist der Alert Code für TLS Handshake Failure.
    Dann akzeptiert der ZETA Guard Endpunkt das ClientHello nicht und sendet eine Alert Nachricht mit Description Id "28"
    Beispiele:
      | host             |
      | ${zeta_base_url} |

  @Hashfunktionen_gueltig
  Szenariogrundriss: TLS-Verbindungen, zulässige Hashfunktionen bei Signaturen im TLS-Handshake - ZETA Guard - erlaubte Hashfunktionen
    Gegeben sei die TlsTestTool-Konfigurationsdaten für den Host "<host>" mit den folgenden unterstützten Hashfunktionen wurden festgelegt:
      | SHA256 |
      | SHA384 |
      | SHA512 |
    Dann verwendet der Server-Schlüsselaustausch eine der unterstützten Hashfunktionen
    Und ist der TLS-Handshake erfolgreich
    Beispiele:
      | host             |
      | ${zeta_base_url} |

  @Ciphersuiten_ungueltig
  Szenariogrundriss: Cipher-Suiten die nicht in TR-02102-2, Abschnitt 3.3.1 Tabelle 1 aufgeführt sind, werden nicht unterstützt.
    Gegeben sei die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host "<host>" für die nicht unterstützten Cipher-Suiten
    Dann wird der ServerHello-Record nicht empfangen
    Beispiele:
      | host             |
      | ${zeta_base_url} |

  @Kurven
  Szenariogrundriss: TLS-Verbindungen - ZETA Guard - PEP HTTP Proxy - elliptische Kurven.
    # Profile:
    # p256: secp256r1 (0017)
    # p384: secp384r1 (0018)
    # unsupported_mix: secp160r1, secp192r1, secp224r1, secp521r1, brainpoolP512r1, x25519, x448, ffdhe3072, ffdhe4096
    Gegeben sei die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host "<host>" für das unterstützte-Gruppen-Profil "<supported_group>"
    Dann wird der Server-Key-Exchange-Datensatz <ske_erwartung>
    Und ist der TLS-Handshake <handshake_erwartung>
    Beispiele:
      | host             | supported_group | handshake_erwartung | ske_erwartung  |
      | ${zeta_base_url} | secp256r1       | erfolgreich         | gesendet       |
      | ${zeta_base_url} | secp384r1       | erfolgreich         | gesendet       |
      | ${zeta_base_url} | unsupported_mix | nicht erfolgreich   | nicht gesendet |

  @Ciphersuiten_pflicht
  Szenariogrundriss: Als Cipher-Suite MÜSSEN TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 und TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 unterstützt werden.
    # Profile:
    # ecdhe_rsa_aes_128_gcm_sha256 -> TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 (0xC0,0x2F)
    # ecdhe_rsa_aes_256_gcm_sha384 -> TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 (0xC0,0x30)
    Gegeben sei die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host "<host>" für das Cipher-Suite-Profil <ciphersuite_profil>
    Dann ist der TLS-Handshake erfolgreich
    Beispiele:
      | host             | ciphersuite_profil            |
      | ${zeta_base_url} | ecdhe_rsa_aes_128_gcm_sha256 |
      | ${zeta_base_url} | ecdhe_rsa_aes_256_gcm_sha384 |

  @Zertifikat
  Szenariogrundriss: Zur Authentifizierung MUSS eine X.509-Identität gemäß [gemSpec_Krypt#GS-A_4359-*] verwendet werden.
    Gegeben sei die TLS 1.2 TlsTestTool-Konfigurationsdaten für den Host "<host>"
    Dann ist der TLS-Handshake erfolgreich
    Und erhält der Client ein X.509-Zertifikat gemäß [gemSpec_Krypt#GS-A_4359-*] vom Server
    Beispiele:
      | host             |
      | ${zeta_base_url} |

  @RSA_TLS12
  Szenariogrundriss: RSA-Signaturalgorithmen dürfen für TLS 1.2 nicht unterstützt werden
    Gegeben sei die TlsTestTool-Konfigurationsdaten für den Host "<host>" mit den folgenden TLS 1.2 Signatur-Hash-Algorithmen wurden festgelegt:
      | RSA_MD5    |
      | RSA_SHA1   |
      | RSA_SHA224 |
      | RSA_SHA256 |
      | RSA_SHA384 |
      | RSA_SHA512 |
    # 28 in hexadezimal entspricht 40 in dezimal und ist der Alert Code für TLS Handshake Failure.
    Dann akzeptiert der ZETA Guard Endpunkt das ClientHello nicht und sendet eine Alert Nachricht mit Description Id "28"
    Beispiele:
      | host             |
      | ${zeta_base_url} |

  @RSA_TLS13
  Szenariogrundriss: RSA-Signaturalgorithmen dürfen für TLS 1.3 nicht unterstützt werden
    Gegeben sei die TlsTestTool-Konfigurationsdaten für den Host "<host>" mit den folgenden TLS 1.3 Signature-Schemes wurden festgelegt:
      | rsa_pkcs1_sha256 |
      | rsa_pkcs1_sha384 |
      | rsa_pkcs1_sha512 |
    # 28 in hexadezimal entspricht 40 in dezimal und ist der Alert Code für TLS Handshake Failure.
    Dann akzeptiert der ZETA Guard Endpunkt das ClientHello nicht und sendet eine Alert Nachricht mit Description Id "28"
    Beispiele:
      | host             |
      | ${zeta_base_url} |
