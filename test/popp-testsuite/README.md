<img align="right" width="250" height="47" src="images/Gematik_Logo_Flag_With_Background.png"/><br/>

# PoPP Testsuite

Die PoPP Testsuite ist Teil des TI-Testhubs und stellt Ende-zu-Ende-Testfälle im PoPP-Kontext
bereit. Ziel ist zu prüfen, ob der PoPP-Token konform zur Spezifikation **gemSpec_PoPP** erzeugt
wird.

## Einleitung

Aktuell umfasst die Testsuite den Gutfall: das Erzeugen eines PoPP-Tokens gegen die
PoPP-Beispielimplementierung (separates Projekt, siehe „LINK“).
Weiterhin bietet sie Tests der HashDB-Import-Schnittstelle an.
Die Testfälle sind mit dem gematik-Testframework **Tiger** umgesetzt. Tiger basiert auf **Cucumber**
und **Gherkin** und bietet einen Proxy, der den Datenverkehr mitschneidet.

> Hinweis: Derzeit werden ausschließlich Komponenten der PoPP-Beispielimplementierung genutzt;
> Zeta-Komponenten sind noch nicht integriert.

## Vorbedingungen

1. PoPP-Beispielimplementierung lokal
   aufgebaut [siehe README der Beispielimplementierung](https://github.com/gematik/popp-sample-code/blob/main/README.md):
    - PoPP-Server-DB Docker-Container gestartet
    - PoPP-Server Docker-Container gestartet
    - PoPP-Client aus der IDE gestartet
2. Run Configurations für die Nutzung mit Tiger
   konfiguriert [siehe Tiger-README](https://github.com/gematik/app-Tiger/blob/master/README.md)
3. Die von der Testsuite benötigeten p12-Files sind nicht auf github veröffentlicht. Sie werden auf
   Anfrage von der gematik bereitgestellt.
   Benötigt werden:
    * die unter `TestConstants.java` angegebenen p12-Files
    * die unter `*.feature` verwendeten p12-Files

## PoPP-Token generieren

Nach erfolgreicher Einrichtung kann der Gutfall ausgeführt werden.  
Der Testfall lässt sich mit verschiedenen Kombinationen starten. Im Feature
`src/test/resources/features/e2e/UC_PoPP_1_2a_Valid.feature` werden unter **Examples** die
gewünschten Varianten konfiguriert. Nicht benötigte Kombinationen können mit `#` auskommentiert
werden.

| readerType      | commType          |
|-----------------|-------------------|
| "eH-KT"         | "kontaktbehaftet" |
| "Standardleser" | "kontaktbehaftet" |
| "virtuell"      | "kontaktbehaftet" |

## Nutzung des Tiger-Proxys

Der Tiger-Proxy wird in der `tiger.yaml` konfiguriert:

```yaml
tigerProxy:
  activateRbelParsing: true
  adminPort: 1300
  activateRbelParsingFor:
    - websocket
  proxyPort: 1200
  proxyRoutes:
    # route to PoPP-Server
    - from: /ws
      to: https://localhost:8443/ws
    # route to PoPP-Client
    - from: /token
      to: http://localhost:8081/token
  tls:
    serverIdentity: src/test/resources/certificates/mykeystore.p12;popp-store
    masterSecretsFile: wireshark.txt
    keyFolders:
      - .
```

> Wichtig: Wird der Tigerproxy genutzt muss in der Beispielimplentierung im PoPP-Client der proxy
> Port (hier 1200) in der application.yaml für die verbindung zum PoPP-Sever angegenben werden
>```yaml
>   url: wss://localhost:1200/ws
>```
