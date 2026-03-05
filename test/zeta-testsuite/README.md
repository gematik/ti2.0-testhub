<img align="right" width="250" height="47" src="./src/test/resources/images/Gematik_Logo.png"/><br/>

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
je nach Testtyp:

**WebSocket-Tests (`@websocket`):**
* ZETA PEP Server Mockservice (Port 9110)
* PoPP Server Mockservice (Backend hinter PEP)

**Smoke-Tests (`@smoke`):**
* ZETA PDP Server Mockservice (PoPP)
* ZETA PEP Server Mockservice (PoPP)
* ZETA PDP Ingress (VSDM)

**Client-Registrierungs-Tests (`@client_registrierung`):**
* ZETA PDP Server Mockservice (Port 9112)
* Tiger-Proxy

Am einfachsten werden alle Backend-Dienste gemeinsam gestartet. Dies kann im Projekt-Root-Verzeichnis mit folgendem
Befehl erfolgen:

```bash
./doc/bin/docker-compose-local-rebuild.sh
```

Anschließend stehen u. a. folgende relevanten Endpunkte zur Verfügung (Standard-Setup des TestHubs):

* ZETA-PEP (PoPP): `http://localhost:9110` (für HTTP) bzw. `ws://localhost:9110` (für WebSocket)
* ZETA-PDP (PoPP): `http://localhost:9112`

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

## Client-Registrierungs-Tests

Die Client-Registrierungs-Tests prüfen den Token-Exchange-Flow über den ZETA-PDP. Die Szenarien sind in der Feature-Datei
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

Alle ZETA-Tests können über den gemeinsamen Tag `@PRODUKT:ZETA` ausgeführt werden, der in jeder Feature-Datei vorhanden ist:

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

* `src/test/resources/features` – Gherkin-Feature-Dateien (z. B. `popp_websocket_via_pep.feature`)
* `src/test/java/de/gematik/zeta/steps` – Cucumber-Step-Definitions (u. a. WebSocket-Schritte)
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
