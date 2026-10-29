# Client Registrierung – Test Flow Dokumentation

Diese Dokumentation beschreibt die Testszenarien aus `client_registrierung.feature`.

## Übersicht

Das Feature testet den vollständigen ZETA-Protokollablauf gegen die **echte VSDM-ZeTA-Infrastruktur**:
- **PEP**: ngx_pep (Port 9120)
- **PDP**: Keycloak mit ZeTA Extension (Port 9122) + PostgreSQL
- **Ingress**: nginx SSL-Termination (Port 9119)


| #   | Szenario                                                   | Tag                   | Status                                                 |
| --- | ---------------------------------------------------------- | --------------------- | -------------------------------------------------------|
| 1   | Service Discovery – Protected Resource Metadata (RFC 9728) | `@service_discovery`  | ✅ Aktiv                                               |
| 2   | Service Discovery – OAuth AS Metadata (Keycloak)           | `@service_discovery`  | ✅ Aktiv                                               |
| 3   | Dynamic Client Registration (Keycloak + PostgreSQL)        | `@dcr`                | ✅ Aktiv                                               |
| 4   | Token Exchange – SMC-B / brainpoolP256r1                   | `@token_exchange`     | ❌ `@Ignore` – keycloak-zeta unterstützt BP256R1 nicht |
| 5   | Policy-Ablehnung (403 Fehlerfall)                          | `@policy_ablehnungen` | ❌ `@Ignore` – OPA-Routing + Policy fehlen             |
| 6   | SMC-B maxClients Limit und LRU Eviction                    | `@smcb_max_clients`   | ✅ Aktiv                                               |
| 7   | IP-Bindung des Access-Tokens an die Token-Request-IP       | `@ip_bindung`         | ✅ Aktiv (Negativ-Szenario `@local`)                   |

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


| Prüfung                             | Erwarteter Wert                   |
| ----------------------------------- | --------------------------------- |
| HTTP Response Code                  | 201 Created                       |
| `$.body.client_id`                  | vorhanden                         |
| `$.body.client_id_issued_at`        | vorhanden                         |
| `$.body.token_endpoint_auth_method` | `private_key_jwt`                 |
| `$.body.grant_types`                | vorhanden                         |
| `$.body.jwks`                       | vorhanden                         |
| `$.body.redirect_uris`              | vorhanden                         |
| `$.body.registration_client_uri`    | vorhanden                         |
| `$.body.registration_access_token`  | vorhanden                         |
| Schema-Validierung                  | `schemas/v_1_0/dcr-response.yaml` |
| Request Method                      | POST                              |
| Request Content-Type                | `application/json`                |
| Request `client_name`               | `sdk-client`                      |


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

## Szenario 6: SMC-B maxClients Limit und LRU Eviction

**Feature:** `client_registrierung_max_clients.feature`  
**Tags:** `@client_registrierung @smcb_max_clients`

Prüft `SMCB_USER_MAX_CLIENTS` (Default **256**, Helm: `authserver.config.maxClients`; gemSpec **A_25748**).
Das Limit gilt **pro SMC-B-User** auf dem Attribut `zetaguard.smcbuser.client_ids` und wird
in `SMCBIdentityProvider.updateBrokeredUser()` beim **Token-Exchange** geprüft (nicht bei reiner DCR).

Stand ab **ZG 1.3 (A_25748-02)**: "Zu viele Clients" ist der Ausnahmefall, kein Regelfall mehr. Beim
Überschreiten von `SMCB_USER_MAX_CLIENTS` entfernt der Authserver automatisch den am längsten
ungenutzten Client (**LRU-Eviction**) aus der `client_ids`-Liste, und die neue Registrierung bzw.
der Token-Exchange gehen trotzdem durch (**HTTP 2xx**).
Die Tests verifizieren die Eviction über die Keycloak Admin API, mit einer klassischen Grenzwertanalyse:

| TCID                                        | Beispiele                                            | Erwartung                                                                                  |
| -------------------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| `@TCID:ZETA_SMCB_MAX_CLIENTS_BELOW_LIMIT`    | client_ids_count=255, configured_max_client_ids_count=256 | Token-Exchange mit neuem `client_id` → **2xx** + `access_token`, keine Eviction nötig        |
| `@TCID:ZETA_SMCB_MAX_CLIENTS_LIMIT_REACHED`  | client_ids_count=256, configured_max_client_ids_count=256 | Token-Exchange mit neuem `client_id` → **2xx** + `access_token` dank LRU-Eviction; `client_ids`-Liste bleibt ≤ `configured_max_client_ids_count` |

Ein Aufräumschritt (SMC-B User löschen + DCR-Cache leeren) läuft automatisch als `@After`-Hook
(`SmcbMaxClientsSteps.cleanupSmcbUserAfterScenario`), gescoped auf `@smcb_max_clients`.
---

## Szenario 7: IP-Bindung des Access-Tokens an die Token-Request-IP

**Feature:** `client_registrierung_ip_bindung.feature`
**Tags:** `@ip_bindung` (Negativ-Szenario zusätzlich `@local`)

Prüft, dass ein Access-Token nur von der IP-Adresse verwendet werden darf, von der es per
**Token-Exchange** ausgestellt wurde (A_28827/A_28803) — **nicht** von der Registrierungs-IP
(DCR und Token-Exchange sind zwei unabhängige Requests, ggf. von unterschiedlichen IPs).

Geltungsbereich: nur Stufe 1 / stationäre Clients der Rolle LEI. Der erwartete Statuscode ist laut
Tabelle ZT_HTTP_Statuscodes (Kap. 5.8.6) `401` ODER `403` — A_28803 selbst legt keinen konkreten
Code fest.

Die Szenarien nutzen NICHT `ZetaJwtTestFactory` (eigenhändig gebaute DCR/Token-Exchange/DPoP), sondern
den echten `vsdm-client`-Container (Endpoint `/client/vsdm/vsd`): dessen eingebauter ZETA-SDK führt
DCR, Token-Exchange, DPoP-Proof und PoPP-Token vollständig selbst aus und greift anschließend auf den
echten VSDM-ZETA-PEP zu.

Der **Positiv-Fall** (Zugriff von derselben IP, die den Token ausgestellt hat) wird hier bewusst
NICHT dupliziert — er ist bereits durch `ZETA_REST_PEP_TRANSFER_WITH_VALID_ACCESS_TOKEN`
(`zeta-datatransfer-rest/rest_data_transfer_via_pep.feature`) abgedeckt, das denselben echten
VSD-Read über den `vsdm-client` nutzt und ebenfalls `≠ 401` am PEP prüft.

| Szenario                                                 | Ablauf                                                                                                                                                                                                      |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `@TCID:ZETA_TOKEN_IP_BINDING_DIFFERENT_SOURCE_IP_DENIED`   | Erster VSD-Read von IP A (Token wird an IP A gebunden) → Hostname `vsdm-zeta-ingress` wird NUR im `vsdm-client`-Container per `/etc/hosts` auf einen zweiten, unabhängigen Tiger-Proxy-Container mit eigener IP umgeleitet → zweiter VSD-Read (gleicher Access-Token, jetzt von IP B) → `401`/`403` (oder SDK-seitig `500` nach erschöpften Step-Up-Retries, siehe Kommentare im Feature) |

> **Voraussetzung in der lokalen Testumgebung:** Der nginx-basierte PEP (`vsdm-zeta-pep`) muss
> `pep_no_travel on;` gesetzt haben (siehe `doc/docker/backend/zeta/vsdm-pep-conf/nginx.conf`) und
> der Ingress muss die reale Client-IP per `X-Forwarded-For`/`X-Real-IP` durchreichen (siehe
> `doc/docker/backend/zeta/ingress/conf/ingress-nginx.conf`). Für eine echte, abweichende Quell-IP
> wird ein zweiter, unabhängiger `docker-tiger-proxy-b`-Container genutzt (siehe
> `doc/docker/compose-local.yaml`, `TigerProxyRedirectSteps.java`), da alle per Canopy-DNS-Interception
> umgeleiteten Hostnamen sonst strukturell auf dieselbe Proxy-IP aufgelöst würden.
>
> **Sperrung des Access-Tokens (Teil von A_28803) wird hier NICHT geprüft:** `pep_no_travel` lehnt
> nachweislich nur die einzelne Anfrage von der fremden IP ab (zustandslose Prüfung), sperrt den
> Token dabei aber nicht dauerhaft. Eine echte Sperrung würde eine Anbindung des PEP an einen
> proprietären Revocation-Stream des ZeTA-Guard-PDP voraussetzen, dessen Endpoint in dieser
> Testumgebung nicht bekannt/erreichbar ist.

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

# Nur Max SMCB
./mvnw -pl test/zeta-testsuite verify -Dskip.inttests=false -Dzeta.env=local \
  -Dcucumber.filter.tags='@smcb_max_clients'

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
