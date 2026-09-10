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

Die **Stufe** kommt aus dem zentralen Schalter **`env`** (`tiger/flags.yaml`) und gilt für alle
Produkte gleichermaßen — `local` (Default), `ru-dev`, `ru`, `tu`. Alle URLs
(PDP-Token-/DCR-/Nonce-/JWKS-Endpunkt, PEP, PoPP-Client, Smoke-Endpoints, `zeta_base_url`) werden
automatisch aus dem aktiven Umgebungs-Block in `tiger/zeta-environments.yaml` abgeleitet — es sind
**keine** URLs in Java-Klassen oder Feature-Dateien hardcodiert.

Bei ZETA ist die Übersteuerung **`zeta.env`** ein *vollständiger Blockname*, kein Stufenname: hier
kommen zwei Achsen zusammen — die Stufe **und** welcher der beiden ZETA-Guards gemeint ist
(`popp-zeta-*` oder `vsdm-zeta-*` im Docker-Stack).

Unterstützte Blöcke:

| Block        | Bedeutung                                                              | Stufe   |
|--------------|------------------------------------------------------------------------|---------|
| `local`      | Lokaler Docker-Mock (PDP/Keycloak und PoPP-Server laufen lokal)         | `local` |
| `popp-rudev` | Echter PoPP-Server `popp.dev.poppservice.de` (RU-DEV)                  | RU-DEV  |
| `vsdm-tu`    | Echter VSDM 2.0 Fachdienst `vsdm-test.tk.de` (TU)                      | TU      |
| `vsdm-rudev` | Echter VSDM 2.0 Fachdienst `vsdm-dev.tk.de` (RU-DEV)                   | RU-DEV  |
| `custom`     | Ad-hoc-Ziel — setzt bewusst nur die Domain, siehe unten                 | –       |

`local` ist der einzige Block, der zugleich ein Stufenname ist — deshalb greift der Default
`env=local` bei ZETA ohne Zutun. Wer `-Denv=ru-dev` oder `-Denv=tu` setzt und die ZETA-Suite laufen
lässt, muss zusätzlich `-Dzeta.env=<blockname>` angeben; `PoPpConfig` bricht sonst mit einer Meldung
ab, die die verfügbaren Blöcke nennt.

Jeder Block konfiguriert im Normalfall nur die **Domain**; Realm-, Token-, DCR-, JWKS- und
Nonce-Pfad stehen als Defaults in `tiger/paths.yaml` (`zeta.paths.pdp.*`). Ein einzelnes Ziel
lässt sich auch ohne neuen Block anspringen:

```bash
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dzeta.env=custom -Dzeta.server.domain=https://vsdm-test.tk.de
```

Der Block `custom` setzt bewusst nichts ausser der Domain, sodass Issuer, Audiences, PEP und
alle PDP-Endpunkte daraus abgeleitet werden. (Ohne `-Dzeta.env=custom` gewinnen die im
aktiven Block explizit gesetzten Werte — bei `local` z. B. der Ingress-Issuer.)

Umschalten (höchste Priorität zuerst):

```bash
# 1. System-Property beim Testlauf
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dzeta.env=vsdm-tu

# 2. Umgebungsvariable
export ZETA_ENV=vsdm-tu

# 3. Ohne zeta.env: die Stufe aus `env` (Default `local` in tiger/flags.yaml)
```

## Testausführung

Alle ZETA-Tests können über den gemeinsamen Tag `@PRODUKT:ZETA` ausgeführt werden, der in jeder Feature-Datei vorhanden
ist:

Vom Root-Verzeichnis (ti2.0-testhub/) aus:
```bash
./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false \
  -Dcucumber.filter.tags="@PRODUKT:ZETA and not @Ignore"
```


## Testausführung gegen VSDM 2.0

Der Block `zeta.environments.vsdm-rudev` in `tiger/zeta-environments.yaml` enthält die Konfiguration
für die Ausführung der ZETA-Tests gegen den echten VSDM 2.0 Server in der RU-DEV Umgebung
(für die TU entsprechend `vsdm-tu`). Die Testsuite kann mit dem Tag-Filter `@PRODUKT:VSDM_2_FD` gestartet werden, 
um nur die Tests auszuführen, die für die Kommunikation mit dem VSDM 2.0 Server relevant sind. 
Alle anderen Tests werden mit `not @local` ausgeschlossen, da sie nur gegen die lokale Testumgebung laufen. 
Der Tag `not @Ignore` schließt Tests aus, die aktuell nicht relevant sind oder noch nicht implementiert wurden.   

Vom Root-Verzeichnis (ti2.0-testhub/) aus:
```bash
./mvnw -pl test/zeta-testsuite clean verify \
  -Dskip.inttests=false \
  -Dcucumber.filter.tags="(@PRODUKT:VSDM_2_FD or @PRODUKT:Anb_FD_VSDM) and not @local and not @Ignore" \
  -Dzeta.env=vsdm-tu
```



## Weiterführende Informationen

* TI 2.0 TestHub – Gesamtprojekt und Dokumentation (Root-README im Repository)
* VSDM 2.0 Testsuite (`test/vsdm-testsuite/README.md`) als Referenz für Aufbau, Ausführung und Lasttests
* Tiger-Framework: Traffic-Mitschnitt, RBel-UI und Proxy-Konfiguration

Jedes Feature-Verzeichnis enthält:
- `.feature`-Datei mit Gherkin-Szenarien
- `README.md` mit detaillierter Feature-spezifischer Dokumentation

Die Step-Implementierungen befinden sich in `src/test/java/de/gematik/zeta/steps/`.
