# PEP Header Management

## Übersicht

Dieses Feature (`pep_header_management.feature`) testet die Header-Transformation des
ZETA-PEP (Mockservice) gemäß den folgenden Anforderungen:

| AFO | Beschreibung |
|-----|-------------|
| A_25669-01 | PEP muss zusätzliche HTTP-Header einfügen (`ZETA-User-Info`, `ZETA-PoPP-Token-Content`, `ZETA-Client-Data`) beim Weiterleiten an das Backend |
| A_27265 | PEP muss Host Header und Request-Zeile unverändert weiterleiten |
| A_26492-02 | PEP muss Client-Daten-Weiterleitung pro Endpunkt konfigurierbar machen |

### Limitierung im Mockservice-Setup

Da der PEP-Mockservice intern (per `RestTemplate`) an das Backend weiterleitet, ist der
**PEP→Backend-Traffic nicht über den Tiger-Proxy sichtbar**. Die Header-Transformation
wird daher **indirekt** geprüft:

- **200-Response vom Backend** = PEP hat `ZETA-User-Info` korrekt erzeugt und ans Backend
  weitergeleitet (sonst antwortet das Backend mit Error 1237: "No ZETA-User-Info").
- **Client→PEP Request** = Die eingehenden Header (Authorization, PoPP) sind korrekt gesetzt.

Für die **vollständige Prüfung** der transformierten Header (VOR und NACH PEP) siehe die
Tests in der `tiger-testsuite` (UseCase_12/UseCase_23), die auf einem echten ZeTA-Deployment
mit sichtbarem PEP→Backend-Traffic laufen.

## Testablauf

```
┌─────────┐         ┌─────────────┐         ┌──────────┐         ┌──────────┐         ┌─────────────┐
│  Test   │         │ Tiger Proxy │         │ ZETA-PEP │         │ ZETA-PDP │         │ PoPP Server │
│ Client  │         │  (localhost)│         │ (:9110)  │         │ (:9112)  │         │  (:9210)    │
└────┬────┘         └──────┬──────┘         └────┬─────┘         └────┬─────┘         └───────┬─────┘
     │                     │                     │                    │                       │
     │  1. PoPP-Token erzeugen                   │                    │                       │
     │───────────────────────────────────────────────────────────────────────────────────────>│
     │  PoPP-Token ◄──────────────────────────────────────────────────────────────────────────│
     │                     │                     │                    │                       │
     │  2. Token-Exchange (Subject-Token → Access-Token)              │                       │
     │───────────────────────────────────────────────────────────────>│                       │
     │  Access-Token ◄─────────────────────────────────────────────── │                       │
     │                     │                     │                    │                       │
     │  3. GET /openapi.yaml                     │                    │                       │
     │  Authorization: Bearer <Access-Token>     │                    │                       │
     │  PoPP: <PoPP-Token> │                     │                    │                       │
     │────────────────────>│   (sichtbar)        │                    │                       │
     │                     │────────────────────>│                    │                       │
     │                     │                     │  4. Header-Transformation (A_25669-01):    │
     │                     │                     │     Authorization → ZETA-User-Info         │
     │                     │                     │     PoPP → ZETA-PoPP-Token-Content         │
     │                     │                     │     + ZETA-Client-Data                     │
     │                     │                     │  5. Forward to Backend (nicht sichtbar)    │
     │                     │                     │──────────────────────────────────────────> │
     │                     │                     │  6. HTTP 200 OK (ZETA-User-Info ok)        │
     │                     │                     │◄───────────────────────────────────────────│
     │  7. HTTP 200 OK     │                     │                    │                       │
     │◄────────────────────│◄────────────────────│                    │                       │
     │                     │                     │                    │                       │
     │  8. Tiger-Proxy: Sichtbaren Client→PEP Request validieren     │                       │
     │     ├─ Authorization Header vorhanden                         │                       │
     │     ├─ PoPP Header vorhanden & schema-konform (gemSpec_PoPP)  │                       │
     │     ├─ ES256-Signatur strukturell gültig                      │                       │
     │     ├─ actorId vorhanden                                      │                       │
     │     └─ Zeitstempel (iat, patientProofTime) in der Vergangenheit                       │
     │                     │                     │                    │                       │
     │  9. Indirekte Prüfung: 200 = Header-Transformation erfolgreich│                       │
```

### Schritte im Detail

1. **PoPP-Token erzeugen** – Test-Client ruft den PoPP-Server auf, um ein PoPP-Token zu generieren
2. **Token-Exchange** – Subject-Token wird gegen ein Access-Token beim ZETA-PDP getauscht
3. **Ressourcen-Anfrage senden** – GET-Request mit Authorization- und PoPP-Header über Tiger-Proxy an den PEP
4. **Client→PEP Header validieren** (im Tiger-Proxy sichtbar):
   - Authorization Header ist gesetzt
   - PoPP Header ist vorhanden und schema-konform (gemSpec_PoPP)
   - ES256-Signatur des PoPP-Tokens ist strukturell gültig
   - `actorId` im PoPP-Token ist vorhanden (nicht leer)
   - Zeitstempel (`iat` und `patientProofTime`) liegen in der Vergangenheit
5. **Indirekte Prüfung** der PEP-Header-Transformation:
   - Response 200 = PEP hat `ZETA-User-Info` korrekt an das Backend weitergeleitet
   - Wäre die Transformation fehlgeschlagen, hätte das Backend mit Error 1237 geantwortet

### Negativ-Szenarien

- **Ohne PoPP-Header**: PEP leitet trotzdem weiter (PoPP ist im Mockservice optional) → 200
- **Ohne Authorization**: PEP antwortet mit 401 Unauthorized

## Benötigte Container

* ZETA PEP Server Mockservice (Port 9110)
* ZETA PDP Server Mockservice (Port 9112)
* PoPP Server Mockservice (Port 9210)

## Ausführung

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="@pep_header_management"
```

## Verwandte Tests

Für die vollständige Prüfung der Header-Transformation mit sichtbarem PEP→Backend-Traffic:
- `tiger-testsuite/UseCase_12`: Client-Ressourcen-Anfrage an Fachdienst
- `tiger-testsuite/UseCase_23`: Client-Daten-Header Weiterleitung

