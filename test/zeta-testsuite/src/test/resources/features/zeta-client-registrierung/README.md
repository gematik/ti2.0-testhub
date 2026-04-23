# Client Registrierung – Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `client_registrierung.feature`.

## Übersicht

Das Feature testet den vollständigen ZETA-Protokollablauf gegen die **echte VSDM-ZeTA-Infrastruktur**:
- **PEP**: ngx_pep (Port 9120)
- **PDP**: Keycloak mit ZeTA Extension (Port 9122) + PostgreSQL
- **Ingress**: nginx SSL-Termination (Port 9119)

| # | Szenario | Tag | Status |
|---|----------|-----|--------|
| 1 | Service Discovery – Protected Resource Metadata (RFC 9728) | `@service_discovery` | ✅ Aktiv |
| 2 | Service Discovery – OAuth AS Metadata (Keycloak) | `@service_discovery` | ✅ Aktiv |
| 3 | Dynamic Client Registration (Keycloak + PostgreSQL) | `@dcr` | ✅ Aktiv |
| 4 | Token Exchange – SMC-B / brainpoolP256r1 | `@token_exchange` | ❌ `@Ignore` – keycloak-zeta unterstützt BP256R1 nicht |
| 5 | Policy-Ablehnung (403 Fehlerfall) | `@policy_ablehnungen` | ❌ `@Ignore` – OPA-Routing + Policy fehlen |

---

## Szenario 1: Service Discovery – Protected Resource Metadata (RFC 9728)

**Tag:** `@client_registrierung @service_discovery`

Testet den ersten Schritt der initialen Client-Registrierung:
Der Test ruft den well-known Endpunkt des **echten PEP** (ngx_pep) auf und erhält ein
RFC-9728 Protected Resource Metadata Dokument.

### Flow

```
┌──────────┐                    ┌──────────────────────────┐
│  Test    │                    │  VSDM ZeTA PEP           │
│  Client  │                    │  ngx_pep (Port 9120)     │
└────┬─────┘                    └──────────────┬───────────┘
     │                                         │
     │  GET /.well-known/                      │
     │    oauth-protected-resource             │
     │────────────────────────────────────────>│
     │                                         │
     │  200 OK                                 │
     │  { resource, authorization_servers, ... }│
     │<────────────────────────────────────────│
```

### Validierung
- HTTP 200 OK
- Body enthält Pflichtfelder: `resource`, `authorization_servers`
- Schema-Validierung gegen `schemas/v_1_0/opr-well-known-rfc9728.yaml`

---

## Szenario 2: Service Discovery – OAuth AS Metadata (Keycloak)

**Tag:** `@client_registrierung @service_discovery`

Testet den OAuth Authorization Server Metadata Endpunkt.
Der Request geht über Ingress (nginx) → PEP → Keycloak.

### Flow

```
┌──────────┐     ┌──────────────────┐     ┌────────────────┐     ┌──────────┐
│  Test    │────>│ VSDM ZeTA Ingress│────>│ VSDM ZeTA PEP  │────>│ Keycloak │
│  Client  │     │ nginx (9119)     │     │ ngx_pep (9120) │     │  (9122)  │
└──────────┘     └──────────────────┘     └────────────────┘     └──────────┘
```

### Validierung
- HTTP 200 OK
- Schema-Validierung gegen `schemas/v_1_0/as-well-known.yaml`
- `jwks_uri` ist ein gültiger HTTP(S)-URI

---

## Szenario 3: Dynamic Client Registration (201 Created)

**Tag:** `@client_registrierung @dcr`

**Kern-Test der DB-Integration.** Direkter POST an den Keycloak-Registrierungsendpunkt (RFC 7591).
Keycloak schreibt die Clientdaten in PostgreSQL → impliziter DB-Integrationstest.

### Flow

```
┌──────────┐     ┌──────────────────────┐     ┌────────────┐
│  Test    │────>│ Keycloak             │────>│ PostgreSQL │
│  Client  │     │ POST /register       │     │            │
└──────────┘     │ (pdpBaseUrl, 9122)   │     │            │
                 └──────────────────────┘     └────────────┘
                       └── schreibt client_id → DB
```

### Validierung

| Prüfung | Erwarteter Wert |
|---------|-----------------|
| HTTP Response Code | 201 Created |
| `$.body.client_id` | vorhanden |
| `$.body.client_id_issued_at` | vorhanden |
| `$.body.token_endpoint_auth_method` | `private_key_jwt` |
| `$.body.grant_types` | vorhanden |
| `$.body.jwks` | vorhanden |
| `$.body.redirect_uris` | vorhanden |
| `$.body.registration_client_uri` | vorhanden |
| `$.body.registration_access_token` | vorhanden |
| Schema-Validierung | `schemas/v_1_0/dcr-response.yaml` |
| Request Method | POST |
| Request Content-Type | `application/json` |
| Request `client_name` | `sdk-client` |

---

## Szenario 4: Token Exchange `@Ignore`

**Tag:** `@Ignore @client_registrierung @token_exchange`

Testet den Token Exchange (RFC 8693) gegen den echten Keycloak mit PoPP-Token.

> **Status: `@Ignore`** – keycloak-zeta (0.4.1) unterstützt brainpoolP256r1 in JWKS/x5c nicht.
> DCR mit SMC-B-Zertifikat liefert HTTP 500.
>
> **Lösung:** Warten auf keycloak-zeta Update mit brainpoolP256r1-Support.

---

## Szenario 5: Policy-Ablehnung (403 Fehlerfall) `@Ignore`

**Tag:** `@Ignore @client_registrierung @policy_ablehnungen`

Testet, dass der ZETA Guard Anfragen mit ungültigen Policy-Werten mit HTTP 403 ablehnt.

> **Status: `@Ignore`** – zwei Voraussetzungen fehlen:
> 1. **Netzwerk-Topologie** – OPA-Requests laufen Docker-intern und durchlaufen nicht den TigerProxy.
> 2. **OPA-Policy** – `authz.rego` gibt aktuell immer `allow=true` zurück.

---

## Tests ausführen

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:

# Alle aktiven client_registrierung Tests
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags='@client_registrierung and not @Ignore'

# Nur Service Discovery
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags='@client_registrierung and @service_discovery'

# Nur DCR
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags='@client_registrierung and @dcr'
```

---

## Voraussetzungen

Alle Tests benötigen den Docker-Stack. Die verfügbaren Docker-Compose-Profile und Startbefehle
sind in der [Testhub-Dokumentation](https://gematik.github.io/ti2.0-testhub/#_docker_compose_profiles) beschrieben.

Insbesondere müssen laufen:
- `vsdm-zeta-ingress` – nginx SSL (Port 9119)
- `vsdm-zeta-pep` – ngx_pep (Port 9120)
- `vsdm-zeta-pdp` – Keycloak (Port 9122)
- `vsdm-zeta-pdp-db` – PostgreSQL
- `popp-token-generator` – PoPP Token Generator (Port 9500, für Token Exchange Tests)
