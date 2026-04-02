#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
#
# Hinweis: Diese Tests verwenden den Tiger-Proxy für HTTP-Traffic-Mitschnitt.
@PRODUKT:ZETA

Funktionalität: Client-Registrierung über ZETA-PDP

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "proxy" auf "http://${zeta_proxy_url}"
    Und TGR setze lokale Variable "pepBaseUrl" auf "http://127.0.0.1:${ports.poppPepPort}"
    Und TGR setze lokale Variable "pdpBaseUrl" auf "http://127.0.0.1:${ports.poppPdpPort}"

  # ===========================================================================
  # Initiale Client-Registrierung: Service Discovery (well-known)
  # ===========================================================================

  @client_registrierung @service_discovery
  Szenario: Service Discovery - mock well-known AS-Metadata abrufen
    # Testet den ersten Schritt der initialen Client-Registrierung:
    # Der ZETA Client ruft den well-known Endpunkt des PEP-Mocks auf
    # und erhält aktuell ein Authorization-Server-Metadata-ähnliches Dokument.
    # Der Mock verwendet dafür den Pfad `/.well-known/oauth-protected-resource`,
    # liefert aber kein striktes RFC-9728 Protected Resource Metadata Dokument.

    Wenn TGR sende eine leere GET Anfrage an "${pepBaseUrl}/.well-known/oauth-protected-resource"

    # Response muss 200 OK sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/.well-known/oauth-protected-resource"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    # Response-Body muss ein gültiges Mock-well-known JSON Dokument sein
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body"
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "OPR_WELL_KNOWN"
    Und validiere "${OPR_WELL_KNOWN}" gegen Schema "schemas/v_1_0/opr-well-known.yaml"

    # Felder der aktuellen Mock-Response prüfen
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.issuer"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.token_endpoint"

  # ===========================================================================
  # Initiale Client-Registrierung: Dynamic Client Registration (DCR)
  # ===========================================================================

  @client_registrierung @dcr
  Szenario: Dynamic Client Registration - Client erfolgreich registrieren (POST /register)
    # Testet die Dynamic Client Registration (RFC 7591):
    # Der ZETA Client sendet einen DCR-Request an den PDP Authorization Server.
    # Der Server erzeugt eine client_id und legt einen Client Placeholder an.

    # DCR-Request an den PDP-Mock senden
    Wenn TGR sende eine POST Anfrage an "${pdpBaseUrl}/register" mit ContentType "application/json" und folgenden mehrzeiligen Daten:
      """
      !{file('src/test/resources/mocks/register-request.json')}
      """

    # Response muss 201 Created sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/register"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "201"

    # Pflichtfelder der DCR-Response prüfen
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id_issued_at"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.token_endpoint_auth_method" überein mit "private_key_jwt"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.redirect_uris"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_client_uri"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_access_token"

    # DCR-Response gegen Schema validieren
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "DCR_RESPONSE"
    Und validiere "${DCR_RESPONSE}" gegen Schema "schemas/v_1_0/dcr-response.yaml"

    # Request-Validierung: korrekte Methode und Content-Type
    Und TGR prüfe aktueller Request stimmt im Knoten "$.method" überein mit "POST"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.header.Content-Type" überein mit "application/json"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_name" überein mit "sdk-client"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.token_endpoint_auth_method"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.jwks"


  # ===========================================================================
  # Token Exchange
  # ===========================================================================

  @client_registrierung @token_exchange
  Szenario: ZETA-PDP erlaubt Zugriff bei gültiger Policy (Gutfall)
    # Dieser Test prüft den erfolgreichen PDP-Flow:
    # 1. Test sendet Token-Request an ZETA-PDP über Tiger-Proxy
    # 2. PDP stellt Access-Token aus
    #
    # HINWEIS: Der PDP-Mock (zeta-pdp-server-mockservice) verwendet aktuell
    # keine echte OPA-Policy. Die authz.rego gibt immer allow=true zurück.

    # Token-Request über Tiger-Proxy an ZETA-PDP senden
    Wenn sende Token-Exchange-Request für Client "zeta-client" an "${pdpBaseUrl}/token" über Tiger-Proxy "http://localhost:${tiger.tigerProxy.proxyPort}"

    # Token-Request muss erfolgreich sein (2xx)
    Dann TGR finde die letzte Anfrage mit dem Pfad "/token"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "2.."
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.access_token" überein mit ".*"

  # ===========================================================================
  # Fehlerfälle (aktuell mit @ignore markiert, da Tiger Manipulation Steps noch nicht implementiert sind)
  # ===========================================================================

  @Ignore @client_registrierung @no_proxy
  Szenariogrundriss: Client-Registrierung wird wegen Client Policy abgelehnt und begründet
    # STATUS: Nicht implementiert
    # HINWEIS: Dieser Test benötigt eine echte OPA-Policy in authz.rego,
    # die Input-Werte validiert. Aktuell gibt der Mock immer allow=true zurück.

    Gegeben sei TGR sende eine leere GET Anfrage an "${zeta.paths.client.reset}"

    # OPA Request manipulieren - ungültige Werte setzen
    # Der Guard sendet diese Daten an OPA, wir manipulieren den Request um Policy-Ablehnungen zu testen
    Und TGR setze lokale Variable "opaCondition" auf "isRequest && request.path =~ '.*${zeta.paths.opa.decisionPath}'"
    Dann Setze im TigerProxy für die Nachricht "${opaCondition}" die Manipulation auf Feld "<OpaInputField>" und Wert "<NeuerWert>" und 1 Ausführungen

    Wenn TGR sende eine leere GET Anfrage an "${zeta.paths.client.vsdRequest}"

    # OPA Decision prüfen - sollte allow=false liefern
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.opa.decisionPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.result.allow" überein mit "false"

    # Registrierungs-/Token-Request muss mit 403 abgelehnt werden
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.guard.tokenEndpointPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "403"
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "body"
    Und validiere "${body}" gegen Schema "schemas/v_1_0/zeta-error.yaml"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.error_description" überein mit ".*<ErwarteterHinweis>.*"

    Beispiele: Ungültige Policy-Werte
      | OpaInputField                                         | NeuerWert                    | ErwarteterHinweis                                               |
      | $.body.input.user_info.professionOID                  | 1.2.276.0.76.4.999           | ${testdata.policy_rejection.invalid_profession_oid_error_hint}  |
      | $.body.input.client_assertion.posture.product_id      | unknown_product              | ${testdata.policy_rejection.invalid_product_id_error_hint}      |
      | $.body.input.client_assertion.posture.product_version | 99.99.99                     | ${testdata.policy_rejection.invalid_product_version_error_hint} |
      | $.body.input.authorization_request.scopes.0           | invalid_scope_xyz            | ${testdata.policy_rejection.invalid_scope_error_hint}           |
      | $.body.input.authorization_request.aud.0              | https://evil.example.com/api | ${testdata.policy_rejection.invalid_audience_error_hint}        |
