# ZETA Client-Registrierungs-Policy (Policy-Ablehnungen und -Erlaubnis)

## Beschreibung
Dieser Test prüft, dass der ZeTA-PDP (Keycloak) eine Client-Registrierung
anhand der von OPA (Open Policy Agent) getroffenen Autorisierungsentscheidung
korrekt behandelt: sowohl den Ablehnungsfall (`allow: false` → HTTP 403,
`access_denied` / `policy_denied`, durchgereicht bis zum VSDM-Client) als
auch den Erlaubnisfall (`allow: true` → Registrierung/Token werden erteilt,
die Anfrage dringt bis zum ASL-Handshake mit dem PEP durch).

Im Gegensatz zu `zeta-policy-updateability` (das OPA isoliert testet) prüft
dieser Test den **vollständigen Flow** VSDM-Client → PDP (Keycloak) → OPA →
Entscheidung, mit einer echten, frisch ausgewerteten OPA-Entscheidung.

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

Im Positivfall (`@policy_erlaubnis`) läuft derselbe Ablauf (Schritte 1-8),
die Manipulation setzt das jeweilige Feld dabei aber auf seinen ECHTEN,
gültigen Wert. OPA wertet die Policy dann mit `allow=true` aus (Schritt 9),
der PDP erteilt Registrierung/Token (Schritt 10) und die Anfrage dringt bis
zum ASL-Handshake mit dem PEP durch (HTTP 200 statt 403 in Schritt 11).

## Getestete Ablehnungs- bzw. Erlaubnisgründe
Je als `Szenariogrundriss` mit einer manipulierten OPA-Input-Größe pro
Beispielzeile (siehe `authz.rego`):
- `professionOID`
- `scope`
- `audience`
- `product_id`
- `product_version`

`@policy_ablehnungen` manipuliert jedes Feld auf einen eindeutig ungültigen
(Deny-Listen-)Wert, `@policy_erlaubnis` auf den jeweils echten, gültigen Wert.

## Implementierung
- **Policy**: `doc/docker/backend/zeta/policies/authz.rego` – je eine
  `*_is_allowed`-Regel pro geprüftem Feld, als Deny-Liste umgesetzt (nur die
  synthetischen, im Negativ-Test verwendeten Werte werden geblockt). So bleibt
  die Policy für beliebige echte Werte (z. B. andere SMC-B-professionOIDs)
  durchlässig, statt nur den einen in diesem Test verwendeten Realwert zu
  erlauben.
- **Steps**: `PolicyRejectionSteps.java` (PDP-Registrierung zurücksetzen),
  `TigerProxyManipulationsSteps.java` (Manipulation auf Remote-Proxy
  registrieren), `CardTerminalSteps.java` (Kartenterminal/Karten laden).

## Voraussetzungen
- Docker-Compose-Stack muss laufen: `docker compose -f doc/docker/compose-local.yaml --profile full up -d`
- Docker-CLI/-Socket muss vom Maven-Prozess aus erreichbar sein (für
  `docker restart vsdm-client` in `PolicyRejectionSteps`).

## Hinweis (`@local`)
Diese Szenarien nutzen Docker-spezifische Mechanismen (Keycloak-Admin-API,
`docker restart`, Remote-TigerProxy-Manipulation), die gegen echte/RU-DEV-
Infrastruktur nicht existieren. Sie sind deshalb mit `@local` markiert und
laufen nur gegen den lokalen Docker-Compose-Stack.

## Ausführung
```bash
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_ablehnungen and not @Ignore' -Dzeta.env=local
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_erlaubnis and not @Ignore' -Dzeta.env=local
```
