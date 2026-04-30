# REST Datenübertragung via ZETA-PEP Proxy - Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `rest_data_transfer_via_pep.feature`.

## Übersicht

Das Feature testet die **REST-basierte Datenübertragung** über den ZETA-PEP Proxy:
- Client sendet REST-Anfragen (GET) an den ZETA-PEP Proxy
- PEP prüft den `Authorization`-Header (Bearer JWT)
- Bei gültigem Token leitet der PEP die Anfrage an das Backend (PoPP-Server) weiter
- Bei fehlendem oder ungültigem Token antwortet der PEP direkt mit HTTP 401

## Vorbedingungen

* ZETA PEP (ngx_pep Docker Container, Port 2101)
* ZETA PDP (Keycloak Docker Container, Port 2201/2202)
* PoPP Server (Backend hinter PEP)
* Tiger-Proxy

## Architektur-Hinweis

Der `HttpProxyController` des PEP fängt alle Anfragen unter `/**` ab (außer `/service/**` und `/.well-known/**`).
Für die Tests wird der Endpunkt `/v3/api-docs` des PoPP-Servers verwendet, da dieser:
1. Durch den Auth-geschützten `HttpProxyController` geroutet wird
2. Im Backend existiert und mit HTTP 200 antwortet

---

## Szenario 1: PEP akzeptiert gültigen Token und leitet Anfrage an Backend weiter

**Tag:** `@rest_pep_transfer`

```
┌─────────┐         ┌─────────────┐         ┌──────────┐         ┌─────────────┐
│  Test   │         │ Tiger Proxy │         │ ZETA-PEP │         │ PoPP Server │
│ Client  │         │  (localhost)│         │          │         │  (Backend)  │
└────┬────┘         └──────┬──────┘         └────┬─────┘         └──────┬──────┘
     │                     │                     │                      │
     │  1. GET /v3/api-docs                      │                      │
     │  Authorization:     │                     │                      │
     │  Bearer <JWT>       │                     │                      │
     │────────────────────>│                     │                      │
     │                     │  2. Forward to PEP  │                      │
     │                     │────────────────────>│                      │
     │                     │                     │  3. JWT validieren   │
     │                     │                     │     → GÜLTIG ✅      │
     │                     │                     │  4. Anfrage an       │
     │                     │                     │     Backend weiter-  │
     │                     │                     │     leiten           │
     │                     │                     │─────────────────────>│
     │                     │                     │  5. HTTP 200 OK      │
     │                     │                     │<─────────────────────│
     │  6. HTTP 200 OK     │                     │                      │
     │<────────────────────│<────────────────────│                      │
```

### Prüfkriterien

| Kriterium | Erwarteter Wert |
|-----------|-----------------|
| Response Code | 200 |
| Bedeutung | PEP hat Token akzeptiert und Anfrage ans Backend weitergeleitet |

---

## Szenario 2: REST-Anfrage ohne Authorization wird vom PEP abgelehnt

**Tag:** `@rest_pep_transfer`

```
┌─────────┐         ┌─────────────┐         ┌──────────┐
│  Test   │         │ Tiger Proxy │         │ ZETA-PEP │
│ Client  │         │  (localhost)│         │          │
└────┬────┘         └──────┬──────┘         └────┬─────┘
     │                     │                     │
     │  1. GET /v3/api-docs                      │
     │  (KEIN Authorization│                     │
     │   Header!)          │                     │
     │────────────────────>│                     │
     │                     │  2. Forward to PEP  │
     │                     │────────────────────>│
     │                     │                     │  3. Authorization
     │                     │                     │     Header fehlt!
     │  4. HTTP 401        │                     │
     │     Unauthorized    │                     │
     │<────────────────────│<────────────────────│
```

### Prüfkriterien

| Kriterium | Erwarteter Wert |
|-----------|-----------------|
| Response Code | 401 |
| Bedeutung | PEP hat Anfrage ohne Token abgelehnt |

---

## Szenario 3: REST-Anfrage mit ungültigem Token wird vom PEP abgelehnt

**Tag:** `@rest_pep_transfer`

```
┌─────────┐         ┌─────────────┐         ┌──────────┐
│  Test   │         │ Tiger Proxy │         │ ZETA-PEP │
│ Client  │         │  (localhost)│         │          │
└────┬────┘         └──────┬──────┘         └────┬─────┘
     │                     │                     │
     │  1. GET /v3/api-docs                      │
     │  Authorization:     │                     │
     │  Bearer <UNGÜLTIGES │                     │
     │         JWT>        │                     │
     │────────────────────>│                     │
     │                     │  2. Forward to PEP  │
     │                     │────────────────────>│
     │                     │                     │  3. JWT validieren
     │                     │                     │     → UNGÜLTIG! ❌
     │  4. HTTP 401        │                     │
     │     Unauthorized    │                     │
     │<────────────────────│<────────────────────│
```

### Prüfkriterien

| Kriterium | Erwarteter Wert |
|-----------|-----------------|
| Response Code | 401 |
| Bedeutung | PEP hat ungültigen Token erkannt und Anfrage abgelehnt |

---

## Zusammenfassung

| Szenario | Authorization Header | JWT gültig? | Ergebnis |
|----------|---------------------|-------------|----------|
| 1 | ✅ Vorhanden | ✅ Ja | HTTP 200 (Backend antwortet) |
| 2 | ❌ Fehlt | — | HTTP 401 (PEP lehnt ab) |
| 3 | ✅ Vorhanden | ❌ Nein | HTTP 401 (PEP lehnt ab) |

---

## Test ausführen

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@rest_pep_transfer'
```
