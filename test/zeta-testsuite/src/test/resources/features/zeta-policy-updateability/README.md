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
- Docker-Compose-Stack muss laufen: `docker compose -f doc/docker/compose-local.yaml --profile full up -d`
- OPA muss auf Port 2401 erreichbar sein (konfigurierbar via `zeta.server.opa.baseUrl`)

## Hinweis (Produktion)
In der Produktionsumgebung würde OPA Policy-Bundles via Bundle-Polling aus einer
OCI-Registry (z.B. [Zot Registry](https://zotregistry.dev/)) laden. Dieser Test
nutzt die OPA Policy REST API als pragmatische Alternative.

## Ausführung
```bash
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@policy_updateability'
```
