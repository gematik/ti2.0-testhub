#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@rest_pep_transfer'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
@PRODUKT:ZETA


Funktionalität: REST Datenübertragung zwischen Client und Server via ZETA-PEP Proxy

  # Dieses Feature testet die REST-basierte Datenübertragung über einen ZETA-PEP Proxy.
  # Der positive Fall wird über einen ECHTEN Client ausgelöst (VSDM-Client VSD-Read über den
  # VSDM-ZETA-PEP, wie in zeta-header-management/pep_header_management.feature und
  # zeta-asl/asl.feature), statt über eine selbstgebaute Anfrage. Die beiden Negativ-Szenarien
  # (fehlender/ungültiger Access-Token) bleiben unverändert, siehe Begründung dort.

  Grundlage:
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "pepProxyUrl" auf "${zeta.server.pep.url}"
    Und TGR setze lokale Variable "pepTestPath" auf "/v3/api-docs"
    Und TGR setze lokale Variable "pepFindPath" auf "/v3/api-docs"
    Und TGR lösche alle default headers

  @TCID:ZETA_REST_PEP_TRANSFER_WITH_VALID_ACCESS_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @rest_pep_transfer
  Szenario: PEP akzeptiert gültigen Token und leitet Anfrage an Backend weiter
    # Umgestellt auf einen echten Client-Aufruf statt einer handgebauten Anfrage an den PEP:
    # Der VSDM-Client führt bei einem VSD-Read intern DCR + Token-Exchange + PoPP-Token-Abruf
    # (über den PoPP-Client) durch und sendet eine vollständig authentifizierte REST-Anfrage
    # über den VSDM-ZETA-PEP an das reale VSDM-Backend.
    Und TGR setze lokale Variable "vsdmClientUrl" auf "http://127.0.0.1:${ports.vsdmClientPort}"
    Und das Kartenterminal "ws://card-terminal-client" ist am VSDM-Client konfiguriert
    Und die Karte "test/vsdm-testsuite/src/test/resources/private/smcb/smcbCardImage.xml" ist in Slot 1 des Kartenterminals geladen
    Und die Karte "test/vsdm-testsuite/src/test/resources/data/cards/egkCardImage.xml" ist in Slot 2 des Kartenterminals geladen
    Und TGR setze den default header "If-None-Match" auf den Wert "0"

    Wenn TGR sende eine leere GET Anfrage an "${vsdmClientUrl}/client/vsdm/vsd?terminalId=0&egkSlotId=2&smcBSlotId=1&isFhirXml=false&profileVersion=1.0"
    Und TGR warte auf eine Nachricht, in der Knoten "$.responseCode" mit "(200|304)" übereinstimmt

    # Beweis der Token-Akzeptanz: Mit gültigem Token leitet der echte PEP die Anfrage an das
    # reale VSDM-Backend weiter (≠ 401). Ohne Token liefert der PEP 401 (s. Negativtests unten).
    Dann TGR finde die letzte Anfrage mit dem Pfad "/ASL/.*"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "(?!401)\d{3}"

  # HINWEIS zu den beiden folgenden Negativ-Szenarien: Sie wurden bewusst NICHT auf den
  # VSDM-/PoPP-Client umgestellt. Beide Clients verwalten ihren ZETA-Access-Token vollständig
  # intern über ihr eingebettetes ZETA-SDK (DCR + Token-Exchange) und bieten keinen der laut
  # Aufgabenstellung erlaubten Endpunkte, um bewusst OHNE bzw. mit einem manipulierten
  # Access-Token zu senden. Die Client→PEP-Anfrage läuft außerdem als Docker-zu-Docker-Traffic
  # über den docker-tiger-proxy; die in TigerProxyManipulationsSteps verfügbaren Manipulationen
  # wirken laut deren Klassendokumentation ausdrücklich NUR auf Anfragen, die physisch durch den
  # lokalen Tiger-Proxy (Port ${ports.localTigerProxyProxyPort}) laufen, nicht auf
  # Docker-zu-Docker-Verkehr zwischen Client und PEP. Eine Umstellung würde daher eine
  # künstliche, nicht produktionsnahe Lösung erzwingen; beide Szenarien bleiben unverändert, um
  # die Negativ-Absicherung des ZETA-PEP direkt zu testen.
  @TCID:ZETA_REST_PEP_TRANSFER_WITH_MISSING_ACCESS_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @rest_pep_transfer
  Szenario: REST-Anfrage ohne Authorization wird vom PEP abgelehnt
    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    # PEP muss mit 401 Unauthorized antworten
    Dann TGR finde die letzte Anfrage
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "401"


  @TCID:ZETA_REST_PEP_TRANSFER_WITH_INVALID_ACCESS_TOKEN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @rest_pep_transfer
  Szenario: REST-Anfrage mit ungültigem Token wird vom PEP abgelehnt
    Gegeben sei ein ungültiger ZETA-PEP AccessToken wird erzeugt
    Und die ZeTA-PDP-Registrierung des popp-client ist vollständig zurückgesetzt

    Wenn TGR sende eine leere GET Anfrage an "${pepProxyUrl}${pepTestPath}"

    # Der echte PEP liefert bei ungültigem Token 401; wir akzeptieren 4xx/5xx.
    Dann TGR finde die letzte Anfrage
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "4..|5.."

