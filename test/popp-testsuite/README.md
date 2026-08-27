<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# PoPP Testsuite

Die PoPP Testsuite ist Teil des TI-Testhubs und stellt Ende-zu-Ende-Testfälle im PoPP-Kontext
bereit. Ziel ist die Validierung der PoPP-Implementierung hinsichtlich der Anforderungen der
Spezifikation **gemSpec_PoPP**, einschließlich Token-Erzeugung, Zertifikatsvalidierung und
HashDB-Import.

## Einleitung

Aktuell umfasst die Testsuite Ende-zu-Ende-Tests zur Erzeugung eines PoPP-Tokens gegen die
PoPP-Beispielimplementierung (separates Projekt, siehe [LINK](https://github.com/gematik/popp-sample-code/blob/main/README.md)).
Hierbei werden die Varianten mit echter eHealth-Hardware, einem Standardkartenleser,
sowie den virtuellen Karten des PoPP-Clients unterstützt.
Zudem sind Negativfälle für Karten mit abgelaufenen oder gesperrten Zertifikaten angelegt.
Auch diese können mit echter eHealth-Hardware und simulierten Karten ausgeführt werden.
Weiterhin bietet sie Tests der HashDB-Import-Schnittstelle an.
Die Testfälle sind mit dem gematik-Testframework **Tiger** umgesetzt. Tiger basiert auf **Cucumber**
und **Gherkin** und bietet einen Proxy, der den Datenverkehr mitschneidet.

## Vorbedingungen

1. PoPP-Beispielimplementierung lokal aufgebaut
   [siehe README der Beispielimplementierung](https://github.com/gematik/popp-sample-code/blob/main/README.md)
    - PoPP-Client aus der IDE gestartet

2. Run Configurations für die Nutzung mit Tiger konfiguriert [siehe Tiger-README](https://github.com/gematik/app-Tiger/blob/master/README.md)

3. Die von der Testsuite benötigten p12-Files sind nicht auf GitHub veröffentlicht. Sie werden auf Anfrage von der gematik bereitgestellt.

   Benötigt werden:
    * die unter `TestConstants.java` angegebenen p12-Files
    * die unter `*.feature` verwendeten p12-Files

## Nutzung des Tiger-Proxys

Je nach gewünschter Umgebung ist in der Run Configuration unter Environment Variables die Variable *env* zu setzen.

Der PoPP-Server von Rise steht dabei in drei Instanzen zum Testen zur Verfügung:

```text
lokal:  env=local  (Default, nichts setzen)
TU:     env=tu
RU:     env=ru
RU-DEV: env=ru-dev
```

Durch Setzen der Variable wird der Tiger-Proxy auf die entsprechende Umgebung konfiguriert.

Zwei Achsen, bewusst getrennt:

* **Welche Suite laeuft** bestimmt die Proxy-Topologie (Port 443, asn1-Parser, Route).
  Sie steckt in `tiger-popp.yaml` und wird automatisch geladen, weil Maven und die IDE
  im Modulverzeichnis `test/popp-testsuite` arbeiten — kein Schalter noetig.
* **Wohin gezeigt wird** bestimmt `env` ueber die Knoten in `tiger/popp-environments.yaml`.
  `env` benennt die Stufe und gilt fuer alle Produkte (PoPP, VSDM, ZETA); soll nur der
  PoPP-Proxy abweichen, uebersteuert `-Dpopp.env=<stufe>`. Eine neue Stufe ist ein
  weiterer Block unter `popp.environments`; nur `host`, ggf. `ip` und `kidTokenKey`
  weichen ab, alles Weitere kommt aus `popp.common`.

Zusätzlich MUSS die Datei `Hosts.txt` wie folgt editiert werden.

```text
TU:     127.0.0.1 popp.test.poppservice.de
RU:     127.0.0.1 popp.ref.poppservice.de
RU-DEV: 127.0.0.1 popp.dev.poppservice.de
```

## PoPP-Token generieren

Nach erfolgreicher Einrichtung kann der Gutfall ausgeführt werden.

Der Testfall lässt sich mit verschiedenen Kombinationen starten. Im Feature

`src/test/resources/features/e2e/UC_PoPP_1_2a_Valid.feature`

werden unter **Examples** die gewünschten Varianten konfiguriert. Nicht benötigte Kombinationen können mit `#` auskommentiert werden.

| readerType      | commType          |
|-----------------|-------------------|
| "Standardleser" | "kontaktbehaftet" |
| "virtuell"      | "kontaktbehaftet" |

Alternativ kann der folgende Maven-Befehl im Root-Verzeichnis des Projektes ausgeführt werden:

Beispiel RU-DEV:

```bash
mvn -Denv=ru-dev verify -Dcucumber.filter.tags="@TCID:UC_PoPP_1_2a_Valid"
```

> Tipp: Immer vorher einmal ins Feature-File schauen und nachsehen, welche Detailvarianten unter *Beispiele* ausgewählt sind.

Testfälle können dem Maven-Befehl nach Belieben mit `or @TCID:UC_Popp_***` hinzugefügt werden.
