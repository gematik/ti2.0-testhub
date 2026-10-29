#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@ip_bindung'
# Details siehe README.md im gleichen Verzeichnis (Szenario 7).
@PRODUKT:ZT_Cluster
@PRODUKT:ZETA

Funktionalität: IP-Bindung des Access-Tokens an die Quell-IP des Token-Requests

  # Der Positiv-Fall (Zugriff von derselben IP, die den Token ausgestellt hat) ist bereits durch
  # ZETA_REST_PEP_TRANSFER_WITH_VALID_ACCESS_TOKEN (zeta-datatransfer-rest/rest_data_transfer_via_pep.feature)
  # abgedeckt und wird hier nicht dupliziert.

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers
    Und TGR setze den default header "If-None-Match" auf den Wert "0"
    Und TGR setze lokale Variable "vsdmClientUrl" auf "http://127.0.0.1:${ports.vsdmClientPort}"
    Und das Kartenterminal "ws://card-terminal-client" ist am VSDM-Client konfiguriert
    Und die Karte "test/vsdm-testsuite/src/test/resources/private/smcb/smcbCardImage.xml" ist in Slot 1 des Kartenterminals geladen
    Und die Karte "test/vsdm-testsuite/src/test/resources/data/cards/egkCardImage.xml" ist in Slot 2 des Kartenterminals geladen
    # VSD-Daten-Cache vor jedem Szenario leeren, damit immer ein neuer PEP-Aufruf stattfindet.
    Und TGR sende eine leere DELETE Anfrage an "http://127.0.0.1:${ports.vsdmClientPort}/client/test/vsdmData"

  @TCID:ZETA_TOKEN_IP_BINDING_DIFFERENT_SOURCE_IP_DENIED
  @STATUS:Spezifiziert
  @MODUS:Automatisch
  @TESTSTUFE:1
  @PRIO:2
  @ip_bindung
  @local
  Szenario: Access-Token wird bei abweichender Quell-IP abgelehnt (A_28803)
    # Stufe 1, stationärer Client der Rolle LEI (A_28827/A_28803).
    # Erster VSD-Read von IP A: stellt einen gültigen, an IP A gebundenen Access-Token sicher.
    Wenn TGR sende eine leere GET Anfrage an "${vsdmClientUrl}/client/vsdm/vsd?terminalId=0&egkSlotId=2&smcBSlotId=1&isFhirXml=false&profileVersion=1.1"
    Und TGR warte auf eine Nachricht, in der Knoten "$.responseCode" mit "(200|304)" übereinstimmt
    Dann TGR finde die letzte Anfrage mit dem Pfad "/ASL/.*"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "(?!401)(?!403)\d{3}"

    # Nur den VSD-Daten-Cache leeren (nicht den Access-Token), damit der nächste Read den PEP erneut aufruft.
    Wenn TGR sende eine leere DELETE Anfrage an "${vsdmClientUrl}/client/test/vsdmData"

    # Hostname nur im vsdm-client-Container auf die andere Docker-IP von "docker-tiger-proxy-b" umleiten,
    # Access-Token bleibt unverändert (gleicher Token, andere Quell-IP).
    Und Der Hostname "vsdm-zeta-ingress" wird im Container "vsdm-client" auf die IP des Containers "docker-tiger-proxy-b" umgeleitet

    # 500 zusätzlich zu 401/403 akzeptiert, da der ZETA-SDK nach erschöpften Step-Up-Retries
    # "StepUpFailed" wirft, was der VSDM-Client als generischen 500 durchreicht (siehe README).
    Wenn TGR sende eine leere GET Anfrage an "${vsdmClientUrl}/client/vsdm/vsd?terminalId=0&egkSlotId=2&smcBSlotId=1&isFhirXml=false&profileVersion=1.1"
    Und TGR warte auf eine Nachricht, in der Knoten "$.responseCode" mit "40(1|3)|500" übereinstimmt

    # PEP-Antwort über docker-tiger-proxy-b ist im lokalen RBel-Mitschnitt nicht sichtbar,
    # deshalb wird die durchgereichte Antwort des "/client/vsdm/vsd"-Requests geprüft.
    Dann TGR finde die letzte Anfrage mit dem Pfad ".*/client/vsdm/vsd.*"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "40(1|3)|500"

    # Umleitung wieder entfernen, damit nachfolgende Szenarien unverändert laufen.
    Und die Hostnamen-Umleitung für "vsdm-zeta-ingress" im Container "vsdm-client" wird aufgehoben
