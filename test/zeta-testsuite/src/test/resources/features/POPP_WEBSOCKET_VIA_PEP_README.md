# PoPP WebSocket via ZETA-PEP - Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `popp_websocket_via_pep.feature`.

## Übersicht

Das Feature testet den **ZETA-PEP Flow**:
- Client baut WebSocket zu ZETA-PEP Proxy auf
- Client liefert `Authorization: Bearer <JWT>` im WS-Handshake
- ZETA-PEP validiert JWT (Signatur via `zetakeystore.p12`) und erzeugt daraus `ZETA-User-Info`
- ZETA-PEP öffnet WebSocket zum PoPP Backend und reicht `ZETA-User-Info` im Handshake weiter
- PoPP erstellt danach eine `TokenMessage` (statt Error 1237)

---

## Szenario 1: Erfolgreicher WebSocket-Flow über ZETA-PEP

**Tag:** `@websocket @popp @pep`

```
┌─────────┐         ┌─────────────┐         ┌──────────┐         ┌─────────────┐
│  Test   │         │ Tiger Proxy │         │ ZETA-PEP │         │ PoPP Server │
│ Client  │         │  (localhost)│         │          │         │  (Backend)  │
└────┬────┘         └──────┬──────┘         └────┬─────┘         └──────┬──────┘
     │                     │                     │                      │
     │  1. WSS Handshake   │                     │                      │
     │  Authorization:     │                     │                      │
     │  Bearer <JWT>       │                     │                      │
     │────────────────────>│                     │                      │
     │                     │  2. Forward to PEP  │                      │
     │                     │────────────────────>│                      │
     │                     │                     │                      │
     │                     │                     │  3. JWT validieren   │
     │                     │                     │  (Signatur prüfen    │
     │                     │                     │   via zetakeystore)  │
     │                     │                     │                      │
     │                     │                     │  4. ZETA-User-Info   │
     │                     │                     │     Header erzeugen  │
     │                     │                     │                      │
     │                     │                     │  5. WS zu Backend    │
     │                     │                     │  + ZETA-User-Info    │
     │                     │                     │─────────────────────>│
     │                     │                     │                      │
     │                     │  6. WS Established  │<─────────────────────│
     │<────────────────────│<────────────────────│                      │
     │                     │                     │                      │
     │  7. StartMessage    │                     │                      │
     │────────────────────>│────────────────────>│─────────────────────>│
     │                     │                     │                      │
     │  8. StandardScenario│                     │                      │
     │<────────────────────│<────────────────────│<─────────────────────│
     │                     │                     │                      │
     │  9. ScenarioResponse│                     │                      │
     │────────────────────>│────────────────────>│─────────────────────>│
     │                     │                     │                      │
     │  10. TokenMessage   │                     │                      │
     │  (Erfolg!)          │                     │                      │
     │<────────────────────│<────────────────────│<─────────────────────│
     │                     │                     │                      │
     │  11. WS Close       │                     │                      │
     │────────────────────>│────────────────────>│─────────────────────>│
```

---

## Szenario 2: Ungültiger Token → Handshake abgelehnt

**Tag:** `@websocket @popp @pep`

```
┌─────────┐         ┌─────────────┐         ┌──────────┐
│  Test   │         │ Tiger Proxy │         │ ZETA-PEP │
│ Client  │         │  (localhost)│         │          │
└────┬────┘         └──────┬──────┘         └────┬─────┘
     │                     │                     │
     │  1. WSS Handshake   │                     │
     │  Authorization:     │                     │
     │  Bearer <INVALID>   │                     │
     │────────────────────>│                     │
     │                     │  2. Forward to PEP  │
     │                     │────────────────────>│
     │                     │                     │
     │                     │                     │  3. JWT validieren
     │                     │                     │     → UNGÜLTIG!
     │                     │                     │
     │                     │  4. HTTP 401/403    │
     │  Handshake FAILED   │<────────────────────│
     │<────────────────────│                     │
     │                     │                     │
     │  ❌ Keine WS-       │                     │
     │     Verbindung      │                     │
```

---

## Szenario 3: Kein Token → Handshake abgelehnt

**Tag:** `@websocket @popp @pep`

```
┌─────────┐         ┌─────────────┐         ┌──────────┐
│  Test   │         │ Tiger Proxy │         │ ZETA-PEP │
│ Client  │         │  (localhost)│         │          │
└────┬────┘         └──────┬──────┘         └────┬─────┘
     │                     │                     │
     │  1. WSS Handshake   │                     │
     │  (KEIN Authorization│                     │
     │   Header!)          │                     │
     │────────────────────>│                     │
     │                     │  2. Forward to PEP  │
     │                     │────────────────────>│
     │                     │                     │
     │                     │                     │  3. Authorization
     │                     │                     │     Header fehlt!
     │                     │                     │
     │                     │  4. HTTP 401        │
     │  Handshake FAILED   │<────────────────────│
     │<────────────────────│                     │
     │                     │                     │
     │  ❌ Keine WS-       │                     │
     │     Verbindung      │                     │
```

---

## Zusammenfassung

| Szenario | Authorization Header | JWT gültig? | Ergebnis |
|----------|---------------------|-------------|----------|
| 1 | ✅ Vorhanden | ✅ Ja | TokenMessage (Erfolg) |
| 2 | ✅ Vorhanden | ❌ Nein | Handshake fehlgeschlagen |
| 3 | ❌ Fehlt | — | Handshake fehlgeschlagen (401) |

---

## Test ausführen

```bash
# Alle WebSocket-Tests ausführen
# Hinweis: WebSocket-Traffic kann nicht über den Tiger-Proxy geroutet werden,
# daher ist -Dzeta_proxy=proxy hier nicht erforderlich.
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@websocket'
```

---
