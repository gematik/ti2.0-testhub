<img align="right" width="250" height="47" src="../../images/Gematik_Logo_Flag_With_Background.png"/><br/>

# VSDM 2.0 Testsuite

## Einleitung

Die vorliegende Testsuite beinhaltet verschiedene E2E- und IT-Tests, welche die Funktionen des Fachdienstes VSDM 2.0
prüfen und sich an der gematik Spezifikation für den Fachdienst VSDM 2.0 (gemSpec_VSDM_2) orientieren. Die Testfälle
sind mittels Gherkin beschrieben und werden durch das Cucumber und Serenity-Framework zur Ausführung gebracht.
Zusätzlich wird das Tiger-Framework der gematik zur Visualisierung und Auswertung der Tests verwendet.

### Screenplay Pattern

Die Implementierung der Testschritte folgt dem sogenannten Screenplay Pattern von Serenity. Im Zentrum steht der Actor,
hier das Primärsystem, welcher bestimmte Fähigkeiten (abilities) besitzt, um bestimmte Aufgaben (tasks) übernehmen zu
können. Schließlich kann der Actor auch Fragen (questions) beantworten, welche die Ergebnisse der Aufgaben prüfen.

* Actor → HCCS (Healthcare Client System)
* Abilities → API-Knowledge
* Tasks → API-Requests
* Questions → API-Responses

Weiterhin bietet die VSDM 2.0 Testsuite eine Lastsimulation basierend auf Gatling an. Diese Simulation kann je nach
Konfiguration verschiedene Laststufen und Lastkurven (linear, nicht-linear) generieren. Die Konfiguration für die
Simulation orientiert sich an den Vorgaben der gematik Performance-Spezifikation (gemSpec_Perf), d.h. die Maximallast
beträgt 1.000 Calls/sec und die maximale Antwortzeit liegt bei 1.000 msecs.

## Vorbedingungen

Alle Tests der VSDM 2.0 Testsuite verwenden aktuell die simulierten Dienste des TI 2.0 TestHubs. Dieser besteht aus
den folgenden Komponenten:

* Card Client Simulator
* ZETA Client SDK
* PoPP Client Simulator
* VSDM Client Simulator
* ZETA Server SDK (PDP, PEP)
* PoPP Server Mock (PoPP Token Generator)
* VSDM Server Simulator

Sämtliche Tests der VSDM-Testsuite setzen diese simulierten Dienste voraus. Diese können mit folgenden Befehlen als
Docker-Container gestartet werden: (Die Befehle sollten im Projekt-Root-Verzeichnis ausgeführt werden.)

### Bauen der Docker-Images
```
./mvnw clean install -Pdocker -DskipTests
```
### Start der Docker-Container
```
docker compose -f ./doc/docker/compose-local.yaml --profile full up -d --remove-orphans
```

### Profile

Der TestHub unterstützt verschiedene Docker Compose Profile. Diese Profile ermöglichen es, je nach Anwendungsfall
unterschiedliche Kombinationen von Diensten zu starten. Die folgenden Profile werden aktuell unterstützt:

**Tabelle: Dockerprofile**

| Bezeichnung | Einsatz             | Anmerkung                                                                                                                   |
|:------------|:--------------------|:----------------------------------------------------------------------------------------------------------------------------|
| full        | Lokale Testumgebung | Dieses Profil startet alle Module des Testhubs und kann zur Durchführung von E2E-, ERROR- und PERF-Tests verwendet werden.  |
| perf        | Lokale Testumgebung | Dieses Profil startet die Module alle Vsdm-Client und Vsdm-Server-Module und eignet sich zum Test der Gatling-Simulationen. |
| ps-only     | Remote Testumgebung | Dieses Profil startet alle Client-Module und kann für die Zulassungstests verwendet werden.                                 | 

Die untere Grafik zeigt den TI 2.0 TestHub in seiner aktuellen Ausbaustufe. Die VSDM 2.0 Testsuite sendet Anfragen an
den Card, den PoPP und den VSDM Client Simulator. Diese kommunizieren mit den jeweiligen Server Simulatoren oder auch
mit den realen Servern.

<br/>
<img src="./src/test/resources/images/TI20_TestHub_Stufe_7.png" alt="" style="border: 1px solid black;"/>
<br/>

## Integrationstests

Die Testsuite enthält zwei Integrationstests (IT), welche die Funktionen der VSDM Client und Server Simulationen prüfen
und somit deren Funktionsweise demonstrieren. Die Tests verwenden das Jupiter-Framework von junit 5. Testfälle:

* VsdmClientIT.java
* VsdmServerIT.java

Die Integrationstests können mit folgender Kommandozeile im Projekt-Root-Verzeichnis gestartet werden:

```
./mvnw -pl test/vsdm-testsuite/ -Dit.test="Vsdm*IT" -Dskip.inttests=false verify
```

## E2E-Tests

Die Testsuite beinhaltet drei E2E-Testfälle, welche die Abfrage der Versichertenstammdaten (VSD) vom Fachdienst VSDM 2.0
als Testziel haben. Zwei Testfälle behandeln den Normalfall, wenn der Fachdienst verfügbar ist und dem Primärsystem (PS)
antwortet. Der Fachdienst liefert die VSD aus, wenn sich das sogenannte Entity-Tag (E-Tag), das vom PS gesendet wird,
vom E-Tag des Fachdienstes unterscheidet. Liefert der Vergleich keinen Unterschied, sendet der Fachdienst keine VSD an
das PS. Testfälle:

* UC_VSDM2_RVSD_FROM_SERVER_WITH_UPDATE.feature (E-Tag ungleich)
* UC_VSDM2_RVSD_FROM_SERVER_WITHOUT_UPDATE.feature (E-Tag gleich)

Ein weiterer Testfall behandelt den Ausnahmefall, dass der Fachdienst nicht verfügbar ist oder dem PS mit einem Fehler
antwortet. In diesem Fall liest das PS die wichtigsten VSD von der eGK direkt. Testfall:

* UC_VSDM2_RVSD_FROM_EGK_CARD_VALID.feature

Die E2E-Tests können mit folgender Kommandozeile im Projekt-Root-Verzeichnis gestartet werden:

```
./mvnw -pl test/vsdm-testsuite/ verify -Dcucumber.filter.tags="@TYPE:E2E" -Dskip.inttests=false
```

## ERROR-Tests

Die Testsuite beinhaltet aktuell sieben ERROR-Testfälle, welche bestimmte Fehlersituationen durch die Verwendung von
manipulierten Popp-Token auslösen. Hierzu ist es erforderlich, dass der Popp-Token-Generator (kurz: PTG) gestartet ist.
Die folgenden Error-Codes werden getestet:

* 79010 - VSDSERVICE_INVALID_IK
* 79011 - VSDSERVICE_INVALID_KVNR
* 79012 - VSDSERVICE_UNKNOWN_IK
* 79013 - VSDSERVICE_UNKNOWN_KVNR
* 79014 - VSDSERVICE_MISSING_PATIENT_RECORD_VERSION
* 79015 - VSDSERVICE_INVALID_PROFILE_VERSION
* 79016 - VSDSERVICE_MISSING_PROFILE_VERSION

Die ERROR-Tests können mit folgender Kommandozeile im Projekt-Root-Verzeichnis gestartet werden:

```
./mvnw -pl test/vsdm-testsuite/ verify -Dcucumber.filter.tags="@TYPE:ERROR" -Dskip.inttests=false
```

## PERF-Tests

Die VSDM 2.0 Testsuite enthält aktuell vier Lasttests, welche die Antwortzeiten der VSDM 2.0 Server Simulation prüfen.
Hierbei werden die beiden Varianten Antwort HTTP Code 200 mit VSD und HTTP Code 304 ohne VSD sowie einzelne und
mehrfache Anfragen getestet. Die Antwortzeit des Servers darf in allen Fällen nicht größer als 1.000 msecs sein.
Testfälle:

* UC_VSDM2_RVSD_PERF_SINGLE_WITH_UPDATE.feature
* UC_VSDM2_RVSD_PERF_SINGLE_WITHOUT_UPDATE.feature
* UC_VSDM2_RVSD_PERF_MULTI_WITH_UPDATE.feature
* UC_VSDM2_RVSD_PERF_MULTI_WITHOUT_UPDATE.feature

> **HINWEIS**
> Die Lasttests erwarten eine geeignete Hintergrundlast, welche mit der Gatling-Simulation 'VsdmBackgroundLoadSimulation'
> erzeugt werden kann. Wenn die Umgebungsvariable 'vsdm.loadtesting.active=true' gesetzt ist, wird die Ausführung der
> Lasttests pausiert und der Anwender zum Starten der Hintergrundlast aufgefordert. D.h. die automatische wird zu einer
> halbautomatischen Testausführung.

Die Lasttests können mit folgender Kommandozeile im Projekt-Root-Verzeichnis gestartet werden:

```
./mvnw -pl test/vsdm-testsuite/ verify -Dcucumber.filter.tags="@TYPE:PERF" -Dskip.inttests=false
```

### Konfiguration der Tiger-Tests (E2E, ERROR, PERF)

Die Konfiguration der Tiger-Tests ist in der Datei "tiger.conf" hinterlegt. Hier werden die eGK-, das SMC-B-Image
und der Parameter zur Steuerung der Dialoge für die Lasttests definiert.  Der Anwender kann auch eine eigene 
Konfigurationsdatei, welche sich im Resource-Ordner befinden sollte, definieren. Die  eigene Datei muss sich jedoch 
strukturell an der Datei "tiger.conf" orientieren. Für eine Datei mit dem Pfad "src/test/resources/my-own-tiger.conf"
sähe der Parameter zur Angabe der Konfiguration dann wie folgt aus:

```
-Dconfig=my-own-tiger.conf
```

## Lasttest-Simulationen

Die VSDM 2.0 Testsuite enthält mehrere Lasttest-Simulationen basierend auf Gatling, welche sich im Ablauf unterscheiden
und für unterschiedliche Zwecke einsetzbar sind.

> **HINWEIS**
> Für Performance-Tests empfiehlt sich das `perf` Profil, da hierbei die Clients direkt mit dem Backend kommunizieren
> und sämtliche Tiger-Proxies umgangen werden:

> ```
> docker compose -f doc/docker/compose-local.yaml --profile perf up -d
> ```

### VsdmClientJourneySimulation.java

Diese Simulation simuliert den kompletten Ablauf vom Einstecken der Karten, über die Erlangung des Versorgungskontextes
bis hin zur Abfrage der VSD vom Fachdienst VSDM 2.0 und kann im Projekt-Root-Verzeichnis wie folgt gestartet werden:

```
./mvnw -pl test/vsdm-testsuite/ gatling:test -Dgatling.simulationClass=de.gematik.ti20.vsdm.test.load.VsdmClientJourneySimulation
```

> **HINWEIS**
> Durch die aktuelle Integration weiterer Tiger-Komponenten, welche die Client-Simulationen starten und mehrere
> Proxies in den Datenverkehr einschleusen, ist die Lauffähigkeit der Lastsimulation "VsdmClientJourneySimulation"
> beeinträchtigt. Es wird empfohlen, die Last entsprechend zu reduzieren. Siehe "Konfiguration der Simulation".

### VsdmBackgroundLoadSimulation.java

Diese Simulation verwendet die, von der GeneratePoppTokenSimulation erzeugte, Liste aus Popp-Token, fragt die VSD vom
VsdmServerSimulator ab und wird in Verbindung mit den Lasttests zur Generierung der Hintergrundlast eingesetzt.
Die Simulation kann mittels Maven und folgender Kommandozeile im Projekt-Root-Verzeichnis gestartet werden:

```
./mvnw -pl test/vsdm-testsuite/ gatling:test -Dgatling.simulationClass=de.gematik.ti20.vsdm.test.load.VsdmBackgroundLoadSimulation
```

> **HINWEIS**
> Der Standardwert für den Access-Token beträgt 300 Sekunden, sodass der ZETA Guard nach dieser Zeitspanne nicht mit 200
> OK, sondern mit 401 UNAUTHORIZED antworten würde. Die Gültigkeitsdauer (TTL) des Access-Tokens lässt sich in der Datei
> "doc/docker/backend/zeta/policies/authz.rego" jedoch erhöhen.

### Konfiguration der Simulationen

Die Lastverteilung ist aktuell in der Datei "simulation.conf" so konfiguriert, dass diese eine zufällige Last im Bereich
von 95 und 105 Aufrufen über einen Zeitraum von 100 Sekunden versendet. Wird der Parameter "randomReadVsd" in dieser Datei
auf den Wert "false" gesetzt, werden stattdessen konstant 100 Aufrufe über einen Zeitraum von 100 Sekunden gesendet.

Der Anwender kann auch eine eigene Konfigurationsdatei, welche sich im Resource-Ordner befinden sollte, definieren. Die
eigene Datei muss sich jedoch strukturell an der Datei "simulation.conf" orientieren. Für eine Datei mit dem Pfad
"src/test/resources/my-own-simulation.conf" sähe der Parameter zur Angabe der Konfiguration dann wie folgt aus:

```
-Dconfig=my-own-simulation.conf
```
