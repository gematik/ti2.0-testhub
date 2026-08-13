#language:de
# Befehl zum Ausführen der Tests (vom Root-Verzeichnis ti2.0-testhub/):
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@asl' -Dzeta.env=local
#
# VORAUSSETZUNG: TestHub im Profil "full":
#   docker compose -f doc/docker/compose-local.yaml --profile full up -d
@PRODUKT:ZT_Cluster
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
Funktionalität: ASL-Handshake am ZETA-PEP – Positiv- und Negativfall

  Grundlage:
    Wenn TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "vsdmClientUrl" auf "http://127.0.0.1:${ports.vsdmClientPort}"
    Und TGR lösche alle default headers
    Gegeben sei das Kartenterminal "ws://card-terminal-client" ist am VSDM-Client konfiguriert
    Und die Karte "test/vsdm-testsuite/src/test/resources/data/cards/smcbCardImage.xml" ist in Slot 1 des Kartenterminals geladen
    Und die Karte "test/vsdm-testsuite/src/test/resources/data/cards/egkCardImage.xml" ist in Slot 2 des Kartenterminals geladen

  @TCID:ZETA_CONNECTIVITY_ASL_HANDSHAKE_VSDM
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @asl_handshake_success @pep @asl @positiv
  Szenariogrundriss: ASL-Handshake gelingt, wenn der VSDM-Client einen VSD-Read durchführt
    # E2E über den VSDM-Client. Das Well-Known des
    # VSDM-PEP fordert "zeta_asl_use": "required", das SDK initiiert daraufhin den
    # ASL-Handshake (POST /ASL → POST /ASL/<sessionId>).
    #
    # VSDM-Client triggern – SDK führt SMC-B-Auth, ASL-Handshake und VSD-Read aus.
    Und TGR setze den default header "If-None-Match" auf den Wert "0"
    Wenn TGR sende eine leere GET Anfrage an "${vsdmClientUrl}/client/vsdm/vsd?terminalId=0&egkSlotId=<Egk-Slot>&smcBSlotId=<Smcb-Slot>&isFhirXml=false"
    Und TGR warte auf eine Nachricht, in der Knoten "$.responseCode" mit "200" übereinstimmt
    Und TGR finde die letzte Anfrage mit dem Pfad "/ASL/.*"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    Beispiele:
      | Smcb-Slot | Egk-Slot |
      | 1         | 2        |
