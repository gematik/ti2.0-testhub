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
    Und TGR setze lokale Variable "proxy" auf "http://${zeta_proxy_url}"
    Und TGR setze lokale Variable "pepBaseUrl" auf "${zeta.paths.vsdm.pep.baseUrl}"
    Und TGR setze lokale Variable "pdpBaseUrl" auf "${zeta.paths.vsdm.pdp.baseUrl}"
    Und TGR setze lokale Variable "tigerProxyUrl" auf "http://localhost:${tiger.tigerProxy.proxyPort}"

  # ===========================================================================
  # Token Exchange: DCR + PoPP-Token + client_assertion gegen echten Keycloak
  # ===========================================================================
  @TCID:ZETA_REGISTRATION_AND_AUTH_WITH_POPP_DCR_CLIENT_ASSERTION
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @client_registrierung
  Szenario: Keycloak-Token-Exchange mit SMC-B client_assertion und PoPP-Token (Gutfall)
    # Voraussetzungen: echte Keycloak-Instanz mit ZeTA-Extension, PoPP-Token-Generator und
    # private Schlüssel (SMC-B) verfügbar; secrets nicht im Repo ablegen.
    #
    # Ablauf:
    # 1. (optional) Client-Registrierung per DCR oder Wiederverwendung eines vorhandenen client_id
    # 2. PoPP-Token via popp-token-generator erzeugen
    # 3. client_assertion JWT mit SMC-B-Schlüssel signieren (BP256R1)
    # 4. DPoP-Proof JWT erstellen
    # 5. Token-Exchange-Request an Keycloak senden

    # Token-Request über Tiger-Proxy an ZETA-PDP (Keycloak) senden
    Wenn sende Token-Exchange-Request für Client "zeta-client" an "${zeta.server.pdp.tokenUrl}" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Request muss erfolgreich sein (2xx)
    Dann TGR finde die letzte Anfrage mit dem Pfad "/auth/realms/zeta-guard/protocol/openid-connect/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"
