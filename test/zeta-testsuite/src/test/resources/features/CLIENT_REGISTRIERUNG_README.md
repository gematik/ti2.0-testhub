# Client Registrierung - Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `client_registrierung.feature`.

## Übersicht

Das Feature testet die **Client-Registrierung** am ZETA-PDP:
- Token-Exchange-Request an den ZETA-PDP senden (RFC 8693)
- Prüfung der Policy-Entscheidung (OPA)
- Gutfall: Access-Token wird ausgestellt
- Fehlerfall: Zugriff wird mit 403 abgelehnt (noch nicht implementiert)

---

## Szenario 1: Erfolgreicher Token-Request (Gutfall)

**Tag:** `@staging @client_registrierung`

Dieser Test prüft den erfolgreichen PDP-Flow. Der Request geht über den Tiger-Proxy, damit der Traffic mitgeschnitten und mit TGR-Steps validiert werden kann.

### Flow-Diagramm

```
┌─────────┐         ┌─────────────┐         ┌──────────────┐
│  Test   │         │ Tiger Proxy │         │  ZETA-PDP    │
│ Client  │         │ (Mitschnitt)│         │  (Mock)      │
└────┬────┘         └──────┬──────┘         └──────┬───────┘
     │                     │                       │
     │  1. POST /token     │                       │
     │     (via Proxy)     │                       │
     │────────────────────>│                       │
     │                     │                       │
     │                     │  2. Forward           │
     │                     │──────────────────────>│
     │                     │                       │
     │                     │     Request-Body:     │
     │                     │     grant_type=       │
     │                     │       urn:ietf:params:oauth:grant-type:token-exchange
     │                     │     requested_token_type=
     │                     │       urn:ietf:params:oauth:token-type:access_token
     │                     │     subject_token=<JWT>
     │                     │     subject_token_type=
     │                     │       urn:ietf:params:oauth:token-type:access_token
     │                     │                       │
     │                     │                       │  3. JWT parsen
     │                     │                       │     (sub, professionOid)
     │                     │                       │
     │                     │                       │  4. OPA Policy prüfen
     │                     │                       │     (Mock: immer allow=true)
     │                     │                       │
     │                     │                       │  5. Access-Token generieren
     │                     │                       │
     │  6. HTTP 200        │                       │
     │     {"access_token":│"...", "refresh_token": "..."}
     │<────────────────────│<──────────────────────│
     │                     │                       │
     │  7. TGR-Steps       │                       │
     │     prüfen Response │                       │
     │                     │                       │
     │  [OK] Test erfolgreich                      │
     │                     │                       │
```

### Request-Format (RFC 8693 Token Exchange)

Der Token-Request verwendet das **OAuth 2.0 Token Exchange**-Format (RFC 8693):

| Parameter | Wert | Beschreibung |
|-----------|------|--------------|
| `grant_type` | `urn:ietf:params:oauth:grant-type:token-exchange` | Token-Exchange Grant |
| `requested_token_type` | `urn:ietf:params:oauth:token-type:access_token` | Gewünschter Token-Typ |
| `subject_token` | `<JWT>` | JWT mit `sub` und `professionOid` Claims |
| `subject_token_type` | `urn:ietf:params:oauth:token-type:access_token` | Typ des Subject-Tokens |

### Subject-Token Format

Das `subject_token` ist ein JWT mit folgenden Claims:

**Header:**
```
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```
{
  "sub": "zeta-client",
  "professionOid": "1.2.276.0.76.4.49"
}
```

> **Hinweis:** Der PDP-Mock prüft die Signatur des Subject-Tokens nicht (`setSkipSignatureVerification`).
> Er liest nur die Claims `sub` und `professionOid` aus dem Payload.

### Validierung mit Tiger-Steps

Der Test nutzt Standard-TGR-Steps zur Validierung:

```gherkin
Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"
```

### Hinweis

> **ACHTUNG:** Der PDP-Mock (`zeta-pdp-server-mockservice`) verwendet aktuell keine echte OPA-Policy.
> Die `authz.rego` gibt immer `allow=true` zurück. Dieser Test prüft daher nur die Erreichbarkeit
> des Token-Endpoints und die korrekte Token-Ausstellung, nicht die echte Policy-Validierung.

---

## Szenario 2: Policy-Ablehnung (Fehlerfall) - NICHT IMPLEMENTIERT

**Tag:** `@Ignore @staging @client_registrierung @no_proxy`

Dieser Test würde prüfen, ob ungültige Client-Daten korrekt abgelehnt werden.

### Flow-Diagramm (geplant)

```
┌─────────┐         ┌─────────────┐         ┌──────────────┐         ┌─────────┐
│  Test   │         │ Tiger Proxy │         │  ZETA-Guard  │         │   OPA   │
│ Client  │         │ (Manipul.)  │         │              │         │         │
└────┬────┘         └──────┬──────┘         └──────┬───────┘         └────┬────┘
     │                     │                       │                      │
     │  1. GET /vsd        │                       │                      │
     │────────────────────>│                       │                      │
     │                     │                       │                      │
     │                     │  2. Forward           │                      │
     │                     │──────────────────────>│                      │
     │                     │                       │                      │
     │                     │                       │  3. OPA Decision     │
     │                     │                       │     Request          │
     │                     │                       │     (manipuliert:    │
     │                     │                       │     professionOID,   │
     │                     │                       │     product_id, etc.)│
     │                     │                       │─────────────────────>│
     │                     │                       │                      │
     │                     │                       │  4. OPA Response     │
     │                     │                       │     {"allow": false, │
     │                     │                       │      "reason": "..."}│
     │                     │                       │<─────────────────────│
     │                     │                       │                      │
     │  5. HTTP 403        │                       │                      │
     │     {"error": "...",│                       │                      │
     │      "error_description": "..."}            │                      │
     │<────────────────────│<──────────────────────│                      │
     │                     │                       │                      │
     │  [FEHLER] Zugriff verweigert                │                      │
     │                     │                       │                      │
```

### Getestete Ablehnungsgründe (geplant)

| OPA Input Field | Ungültiger Wert | Erwarteter Fehler |
|-----------------|-----------------|-------------------|
| `professionOID` | `1.2.276.0.76.4.999` | Ungültige Profession |
| `product_id` | `unknown_product` | Unbekanntes Produkt |
| `product_version` | `99.99.99` | Ungültige Version |
| `scopes` | `invalid_scope_xyz` | Ungültiger Scope |
| `aud` | `https://evil.example.com/api` | Ungültige Audience |

---

## Test ausführen

```bash
# Gutfall-Test ausführen
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
```

---

## Technische Details

### Implementierung

Die Step-Implementierung befindet sich in:
- `de.gematik.zeta.steps.ZetaPepJwtSteps`

Der Helper für die Subject-Token-Erzeugung:
- `de.gematik.zeta.services.ZetaPdpSubjectTokenFactory`

### Konfiguration

Der PDP-Mock nutzt folgende Keystore-Konfiguration:
- **Keystore:** `zetakeystore.p12`
- **Alias:** `zetamock`
- **Passwort:** `testpassword`

> **Hinweis:** Die Signatur des Subject-Tokens wird vom PDP-Mock nicht geprüft.

---

## Zusammenfassung

| Szenario | Status | Prüfung |
|----------|--------|---------|
| Gutfall (Token-Exchange) | ✅ Aktiv | PDP erreichbar, Token wird ausgestellt, TGR-Validierung |
| Fehlerfall (Policy-Ablehnung) | ❌ Ignoriert | Benötigt echte OPA-Policy |

**Hinweis:** Für echte Policy-Tests muss die `authz.rego` im PDP-Mock durch eine Policy ersetzt werden, die die Input-Werte validiert.
