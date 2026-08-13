<img align="right" width="250" height="47" src="../../images/Gematik_Logo_Flag_With_Background.png"/><br/>

# ZETA Testsuite

## Einleitung

Die vorliegende ZETA-Testsuite beinhaltet verschiedene Integrations- und E2E-Tests, welche die Funktionen der
ZETA-Komponenten im TI 2.0 TestHub prüfen. Im Fokus stehen insbesondere der ZETA-Guard (PDP/PEP) sowie dessen
Zusammenspiel mit den PoPP- und VSDM-Simulatoren. Die Testfälle sind mittels Gherkin beschrieben und werden über das
Cucumber- und Serenity-Framework ausgeführt. Zur Visualisierung und Auswertung der HTTP-/WebSocket-Kommunikation kommt
zusätzlich das Tiger-Framework der gematik zum Einsatz.

Ein Schwerpunkt der ZETA-Testsuite liegt auf:

* der Prüfung von Autorisierungsentscheidungen des ZETA-Guards (PDP/PEP),
* der korrekten Transformation und Weitergabe von Header- und Token-Informationen an nachgelagerte Dienste,
* der End-to-End-Kommunikation über WebSockets zwischen ZETA-PEP und PoPP-Backend.

## Vorbedingungen

Die Tests der ZETA-Testsuite verwenden die simulierten Dienste des TI 2.0 TestHubs. Die benötigten Komponenten variieren
je nach Testtyp. Unser Vorschlag ist, alle Backend-Dienste gemeinsam zu staren.

Anschließend stehen u. a. folgende relevanten Endpunkte zur Verfügung (Standard-Setup des TestHubs):

* ZETA-PEP (PoPP): `http://localhost:2101` (für HTTP) bzw. `ws://localhost:2101` (für WebSocket)
* ZETA-PDP (PoPP): `http://localhost:2201`

## Testumgebung wählen

Gegen welche Umgebung die Testsuite läuft, wird an **genau einer Stelle** gesteuert: über die
Variable **`zeta.env`** in `tiger/zeta-environments.yaml`. Alle URLs (PDP-Token-/DCR-/Nonce-/JWKS-Endpunkt,
PEP, PoPP-Client, Smoke-Endpoints, `zeta_base_url`) werden automatisch aus dem ausgewählten
Umgebungs-Block `zeta.environments.<zeta.env>` abgeleitet — es sind **keine** URLs in Java-Klassen
oder Feature-Dateien hardcodiert.

Unterstützte Werte:

| `zeta.env`   | Bedeutung                                                              |
|--------------|-------------------------------------------------------------------------|
| `local`      | Lokaler Docker-Mock (PDP/Keycloak und PoPP-Server laufen lokal) — **Default** |
| `popp-rudev` | Echter PoPP-Server `popp.dev.poppservice.de` (RU-DEV)                  |

Umschalten (höchste Priorität zuerst):

```bash
# 1. System-Property beim Testlauf
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dzeta.env=local

# 2. Umgebungsvariable
export ZETA_ENV=local

# 3. Default in tiger/zeta-environments.yaml (Schlüssel: zeta.env)
```

**Neue Umgebung ergänzen:** einen weiteren Block unter `zeta.environments` in `tiger/zeta-environments.yaml`
anlegen (z. B. `tu`, `staging`) und `zeta.env` auf dessen Namen setzen. Code muss dafür nicht
angepasst werden. Aus dem Java-Code werden die abgeleiteten Werte über
`de.gematik.zeta.config.PoPpConfig` gelesen.

> [!NOTE]
> Die lokale PEP-/nginx-Infrastruktur in Docker wird **nicht** über `zeta.env` gesteuert, sondern
> über `POPP_SERVER_HOST` in `doc/docker/backend/compose-popp-services.yaml`. Beim Test gegen den
> echten PoPP-Server müssen diese Docker-Variablen zusätzlich gesetzt werden. Details und das
> vollständige RU-DEV-Setup: siehe [`README-real-popp.md`](README-real-popp.md).

### Smoke-Tests

Die Smoke-Tests prüfen die grundlegende Erreichbarkeit der ZETA-Komponenten. Die Szenarien sind in der
Feature-Datei `src/test/resources/features/smoke.feature` definiert.

Geprüfte Komponenten (`@smoke`):

* ZETA-PDP (PoPP)
* ZETA-PEP (PoPP)
* ZETA-PDP Ingress

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags="@smoke"
```

### REST-Datenübertragungs-Tests (via ZETA-PEP)

Die REST-Datenübertragungs-Tests prüfen die HTTP-basierte Kommunikation zwischen Client und Backend über den ZETA-PEP
Proxy.
Die Szenarien sind in der Feature-Datei `src/test/resources/features/rest_data_transfer_via_pep.feature` definiert.

Die Tests nutzen den Endpunkt `/openapi.yaml` des PoPP-Servers, da dieser sowohl durch den Auth-geschützten
`HttpProxyController` des PEP geroutet wird als auch im Backend existiert und mit HTTP 200 antwortet.

**Szenarien (`@rest_pep_transfer`):**

| Szenario           | Beschreibung                          | Erwartung                               |
|--------------------|---------------------------------------|-----------------------------------------|
| Gültiger Token     | GET-Anfrage mit gültigem JWT          | PEP leitet an Backend weiter → HTTP 200 |
| Ohne Authorization | GET-Anfrage ohne Authorization-Header | PEP lehnt ab → HTTP 401                 |
| Ungültiger Token   | GET-Anfrage mit ungültigem JWT        | PEP lehnt ab → HTTP 401                 |

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="@rest_pep_transfer"
```

### Client-Registrierungs-Tests

Die Client-Registrierungs-Tests prüfen den Token-Exchange-Flow über den ZETA-PDP. Die Szenarien sind in der
Feature-Datei
`src/test/resources/features/client_registrierung.feature` definiert.

**Gutfall-Szenario (`@client_registrierung`):**

* Sendet einen Token-Exchange-Request an den ZETA-PDP über Tiger-Proxy
* Prüft, dass ein gültiges Access-Token zurückgegeben wird

**Fehlerfall-Szenarien (`@Ignore`):**

* Testen die Ablehnung von Requests bei ungültigen Policy-Werten (z.B. ungültige professionOID, product_id, scopes)
* Diese Tests sind aktuell mit `@Ignore` markiert, da der PDP-Mock keine echte OPA-Policy verwendet

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="@client_registrierung and not @Ignore"
```

## Testausführung

Alle ZETA-Tests können über den gemeinsamen Tag `@PRODUKT:ZETA` ausgeführt werden, der in jeder Feature-Datei vorhanden
ist:

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="@PRODUKT:ZETA and not @Ignore" -Dzeta.env=local
```

Alternativ kann der Befehl ohne Tag-Filter ausgeführt werden, um alle Tests der Testsuite zu starten:

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="not @Ignore" -Dzeta.env=local
```

## Testausführung gegen VSDM 2.0

tiger/zeta-vsdm-rudev.yaml enthält die Konfiguration für die Ausführung der ZETA-Tests gegen den echten VSDM 2.0
Server in der RU-DEV Umgebung. Die Testsuite kann mit dem Tag-Filter `@PRODUKT:VSDM_2_FD` gestartet werden, 
um nur die Tests auszuführen, die für die Kommunikation mit dem VSDM 2.0 Server relevant sind. 
Alle anderen Tests werden mit `not @local` ausgeschlossen, da sie nur gegen die lokale Testumgebung laufen. 
Der Tag `not @Ignore` schließt Tests aus, die aktuell nicht relevant sind oder noch nicht implementiert wurden.   

Vom Root-Verzeichnis (ti2.0-testhub/) aus:
```bash
./mvnw -pl test/zeta-testsuite clean verify \
  -Dskip.inttests=false \
  -Dcucumber.filter.tags="(@PRODUKT:VSDM_2_FD or @PRODUKT:Anb_FD_VSDM) and not @local and not @Ignore" \
  -D.env=vsdm-tu
```


## Hinweise zur Anpassung

* **Timeouts:**
  Die Timeouts für Verbindungsaufbau und Nachrichtenempfang können über die Gherkin-Schritte
  `setze Anfrage Timeout für WebSocket Verbindungen auf ... Sekunden` und
  `setze Timeout für WebSocket Nachrichten auf ... Sekunden` konfiguriert werden.

* **Tokens und Header:**
  Access-Tokens für den ZETA-PEP werden in dedizierten Schritten erzeugt (z. B. `ein gültiger ZETA-PEP AccessToken
  wird erzeugt`) und anschließend als WebSocket-Handshake-Header (`Authorization`) gesetzt.

* **Proxy-Einsatz:**
  WebSocket-Traffic kann nicht über den Tiger-Proxy geroutet werden. Die WebSocket-Tests kommunizieren
  daher direkt mit dem ZETA-PEP (`ws://127.0.0.1:2101`). HTTP-Traffic kann weiterhin über den Tiger-Proxy
  mitgeschnitten werden.

## Weiterführende Informationen

* TI 2.0 TestHub – Gesamtprojekt und Dokumentation (Root-README im Repository)
* VSDM 2.0 Testsuite (`test/vsdm-testsuite/README.md`) als Referenz für Aufbau, Ausführung und Lasttests
* Tiger-Framework: Traffic-Mitschnitt, RBel-UI und Proxy-Konfiguration

Jedes Feature-Verzeichnis enthält:
- `.feature`-Datei mit Gherkin-Szenarien
- `README.md` mit detaillierter Feature-spezifischer Dokumentation

Die Step-Implementierungen befinden sich in `src/test/java/de/gematik/zeta/steps/`.
