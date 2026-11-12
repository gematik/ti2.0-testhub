# ZETA Policy Updateability

## Beschreibung
Dieser Test prüft, dass OPA (Open Policy Agent) Policy-Updates im laufenden
Betrieb erkennt und anwendet (Hot-Reload), ohne Neustart.

## Testabdeckung
Der Test fragt die OPA Decision API direkt ab (`POST /v1/data/zeta/authz/decision`)
und verifiziert, dass sich die Autorisierungsentscheidung nach einem Policy-Update ändert.

**Hinweis:** Der Test prüft nur OPA isoliert, nicht den vollständigen Flow über
PEP → PDP → OPA.

## Szenario
1. Alle bestehenden Policies aus OPA entfernen (Clean State)
2. Policy P1 in OPA veröffentlichen → erlaubt nur `professionOid=1.2.276.0.76.4.49` (Arzt)
3. PS-Profil PS1 (Arzt) → **erlaubt**, PS-Profil PS11 (Zahnarzt) → **abgelehnt**
4. Policy P11 (Patch) veröffentlichen → erlaubt nur `professionOid=1.2.276.0.76.4.50` (Zahnarzt)
5. PS-Profil PS1 → **abgelehnt**, PS-Profil PS11 → **erlaubt**

## Implementierung
- **Policies**: `src/test/resources/policies/policy_p1.rego` und `policy_p11.rego`
- **Steps**: `PolicyUpdateabilitySteps.java` – nutzt OPA Policy REST API (`PUT /v1/policies/`)
- **Decision**: `POST /v1/data/zeta/authz/decision` mit `professionOID` als Input

## Voraussetzungen
- Docker-Compose-Stack muss laufen: `docker compose -f infra/docker/compose-local.yaml --profile full up -d`
- OPA muss auf Port 2401 erreichbar sein (konfigurierbar via `zeta.server.opa.baseUrl`)

## Hinweis (Produktion)
In der Produktionsumgebung würde OPA Policy-Bundles via Bundle-Polling aus einer
OCI-Registry (z.B. [Zot Registry](https://zotregistry.dev/)) laden. Das erste Szenario
oben nutzt die OPA Policy REST API als pragmatische Alternative dazu.

## Zweites Szenario: Hot-Reload via Bundle-Polling
Deckt genau dieses produktive Bundle-Polling-Verhalten zusätzlich ab:
- **Local Artifact Registry**: `vsdm-zeta-bundle-registry`, ein einfacher statischer
  nginx-Server (siehe `compose-vsdm-services.yaml`) als lokaler Stellvertreter für eine
  echte OCI-Registry wie Zot.
- **OPA-Konfiguration**: `zeta/opa-bundle-config/config.yaml` - konfiguriert `vsdm-zeta-pdp-opa`
  so, dass es sein Test-Bundle von dort per Polling lädt (Intervall 1-2s).
- **Policies**: `src/test/resources/policies/policy_bp1_bundle.rego` und
  `policy_bp11_bundle.rego` - inhaltlich identisch zu `policy_p1.rego`/`policy_p11.rego`, aber im
  isolierten Test-Paket `zeta.bundletest.authz` (Bundle-Root `zeta/bundletest`), damit es nicht mit
  der produktiven `zeta.authz`-Policy aus dem ersten Szenario kollidiert.
- **Steps**: `PolicyBundlePollingSteps.java` – baut das Bundle (`.manifest` + `authz.rego`) als
  `.tar.gz` im Speicher und veröffentlicht es über `BundleRegistryClient` per Docker-API
  im Named Volume der Local Artifact Registry; OPA übernimmt das
  eigentliche Hot-Reload komplett eigenständig über sein Bundle-Polling.
- **Decision**: `POST /v1/data/zeta/bundletest/authz/decision` mit `professionOID` als Input (da
  dieses isolierte Test-Paket nicht über echten PDP-Traffic erreichbar ist).

### Docker und Volume-Lebenszyklus

Lokal und in Jenkins/DooD nutzt nginx dasselbe Volume
`zeta_bundle_registry`. Der Test schreibt die Bundle-Dateien dort hinein.
Die Docker-CLI muss für den Testprozess verfügbar sein und über denselben Context
bzw. `DOCKER_HOST` wie beim Stackstart auf den Daemon zugreifen können.

Der Test ermittelt den laufenden Registry-Container über `docker compose ps -q`.
`COMPOSE_FILES` enthält die beim Start verwendeten `-f`-Argumente (Standard:
`-f compose-local.yaml`); Jenkins setzt bereits
`-f compose-local.yaml -f compose-ci-vsdm.yaml` und `COMPOSE_PROJECT_NAME`.
Die Dateipfade sind relativ zu `infra/docker`; der Test kann vom Repository-Root
oder aus dem Testsuite-Modul gestartet werden. Ein abweichendes Compose-Projekt
muss auch beim Testlauf über `COMPOSE_PROJECT_NAME` ausgewählt werden.

`docker cp` überträgt das im Speicher erzeugte Archiv über stdin in ein Staging-Verzeichnis im 
vorhandenen nginx-Container. Anschließend setzt `docker exec` die Leserechte auf (`0644`)
und veröffentlicht das Bundle durch Rename innerhalb des Volumes. Der nginx-Mount ist
deshalb absichtlich schreibbar. Auch das Aufräumen erfolgt im Container.

Das Volume wird von Compose angelegt und bei `docker compose down -v` entfernt.
Container-Neustarts erhalten es. Der Szenario-Hook entfernt die veröffentlichte Datei,
entlädt aber keine bereits aktivierte Policy aus OPA. Statische OPA-Konfiguration bleibt in CI im
vorab befüllten `ci_backend`-Volume; die Bundle-Daten gehören ausdrücklich nicht dorthin.

## Ausführung
```bash
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_updateability'
```
