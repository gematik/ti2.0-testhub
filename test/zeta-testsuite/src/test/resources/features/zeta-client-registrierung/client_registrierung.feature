#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
#
# EINSCHRÄNKUNG: ein ZETA-SDK-Client registriert sich per DCR
# nur einmal und cached danach seinen client_id. Die Service-Discovery- und DCR-Anfragen werden
# deshalb nur bei der ERSTEN ZETA-Interaktion des laufenden popp-client-Containers innerhalb eines
# Testlaufs erzeugt (exakt wie bei der "Erstregistrierung" in zeta-gitti.feature). Damit dieses
# Szenario unabhängig von der Suite-Reihenfolge zuverlässig läuft, setzt es die ZETA-PDP-
# Registrierung und den popp-client-Container vor der eigentlichen Anfrage explizit zurück
# (siehe PoppClientResetSteps).
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
@PRODUKT:ZETA
@local

Funktionalität: Client-Registrierung und ZETA Service Discovery

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers

  @TCID:ZETA_CLIENT_REGISTRATION_MAIN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @client_registrierung @service_discovery @dcr
  Szenario: Erstregistrierung des popp-client löst Service Discovery und Dynamic Client Registration am echten ZETA-Guard aus
    # ACHTUNG: Dieser Test registriert tatsächlich einen Client auf der Zielumgebung (siehe
    # Einschränkung oben).

    Gegeben sei die ZeTA-PDP-Registrierung des popp-client ist vollständig zurückgesetzt

    Wenn TGR sende eine POST Anfrage an "${popp.client.tokenUrl}" mit ContentType "application/json" und folgenden mehrzeiligen Daten:
      """
      {"communicationType": "contact-virtual"}
      """

    # ===========================================================================
    # Service Discovery: Protected Resource Metadata (RFC 9728) vom echten PEP
    # ===========================================================================
    Dann TGR finde die letzte Anfrage mit dem Pfad "/.well-known/oauth-protected-resource"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.resource"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.authorization_servers"

    # ===========================================================================
    # Service Discovery: OAuth Authorization Server Metadata vom echten PDP
    # Das popp-client-Image 2.8.0 nutzt die RFC-8414-Metadata (/.well-known/
    # oauth-authorization-server), NICHT den klassischen OIDC-Discovery-Pfad.
    # ===========================================================================
    Und TGR finde die letzte Anfrage mit dem Pfad ".*/.well-known/oauth-authorization-server$"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.issuer"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.authorization_endpoint"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.token_endpoint"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks_uri"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_endpoint"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.grant_types_supported"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.response_types_supported"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.token_endpoint_auth_methods_supported"
    Und TGR speichere Wert des Knotens "$.body.jwks_uri" der aktuellen Antwort in der Variable "jwksUri"
    Und TGR prüfe Variable "jwksUri" stimmt überein mit "^https?://[^/]+/.*$"

    # ===========================================================================
    # Dynamic Client Registration (RFC 7591, POST /register) gegen echten PDP
    # ===========================================================================
    Und TGR finde die letzte Anfrage mit dem Pfad "/auth/realms/zeta-guard/clients-registrations/openid-connect"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "201"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id_issued_at"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.token_endpoint_auth_method" überein mit "private_key_jwt"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.redirect_uris"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_client_uri"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_access_token"

    # --- Anfrage-Validierung ---
    Und TGR prüfe aktueller Request stimmt im Knoten "$.method" überein mit "POST"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.header.content-type" überein mit "application/json"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.client_name"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.token_endpoint_auth_method"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.jwks"

    # ===========================================================================
    # Token Exchange (RFC 8693): SMC-B Token-Exchange, der auf die DCR folgt
    # ===========================================================================
    Und TGR finde die letzte Anfrage mit dem Pfad ".*/protocol/openid-connect/token$"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.access_token"
