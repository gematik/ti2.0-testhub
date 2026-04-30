# TLS-Konformitätstests – Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `tls_guard.feature`.

## Übersicht

Das Feature testet die **TLS-Konformität** des ZETA Guard (Server-Seite) gemäß BSI TR-02102-2 und gemSpec_Krypt.

- **`tls_guard.feature`** – Prüft den ZETA Guard (Ingress-Endpunkt): TLS-Konfiguration und Handshake-Verhalten

## Voraussetzungen

* ZETA Guard Ingress (nginx) – erreichbar unter `${zeta_base_url}` (Standard: `localhost:9119`)
* Tiger-Proxy (für Traffic-Routing)
* Java 21 (TLS-Verbindungen werden über `javax.net.ssl.SSLSocket` aufgebaut)

## Architektur-Hinweis

Die Tests nutzen **keine externen TLS-Test-Tool-Binaries**. Stattdessen werden reine Java `SSLSocket`-Verbindungen verwendet:

- **Guard-Tests:** Ein Java-TLS-Client verbindet sich direkt mit dem ZETA Guard und prüft das Handshake-Verhalten.

```
Guard-Tests (tls_guard.feature):

┌──────────┐              ┌─────────────┐
│ Java TLS │  TLS 1.x     │ ZETA Guard  │
│  Client  │─────────────>│  (Ingress)  │
│(SSLSocket)              │ localhost:  │
└──────────┘              │   9119      │
     │                    └─────────────┘
     │
     ▼
 Handshake-Ergebnis auswerten:
 ✅ Protokollversion, Cipher Suite, Zertifikat, Renegotiation
 ❌ Abgelehnte Protokolle, Cipher Suites, Hashfunktionen
```

---

## Feature: TLS-Konformität ZETA Guard (`tls_guard.feature`)

**Feature-Tag:** `@TLS_Guard`

### Szenarien

| Tag | Szenario | Beschreibung | Erwartung |
|-----|----------|--------------|-----------|
| `@Protokollversion` | TLS 1.1 darf nicht unterstützt werden | Verbindung mit TLS 1.1 zum Guard | Alert 0x46 (protocol_version) |
| `@Renegotiation` | TLS-Renegotiation-Indication-Extension | TLS 1.2 Verbindung mit Renegotiation | Handshake erfolgreich, `renegotiation_info` vorhanden |
| `@Hashfunktionen_ungueltig` | Unsichere Hashfunktionen (MD5, SHA1, SHA224) | Nur schwache Hashes anbieten | Alert 0x28 (handshake_failure) |
| `@Hashfunktionen_gueltig` | Erlaubte Hashfunktionen (SHA256, SHA384, SHA512) | Starke Hashes anbieten | Handshake erfolgreich |
| `@Ciphersuiten_ungueltig` | Nicht unterstützte Cipher Suiten | Nur nicht-TR-02102-2-konforme Cipher Suiten | Kein ServerHello |
| `@Kurven` | Elliptische Kurven | secp256r1, secp384r1, unsupported_mix | secp256r1/secp384r1 ✅, unsupported_mix ❌ |
| `@Ciphersuiten_pflicht` | Pflicht-Cipher-Suiten | ECDHE_RSA_AES_128/256_GCM | Handshake erfolgreich |
| `@Zertifikat` | X.509-Identität gemäß GS-A_4359 | TLS 1.2 Default-Verbindung | Gültiges Zertifikat empfangen |
| `@RSA_TLS12` | RSA-Signaturalgorithmen TLS 1.2 | Nur RSA-basierte Signatur-Hash-Algorithmen | Alert 0x28 |
| `@RSA_TLS13` | RSA-Signaturalgorithmen TLS 1.3 | Nur RSA-basierte Signature-Schemes | Alert 0x28 |

---

## Spezifikationsreferenzen

| Anforderung | Beschreibung |
|-------------|--------------|
| **A_18464** | TLS 1.1 darf nicht unterstützt werden |
| **GS-A_5526** | TLS-Renegotiation-Indication-Extension |
| **A_21275-01** | Zulässige Hashfunktionen bei TLS-Signaturen (mindestens SHA-256) |
| **GS-A_4384-03** | Cipher-Suiten gemäß TR-02102-2, Abschnitt 3.3.1 Tabelle 1 |
| **GS-A_5542** | TLS-Protokollversion |
| **GS-A_4359** | X.509-Identität für Authentifizierung |

---

## Tests ausführen

```bash
# Alle TLS-Tests (Guard):
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@TLS_Guard'

# Einzelnes Szenario (z.B. nur Cipher-Suiten):
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@Ciphersuiten_pflicht'
```
