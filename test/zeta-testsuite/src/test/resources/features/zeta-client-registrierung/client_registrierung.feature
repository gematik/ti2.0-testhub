#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
@PRODUKT:ZT_Cluster
@PRODUKT:PoPP_Service
@PRODUKT:Anb_PoPP_Service
@PRODUKT:VSDM_2_FD
@PRODUKT:Anb_FD_VSDM
@PRODUKT:ZETA

Funktionalität: Client-Registrierung und ZETA Service Discovery gegen echte RU-DEV-Komponenten

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR lösche alle default headers
    Und TGR setze lokale Variable "pepBaseUrl" auf "${zeta.server.pep.url}"
    Und TGR setze lokale Variable "pdpBaseUrl" auf "${zeta.server.pdp.issuer}"
    Und TGR setze lokale Variable "tigerProxyUrl" auf "http://localhost:${tiger.tigerProxy.proxyPort}"

  # ===========================================================================
  # Service Discovery: Protected Resource Metadata (RFC 9728) via echten PEP
  # ===========================================================================

  @TCID:ZETA_DISCOVERY_PROTECTED_RESSOURCE_METADATA
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @client_registrierung @service_discovery
  Szenario: Service Discovery - Protected Resource Metadata vom echten PEP abrufen (RFC 9728)
    # Testet den ersten Schritt der initialen Client-Registrierung:
    # Der ZETA Client ruft den well-known Endpunkt des echten PEP auf
    # und erhält ein RFC-9728 Protected Resource Metadata Dokument.

    Wenn TGR sende eine leere GET Anfrage an "${pepBaseUrl}/.well-known/oauth-protected-resource"

    # Response muss 200 OK sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/.well-known/oauth-protected-resource"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    # Response-Body muss ein gültiges RFC-9728 Dokument sein
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body"

    # Felder der aktuellen RFC 9728 Response prüfen
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.resource"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.authorization_servers"

  # ===========================================================================
  # Service Discovery: OAuth Authorization Server Metadata vom echten PDP
  # ===========================================================================

  @TCID:ZETA_DISCOVERY_OAUTH_SERVER_METADATA
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @client_registrierung @service_discovery
  Szenario: well-known OAuth Authorization Server liefert valides Dokument (200)
    # Ruft das OpenID/OAuth Metadata-Dokument direkt vom echten PDP/Authorization Server ab.

    Wenn TGR sende eine leere GET Anfrage an "${pdpBaseUrl}/.well-known/openid-configuration"

    Dann TGR finde die letzte Anfrage mit dem Pfad ".*/.well-known/openid-configuration$"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    # Basisfelder des echten OIDC Discovery Dokuments prüfen
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.issuer"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.authorization_endpoint"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.token_endpoint"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks_uri"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_endpoint"

    # Unterstützte Fähigkeiten prüfen
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.grant_types_supported"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.response_types_supported"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.token_endpoint_auth_methods_supported"

    # jwks_uri muss ein gültiger HTTP(S)-URI sein
    Und TGR speichere Wert des Knotens "$.body.jwks_uri" der aktuellen Antwort in der Variable "jwksUri"
    Und TGR prüfe Variable "jwksUri" stimmt überein mit "^https?://[^/]+/.*$"

  # ===========================================================================
  # Dynamic Client Registration gegen echten PDP / Authorization Server
  # ===========================================================================

  @TCID:ZETA_CLIENT_REGISTRATION_MAIN
  @STATUS:Implementiert
  @MODUS:Automatisch
  @TESTSTUFE:3
  @PRIO:1
  @client_registrierung @dcr
  Szenario: Dynamic Client Registration - Client erfolgreich registrieren (POST /register)
    # Testet die Dynamic Client Registration (RFC 7591) gegen den echten PDP.
    # ACHTUNG: Dieser Test registriert tatsächlich einen Client auf der Zielumgebung.

    Wenn TGR sende eine POST Anfrage an "${pdpBaseUrl}/clients-registrations/openid-connect" mit ContentType "application/json" und folgenden mehrzeiligen Daten:
      """
      !{file('test/zeta-testsuite/src/test/resources/mocks/register-request.json')}
      """

    # Response muss 201 Created sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/auth/realms/zeta-guard/clients-registrations/openid-connect"
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
    Und TGR prüfe aktueller Request stimmt im Knoten "$.header.Content-Type" überein mit "application/json"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_name" überein mit "sdk-client"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.token_endpoint_auth_method"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.jwks"

  # ===========================================================================
  # Token Exchange: DCR + PoPP-Token + client_assertion gegen PDP
  # ===========================================================================

  @client_registrierung @token_exchange
  Szenario: Keycloak-Token-Exchange mit SMC-B client_assertion und PoPP-Token (Gutfall)
    # Token-Request über Tiger-Proxy an ZETA-PDP senden.

    Wenn sende Token-Exchange-Request für Client "zeta-client" an "${zeta.server.pdp.tokenUrl}" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Request muss erfolgreich sein (2xx)
    Dann TGR finde die letzte Anfrage mit dem Pfad "/auth/realms/zeta-guard/protocol/openid-connect/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"
