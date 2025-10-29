<br/>
<img style="float: right;" width="250" height="47" src="../../images/Gematik_Logo_Flag_With_Background.png" alt=""/>
<br/>

# VSDM 2.0 Testsuite

## Einleitung
Die vorliegende Testsuite beinhaltet verschiedene E2E- und IT-Tests, welche die Funktionen des Fachdienstes VSDM 2.0
prüfen und sich an der gematik Spezifikation für den Fachdienst VSDM 2.0 (gemSpec_VSDM_2) orientieren. Die Testfälle
sind mittels Gherkin beschrieben und werden durch das Cucumber und Serenity-Framework zur Ausführung gebracht.
Zusätzlich wird das Tiger-Framework der gematik zur Visualisierung und Auswertung der Tests verwendet.

Weiterhin bietet die VSDM 2.0 Testsuite eine Lastsimulation basierend auf Gatling an. Diese Simulation kann je nach 
Konfiguration verschiedene Laststufen und Lastkurven (linear, nicht-linear) generieren. Die Konfiguration für die
Simulation orientiert sich an den Vorgaben der gematik Performance-Spezifikation (gemSpec_Perf), d.h. die Maximallast
beträgt 1.000 Calls/sec und die maximale Antwortzeit liegt bei 1.000 msecs.

## Vorbedingungen
Alle Tests der VSDM 2.0 Testsuite verwenden aktuell die simulierten Dienste des TI 2.0 TestHubs. Dieser besteht aus
den folgenden Komponenten:
* Card Client Simulator
* ZETA Client Simulator
* PoPP Client Simulator
* VSDM Client Simulator
* ZETA PDP Server Simulator (ZETA Guard)
* ZETA PEP Server Simulator (ZETA Guard)
* PoPP Server Mock (PoPP Token Generator)
* VSDM Server Simulator

Sämtliche Tests der VSDM-Testsuite setzen diese simulierten Dienste voraus. Diese können mit folgendem Skript-Aufruf als
Docker-Container gestartet werden: (Der Skript-Aufruf sollte im Projekt-Root-Verzeichnis erfolgen.)
```
./doc/bin/vsdm/docker-compose-local-rebuild.sh
```

Das obige Skript kennt die folgenden Parameter:
* -h --help --> Anzeige der Hilfe
* -d --dry-run --> Anzeige der Befehle
* -s --skip-tests --> Alle Tests überspringen
* -p --projects LIST --> Auswahl einzelner Projekte

Die untere Grafik zeigt den TI 2.0 TestHub in seiner ersten Ausbaustufe, welche ausschließlich aus Simulatoren bzw.
Mocks besteht. Die VSDM 2.0 Testsuite sendet Anfragen an den Card, den PoPP und den VSDM Client Simulator. Diese
kommunizieren mit den jeweiligen Server Simulatoren bzw. Mocks.

<br/>
<img width="1108" height="744" src="./src/test/resources/images/TI20_TestHub.png" alt=""/>
<br/>

## Integrationstests
Die Testsuite enthält zwei Integrationstests (IT), welche die Funktionen der VSDM Client und Server Simulationen prüfen
und somit deren Funktionsweise demonstrieren. Die Tests verwenden das Jupiter-Framework von jUnit 5. Testfälle:
* VsdmClientIT.java
* VsdmServerIT.java

Die Integrationstests können mit folgender Kommandozeile im Verzeichnis 'vsdm-testsuite' gestartet werden:
```
cd test/vsdm-testsuite/
mvn clean verify -Dtest="Vsdm*IT"
```

## E2E-Tests
Die Testsuite beinhaltet vier E2E-Testfälle, welche die Abfrage der Versichertenstammdaten (VSD) vom Fachdienst VSDM 2.0 
als Testziel haben. Zwei Testfälle behandeln den Normalfall, wenn der Fachdienst verfügbar ist und dem Primärsystem (PS) 
antwortet. Der Fachdienst liefert die VSD aus, wenn sich das sogenannte Entity-Tag (E-Tag), das vom PS gesendet wird, 
vom E-Tag des Fachdienstes unterscheidet. Liefert der Vergleich keinen Unterschied, sendet der Fachdienst keine VSD an
das PS. Testfälle:
* UC_VSDM2_RVSD_FROM_SERVER_WITH_UPDATE.feature (E-Tag ungleich)
* UC_VSDM2_RVSD_FROM_SERVER_WITHOUT_UPDATE.feature (E-Tag gleich)

Zwei weitere Testfälle behandeln den Ausnahmefall, dass der Fachdienst nicht verfügbar ist oder dem PS mit einem Fehler
antwortet. In diesem Fall liest das PS die wichtigsten VSD von der eGK direkt. Auch hier werden zwei unterschiedliche
Szenarien getestet. In einem Szenario können die VSD erfolgreich von der eGK gelesen werden, im anderen Szenario ist 
dies nicht möglich, da die eGK ungültig ist. Testfälle:
* UC_VSDM2_RVSD_FROM_EGK_CARD_VALID.feature
* UC_VSDM2_RVSD_FROM_EGK_CARD_INVALID.feature

Die E2E-Tests können mit folgender Kommandozeile im Verzeichnis 'vsdm-testsuite' gestartet werden:
```
cd test/vsdm-testsuite/
mvn clean verify -Dcucumber.filter.tags="@TYPE:E2E" -Dskip.inttests=false
```

## BDE-Tests
Aktuell beinhaltet die VSDM 2.0 Testsuite fünf Testfälle, die ausgewählte Fehlerszenarien prüfen und sich dabei an der
Spezifikation für VSDM 2.0 (gemSpec_VSDM_2) orientieren. Testfälle:
* UC_VSDM2_RVSD_ERROR_BDE_CODE_79010.feature (VSDSERVICE_INVALID_IK)
* UC_VSDM2_RVSD_ERROR_BDE_CODE_79011.feature (VSDSERVICE_INVALID_KVNR)
* UC_VSDM2_RVSD_ERROR_BDE_CODE_79020.feature (VSDSERVICE_PATIENT_RECORD_NOT_FOUND)
* UC_VSDM2_RVSD_ERROR_BDE_CODE_79033.feature (VSDSERVICE_INVALID_PATIENT_RECORD_VERSION)
* UC_VSDM2_RVSD_ERROR_BDE_CODE_79100.feature (VSDSERVICE_INTERNAL_SERVER_ERROR)

Die BDE-Tests können mit folgender Kommandozeile im Verzeichnis 'vsdm-testsuite' gestartet werden:
```
cd test/vsdm-testsuite/
mvn clean verify -Dcucumber.filter.tags="@TYPE:BDE" -Dskip.inttests=false
```

## Lasttests
Die VSDM 2.0 Testsuite enthält aktuell vier Lasttests, welche die Antwortzeiten der VSDM 2.0 Server Simulation prüfen.
Hierbei werden die beiden Varianten Antwort HTTP Code 200 mit VSD und HTTP Code 304 ohne VSD sowie einzelne und
mehrfache Anfragen getestet. Die Antwortzeit des Servers darf in allen Fällen nicht größer als 1.000 msecs sein. 
Testfälle:
* UC_VSDM2_RVSD_LOAD_SINGLE_WITH_UPDATE.feature
* UC_VSDM2_RVSD_LOAD_SINGLE_WITHOUT_UPDATE.feature
* UC_VSDM2_RVSD_LOAD_MULTI_WITH_UPDATE.feature
* UC_VSDM2_RVSD_LOAD_MULTI_WITHOUT_UPDATE.feature

Die Lasttests können mit folgender Kommandozeile im Verzeichnis 'vsdm-testsuite' gestartet werden:
```
cd test/vsdm-testsuite/
mvn clean verify -Dcucumber.filter.tags="@TYPE:LOAD" -Dskip.inttests=false
```

## Hintergrundlast
Die VSDM 2.0 Testsuite enthält eine Lasttest-Simulation basierend auf Gatling. Diese simuliert den kompletten Ablauf vom
Einstecken der Karten, über die Erlangung des Versorgungskontextes bis hin zur Abfrage der VSD vom Fachdienst VSDM 2.0.
In Kombination mit den Load Tests wird die Simulation zur Erzeugung der Hintergrundlast verwendet. Die Hintergrundlast
kann mittels Maven und folgender Kommandozeile im Verzeichnis 'vsdm-testsuite' gestartet werden:
```
cd test/vsdm-testsuite/
mvn gatling:test -Dgatling.simulationClass=de.gematik.ti20.vsdm.test.load.VsdmLoadSimulation [-DrandomReadVsd=true]
```
Die Hintergrundlast ist aktuell in der Datei "VsdmLoadSimulation.java" so konfiguriert, dass diese maximal 25 Aufrufe in
der Sekunde über einen Zeitraum von ca. einer Minute versendet. Das Versenden kann mit dem Parameter "randomReadVsd"
gleichförmig oder variabel erfolgen. Bei Auswahl einer variablen Hintergrundlast bewegen sich die Aufrufe zufällig
zwischen den Werten 5 bis 25 Aufrufe pro Sekunde. Natürlich können die aktuell eingestellten Parameter durch passende
System-Parameter (Option -D) überschrieben werden.

### Beispiel: Gleichförmige Hintergrundlast (25 Aufrufe/sec für 60 Sekunden)
* randomReadVsd=false
* usersPerSec=25
* usersDurationSecs=60

### Beispiel: Variable Hintergrundlast (5-25 Aufrufe/sec für 300 Sekunden)
* randomReadVsd=true
* readVsdPerSecMin=5
* readVsdPerSecMax=25
* readVsdDurationSecs=30
* readVsdNumberCycles=10

## Hinweise
In der aktuellen Ausbaustufe sind die ZETA-Komponenten nur als Mocks implementiert und sollen zeitnah durch die realen
Komponenten ersetzt werden. Daher enthalten die Testschritte zur Authentifizierung am ZETA Guard keine Implementierung.

Die PoPP-Komponenten sind ebenfalls nur als Mocks ausgeführt und sollen zeitnah durch eine Beispiel-Implementierung 
ersetzt werden. Gegenwärtig generiert der PoPP-Server-Mock nur einen PoPP-Token basierend auf der IK- und KVNR-Nummer.

Aktuell verwendet die Gatling-Lastsimulation nur eine Testidentität mit der KVNR X110639491. Es ist aber geplant, die
Simulation mit dem PoPP-Token-Generator zu kombinieren, sodass diese einfacher wird und deutlich mehr Testidentitäten
unterstützt.
