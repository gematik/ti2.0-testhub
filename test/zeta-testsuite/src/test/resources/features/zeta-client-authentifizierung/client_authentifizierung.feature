#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
@PRODUKT:ZETA

Funktionalität: Client-Authentifizierung, Token-Exchange und DPoP/PoP (Integrationstests)

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers

  # ===========================================================================
  # Token Exchange: DCR + PoPP-Token + client_assertion gegen echten Keycloak
  # ===========================================================================
  @TCID:ZETA_REGISTRATION_AND_AUTH_WITH_POPP_DCR_CLIENT_ASSERTION
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @client_registrierung
  @local
  Szenario: Popp-Client löst Keycloak-Token-Exchange mit SMC-B client_assertion und PoPP-Token aus (Gutfall)
    # Voraussetzungen: echte Keycloak-Instanz mit ZeTA-Extension und popp-client-Docker-Service
    # verfügbar (siehe infra/docker/backend/compose-popp-services.yaml).
    #
    # Ablauf (alles innerhalb des popp-client, ausgelöst durch den einzigen bereits vorhandenen
    # Endpunkt POST /token):
    # 1. (falls noch nicht geschehen) Client-Registrierung per DCR
    # 2. client_assertion JWT mit SMC-B-Schlüssel signieren (BP256R1) und DPoP-Proof erstellen
    # 3. Token-Exchange-Request an Keycloak senden
    # 4. Erst danach erzeugt der popp-client das eigentliche PoPP-Token über die PoPP-Server-WebSocket
    #
    # Der popp-client führt DCR + Token-Exchange nur bei seiner ALLERERSTEN /token-Anfrage pro
    # Container-Lebenszeit durch; danach verwendet er den intern zwischengespeicherten Token, ohne
    # erneut Keycloak zu kontaktieren. Damit dieses Szenario zuverlässig einen ECHTEN
    # Token-Exchange-Request beobachten kann, wird die PDP-Registrierung des popp-client vorher
    # vollständig zurückgesetzt (siehe PoppClientResetSteps).
    Gegeben sei die ZeTA-PDP-Registrierung des popp-client ist vollständig zurückgesetzt

    Wenn TGR sende eine POST Anfrage an "${popp.client.tokenUrl}" mit ContentType "application/json" und folgenden mehrzeiligen Daten:
      """
      {"communicationType": "contact-virtual"}
      """

    # Token-Request muss erfolgreich sein (2xx)
    Dann TGR finde die letzte Anfrage mit dem Pfad ".*/protocol/openid-connect/token$"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"
