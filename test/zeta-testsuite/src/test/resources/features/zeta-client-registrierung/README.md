# Client Registrierung - Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `client_registrierung.feature`.

## Übersicht

Das Feature testet die **initiale Client-Registrierung** und den **Token-Exchange** am ZETA-PDP:
- Service Discovery über den well-known Endpunkt des PEP-Mocks
- Prüfung eines **Mock-well-known-Dokuments im Authorization-Server-Metadata-Stil**
  (ähnlich RFC 8414, nicht strikt RFC 9728)
- Dynamic Client Registration (DCR) am PDP (RFC 7591)
- Token-Exchange-Request an den ZETA-PDP (RFC 8693)
- Vollständiger Registrierungsflow (Service Discovery + DCR + Token Exchange)
- Prüfung der Policy-Entscheidung (OPA)
- Gutfall: Access-Token wird ausgestellt
- Fehlerfall: Zugriff wird mit 403 abgelehnt (noch nicht implementiert)

## Vorbedienungen

* ZETA PDP Server Mockservice
* Tiger-Proxy

---

## Szenario 1: Service Discovery - mock well-known AS-Metadata

**Tag:** `@staging @client_registrierung @service_discovery`

Testet den ersten Schritt der initialen Client-Registrierung:
Der ZETA Client ruft den well-known Endpunkt des Resource Servers (PEP-Mock, Port 9110)
auf und erhält aktuell ein **Authorization-Server-Metadata-ähnliches Dokument**.
Der Mock verwendet zwar den Pfad `/.well-known/oauth-protected-resource`, liefert aber
kein striktes RFC-9728 Protected Resource Metadata Dokument.

### Flow

```
┌──────────┐                    ┌──────────────┐
│  Test    │                    │  ZETA-PEP    │
│  Client  │                    │  (Mock:9110) │
└────┬─────┘                    └──────┬───────┘
     │                                 │
     │  GET /.well-known/              │
     │    oauth-protected-resource     │
     │────────────────────────────────>│
     │                                 │
     │  200 OK                         │
     │  { issuer, token_endpoint, ...} │
     │<────────────────────────────────│
     │                                 │
     │  Schema-Validierung             │
     │  (opr-well-known.yaml)          │
```

### Validierung
- HTTP 200 OK
- Body enthält `issuer` und `token_endpoint`
- Schema-Validierung gegen `schemas/v_1_0/opr-well-known.yaml`
- Das Schema validiert die **tatsächliche Mock-Response**, nicht ein striktes RFC-9728 Dokument

---

## Szenario 2: Dynamic Client Registration (DCR)

**Tag:** `@staging @client_registrierung @dcr`

Testet die Dynamic Client Registration (RFC 7591):
Der ZETA Client sendet einen DCR-Request (`POST /register`) an den PDP Authorization Server (Mock, Port 9112).

### Flow

```
┌──────────┐                    ┌──────────────┐
│  Test    │                    │  ZETA-PDP    │
│  Client  │                    │  (Mock:9112) │
└────┬─────┘                    └──────┬───────┘
     │                                 │
     │  POST /register                 │
     │  Content-Type: application/json │
     │  {                              │
     │    "client_name": "sdk-client", │
     │    "grant_types": [...],        │
     │    "jwks": { "keys": [...] },   │
     │    "token_endpoint_auth_method":│
     │      "private_key_jwt"          │
     │  }                              │
     │────────────────────────────────>│
     │                                 │
     │  201 Created                    │
     │  {                              │
     │    "client_id": "...",          │
     │    "client_id_issued_at": ...,  │
     │    "registration_client_uri",   │
     │    "registration_access_token"  │
     │  }                              │
     │<────────────────────────────────│
```

### Validierung
- HTTP 201 Created
- Response enthält: `client_id`, `client_id_issued_at`, `token_endpoint_auth_method`,
  `grant_types`, `jwks`, `redirect_uris`, `registration_client_uri`, `registration_access_token`
- Schema-Validierung gegen `schemas/v_1_0/dcr-response.yaml`
- Request-Validierung: POST-Methode, Content-Type `application/json`, Body-Felder

---

| Szenario | Tag | Status | Prüfung |
|----------|-----|--------|---------|
| Service Discovery (well-known) | `@service_discovery` | ✅ Aktiv | PEP Mock-well-known, Schema-Validierung der AS-Metadata-artigen Response |
| Dynamic Client Registration | `@dcr` | ✅ Aktiv | POST /register → 201, Response-Felder, Schema-Validierung |
| Gutfall (Token-Exchange) | `@token_exchange` | ✅ Aktiv | PDP erreichbar, Token wird ausgestellt, TGR-Validierung |
| Fehlerfall (Policy-Ablehnung) | `@no_proxy` | ❌ Ignoriert | Benötigt echte OPA-Policy |

**Hinweis:** Für echte Policy-Tests muss die `authz.rego` im PDP-Mock durch eine Policy ersetzt werden, die die Input-Werte validiert.

**Hinweis (RFC 9728):** Der PEP-Mock gibt am Endpunkt `/.well-known/oauth-protected-resource`
aktuell ein Authorization-Server-Metadata-Dokument (Stil RFC 8414) zurück, nicht ein echtes
RFC 9728 Protected Resource Metadata Dokument. Das Schema `opr-well-known.yaml` validiert
gegen die tatsächliche Mock-Response.
