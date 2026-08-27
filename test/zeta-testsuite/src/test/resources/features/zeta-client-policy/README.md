# ZETA Client-Registrierungs-Policy (Policy-Ablehnungen)

## Beschreibung
Dieser Test prüft, dass der ZeTA-PDP (Keycloak) eine Client-Registrierung
ablehnt, wenn die von OPA (Open Policy Agent) getroffene Autorisierungs-
entscheidung `allow: false` liefert, und dass diese Ablehnung als HTTP 403
(`access_denied` / `policy_denied`) bis zum VSDM-Client durchgereicht wird.

Im Gegensatz zu `zeta-policy-updateability` (das OPA isoliert testet) prüft
dieser Test den **vollständigen Flow** VSDM-Client → PDP (Keycloak) → OPA →
Ablehnung, mit einer echten, frisch ausgewerteten OPA-Entscheidung.

## Mechanismus
Die TigerProxy-`trafficEndpoints`-Relais-Funktion vom Remote-TigerProxy
(`docker-tiger-proxy`) zum lokalen (Test-JVM-)TigerProxy liefert in dieser
Docker-Topologie keine Nachrichten. Daher wird die Manipulation DIREKT auf
der Remote-TigerProxy-Admin-API registriert (`zeta.paths.tigerProxy.baseUrl`
wird hierfür lokal – nur für dieses Szenario – auf den Remote-Admin-Port
umgebogen).

PDP→OPA-Verkehr (Docker-intern) wird per DNS-Interception (Canopy, siehe
`doc/docker/compose-local.yaml` "proxiedHosts" +
`doc/docker/remoteTigerProxy/application.yaml` "proxyRoutes") über
`docker-tiger-proxy` geroutet, damit dieser den Verkehr überhaupt sieht.

Der ZeTA-PDP (Keycloak) befragt OPA nur EINMAL pro SMC-B-Identität, bei der
initialen Client-Registrierung (Dynamic Client Registration, DCR). Danach
wird der registrierte Client dauerhaft wiederverwendet (persistiert in der
Postgres-DB) und der VSDM-Client hält zudem ein gültiges Access-/Refresh-
Token im Speicher – beides umgeht OPA komplett bei Folgeanfragen. Um bei
JEDEM Testlauf eine echte, frische OPA-Entscheidung zu erzwingen, wird
deshalb vor der Anfrage die PDP-Registrierung vollständig zurückgesetzt
(siehe `PolicyRejectionSteps`): der DCR-Client wird über die
Keycloak-Admin-API gelöscht und der `vsdm-client`-Container neu gestartet
(löscht dessen In-Memory-Token-Cache).

### Flow-Diagramm

```
 ┌────────────┐    ┌────────────┐    ┌──────────────┐    ┌──────────────────┐    ┌────────────────┐
 │   Test /   │    │   vsdm-    │    │   ZeTA-PDP   │    │  docker-tiger-   │    │ vsdm-zeta-pdp- │
 │   Tiger    │    │   client   │    │  (Keycloak)  │    │  proxy (Remote)  │    │   opa (OPA)    │
 └────────────┘    └────────────┘    └──────────────┘    └──────────────────┘    └────────────────┘
        │                 │                  │                     │                      │
        │ 1. PDP-Registrierung zurücksetzen  │                     │                      │
        │    (Keycloak-Admin-API: DCR-Client │                     │                      │
        │    löschen)     │                  │                     │                      │
        │────────────────────────────────────>                     │                      │
        │                 │                  │                     │                      │
        │ 2. docker restart                  │                     │                      │
        │    vsdm-client  │                  │                     │                      │
        │    (In-Memory-Token-Cache          │                     │                      │
        │    leeren)      │                  │                     │                      │
        │─────────────────>                  │                     │                      │
        │                 │                  │                     │                      │
        │ 3. Manipulation direkt auf Remote-Proxy-Admin-API        │                      │
        │    registrieren: Feld <OpaInputField> -> <NeuerWert>, nur│                      │
        │    für "authz/decision"-Requests   │                     │                      │
        │──────────────────────────────────────────────────────────>                      │
        │                 │                  │                     │                      │
        │ 4.              │                  │                     │                      │
        │    Kartenterminal/Karten           │                     │                      │
        │    (neu) konfigurieren             │                     │                      │
        │─────────────────>                  │                     │                      │
        │                 │                  │                     │                      │
        │ 5. GET          │                  │                     │                      │
        │    /client/vsdm/vsd                │                     │                      │
        │    (VSD-Anfrage │                  │                     │                      │
        │    auslösen)    │                  │                     │                      │
        │─────────────────>                  │                     │                      │
        │                 │                  │                     │                      │
        │                 │ 6. Dynamic Client│                     │                      │
        │                 │    Registration (DCR),                 │                      │
        │                 │    da kein bestehender                 │                      │
        │                 │    Client mehr   │                     │                      │
        │                 │    existiert     │                     │                      │
        │                 │──────────────────>                     │                      │
        │                 │                  │                     │                      │
        │                 │                  │ 7. POST             │                      │
        │                 │                  │    /v1/data/zeta/authz/decision            │
        │                 │                  │    (PDP -> OPA,     │                      │
        │                 │                  │    DNS-umgeleitet via                      │
        │                 │                  │    Canopy)          │                      │
        │                 │                  │─────────────────────>                      │
        │                 │                  │                     │                      │
        │                 │                  │                     │ 8. Manipulation      │
        │                 │                  │                     │    greift, Feld wird │
        │                 │                  │                     │    ersetzt           │
        │                 │                  │                     │──────────────────────>
        │                 │                  │                     │                      │
        │                 │                  │                     │                      │ 9. OPA wertet
        │                 │                  │                     │                      │    Policy aus:
        │                 │                  │                     │                      │    allow=false
        │                 │                  │                     │                      │
        │                 │                  <─────────────────────<──────────────────────│
        │                 │                  │                     │                      │
        │                 │ 10.              │                     │                      │
        │                 │    decision.allow=false                │                      │
        │                 │    ->            │                     │                      │
        │                 │    Registrierung/Token                 │                      │
        │                 │    wird verweigert                     │                      │
        │                 <──────────────────│                     │                      │
        │                 │                  │                     │                      │
        │ 11. HTTP 403    │                  │                     │                      │
        │    {"error":"access_denied",       │                     │                      │
        │    "error_description":"policy_denied"}                  │                      │
        <─────────────────│                  │                     │                      │
        │                 │                  │                     │                      │
```


## Getestete Ablehnungsgründe
Als `Szenariogrundriss` mit je einer manipulierten OPA-Input-Größe pro
Beispielzeile (siehe `authz.rego`):
- ungültige `professionOID`
- ungültiger `scope`
- ungültige `audience`
- ungültige `product_id`
- ungültige `product_version`

## Implementierung
- **Policy**: `doc/docker/backend/zeta/policies/authz.rego` – je eine
  `*_is_allowed`-Regel pro geprüftem Feld, mit Allow-Liste des jeweils
  echten Werts (regressionssicher: echter Traffic wird weiterhin erlaubt).
- **Steps**: `PolicyRejectionSteps.java` (PDP-Registrierung zurücksetzen),
  `TigerProxyManipulationsSteps.java` (Manipulation auf Remote-Proxy
  registrieren), `CardTerminalSteps.java` (Kartenterminal/Karten laden).
- **Testdaten**: `tiger/testdata.yaml` (`testdata.policy_rejection.*`).

## Voraussetzungen
- Docker-Compose-Stack muss laufen: `docker compose -f doc/docker/compose-local.yaml --profile full up -d`
- Docker-CLI/-Socket muss vom Maven-Prozess aus erreichbar sein (für
  `docker restart vsdm-client` in `PolicyRejectionSteps`).

## Hinweis (`@local`)
Dieses Szenario nutzt Docker-spezifische Mechanismen (Keycloak-Admin-API,
`docker restart`, Remote-TigerProxy-Manipulation), die gegen echte/RU-DEV-
Infrastruktur nicht existieren. Es ist deshalb mit `@local` markiert und
läuft nur gegen den lokalen Docker-Compose-Stack.

## Ausführung
```bash
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_ablehnungen and not @Ignore' -Dzeta.env=local
```
