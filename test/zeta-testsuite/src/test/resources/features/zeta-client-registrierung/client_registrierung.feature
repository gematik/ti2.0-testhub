#language:de
# Befehl zum Ausführen der Tests:
# ./mvnw -pl test/zeta-testsuite clean verify -Dskip.inttests=false -Dcucumber.filter.tags='@client_registrierung and not @Ignore'
@PRODUKT:ZETA

Funktionalität: Client-Registrierung und ZETA Service Discovery (DB-Integrationsprüfung)

  Grundlage:
    Gegeben sei TGR lösche aufgezeichnete Nachrichten
    Und TGR setze lokale Variable "proxy" auf "http://${zeta_proxy_url}"
    Und TGR setze lokale Variable "pepBaseUrl" auf "${zeta.paths.vsdm.pep.baseUrl}"
    Und TGR setze lokale Variable "pdpBaseUrl" auf "${zeta.paths.vsdm.pdp.baseUrl}"
    Und TGR setze lokale Variable "tigerProxyUrl" auf "http://localhost:${tiger.tigerProxy.proxyPort}"

  # ===========================================================================
  # Service Discovery: Protected Resource Metadata (RFC 9728) via echten PEP
  # ===========================================================================

  @client_registrierung @service_discovery
  Szenario: Service Discovery - Protected Resource Metadata vom echten PEP abrufen (RFC 9728)
    # Testet den ersten Schritt der initialen Client-Registrierung:
    # Der ZETA Client ruft den well-known Endpunkt des echten PEP (ngx_pep) auf
    # und erhält ein RFC-9728 Protected Resource Metadata Dokument.

    Wenn TGR sende eine leere GET Anfrage an "${pepBaseUrl}/.well-known/oauth-protected-resource"

    # Response muss 200 OK sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "/.well-known/oauth-protected-resource"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"

    # Response-Body muss ein gültiges RFC-9728 Dokument sein
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body"

    # Pflichtfelder des RFC-9728 Protected Resource Metadata Dokuments prüfen
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.resource"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.authorization_servers"

    # Schema-Validierung gegen RFC-9728
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "OPR_WELL_KNOWN"
    Und validiere JSON "${OPR_WELL_KNOWN}" gegen Schema "schemas/v_1_0/opr-well-known-rfc9728.yaml"

  # ===========================================================================
  # Service Discovery: OAuth Authorization Server Metadata via PEP → Keycloak
  # ===========================================================================

  @client_registrierung @service_discovery
  Szenario: well-known OAuth Authorization Server liefert valides Dokument (200)
    # PEP proxiert diesen Endpunkt zu Keycloak: /auth/realms/zeta-guard/.well-known/zeta-guard-well-known
    Wenn TGR sende eine leere GET Anfrage an "${zeta.paths.vsdm.ingress.baseUrl}${zeta.paths.vsdm.wellKnownOAuthServerPath}"
    Dann TGR finde die letzte Anfrage mit dem Pfad ".*${zeta.paths.vsdm.wellKnownOAuthServerPath}$"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "AS_WELL_KNOWN"
    # Schema-Validierung: issuer, token_endpoint, jwks_uri, ... müssen vorhanden sein
    Und validiere "${AS_WELL_KNOWN}" gegen Schema "schemas/v_1_0/as-well-known.yaml"
    # jwks_uri muss ein gültiger HTTP(S)-URI sein
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks_uri"
    Und TGR speichere Wert des Knotens "$.body.jwks_uri" der aktuellen Antwort in der Variable "jwksUri"
    Und TGR prüfe Variable "jwksUri" stimmt überein mit "^https?://[^/]+/.*$"

  # ===========================================================================
  # Dynamic Client Registration gegen echten Keycloak (DB-Integrationsprüfung)
  # ===========================================================================

  @client_registrierung @dcr
  Szenario: Client erfolgreich am ZETA Guard registrieren (201 Created)
    # Testet die Dynamic Client Registration (RFC 7591) via Keycloak + PostgreSQL.
    # Keycloak schreibt die Clientdaten in PostgreSQL → impliziter DB-Integrationstest.
    # POST-Anfrage an den Keycloak-Registrierungsendpunkt
    Wenn TGR sende eine POST Anfrage an "${pdpBaseUrl}${zeta.paths.vsdm.registerEndpointPath}" mit ContentType "application/json" und folgenden mehrzeiligen Daten:
      """
      !{file('test/zeta-testsuite/src/test/resources/mocks/register-request.json')}
      """
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.vsdm.registerEndpointPath}"
    # ZETA Guard – HTTP Statuscodes – Clientregistrierung – 201 Created
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "201"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id_issued_at"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.body.token_endpoint_auth_method" überein mit "private_key_jwt"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.jwks"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.redirect_uris"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_client_uri"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.registration_access_token"

    # DCR-Response gegen Schema validieren
    # Hinweis: "validiere JSON" statt "validiere" verwenden, da registration_access_token ein JWT ist
    # und TigerResolvedString-Serialisierung fehlschlägt (RbelSerializationException: verifiedUsing-node fehlt)
    Und TGR speichere Wert des Knotens "$.body" der aktuellen Antwort in der Variable "DCR_RESPONSE"
    Und validiere JSON "${DCR_RESPONSE}" gegen Schema "schemas/v_1_0/dcr-response.yaml"

    # --- Anfrage-Validierung ---
    Und TGR prüfe aktueller Request stimmt im Knoten "$.method" überein mit "POST"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.header.Content-Type" überein mit "application/json"
    Und TGR prüfe aktueller Request stimmt im Knoten "$.body.client_name" überein mit "sdk-client"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.token_endpoint_auth_method"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.grant_types"
    Und TGR prüfe aktueller Request enthält Knoten "$.body.jwks"

  # ===========================================================================
  # Token Exchange: DCR + PoPP-Token + client_assertion gegen echten Keycloak
  # ===========================================================================

  @Ignore @client_registrierung @token_exchange
  Szenario: Keycloak-Token-Exchange mit SMC-B client_assertion und PoPP-Token (Gutfall)
    # STATUS: @Ignore – Keycloak ZeTA Extension (keycloak-zeta:0.4.1) liefert HTTP 500
    # bei DCR mit brainpoolP256r1-JWKS (x5c). Das SMC-B-Zertifikat kann aktuell nicht
    # über die DCR-Schnittstelle registriert werden.
    #
    # Sobald die Extension brainpoolP256r1 in JWKS/x5c unterstützt, kann dieser Test
    # aktiviert werden.
    #
    # Ablauf (sobald freigeschaltet):
    # 1. Client-Registrierung (DCR) mit SMC-B JWKS (brainpoolP256r1, x5c)
    # 2. PoPP-Token via popp-token-generator erzeugen
    # 3. client_assertion JWT mit SMC-B-Schlüssel signieren (BP256R1)
    # 4. DPoP-Proof JWT erstellen
    # 5. Token-Exchange-Request an Keycloak senden

    # Schritt 1: Client registrieren – JWKS enthält SMC-B-Zertifikat (x5c)
    Wenn registriere Client "zeta-e2e-test" mit SMC-B-Schlüssel an "${pdpBaseUrl}${zeta.paths.vsdm.registerEndpointPath}" über Tiger-Proxy "${tigerProxyUrl}"

    # DCR muss 201 Created liefern
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.vsdm.registerEndpointPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "201"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.client_id"

    # Schritt 2+3+4: PoPP-Token erzeugen, client_assertion signieren, Token Exchange senden
    Wenn sende Keycloak-Token-Exchange-Request an "${pdpBaseUrl}${zeta.paths.vsdm.tokenEndpointPath}" mit Audience "${zeta.server.pdp.issuer}" und PoPP-Token von "${zeta.server.poppTokenGenerator.url}" über Tiger-Proxy "${tigerProxyUrl}"


    # Token-Exchange-Response muss 200 OK mit access_token sein
    Dann TGR finde die letzte Anfrage mit dem Pfad "${zeta.paths.vsdm.tokenEndpointPath}"
    Und TGR prüfe aktuelle Antwort stimmt im Knoten "$.responseCode" überein mit "200"
    Und TGR prüfe aktuelle Antwort enthält Knoten "$.body.access_token"

  # ===========================================================================
  # Fehlerfälle – @Ignore: zwei Voraussetzungen fehlen noch (s. Kommentar)
  # ===========================================================================

  @Ignore @client_registrierung @policy_ablehnungen
  Szenariogrundriss: Client-Registrierung wird wegen Client Policy abgelehnt und begründet
    # STATUS: @Ignore – zwei Voraussetzungen fehlen noch:
    #
    # 1. NETZWERK-TOPOLOGIE: OPA-Requests laufen intern zwischen Docker-Containern
    #    (vsdm-zeta-pdp → opa) und NICHT durch den lokalen TigerProxy (Port 6950).
    #    TigerProxyManipulationSteps registriert Regeln am lokalen Admin-API (Port 6900) –
    #    diese greifen nicht für Docker-interne Requests.
    #    Lösung: Modifikation auf docker-tiger-proxy (Port 6300) registrieren
    #    UND OPA-Traffic über docker-tiger-proxy routen.
    #
    # 2. OPA-POLICY: authz.rego gibt aktuell immer allow=true zurück.
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

