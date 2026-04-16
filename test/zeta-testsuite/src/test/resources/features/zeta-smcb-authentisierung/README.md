# SMC-B Authentisierung via ZETA-PDP - Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `smcb_authentisierung.feature`.

## Übersicht

Das Feature testet den **SMC-B Token-Exchange Flow**.

Der Test sendet einen Token-Exchange-Request mit:
- **subject_token**: Echtes SMC-B-signiertes JWT (Brainpool P-256 R1, x5c Header)
- **client_assertion**: ES256-signiertes JWT (P-256, jwk Header) mit client_statement
- **client_id**: Eindeutige Client-Kennung
- **client_assertion_type**: `urn:ietf:params:oauth:client-assertion-type:jwt-bearer`

Der mitgeschnittene Traffic wird geprüft auf:
- Korrekte Request-Body-Felder (RFC 8693)
- Client Assertion JWT Struktur und ES256-Signatur
- subject_token Schema-Validierung gegen `smb-id-token-jwt.yaml`
- subject_token ES256-Signaturprüfung (Brainpool P-256 R1)
- TelematikID-Bindung (sub-Claim aus x5c-Zertifikat)
- client_id Konsistenz über alle JWTs

---

## Szenario 1: Erfolgreicher Token Exchange (Gutfall)

Prüft den erfolgreichen End-to-End Token-Exchange-Flow:
1. SMC-B subject_token + client_assertion werden erzeugt
2. Token-Exchange-Request über Tiger-Proxy an PDP-Mock
3. Response enthält `access_token` mit Status 2xx

---

## Szenario 2: Token Exchange Request Body enthält alle Pflichtfelder (RFC 8693)

Prüft, dass der Request alle Felder enthält:
- `grant_type` = `urn:ietf:params:oauth:grant-type:token-exchange`
- `subject_token_type` = `urn:ietf:params:oauth:token-type:jwt`
- `client_id` vorhanden
- `client_assertion` vorhanden
- `client_assertion_type` = `urn:ietf:params:oauth:client-assertion-type:jwt-bearer`

---

## Szenario 3: Client Assertion JWT Struktur und Signatur

"Prüfe client-assertion-jwt im Token Exchange Request Body"

Prüft:
- Schema-Validierung gegen `client-assertion-jwt.yaml`
- Header: `alg=ES256`, `typ=JWT`, `jwk.use=sig`, `jwk.kty=EC`
- ES256-Signatur verifiziert
- Payload: `iss` und `sub` == `client_id`
- Payload: `aud` enthält Token-Endpoint-URL
- Payload: `exp` liegt in der Zukunft (Token noch gültig)

---

## Szenario 4: SMC-B subject_token Struktur, Schema und TelematikID-Bindung

PDP Client Registrierung - TI-Identität in Attestation - Bindung TelematikID

Prüft:
- subject_token gegen Schema `smb-id-token-jwt.yaml` validiert
- ES256-Signatur verifiziert (Brainpool P-256 R1 via BouncyCastle)
- x5c-Zertifikat → TelematikID extrahiert
- `iss` im subject_token == `client_id`
- `iss` in client_assertion == `client_id`
- `sub` im subject_token == TelematikID aus Zertifikat

---

## Szenario 5: Access-Token enthält korrekte User-Info Claims

PDP Client Registrierung - TI-Identität

Prüft End-to-End:
- TelematikID wird aus subject_token x5c extrahiert
- `sub` im subject_token == TelematikID
- Access-Token vorhanden
- Access-Token Schema-Validierung gegen `access-token.yaml` (soft assert)
- `clientId` im Access-Token == TelematikID
- `professionOid` im Access-Token == professionOID aus SMC-B Zertifikat

---

## Szenario 6: Client Assertion enthält Client Statement mit Attestation-Daten

Referenz: "Client Assertion JWT enthält Software Attestation für Linux" (Zeile 645-694)

Prüft:
- `client_statement` im client_assertion Payload vorhanden
- `platform` == `linux`
- `sub` im client_statement == `client_id`
- `attestation_timestamp` vorhanden und in der Vergangenheit
- `exp` der client_assertion liegt nach `attestation_timestamp`
- `posture` vorhanden mit `attestation_challenge` und `public_key`

---

## Zusammenfassung

| Szenario | Prüfung | Referenz |
|----------|---------|----------|
| 1 | Gutfall: access_token erhalten (2xx) + Schema-Validierung | Access Token ausstellen |
| 2 | Alle RFC 8693 Pflichtfelder + client_assertion Schema | Token Exchange Request Body |
| 3 | client_assertion: Schema + Struktur + ES256-Signatur + aud + exp | client-assertion-jwt prüfen |
| 4 | subject_token: Schema + Signatur + TelematikID | TI-Identität Bindung |
| 5 | Access-Token: Schema + TelematikID + professionOID | User-Info Claims |
| 6 | client_statement: platform + sub + timestamps + posture | Software Attestation |

---

## Test ausführen

```bash
# SMC-B-Authentisierungstests ausführen
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@smcb_authentisierung'
```

---
