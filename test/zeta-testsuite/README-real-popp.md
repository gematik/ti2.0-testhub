# ZETA-Testsuite gegen echten PoPP-Server ausführen

Diese Anleitung beschreibt, wie die ZETA-Testsuite gegen den echten PoPP-Server `popp.dev.poppservice.de` ausgeführt wird.

Dabei wird nicht der lokale Docker-Mock für PoPP/PDP verwendet. Stattdessen laufen die lokalen Testkomponenten gegen die RU-DEV-Umgebung des echten PoPP-Servers.

## Ziel

Die Testsuite soll lokal gestartet werden, aber folgende externe Komponenten verwenden:

- echten PoPP-Server (inkl. Token-Generierung)
- echten PDP / Keycloak-Realm
- lokale PEP-/OPA-/Test-Infrastruktur aus Docker

## Zielumgebung

| Komponente | URL |
|---|---|
| PoPP Server REST | `https://popp.dev.poppservice.de` |
| PoPP Token Generation WSS | `wss://popp.dev.poppservice.de:443/popp/practitioner/api/v1/token-generation-ehc` |
| PDP / Keycloak Realm | `https://popp.dev.poppservice.de/auth/realms/zeta-guard` |
| PDP Token Endpoint | `https://popp.dev.poppservice.de/auth/realms/zeta-guard/protocol/openid-connect/token` |
| PDP JWKS | `https://popp.dev.poppservice.de/auth/realms/zeta-guard/protocol/openid-connect/certs` |
| PDP DCR | `https://popp.dev.poppservice.de/auth/realms/zeta-guard/clients-registrations/openid-connect` |
| PDP Nonce | `https://popp.dev.poppservice.de/auth/realms/zeta-guard/zeta-guard-nonce` |

Discovery-Endpoints:

```text
https://popp.dev.poppservice.de/.well-known/oauth-protected-resource
https://popp.dev.poppservice.de/.well-known/openid-federation
https://popp.dev.poppservice.de/auth/realms/zeta-guard/.well-known/openid-configuration
```

## Umgebung umschalten (zentraler Schalter)

> **TL;DR:** Es gibt **genau eine** Stelle, um die Zielumgebung der Testsuite zu wählen: die Variable **`zeta.env`**. Alle URLs im Test-Framework leiten sich automatisch daraus ab. URLs sind weder in Java noch in den Feature-Dateien hardcodiert.

Unterstützte Werte:

| `zeta.env`   | Bedeutung |
|--------------|---|
| `local`      | Lokaler Docker-Mock (PDP/Keycloak und PoPP-Server laufen lokal) — **Default** |
| `popp-rudev` | Echter PoPP-Server `popp.dev.poppservice.de` (RU-DEV) |

Umschalten (höchste Priorität zuerst):

```bash
# 1. System-Property beim Testlauf
./mvnw -pl test/zeta-testsuite verify -Dzeta.env=popp-rudev

# 2. Umgebungsvariable
export ZETA_ENV=popp-rudev

# 3. Default in tiger/zeta-environments.yaml (Schlüssel: zeta.env)
```

Die gesamte Konfiguration liegt zentral in **`tiger/zeta-environments.yaml`**:

```yaml
zeta:
  env: "${ZETA_ENV|local}"          # <-- der einzige Schalter

  environments:
    local:
      pepUrl: "http://${ports.host}:${ports.poppPepPort}"
      poppClientUrl: "http://${ports.host}:${ports.poppClientPort}"
      pdpRealmUrl: "http://${ports.host}:${ports.poppPdpPort}/auth/realms/zeta-guard"
      pdpIssuer: "https://${ports.host}:${ports.poppZetaIngressPort}/auth/realms/zeta-guard"
      smcbAudience: "https://popp-zeta-ingress/auth/realms/zeta-guard/protocol/openid-connect/token"
      # ...
    popp-rudev:
      pepUrl: "https://popp.dev.poppservice.de"
      poppClientUrl: "http://${ports.host}:${ports.poppClientPort}"
      pdpRealmUrl: "https://popp.dev.poppservice.de/auth/realms/zeta-guard"
      pdpIssuer: "https://popp.dev.poppservice.de/auth/realms/zeta-guard"
      smcbAudience: "https://popp.dev.poppservice.de/auth/realms/zeta-guard/protocol/openid-connect/token"
      # ...

  # Abgeleitete Werte – NICHT editieren:
  server:
    pdp:
      tokenUrl: "${zeta.environments.${zeta.env}.pdpRealmUrl}/protocol/openid-connect/token"
      issuer:   "${zeta.environments.${zeta.env}.pdpIssuer}"
      dcrUrl:   "${zeta.environments.${zeta.env}.pdpRealmUrl}/clients-registrations/openid-connect"
      # ... jwksUrl, nonceUrl, smcbAudience
    pep:
      url: "${zeta.environments.${zeta.env}.pepUrl}"
```

Eine **weitere Umgebung** (z. B. `tu`, `staging`) ergänzt man, indem man einen neuen Block unter `zeta.environments` in `tiger/zeta-environments.yaml` einfügt und `zeta.env` auf dessen Namen setzt — keine Java- oder Feature-Datei muss angefasst werden.

### Zugriff aus dem Java-Code

Java-Klassen lesen die abgeleiteten Werte über die zentrale Klasse `PoPpConfig` (typisierte, lazy aufgelöste Getter) bzw. über Tiger-Platzhalter:

```text
test/zeta-testsuite/src/test/java/de/gematik/zeta/config/PoPpConfig.java
```

```java
PoPpConfig.tokenUrl();       // zeta.server.pdp.tokenUrl
PoPpConfig.issuer();         // zeta.server.pdp.issuer
PoPpConfig.dcrUrl();         // zeta.server.pdp.dcrUrl
PoPpConfig.nonceUrl();       // zeta.server.pdp.nonceUrl
PoPpConfig.jwksUrl();        // zeta.server.pdp.jwksUrl
PoPpConfig.smcbAudience();   // zeta.server.pdp.smcbAudience
PoPpConfig.poppClientUrl();  // zeta.server.poppClient.url
PoPpConfig.pepUrl();         // zeta.server.pep.url
```

`PoPpConfig` hält selbst **keine** URLs — es liest ausschließlich die `zeta.server.*`-Schlüssel, die aus `tiger/zeta-environments.yaml` abgeleitet werden. Damit gibt es eine einzige Quelle der Wahrheit, und Tiger berücksichtigt automatisch System-Properties und Umgebungsvariablen.

### Docker-Infrastruktur (separat vom `zeta.env`-Schalter)

Die lokale PEP-/nginx-Infrastruktur in Docker wird **nicht** über `zeta.env` gesteuert, sondern über die Umgebungsvariable `POPP_SERVER_HOST` in der docker-compose-Datei (siehe Abschnitte unten). Beim Wechsel der Zielumgebung müssen daher zusätzlich die Docker-Variablen passend gesetzt werden:

```text
doc/docker/backend/compose-popp-services.yaml
  └─ POPP_SERVER_HOST, POPP_SERVER_URL

doc/docker/backend/zeta-popp/pep/nginx.conf
  └─ pep_popp_issuer, pep_pdp_issuer

doc/docker/backend/zeta-popp/pep/conf/50-pep.conf.template
  └─ proxy_pass https://${POPP_SERVER_HOST}/...
```

### Verhalten der Java-Klassen (nun konfigurationsgesteuert)

**ZetaPepJwtTestFactory.java:**
- DCR-, Token-, JWKS-, Nonce-Endpunkt, Issuer und SMC-B-Audience werden über `PoPpConfig` / `${zeta.server.pdp.*}` aus dem aktiven `zeta.env`-Block gelesen.
- Token Exchange Scope: `popp` (ohne `audience`-Parameter).
- Logik: Dynamische Client Registration (DCR) statt vordefinierter Test-Clients.

**ZetaPepJwtSteps.java:**
- PoPP-Client URL aus `${zeta.server.poppClient.url}`, Token-Endpunkt `POST <poppClient>/token`.
- Logik: Ruft den lokalen PoPP-Client auf (nicht den lokalen Mock-Generator).

**SmcbTokenExchangeSteps.java:**
- SMC-B-Audience aus `PoPpConfig.smcbAudience()`, Token-/Nonce-Endpunkt aus dem aufgelösten Ziel.
- Token Exchange Scope: `popp`, kein expliziter `audience`-Parameter.
- Logik: Nonce wird über denselben Proxy-Pfad geholt wie der Token Exchange (gleiche Backend-Replica); bei Nonce-Problemen wird eine frische Nonce geholt und erneut versucht.

### PoPP-Server / PoPP Backend

Der echte PoPP-Server wird vom lokalen PEP als Upstream verwendet.

Betroffene Dateien:

```text
doc/docker/backend/compose-popp-services.yaml
doc/docker/backend/zeta-popp/pep/conf/50-pep.conf.template
doc/docker/backend/zeta-popp/pep/nginx.conf
```

In `compose-popp-services.yaml` wird der Host für den PEP gesetzt:

```yaml
POPP_SERVER_HOST=popp.dev.poppservice.de:443
```

Im PEP-Template wird dieser Host per HTTPS verwendet:

```nginx
proxy_pass https://${POPP_SERVER_HOST}/ws;
proxy_pass https://${POPP_SERVER_HOST}/;
```

In der PEP-Nginx-Konfiguration muss der PoPP-Issuer auf den echten Server zeigen:

```nginx
pep_popp_issuer https://popp.dev.poppservice.de;
```

### PoPP Token Generation / WebSocket

Die PoPP-Token-Erzeugung läuft über den lokalen PoPP-Client. Der PoPP-Client verbindet sich per WebSocket mit dem echten PoPP-Server.

Betroffene Datei:

```text
doc/docker/backend/compose-popp-services.yaml
```

Im Service `popp-client` wird die WebSocket-URL gesetzt:

```yaml
POPP_SERVER_URL=wss://popp.dev.poppservice.de:443/popp/practitioner/api/v1/token-generation-ehc
```

Falls der PoPP-Client den Pfad `/api/v1/token-generation-ehc` intern selbst ergänzt, kann stattdessen die Basis-URL verwendet werden:

```yaml
POPP_SERVER_URL=wss://popp.dev.poppservice.de:443/popp/practitioner
```

Wichtig: Die konfigurierte URL muss zum Verhalten des PoPP-Clients passen. Es darf nicht gleichzeitig der vollständige Pfad konfiguriert und zusätzlich intern noch einmal angehängt werden.

### SMC-B-Zertifikat für den PoPP-Client

Der PoPP-Client benötigt ein SMC-B-Zertifikat, um die Token-Erzeugung gegen die RU-DEV-Umgebung durchführen zu können.

Betroffene Datei:

```text
doc/docker/backend/compose-popp-services.yaml
```

Im Service `popp-client` wird die PKCS#12-Datei gesetzt:

```yaml
ZETA_AUTHENTICATION_SMB_KEYFILE=/app/smcb_private.p12
```

Falls bewusst eine andere Datei verwendet wird, z. B. ein ausgetauschtes oder neues Testzertifikat, muss der Pfad entsprechend angepasst werden:

```yaml
ZETA_AUTHENTICATION_SMB_KEYFILE=/app/<dateiname>.p12
```

Die Datei muss im Container unter dem angegebenen Pfad verfügbar sein.

### PDP / Keycloak Realm

Der lokale PDP wird durch den echten Keycloak-Realm der RU-DEV-Umgebung ersetzt.

Betroffene Dateien:

```text
tiger/defaults.yaml
doc/docker/backend/zeta-popp/pep/nginx.conf
test/zeta-testsuite/src/test/java/de/gematik/zeta/services/ZetaPepJwtTestFactory.java
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/SmcbTokenExchangeSteps.java
```

In `tiger/defaults.yaml` werden Token-Endpoint und Issuer gesetzt:

```yaml
pdp.tokenUrl: https://popp.dev.poppservice.de/auth/realms/zeta-guard/protocol/openid-connect/token
pdp.issuer: https://popp.dev.poppservice.de/auth/realms/zeta-guard
```

In der PEP-Nginx-Konfiguration muss der PDP-Issuer auf den echten Realm zeigen:

```nginx
pep_pdp_issuer https://popp.dev.poppservice.de/auth/realms/zeta-guard;
```

In den Testklassen müssen alle PDP-bezogenen URLs ebenfalls auf diesen Realm zeigen, insbesondere:

```text
https://popp.dev.poppservice.de/auth/realms/zeta-guard
```

Daraus ergeben sich die folgenden Endpunkte:

```text
Token Endpoint:
https://popp.dev.poppservice.de/auth/realms/zeta-guard/protocol/openid-connect/token

JWKS:
https://popp.dev.poppservice.de/auth/realms/zeta-guard/protocol/openid-connect/certs

DCR:
https://popp.dev.poppservice.de/auth/realms/zeta-guard/clients-registrations/openid-connect

Nonce:
https://popp.dev.poppservice.de/auth/realms/zeta-guard/zeta-guard-nonce
```

### Dynamic Client Registration

Für den echten PDP wird kein hardcodierter lokaler Test-Client verwendet. Stattdessen registriert die Testsuite dynamisch einen Client per DCR.

Betroffene Datei:

```text
test/zeta-testsuite/src/test/java/de/gematik/zeta/services/ZetaPepJwtTestFactory.java
```

Der DCR-Endpunkt muss auf den echten PDP zeigen:

```text
https://popp.dev.poppservice.de/auth/realms/zeta-guard/clients-registrations/openid-connect
```

Die daraus erhaltene `client_id` muss anschließend für den Token Exchange verwendet werden.

### Token Exchange

Der SMC-B Token Exchange läuft gegen den echten PDP-Token-Endpoint.

Betroffene Dateien:

```text
test/zeta-testsuite/src/test/java/de/gematik/zeta/services/ZetaPepJwtTestFactory.java
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/SmcbTokenExchangeSteps.java
tiger/defaults.yaml
```

Der Token-Endpoint lautet:

```text
https://popp.dev.poppservice.de/auth/realms/zeta-guard/protocol/openid-connect/token
```

Der Token-Exchange-Request muss den Scope `popp` enthalten:

```text
scope=popp
```

Ein expliziter `audience`-Parameter wird nicht gesetzt.

### Nonce-Endpoint

Für den Token Exchange wird eine frische Nonce vom echten PDP benötigt.

Betroffene Datei:

```text
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/SmcbTokenExchangeSteps.java
```

Der Nonce-Endpunkt lautet:

```text
https://popp.dev.poppservice.de/auth/realms/zeta-guard/zeta-guard-nonce
```

Die Nonce sollte über denselben Request-/Proxy-Pfad geholt werden wie der anschließende Token Exchange. Dadurch wird vermieden, dass Nonce-Ausstellung und Nonce-Validierung auf unterschiedlichen Backend-Replicas landen.

### Lokaler PoPP-Client

Die Testsuite spricht nicht direkt den WebSocket-Endpunkt des echten PoPP-Servers an. Stattdessen ruft sie lokal den PoPP-Client auf.

Betroffene Dateien:

```text
tiger/defaults.yaml
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/ZetaPepJwtSteps.java
```

In `tiger/defaults.yaml` wird die lokale PoPP-Client-URL gesetzt:

```yaml
zeta.server.poppClient.url: http://127.0.0.1:18081
```

Die Testklasse ruft anschließend lokal den PoPP-Client auf:

```text
POST http://127.0.0.1:18081/token
```

Der PoPP-Client übernimmt danach die Kommunikation mit dem echten PoPP-Server per WebSocket.

### Lokaler PEP

Der lokale PEP bleibt Bestandteil der Docker-Testumgebung. Er validiert Tokens und leitet Requests an den echten PoPP-Server weiter.

Betroffene Dateien:

```text
doc/docker/backend/zeta-popp/pep/nginx.conf
doc/docker/backend/zeta-popp/pep/conf/50-pep.conf.template
doc/docker/backend/compose-popp-services.yaml
```

Der PEP verwendet:

```text
PoPP-Issuer:
https://popp.dev.poppservice.de

PDP-Issuer:
https://popp.dev.poppservice.de/auth/realms/zeta-guard

PoPP-Upstream:
popp.dev.poppservice.de:443
```

### Service Discovery

Die Service-Discovery-Tests müssen gegen die echten Discovery-Endpunkte laufen.

Betroffene Dateien:

```text
tiger/defaults.yaml
test/zeta-testsuite/src/test/java/de/gematik/zeta/services/ZetaPepJwtTestFactory.java
```

Relevante Endpunkte:

```text
https://popp.dev.poppservice.de/.well-known/oauth-protected-resource
https://popp.dev.poppservice.de/.well-known/openid-federation
https://popp.dev.poppservice.de/auth/realms/zeta-guard/.well-known/openid-configuration
```

## Architektur

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                        LOKALE UMGEBUNG (Docker)                              │
│                                                                              │
│  ┌──────────────┐                               ┌─────────────────────────┐  │
│  │              │  PoPP-Token holen             │ PoPP-Client Container   │  │
│  │              │ ─────────────────────────►    │ Port 18081              │  │
│  │              │  POST /token                  │ WSS zum echten PoPP     │  │
│  │  ZETA-Test   │                               │ nutzt virtuelle eGK und │  │
│  │  JUnit /     │ ◄─────────────────────────    │ SMC-B                   │  │
│  │  Cucumber    │  PoPP-Token                   └─────────────────────────┘  │
│  │              │                                                            │
│  │              │  Request an PEP               ┌─────────────────────────┐  │
│  │              │ ─────────────────────────►    │ PEP nginx, Port 2101    │  │
│  │              │  Authorization: Bearer <AT>   │                         │  │
│  │              │  PoPP: <popp-token>           │ validiert Access-Token  │  │
│  │              │                               │ gegen echten PDP/JWKS   │  │
│  │              │                               │                         │  │
│  │              │                               │ validiert PoPP-Token    │  │
│  │              │                               │ gegen echten PoPP       │  │
│  └──────┬───────┘                               └────────────┬────────────┘  │
└─────────┼────────────────────────────────────────────────────┼───────────────┘
          │                                                    │
          │ DCR, Nonce und Token Exchange                      │ proxy_pass
          │ gegen echten PDP                                   │ zum Backend
          │                                                    │
          ▼                                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                ECHTER PoPP-SERVER popp.dev.poppservice.de                   │
│                                                                             │
│  ┌────────────────────┐  ┌────────────────────┐  ┌───────────────────────┐  │
│  │ Keycloak PDP       │  │ PoPP Backend       │  │ PoPP Token Gen WSS    │  │
│  │ /auth/realms/      │  │                    │  │ /popp/practitioner/   │  │
│  │ zeta-guard         │  │ empfängt Requests  │  │ api/v1/token-         │  │
│  │                    │  │ mit ZETA-User-Info │  │ generation-ehc        │  │
│  │ - DCR              │  │                    │  │                       │  │
│  │ - Token Exchange   │  │                    │  │ PoPP-Client verbindet │  │
│  │ - JWKS             │  │                    │  │ sich hierher          │  │
│  │ - Nonce            │  │                    │  │                       │  │
│  └────────────────────┘  └────────────────────┘  └───────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Geänderte Dateien

Für die Ausführung gegen den echten PoPP-Server werden folgende Dateien angepasst:

```text
doc/docker/backend/zeta-popp/pep/nginx.conf
doc/docker/backend/zeta-popp/pep/conf/50-pep.conf.template
doc/docker/backend/compose-popp-services.yaml
tiger/defaults.yaml
test/zeta-testsuite/src/test/java/de/gematik/zeta/config/PoPpConfig.java
test/zeta-testsuite/src/test/java/de/gematik/zeta/services/ZetaPepJwtTestFactory.java
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/ZetaPepJwtSteps.java
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/SmcbTokenExchangeSteps.java
```

## Anpassungen im Detail

### PEP Nginx-Konfiguration

Datei:

```text
doc/docker/backend/zeta-popp/pep/nginx.conf
```

Die lokalen Mock-Issuer werden durch die echten RU-DEV-Issuer ersetzt:

```diff
- pep_popp_issuer http://popp-server:8443;
+ pep_popp_issuer https://popp.dev.poppservice.de;

- pep_pdp_issuer http://popp-zeta-pdp:8080/realms/zeta-guard;
+ pep_pdp_issuer https://popp.dev.poppservice.de/auth/realms/zeta-guard;
```

### PEP Server-Template

Datei:

```text
doc/docker/backend/zeta-popp/pep/conf/50-pep.conf.template
```

Die Proxy-Ziele werden von HTTP auf HTTPS umgestellt:

```diff
- proxy_pass http://${POPP_SERVER_HOST}/ws;
+ proxy_pass https://${POPP_SERVER_HOST}/ws;

- proxy_pass http://${POPP_SERVER_HOST}/;
+ proxy_pass https://${POPP_SERVER_HOST}/;
```

### Docker Compose

Datei:

```text
doc/docker/backend/compose-popp-services.yaml
```

Der PEP zeigt auf den echten PoPP-Server:

```diff
- POPP_SERVER_HOST=popp-server:8443
+ POPP_SERVER_HOST=popp.dev.poppservice.de:443
```

Der PoPP-Client verwendet den echten WebSocket-Endpunkt oder die passende Basis-URL, abhängig vom Verhalten des PoPP-Clients:

```diff
- POPP_SERVER_URL=wss://popp-zeta-ingress:443/ws
+ POPP_SERVER_URL=wss://popp.dev.poppservice.de:443/popp/practitioner/api/v1/token-generation-ehc
```

Falls der PoPP-Client den Pfad intern selbst ergänzt:

```diff
- POPP_SERVER_URL=wss://popp-zeta-ingress:443/ws
+ POPP_SERVER_URL=wss://popp.dev.poppservice.de:443/popp/practitioner
```

Das vom PoPP-Client verwendete SMC-B-Zertifikat wird ebenfalls in dieser Datei konfiguriert:

```yaml
ZETA_AUTHENTICATION_SMB_KEYFILE=/app/smcb_private.p12
```

Falls bewusst eine andere Datei verwendet wird:

```yaml
ZETA_AUTHENTICATION_SMB_KEYFILE=/app/<dateiname>.p12
```

### Tiger-Konfiguration

Datei:

```text
tiger/defaults.yaml
```

Die Token-/Issuer-/PoPP-Client-URLs werden **nicht mehr direkt** editiert, sondern automatisch aus dem aktiven `zeta.environments.<zeta.env>`-Block abgeleitet. Zum Umschalten auf RU-DEV genügt:

```bash
./mvnw -pl test/zeta-testsuite verify -Dzeta.env=rudev
```

(`rudev` ist bereits der Default.) Für den lokalen Mock entsprechend `-Dzeta.env=local`. Siehe Abschnitt [Umgebung umschalten (zentraler Schalter)](#umgebung-umschalten-zentraler-schalter).

### ZetaPepJwtTestFactory

Datei:

```text
test/zeta-testsuite/src/test/java/de/gematik/zeta/services/ZetaPepJwtTestFactory.java
```

Die Factory liest alle Endpunkte aus der zentralen Konfiguration:

- PDP-Ziel `PdpTarget.POPP` hält **keine** URLs mehr; DCR-, Token-, Issuer- und SMC-B-Audience-Werte kommen aus `PoPpConfig` (`${zeta.server.pdp.*}`) und damit aus dem aktiven `zeta.env`-Block.
- Clients werden per Dynamic Client Registration registriert.
- Es wird kein lokal administrierter Keycloak mehr vorbereitet (`setupKeycloak()` ist ungenutzter Alt-Code für den lokalen Mock und wird im aktiven Flow nicht aufgerufen).
- Client Assertions verwenden den bei der DCR registrierten Schlüssel.
- Der Token Exchange sendet `scope=popp`.
- `sub` und `professionOid` werden dynamisch aus dem SMC-B-Zertifikat gelesen.
- Für Requests gegen entfernte Umgebungen wird ein passender TLS-Kontext gesetzt.

### ZetaPepJwtSteps

Datei:

```text
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/ZetaPepJwtSteps.java
```

Die PoPP-Token-Erzeugung verwendet nicht mehr den lokalen Mock-Generator.

Stattdessen ruft `generatePoppToken()` den lokalen PoPP-Client auf:

```text
POST http://127.0.0.1:18081/token
```

Der lokale PoPP-Client stellt anschließend die WebSocket-Verbindung zum echten PoPP-Server her und erzeugt darüber ein echtes PoPP-Token.

### SmcbTokenExchangeSteps

Datei:

```text
test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/SmcbTokenExchangeSteps.java
```

Die Token-Exchange-Schritte werden für den echten PDP angepasst:

- `CLIENT_ID` wird dynamisch aus der DCR verwendet.
- Die PDP-URLs zeigen auf den echten RU-DEV-PDP.
- Der Token Exchange sendet `scope=popp`.
- Es wird kein expliziter `audience`-Parameter gesetzt.
- Die Nonce wird über denselben Proxy-/Request-Pfad geholt wie der Token Exchange.
- Bei Nonce-Problemen wird eine frische Nonce verwendet und der Request erneut versucht.
- TLS wird für Requests gegen die RU-DEV-Umgebung passend initialisiert.

## Voraussetzungen

Vor dem Start müssen folgende Voraussetzungen erfüllt sein:

- Docker ist gestartet.
- Die benötigten Images sind lokal verfügbar oder können gezogen werden.
- Die Datei für das verwendete SMC-B-Zertifikat ist im Container vorhanden.
- Die konfigurierte SMC-B-Datei passt zur Testumgebung.
- Der Host `popp.dev.poppservice.de` ist aus der lokalen Umgebung erreichbar.
- Der lokale Port `18081` ist für den PoPP-Client frei.
- Der lokale Port `2101` ist für den PEP frei.

## Docker-Services starten

```bash
cd doc/docker
docker compose -f compose-local.yaml --profile full up -d
```

Benötigte Services:

- PEP / nginx
- PoPP-Client
- OPA
- Tiger-Testinfrastruktur

Nicht benötigt werden:

- lokaler PoPP-Server
- lokale PoPP-Server-Datenbank
- lokaler Keycloak-PDP

## Tests ausführen

Alle ZETA-Tests ohne `@Ignore` (gegen RU-DEV, Default):

```bash
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dzeta.env=rudev \
  -Dcucumber.filter.tags="@PRODUKT:ZETA and not @Ignore"
```

Gegen den lokalen Docker-Mock stattdessen `-Dzeta.env=local` setzen.

Ein einzelner Testlauf kann alternativ über spezifische Cucumber-Tags oder Testklassen eingeschränkt werden.

## Bekannte Einschränkungen

### Lokale OPA-Prüfungen

Beim Test gegen den echten PDP werden OPA-Entscheidungen serverseitig in der RU-DEV-Umgebung getroffen. Dadurch sind lokale Erwartungen an Requests gegen den lokalen OPA-Endpunkt nicht in jedem Szenario gültig.

Insbesondere können Tests fehlschlagen, die erwarten, dass lokal ein Request auf folgenden Pfad beobachtet wird:

```text
/v1/data/zeta/authz/decision
```

### Schema-Prüfungen für Client Assertions

Die Client Assertion verwendet den bei der DCR registrierten Schlüssel per `kid`. Falls ein Schema-Test explizit ein eingebettetes `jwk` im JWT-Header erwartet, muss der Test an das produktionsnähere Verhalten angepasst werden.

### DCR-Negativtests

Tests, die eine Ablehnung durch lokale Client Policies erwarten, sind gegen den echten PDP nur eingeschränkt aussagekräftig. Die tatsächliche Client-Policy des RU-DEV-PDP kann vom lokalen Mock-Setup abweichen.

### TLS-Handshake-Tests

TLS-Tests, die bestimmte lokale Protokoll- oder Cipher-Konfigurationen erwarten, sind getrennt vom fachlichen Token-Exchange zu betrachten. Sie prüfen nicht zwangsläufig das Verhalten des echten PoPP-/PDP-Setups.

## Rollback

Die Änderungen können mit folgendem Befehl zurückgesetzt werden:

```bash
git checkout -- \
  doc/docker/backend/zeta-popp/pep/nginx.conf \
  doc/docker/backend/zeta-popp/pep/conf/50-pep.conf.template \
  doc/docker/backend/compose-popp-services.yaml \
  tiger/defaults.yaml \
  test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/ZetaPepJwtSteps.java \
  test/zeta-testsuite/src/test/java/de/gematik/zeta/steps/SmcbTokenExchangeSteps.java \
  test/zeta-testsuite/src/test/java/de/gematik/zeta/services/ZetaPepJwtTestFactory.java
```

Anschließend die Docker-Services neu starten:

```bash
cd doc/docker
docker compose -f compose-local.yaml --profile full down
docker compose -f compose-local.yaml --profile full up -d
```