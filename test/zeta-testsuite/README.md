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

* ZETA-PEP (PoPP): `http://localhost:9110` (für HTTP) bzw. `ws://localhost:9110` (für WebSocket)
* ZETA-PDP (PoPP): `http://localhost:9112`

## Features

| Feature                 | Beschreibung                        | Tag                     | Details                                                 |
|-------------------------|-------------------------------------|-------------------------|---------------------------------------------------------|
| Client Registrierung    | Registrierung zur PDP               | @client_registrierung   | src/test/resources/features/zeta-client-registrierung   |
| Datenübertragung (REST) | REST-Anfrage Tests                  | @rest_pep_transfer      | src/test/resources/features/zeta-datatransfer-rest      |
| Datenübertragung (WS)   | WebSocket-basierte Tests            | @websocket              | src/test/resources/features/zeta-datatransfer-websocket |
| Header Management       | Headers weiterleitung und ergänzung | @pep_header_management  | src/test/resources/features/zeta-header-management      |
| Smoketest               | Überprüfung Service-Stabilität      | @smoke                  | src/test/resources/features/smoketest                   |


## WebSocket-Tests (PoPP über ZETA-PEP)

Ein zentraler Bestandteil der ZETA-Testsuite sind WebSocket-basierte Tests des PoPP-Backends über den ZETA-PEP.
Die entsprechenden Szenarien sind in der Feature-Datei

* `src/test/resources/features/popp_websocket_via_pep.feature`

beschrieben und prüfen u. a. folgende Aspekte:

* Aufbau einer WebSocket-Verbindung vom Client zum ZETA-PEP (Proxy-Rolle),
* Übergabe eines gültigen bzw. ungültigen Authorization-Tokens im WebSocket-Handshake,
* Weitergabe der ZETA-User-Info an das PoPP-Backend,
* korrekte Generierung einer `TokenMessage` im Erfolgsfall statt einer Fehlerantwort,
* Ablehnung des Handshakes bei falschem oder fehlendem Authorization-Header.

Die WebSocket-Tests müssen vom **Projekt-Root-Verzeichnis** (`ti2.0-testhub/`) ausgeführt werden:

```bash
# Vom Root-Verzeichnis aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags="@websocket"
```

> [!IMPORTANT]
> Die Integrationstests sind standardmäßig deaktiviert (`skip.inttests=true`), damit der Build auch ohne
> laufende Docker-Container erfolgreich ist. Um die Tests auszuführen, muss `-Dskip.inttests=false` gesetzt werden.

> [!NOTE]
> Die WebSocket-Tests werden **direkt** gegen den ZETA-PEP unter `ws://127.0.0.1:9110/...` ausgeführt.
> WebSocket-Traffic kann **nicht** über den Tiger-Proxy geroutet werden.

## Smoke-Tests

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

## REST-Datenübertragungs-Tests (via ZETA-PEP)

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

## Client-Registrierungs-Tests

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

## Alle Tests ausführen

Alle ZETA-Tests können über den gemeinsamen Tag `@PRODUKT:ZETA` ausgeführt werden, der in jeder Feature-Datei vorhanden
ist:

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="@PRODUKT:ZETA and not @Ignore"
```

Alternativ kann der Befehl ohne Tag-Filter ausgeführt werden, um alle Tests der Testsuite zu starten:

```bash
# Vom Root-Verzeichnis (ti2.0-testhub/) aus:
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="not @Ignore"
```

## Struktur der Testsuite

Die wichtigsten Verzeichnisse der ZETA-Testsuite sind:

* `src/test/resources/features` – Gherkin-Feature-Dateien:
    * `popp_websocket_via_pep.feature` – WebSocket-Tests über ZETA-PEP
    * `rest_data_transfer_via_pep.feature` – REST-Datenübertragungs-Tests über ZETA-PEP
    * `client_registrierung.feature` – Client-Registrierungs-/Token-Exchange-Tests
    * `smoke.feature` – Smoke-Tests für alle Komponenten
* `src/test/java/de/gematik/zeta/steps` – Cucumber-Step-Definitions (u. a. WebSocket- und REST-Schritte)
* `src/test/java/de/gematik/zeta/services` – Hilfs-Services (z. B. `PlainWebSocketSessionManager` für WS-Verbindungen)
* `src/test/resources` – Konfigurationen, ggf. Testdaten und Bilder für die Dokumentation

Die WebSocket-spezifischen Schritte sind in den Klassen unter `de.gematik.zeta.steps` implementiert und nutzen den
`PlainWebSocketSessionManager`, um eine schlanke, nicht STOMP-basierte WebSocket-Kommunikation zu ermöglichen.

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
  daher direkt mit dem ZETA-PEP (`ws://127.0.0.1:9110`). HTTP-Traffic kann weiterhin über den Tiger-Proxy
  mitgeschnitten werden.

## Weiterführende Informationen

* TI 2.0 TestHub – Gesamtprojekt und Dokumentation (Root-README im Repository)
* VSDM 2.0 Testsuite (`test/vsdm-testsuite/README.md`) als Referenz für Aufbau, Ausführung und Lasttests
* Tiger-Framework: Traffic-Mitschnitt, RBel-UI und Proxy-Konfiguration

Jedes Feature-Verzeichnis enthält:
- `.feature`-Datei mit Gherkin-Szenarien
- `README.md` mit detaillierter Feature-spezifischer Dokumentation

Die Step-Implementierungen befinden sich in `src/test/java/de/gematik/zeta/steps/`.
